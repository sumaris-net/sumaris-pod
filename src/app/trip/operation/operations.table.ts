import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input, OnDestroy, OnInit} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {OperationValidatorService} from "../services/validator/operation.validator";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {OperationFilter, OperationService, OperationServiceWatchOptions} from "../services/operation.service";
import {TranslateService} from "@ngx-translate/core";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {Operation} from "../services/model/trip.model";
import {LatLongPattern} from "../../shared/material/latlong/latlong.utils";
import {isNotNil, toBoolean} from "../../shared/functions";
import {OperationsMap} from "./map/operations.map";
import {AccountService} from "../../core/services/account.service";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {referentialToString} from "../../core/services/model/referential.model";
import {EnvironmentService} from "../../../environments/environment.class";


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

  @Input() program: string;

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
    @Inject(EnvironmentService) protected environment
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat(
          platform.is('mobile') ?
            ['physicalGear',
              'targetSpecies',
              'startDateTime',
              'endDateTime']  :
          ['physicalGear',
            'targetSpecies',
            'startDateTime',
            'startPosition',
            'endDateTime',
            'endPosition',
            'comments'])
        .concat(RESERVED_END_COLUMNS),
      new EntitiesTableDataSource<Operation, OperationFilter, OperationServiceWatchOptions>(Operation, dataService,
        environment,
        null,
        // DataSource options
        {
          prependNewElements: false,
          suppressErrors: environment.production,
          dataServiceOptions: {
            readOnly: true,
            withBatchTree: false,
            withSamples: false
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

    settings.ready().then(() => {
      if (this.settings.settings.accountInheritance) {
        const account = this.accountService.account;
        this.latLongPattern = account && account.settings && account.settings.latLongFormat || this.settings.latLongFormat;
      }
      else {
        this.latLongPattern = this.settings.latLongFormat;
      }
    });
  }

  ngOnInit() {
    super.ngOnInit();

    this.showMap = toBoolean(this.showMap, false);

    this.displayAttributes = {
      gear: this.settings.getFieldDisplayAttributes('gear'),
      taxonGroup: this.settings.getFieldDisplayAttributes('taxonGroup'),
    };

    this.registerSubscription(
      this.settings.onChange.subscribe((settings) => {
        if (this.loading) return; // skip
        this.latLongPattern = settings.latLongFormat;

        this.displayAttributes = {
          gear: this.settings.getFieldDisplayAttributes('gear'),
          taxonGroup: this.settings.getFieldDisplayAttributes('taxonGroup'),
        };

        this.markForCheck();
      }));

    // Apply trip id, if already set
    if (isNotNil(this.tripId)) {
      this.setTripId(this.tripId);
    }
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();
  }

  setTripId(id: number, opts?: {emitEvent?: boolean; }) {
    if (this.tripId !== id) {
      this.tripId = id;
      const filter = this.filter || {};
      filter.tripId = id;
      this.dataSource.serviceOptions = this.dataSource.serviceOptions || {};
      this.dataSource.serviceOptions.tripId = id;
      this.setFilter(filter, {emitEvent: (!opts || opts.emitEvent !== false) && isNotNil(id)});
    }
    else if ((!opts || opts.emitEvent !== false) && isNotNil(this.filter.tripId)){
      this.onRefresh.emit();
    }
  }



  async openMapModal(event?: UIEvent) {

    const operations = (await this.dataSource.getRows())
      .map(row => row.currentData);

    const modal = await this.modalCtrl.create({
      component: OperationsMap,
      componentProps: {
        operations,
        latLongPattern: this.latLongPattern,
        program: this.program
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && data instanceof Operation) {
      // Select the row
      const row = (await this.dataSource.getRows()).find(row => row.currentData.id === data.id);
      if (row) {
        this.clickRow(null, row);
      }
    }

  }

  clickRow(event: MouseEvent|undefined, row: TableElement<Operation>): boolean {
    this.highlightedRow = row;

    return super.clickRow(event, row);
  }

  async getUsedPhysicalGearIds(): Promise<number[]> {
    return (await this.dataSource.getRows())
      .map(ope => ope.currentData.physicalGear)
      .filter(isNotNil)
      .map(gear => gear.id)
      .reduce( (res, id) => res.includes(id) ? res : res.concat(id), []);
  }

  referentialToString = referentialToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

