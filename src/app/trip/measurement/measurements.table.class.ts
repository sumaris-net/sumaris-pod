import {Directive, Injector, Input, OnDestroy, OnInit, Optional} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {TableElement, ValidatorService} from '@e-is/ngx-material-table';
import {ModalController, Platform} from '@ionic/angular';
import {ActivatedRoute, Router} from '@angular/router';
import {Location} from '@angular/common';
import {FormBuilder, FormGroup} from '@angular/forms';
import {TranslateService} from '@ngx-translate/core';
import {
  Alerts, AppFormUtils,
  AppTable,
  AppTableDataSourceOptions, AppTableUtils,
  EntitiesTableDataSource,
  Entity,
  filterNotNil,
  firstNotNilPromise,
  IEntitiesService,
  isNil,
  isNotNil,
  LocalSettingsService,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
} from '@sumaris-net/ngx-components';
import {IEntityWithMeasurement, MeasurementValuesUtils} from '../services/model/measurement.model';
import {MeasurementsDataService} from './measurements.service';
import {AcquisitionLevelType} from '../../referential/services/model/model.enum';
import {IPmfm, PMFM_ID_REGEXP, PmfmUtils} from '../../referential/services/model/pmfm.model';
import {MeasurementsValidatorService} from '../services/validator/measurement.validator';
import {ProgramRefService} from '../../referential/services/program-ref.service';


export class AppMeasurementsTableOptions<T extends IEntityWithMeasurement<T>> extends AppTableDataSourceOptions<T>{
  reservedStartColumns?: string[];
  reservedEndColumns?: string[];
  mapPmfms?: (pmfms: IPmfm[]) => IPmfm[] | Promise<IPmfm[]>;
  requiredStrategy?: boolean;
}

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class AppMeasurementsTable<T extends IEntityWithMeasurement<T>, F> extends AppTable<T, F>
  implements OnInit, OnDestroy, ValidatorService {

  private _programLabel: string;
  private _autoLoadAfterPmfm = true;

  protected _acquisitionLevel: AcquisitionLevelType;
  protected _strategyLabel: string;

  protected measurementsDataService: MeasurementsDataService<T, F>;
  protected measurementsValidatorService: MeasurementsValidatorService;

  protected programRefService: ProgramRefService;
  protected translate: TranslateService;
  protected formBuilder: FormBuilder;
  protected readonly options: AppMeasurementsTableOptions<T>;

  measurementValuesFormGroupConfig: { [key: string]: any };
  readonly hasRankOrder: boolean;

  @Input() set requiredStrategy(value: boolean) {
    this.options.requiredStrategy = value;
    if (this.measurementsDataService) {
      this.measurementsDataService.requiredStrategy = value;
    }
  }

  get requiredStrategy(): boolean {
    return this.options.requiredStrategy;
  }

  /**
   * Allow to override the rankOrder. See physical-gear, on ADAP program
   */
  @Input() canEditRankOrder = false;

  @Input()
  set programLabel(value: string) {
    this._programLabel = value;
    if (this.measurementsDataService) {
      this.measurementsDataService.programLabel = value;
    }
  }

  get programLabel(): string {
    return this._programLabel;
  }

  @Input()
  set acquisitionLevel(value: AcquisitionLevelType) {
    this._acquisitionLevel = value;
    if (this.measurementsDataService) {
      this.measurementsDataService.acquisitionLevel = value;
    }
  }

  get acquisitionLevel(): AcquisitionLevelType {
    return this._acquisitionLevel;
  }

  @Input()
  set strategyLabel(value: string) {
    this._strategyLabel = value;
    if (this.measurementsDataService) {
      this.measurementsDataService.strategyLabel = value;
    }
  }

  get strategyLabel(): string {
    return this._strategyLabel;
  }

  @Input()
  set showCommentsColumn(value: boolean) {
    this.setShowColumn('comments', value);
  }

  get showCommentsColumn(): boolean {
    return this.getShowColumn('comments');
  }

  get $pmfms(): BehaviorSubject<IPmfm[]> {
    return this.measurementsDataService.$pmfms;
  }

  @Input() set pmfms(pmfms: Observable<IPmfm[]> | IPmfm[]) {
    this.markAsLoading();
    this.measurementsDataService.pmfms = pmfms;
  }

  @Input() set dataService(value: IEntitiesService<T, F>) {
    this.measurementsDataService.delegate = value;
    if (!this.loading) {
      this.onRefresh.emit("new dataService");
    }
  }

  get dataService(): IEntitiesService<T, F> {
    return this.measurementsDataService.delegate;
  }

  get loading(): boolean {
    return this.measurementsDataService.loadingPmfms && isNotNil(this.$pmfms.value);
  }

  protected constructor(
    protected injector: Injector,
    protected dataType: new() => T,
    dataService?: IEntitiesService<T, F>,
    protected validatorService?: ValidatorService,
    @Optional() options?: AppMeasurementsTableOptions<T>
  ) {
    super(injector.get(ActivatedRoute),
      injector.get(Router),
      injector.get(Platform),
      injector.get(Location),
      injector.get(ModalController),
      injector.get(LocalSettingsService),
      // Columns:
      RESERVED_START_COLUMNS
        .concat(options && options.reservedStartColumns || [])
        .concat(options && options.reservedEndColumns || [])
        .concat(RESERVED_END_COLUMNS),
      null,
      null,
      injector
    );
    // Default options
    this.options = {
      prependNewElements: false,
      suppressErrors: true,
      requiredStrategy: false,
      debug: false,
      ...options
    };

    this.measurementsValidatorService = injector.get(MeasurementsValidatorService);
    this.programRefService = injector.get(ProgramRefService);
    this.translate = injector.get(TranslateService);
    this.formBuilder = injector.get(FormBuilder);
    this.defaultPageSize = -1; // Do not use paginator
    this.hasRankOrder = Object.getOwnPropertyNames(new dataType()).findIndex(key => key === 'rankOrder') !== -1;
    this.markAsLoaded({emitEvent: false});

    this.measurementsDataService = new MeasurementsDataService<T, F>(this.injector, this.dataType, dataService, {
      mapPmfms: options.mapPmfms || undefined,
      requiredStrategy: this.options.requiredStrategy,
      debug: options.debug || false
    });
    this.measurementsDataService.programLabel = this._programLabel;
    this.measurementsDataService.acquisitionLevel = this._acquisitionLevel;
    this.measurementsDataService.strategyLabel = this._strategyLabel;

    this.setValidatorService(this.validatorService);

    // For DEV only
    //this.debug = !environment.production;
  }

  ngOnInit() {
    // Remember the value of autoLoad, but force to false, to make sure pmfm will be loaded before
    this._autoLoadAfterPmfm = this.autoLoad;
    this.autoLoad = false;

    super.ngOnInit();

    this.registerSubscription(
      filterNotNil(this.$pmfms)
        .subscribe(pmfms => {
          // DEBUG
          console.debug("[measurement-table] Received PMFMs to applied: ", pmfms);

          this.measurementValuesFormGroupConfig = this.measurementsValidatorService.getFormGroupConfig(null, {pmfms});

          // Update the settings id, as program could have changed
          this.settingsId = this.generateTableId();

          // Add pmfm columns
          this.updateColumns();

          // Load the table, if already loaded or if autoLoad was set to true
          if (this._autoLoadAfterPmfm || this.dataSource.loaded/*already load*/) {
            this.onRefresh.emit();
          }
        }));

    // Make sure to copy acquisition level to the data service
    if (this._acquisitionLevel && !this.measurementsDataService.acquisitionLevel) {
      this.measurementsDataService.acquisitionLevel = this._acquisitionLevel;
    }
    // Make sure to copy strategyLabel to the data service
    if (this._strategyLabel && !this.measurementsDataService.strategyLabel) {
      this.measurementsDataService.strategyLabel = this._strategyLabel;
    }
  }

  ngOnDestroy() {
    super.ngOnDestroy();

    this.measurementsDataService.ngOnDestroy();
    this.measurementsDataService = null;
  }

  getRowValidator(): FormGroup {
    const formGroup = this.validatorService.getRowValidator();
    if (this.measurementValuesFormGroupConfig) {
      if (formGroup.contains('measurementValues')) {
        formGroup.removeControl('measurementValues');
      }
      formGroup.addControl('measurementValues', this.formBuilder.group(this.measurementValuesFormGroupConfig));
    }
    return formGroup;
  }

  setFilter(filterData: F, opts?: { emitEvent: boolean }) {
    opts = opts || {emitEvent: !this.loading};
    super.setFilter(filterData, opts);
  }

  trackByFn(index: number, row: TableElement<T>) {
    return this.hasRankOrder ? row.currentData.rankOrder : row.currentData.id;
  }

  /**
   * Allow to change the validator service (will recreate the datasource)
   * @param validatorService
   * @protected
   */
  setValidatorService(validatorService?: ValidatorService) {
    if (this.validatorService === validatorService && this._dataSource) return; // Skip if same

    // If already exists: destroy previous database
    if (this._dataSource) {
      this._dataSource.ngOnDestroy();
      this._dataSource = null;
    }

    console.debug('[landings-table] Settings validator service to: ', validatorService);
    this.validatorService = validatorService;

    // Create the new datasource, BUT redirect to this
    const encapsulatedValidator = validatorService ? this : null;
    this.setDatasource(new EntitiesTableDataSource(this.dataType, this.measurementsDataService, encapsulatedValidator, {
      ...this.options,
      // IMPORTANT: Always use this custom onRowCreated, that will call options.onRowCreated if need
      onRowCreated: (row) => this.onRowCreated(row)
    }));
  }

  protected generateTableId(): string {
    // Append the program, if any
    return super.generateTableId() + (isNotNil(this._programLabel) ? ('-' + this._programLabel) : '');
  }

  protected getDisplayColumns(): string[] {

    const pmfms = this.$pmfms.value;
    if (!pmfms) return this.columns;

    const userColumns = this.getUserColumns();

    const pmfmColumnNames = pmfms
      //.filter(p => p.isMandatory || !userColumns || userColumns.includes(p.pmfmId.toString()))
      .filter(p => !p.hidden)
      .map(p => p.id.toString());

    const startColumns = (this.options && this.options.reservedStartColumns || []).filter(c => !userColumns || userColumns.includes(c));
    const endColumns = (this.options && this.options.reservedEndColumns || []).filter(c => !userColumns || userColumns.includes(c));

    return RESERVED_START_COLUMNS
      .concat(startColumns)
      .concat(pmfmColumnNames)
      .concat(endColumns)
      .concat(RESERVED_END_COLUMNS)
      // Remove columns to hide
      .filter(column => !this.excludesColumns.includes(column));

    // DEBUG
    console.debug("[measurement-table] Updating columns: ", this.displayedColumns)
    //if (!this.loading) this.markForCheck();
  }


  setShowColumn(columnName: string, show: boolean) {
    super.setShowColumn(columnName, show, {emitEvent: false});

    if (!this.loading) {
      this.updateColumns();
    }
  }

  async waitIdle(): Promise<any> {
    if (isNotNil(this.$pmfms.value)) return AppTableUtils.waitIdle(this);

    // Wait pmfms load, and controls load
    await firstNotNilPromise(this.$pmfms);

    return AppTableUtils.waitIdle(this);
  }

  async ready() {
    if (isNotNil(this.$pmfms.value)) return;

    // Wait pmfms load, and controls load
    await firstNotNilPromise(this.$pmfms);
  }

  /**
   * Use in ngFor, for trackBy
   * @param index
   * @param pmfm
   */
  trackPmfm(index: number, pmfm: IPmfm) {
    return pmfm && pmfm.id || null;
  }

  /* -- protected methods -- */

  protected updateColumns() {
    if (!this.$pmfms.value) return; // skip
    super.updateColumns();
  }

  // Can be override by subclass
  protected async onNewEntity(data: T): Promise<void> {
    if (this.hasRankOrder && isNil(data.rankOrder)) {
      data.rankOrder = (await this.getMaxRankOrder()) + 1;
    }
  }

  protected async getMaxRankOrder(): Promise<number> {
    const rows = await this.dataSource.getRows();
    return rows.reduce((res, row) => Math.max(res, row.currentData.rankOrder || 0), 0);
  }

  protected async existsRankOrder(rankOrder: number): Promise<boolean> {
    const rows = await this.dataSource.getRows();
    return rows.findIndex(row => row.currentData.rankOrder === rankOrder) !== -1;
  }


  /**
   * Convert (or clone) a row currentData, into <T> instance (that extends Entity)
   * @param row
   * @param clone
   */
  toEntity(row: TableElement<T>, clone?: boolean): T {
    // If no validator, use currentData
    const currentData = row.currentData;

    // Already an entity (e.g. when no validator used): use it
    if (currentData instanceof Entity) {
      return (currentData && clone === true ? currentData.clone() : currentData) as T;
    }

    // If JSON object (e.g. when using validator): create a new entity
    else {
      const target = new this.dataType();
      target.fromObject(currentData);
      return target;
    }
  }

  private async onRowCreated(row: TableElement<T>): Promise<void> {
    const data = row.currentData; // if validator enable, this will call a getter function

    await this.onNewEntity(data);

    // Normalize measurement values
    this.normalizeEntityToRow(data, row);

    // Set row data
    row.currentData = data; // if validator enable, this will call a setter function

    // Execute function from constructor's options (is any)
    if (this.options.onRowCreated) {
      const res = this.options.onRowCreated(row);
      if (res instanceof Promise) await res;
    }

    this.markForCheck();
  }

  /**
   * Insert an entity into the table. This can be usefull when entity is created by a modal (e.g. BatchGroupTable).
   *
   * If hasRankOrder=true, then rankOrder is computed only once.
   * Will call method normalizeEntityToRow().
   * The new row will be the edited row.
   *
   * @param data the entity to insert.
   * @param opts
   */
  protected async addEntityToTable(data: T, opts?: { confirmCreate?: boolean; }): Promise<TableElement<T>> {
    if (!data) throw new Error("Missing data to add");
    if (this.debug) console.debug("[measurement-table] Adding new entity", data);

    // Before using the given rankOrder, check if not already exists
    if (this.canEditRankOrder && isNotNil(data.rankOrder)) {
      if (await this.existsRankOrder(data.rankOrder)) {
        const message = this.translate.instant('TRIP.MEASUREMENT.ERROR.DUPLICATE_RANK_ORDER', data);
        await Alerts.showError(message, this.alertCtrl, this.translate);
        throw new Error('DUPLICATE_RANK_ORDER');
      }
    }

    const row = await this.addRowToTable();
    if (!row) throw new Error("Could not add row to table");

    // Override rankOrder (with a computed value)
    if (this.hasRankOrder
      // Do NOT override if can edit it and set
      && (!this.canEditRankOrder || isNil(data.rankOrder))) {
      data.rankOrder = row.currentData.rankOrder;
    }

    await this.onNewEntity(data);

    // Adapt measurement values to row
    this.normalizeEntityToRow(data, row);

    // Affect new row
    if (row.validator) {
      row.validator.patchValue(data);
      row.validator.markAsDirty();
    } else {
      row.currentData = data;
    }

    // Confirm the created row
    if (!opts || opts.confirmCreate !== false) {
      this.confirmEditCreate(null, row);
      this.editedRow = null;
    }
    else {
      this.editedRow = row;
    }

    this.markAsDirty();

    return row;
  }

  /**
   * Update an row, using the given entity. Useful when entity is updated using a modal (e.g. BatchGroupModal)
   *
   * The updated row will be the edited row.
   * Will call method normalizeEntityToRow()
   *
   * @param data the input entity
   * @param row the row to update
   * @param opts
   */
  protected async updateEntityToTable(data: T, row: TableElement<T>, opts?: { confirmCreate?: boolean; }): Promise<TableElement<T>> {
    if (!data || !row) throw new Error("Missing data, or table row to update");
    if (this.debug) console.debug("[measurement-table] Updating entity to an existing row", data);

    // Adapt measurement values to row
    this.normalizeEntityToRow(data, row);

    // Affect new row
    if (row.validator) {
      row.validator.patchValue(data);
      row.validator.markAsDirty();
    } else {
      row.currentData = data;
    }

    // Confirm the created row
    if (!opts || opts.confirmCreate !== false) {
      this.confirmEditCreate(null, row);
      this.editedRow = null;
    }
    else if (this.inlineEdition) {
      this.editedRow = row;
    }

    this.markAsDirty();

    return row;
  }

  duplicateRow(event?: Event, row?: TableElement<T>, opts?: {
    skipProperties?: string[];
  }): Promise<boolean> {
    const skipProperties = opts && opts.skipProperties
      || ['id', 'rankOrder', 'updateDate', 'creationDate', 'label'].concat(this.hasRankOrder ? ['rankOrder'] : []);
    return super.duplicateRow(event, row, {...opts, skipProperties});
  }

  protected getI18nColumnName(columnName: string): string {

    // Try to resolve PMFM column, using the cached pmfm list
    if (PMFM_ID_REGEXP.test(columnName)) {
      const pmfmId = parseInt(columnName);
      const pmfm = (this.$pmfms.value || []).find(p => p.id === pmfmId);
      if (pmfm) return PmfmUtils.getPmfmName(pmfm);
    }

    return super.getI18nColumnName(columnName);
  }

  protected getI18nFieldName(fieldName: string): string {
    if (fieldName.startsWith('measurementValues.')) {
      const pmfmId = parseInt(fieldName.split('.')[1]);
      const pmfm = (this.$pmfms.value || []).find(p => p.id === pmfmId);
      if (pmfm) return PmfmUtils.getPmfmName(pmfm);
    }
    return super.getI18nFieldName(fieldName);
  }


  protected normalizeEntityToRow(data: T, row: TableElement<T>) {
    if (!data) return; // skip

    const pmfms = this.measurementsDataService.$pmfms.getValue() || [];

    // Adapt entity measurement values to reactive form
    MeasurementValuesUtils.normalizeEntityToForm(data, pmfms, row.validator);
  }
}

