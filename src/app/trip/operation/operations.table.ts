import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {TableElement, ValidatorService} from '@e-is/ngx-material-table';
import {OperationValidatorService} from '../services/validator/operation.validator';
import {AlertController, ModalController, Platform} from '@ionic/angular';
import {ActivatedRoute, Router} from '@angular/router';
import {Location} from '@angular/common';
import {OperationService, OperationServiceWatchOptions} from '../services/operation.service';
import {TranslateService} from '@ngx-translate/core';
import {
  AccountService,
  AppTable,
  EntitiesTableDataSource,
  isNotNil,
  LatLongPattern,
  LocalSettings,
  LocalSettingsService,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  toBoolean,
} from '@sumaris-net/ngx-components';
import { OperationsMapModalOptions, OperationsMap } from './map/operations.map';
import {environment} from '@environments/environment';
import {Operation} from '../services/model/trip.model';
import {OperationFilter} from '@app/trip/services/filter/operation.filter';
import { BehaviorSubject, from, merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';


@Component({
  selector: 'app-operations-table',
  templateUrl: 'operations.table.html',
  styleUrls: ['operations.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: OperationValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationsTable extends AppTable<Operation, OperationFilter> implements OnInit, OnDestroy {

  displayAttributes: {
    [key: string]: string[]
  };
  highlightedRow: TableElement<Operation>;

  @Input() latLongPattern: LatLongPattern;
  @Input() tripId: number;
  @Input() showMap: boolean;
  @Input() programLabel: string;
  @Input() showToolbar = true;
  @Input() showPaginator = true;
  @Input() useSticky = true;

  @Input() set showQualityColumn(value: boolean) {
    this.setShowColumn('quality', value);
  }

  get showQualityColumn(): boolean {
    return this.getShowColumn('quality');
  }

  get sortActive(): string {
    const sortActive = super.sortActive;
    // Local sort
    if (this.tripId < 0) {
      switch (sortActive) {
        case 'physicalGear':
          return 'physicalGear.gear.' + this.displayAttributes.gear[0];
        case 'targetSpecies':
          return 'metier.taxonGroup.' + this.displayAttributes.taxonGroup[0];
        default:
          return sortActive;
      }
    }
    // Remote sort
    else {
      switch (sortActive) {
        case 'targetSpecies':
          return 'metier';
        default:
          return sortActive;
      }
    }
  }

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected validatorService: ValidatorService,
    protected dataService: OperationService,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected accountService: AccountService,
    protected cd: ChangeDetectorRef,
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat(
          platform.is('mobile') ?
            ['quality',
              'physicalGear',
              'targetSpecies',
              'startDateTime',
              'endDateTime'] :
            ['quality',
              'physicalGear',
              'targetSpecies',
              'startDateTime',
              'startPosition',
              'endDateTime',
              'endPosition',
              'comments'])
        .concat(RESERVED_END_COLUMNS),
      new EntitiesTableDataSource<Operation, OperationFilter, number, OperationServiceWatchOptions>(Operation,
        dataService,
        null,
        // DataSource options
        {
          prependNewElements: false,
          suppressErrors: environment.production,
          dataServiceOptions: {
            readOnly: true,
            withBatchTree: false,
            withSamples: false,
            withTotal: true
          }
        })
    );
    this.i18nColumnPrefix = 'TRIP.OPERATION.LIST.';

    this.readOnly = false; // Allow deletion
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.saveBeforeSort = false;
    this.saveBeforeFilter = false;
    this.saveBeforeDelete = false;
    this.autoLoad = false; // waiting parent to be loaded

    this.defaultPageSize = -1; // Do not use paginator
    this.defaultSortBy = this.mobile ? 'startDateTime' : 'endDateTime';
    this.defaultSortDirection = this.mobile ? 'desc' : 'asc';

    // Listen settings changed
    this.registerSubscription(
      merge(
        from(this.settings.ready()),
        this.settings.onChange
      )
      .subscribe(_ => this.configureFromSettings())
    );
  }

  ngOnInit() {
    super.ngOnInit();

    // Default values
    this.showMap = toBoolean(this.showMap, false);

    // Apply trip id, if already set
    if (isNotNil(this.tripId)) {
      this.setTripId(this.tripId);
    }
  }

  setTripId(id: number, opts?: { emitEvent?: boolean; }) {
    if (this.tripId !== id) {
      this.tripId = id;
      const filter = this.filter || new OperationFilter();
      filter.tripId = id;
      this.dataSource.serviceOptions = this.dataSource.serviceOptions || {};
      this.dataSource.serviceOptions.tripId = id;
      this.setFilter(filter, {emitEvent: (!opts || opts.emitEvent !== false) && isNotNil(id)});
    } else if ((!opts || opts.emitEvent !== false) && isNotNil(this.filter.tripId)) {
      this.onRefresh.emit();
    }
  }

  async openMapModal(event?: UIEvent) {

    const res = await this.dataService.loadAllByTrip({
      tripId: this.tripId
    }, {fetchPolicy: 'cache-first', fullLoad: false, withTotal: true});

    const modal = await this.modalCtrl.create({
      component: OperationsMap,
      componentProps: <OperationsMapModalOptions>{
        data: res.data,
        latLongPattern: this.latLongPattern,
        programLabel: this.programLabel
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data instanceof Operation) {
      // Select the row
      const row = (await this.dataSource.getRows()).find(row => row.currentData.id === data.id);
      if (row) {
        this.clickRow(null, row);
      }
    }

  }

  clickRow(event: MouseEvent | undefined, row: TableElement<Operation>): boolean {
    this.highlightedRow = row;

    return super.clickRow(event, row);
  }

  async getUsedPhysicalGearIds(): Promise<number[]> {
    return (await this.dataSource.getRows())
      .map(ope => ope.currentData.physicalGear)
      .filter(isNotNil)
      .map(gear => gear.id)
      .reduce((res, id) => res.includes(id) ? res : res.concat(id), []);
  }

  /* -- protected methods -- */

  protected configureFromSettings(settings?: LocalSettings) {
    console.debug('[operation-table] Configure from local settings (latLong format, display attributes)...')
    settings = settings || this.settings.settings;

    if (settings.accountInheritance) {
      const account = this.accountService.account;
      this.latLongPattern = account && account.settings && account.settings.latLongFormat || this.settings.latLongFormat;
    }
    else {
      this.latLongPattern = this.settings.latLongFormat;
    }

    this.displayAttributes = {
      gear: this.settings.getFieldDisplayAttributes('gear'),
      taxonGroup: this.settings.getFieldDisplayAttributes('taxonGroup'),
    };

    this.markForCheck();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

