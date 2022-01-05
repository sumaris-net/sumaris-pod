import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
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
  changeCaseToUnderscore,
  EntitiesTableDataSource,
  isNotNil,
  LatLongPattern,
  LocalSettings,
  LocalSettingsService,
  ReferentialRef,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  toBoolean,
} from '@sumaris-net/ngx-components';
import {OperationsMap, OperationsMapModalOptions} from './map/operations.map';
import {environment} from '@environments/environment';
import {Operation} from '../services/model/trip.model';
import {OperationFilter} from '@app/trip/services/filter/operation.filter';
import {BehaviorSubject, from, merge} from 'rxjs';
import {AbstractControl, FormBuilder, FormControl, FormGroup} from '@angular/forms';
import {MatExpansionPanel} from '@angular/material/expansion';
import {debounceTime, filter} from 'rxjs/operators';
import {ReferentialRefService} from '@app/referential/services/referential-ref.service';
import {TripFilter} from '@app/trip/services/filter/trip.filter';
import {DataQualityStatusEnum, DataQualityStatusList} from '@app/data/services/model/model.utils';


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
  filterForm: FormGroup;
  filterCriteriaCount = 0;
  $gears = new BehaviorSubject<ReferentialRef[]>(undefined)
  statusList = DataQualityStatusList;
  statusById = DataQualityStatusEnum;

  @Input() latLongPattern: LatLongPattern;
  @Input() tripId: number;
  @Input() showMap: boolean;
  @Input() programLabel: string;
  @Input() showToolbar = true;
  @Input() showPaginator = true;
  @Input() useSticky = true;
  @Input() allowParentOperation = false;
  @Input() showQuality = true;

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
        case 'fishingArea':
          return 'fishingAreas.location.' + this.displayAttributes.fishingArea[0];
        default:
          return sortActive;
      }
    }
    // Remote sort
    else {
      switch (sortActive) {
        case 'targetSpecies':
          return 'metier';
        case 'fishingArea':
          return 'fishingAreas.location.' + this.displayAttributes.fishingArea[0];
        default:
          return sortActive;
      }
    }
  }

  @Input() set showPosition(show: boolean) {
    this.setShowColumn('startPosition', show);
    this.setShowColumn('endPosition', show);
  }

  get showPosition(): boolean {
    return this.getShowColumn('startPosition') &&
      this.getShowColumn('endPosition');
  }

  @Input() set showFishingArea(show: boolean) {
    this.setShowColumn('fishingArea', show);
  }

  get showFishingArea(): boolean {
    return this.getShowColumn('fishingArea');
  }

  get filterDataQualityControl(): FormControl {
    return this.filterForm.controls.dataQualityStatus as FormControl;
  }

  @ViewChild(MatExpansionPanel, {static: true}) filterExpansionPanel: MatExpansionPanel;

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
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef,
    formBuilder: FormBuilder,
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
              'fishingArea',
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

    this.filterForm = formBuilder.group({
      gearIds: [null],
      startDate: null,
      dataQualityStatus: null
    });

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

    // Update filter when changes
    this.registerSubscription(
      this.filterForm.valueChanges
        .pipe(
          debounceTime(250),
          filter(() => this.filterForm.valid)
        )
        // Applying the filter
        .subscribe((json) => {

          const filter = this.asFilter({
            ...this.filter, // Keep previous filter
            ...json
          });
          this.filterCriteriaCount = filter.countNotEmptyCriteria();
          this.setFilter(filter, {emitEvent: true /*always apply*/});
        }));
  }

  setTripId(id: number, opts?: { emitEvent?: boolean; }) {
    const emitEvent = (!opts || opts.emitEvent !== false);
    if (this.tripId !== id) {
      this.tripId = id;
      const filter = this.filter || new OperationFilter();
      filter.tripId = id;
      this.dataSource.serviceOptions = this.dataSource.serviceOptions || {};
      this.dataSource.serviceOptions.tripId = id;
      this.setFilter(filter, {emitEvent: emitEvent && isNotNil(id)});
    }
    // Nothing change, but force to applying filter
    else if (emitEvent && isNotNil(this.filter.tripId)) {
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

  // Changed as public
  getI18nColumnName(columnName: string): string {
    return super.getI18nColumnName(columnName);
  }

  clearFilterValue(key: keyof OperationFilter, event?: UIEvent) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }

    this.filterForm.get(key).reset(null);
  }

  applyFilterAndClosePanel(event?: UIEvent) {
    this.onRefresh.emit(event);
    if (this.filterExpansionPanel) this.filterExpansionPanel.close();
  }

  resetFilter(event?: UIEvent) {
    this.filterForm.reset();
    this.setFilter(null, {emitEvent: true});
    this.filterCriteriaCount = 0;
    this.filterExpansionPanel.close();
  }

  clearFilterStatus(event: UIEvent) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.filterForm.patchValue({statusId: null});
  }

  setError(error: any) {

    const formErrors = error?.details?.errors?.operations;
    if (formErrors) {
      let messages = [];
      Object.keys(formErrors).map(id => {

        const operationErrors = formErrors[id];
        messages.push(Object.keys(operationErrors)
          .map(field => {
            const fieldErrors = operationErrors[field];
            const fieldI18nKey = changeCaseToUnderscore(field).toUpperCase();
            const fieldName = this.translate.instant(fieldI18nKey);
            const errorMsg = Object.keys(fieldErrors).map(errorKey => {
              const key = 'ERROR.FIELD_' + errorKey.toUpperCase();
              return this.translate.instant(key, fieldErrors[key]);
            }).join(', ');
            //TODO : replace id by rank order ?
            return fieldName + ' (' + id + '): ' + errorMsg;
          }).filter(isNotNil));
      });

      if (messages.length) {
        error.details.message = `<ul><li>${messages.join('</li><li>')}</li></ul>`;
      }
      this.errorSubject.next(error.details.message);
    }
  }

  setFilter(filter: OperationFilter, opts?: { emitEvent: boolean }) {

    filter = this.asFilter({
      ...this.filter, // Keep previous filter
      ...filter
    });
    this.filterCriteriaCount = filter.countNotEmptyCriteria();
    this.filterForm.patchValue(filter, {emitEvent: false});
    super.setFilter(filter, opts);
  }

   async loadGears(gearIds: number[]) {
    const { data } = await this.referentialRefService.loadAll(0, 100, null, null,
      {
        entityName: 'Gear',
        includedIds: gearIds,
      },
      {
        withTotal: false
      });

    this.$gears.next(data || []);
  }

  /* -- protected methods -- */

  protected configureFromSettings(settings?: LocalSettings) {
    console.debug('[operation-table] Configure from local settings (latLong format, display attributes)...');
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
      physicalGear: this.settings.getFieldDisplayAttributes('gear', ['rankOrder', 'gear.label', 'gear.name']),
      taxonGroup: this.settings.getFieldDisplayAttributes('taxonGroup'),
      fishingArea: this.settings.getFieldDisplayAttributes('fishingArea', ['label'])
    };

    this.markForCheck();
  }

  protected asFilter(source?: any): OperationFilter {
    source = source || this.filterForm.value;

    if (this._dataSource && this._dataSource.dataService) {
      return this._dataSource.dataService.asFilter(source);
    }

    return OperationFilter.fromObject(source);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

