import {Directive, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {BehaviorSubject, Observable} from 'rxjs';
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {
  AppTable,
  EntitiesTableDataSource,
  environment,
  isNil,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  IEntitiesService, Entity
} from "../../core/core.module";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from '@ngx-translate/core';
import {MeasurementsValidatorService} from "../services/validator/trip.validators";
import {isNotNil} from "../../shared/functions";
import {IEntityWithMeasurement, MeasurementValuesUtils} from "../services/model/measurement.model";
import {MeasurementsDataService} from "./measurements.service";
import {AppTableDataSourceOptions} from "../../core/table/entities-table-datasource.class";
import {filterNotNil, firstNotNilPromise} from "../../shared/observables";
import {AcquisitionLevelType} from "../../referential/services/model/model.enum";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {Alerts} from "../../shared/alerts";
import {getPmfmName, PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {PMFM_ID_REGEXP} from "../../referential/services/model/pmfm.model";
import {ProgramService} from "../../referential/services/program.service";


export interface AppMeasurementsTableOptions<T extends IEntityWithMeasurement<T>> extends AppTableDataSourceOptions<T> {
  reservedStartColumns?: string[];
  reservedEndColumns?: string[];
  mapPmfms?: (pmfms: PmfmStrategy[]) => PmfmStrategy[] | Promise<PmfmStrategy[]>;
}

@Directive()
export abstract class AppMeasurementsTable<T extends IEntityWithMeasurement<T>, F> extends AppTable<T, F>
  implements OnInit, OnDestroy, ValidatorService {

  private _program: string;
  private _autoLoadAfterPmfm = true;

  protected _acquisitionLevel: AcquisitionLevelType;

  protected measurementsDataService: MeasurementsDataService<T, F>;
  protected measurementsValidatorService: MeasurementsValidatorService;

  protected programService: ProgramService;
  protected translate: TranslateService;
  protected formBuilder: FormBuilder;

  measurementValuesFormGroupConfig: { [key: string]: any };
  readonly hasRankOrder: boolean;

  /**
   * Allow to override the rankOrder. See physical-gear, on ADAP program
   */
  @Input() canEditRankOrder = false;

  @Input()
  set program(value: string) {
    this._program = value;
    if (this.measurementsDataService) {
      this.measurementsDataService.program = value;
    }
  }

  get program(): string {
    return this._program;
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
  set showCommentsColumn(value: boolean) {
    this.setShowColumn('comments', value);
  }

  get showCommentsColumn(): boolean {
    return this.getShowColumn('comments');
  }

  get $pmfms(): BehaviorSubject<PmfmStrategy[]> {
    return this.measurementsDataService.$pmfms;
  }

  @Input() set pmfms(pmfms: Observable<PmfmStrategy[]> | PmfmStrategy[]) {
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

  protected constructor(
    protected injector: Injector,
    protected dataType: new() => T,
    dataService?: IEntitiesService<T, F>,
    protected validatorService?: ValidatorService,
    protected options?: AppMeasurementsTableOptions<T>
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

    this.measurementsValidatorService = injector.get(MeasurementsValidatorService);
    this.programService = injector.get(ProgramService);
    this.translate = injector.get(TranslateService);
    this.formBuilder = injector.get(FormBuilder);
    this.defaultPageSize = -1; // Do not use paginator
    this.hasRankOrder = Object.getOwnPropertyNames(new dataType()).findIndex(key => key === 'rankOrder') !== -1;
    this.setLoading(false, {emitEvent: false});

    this.measurementsDataService = new MeasurementsDataService<T, F>(this.injector, this.dataType, dataService, options && {
      mapPmfms: options.mapPmfms
    });
    this.measurementsDataService.program = this._program;
    this.measurementsDataService.acquisitionLevel = this._acquisitionLevel;

    // Default options
    this.options = this.options || {prependNewElements: false, suppressErrors: environment.production};
    if (!this.options.onRowCreated) {
      this.options.onRowCreated = (row) => this.onRowCreated(row);
    }

    const encapsulatedValidator = this.validatorService ? this : null;
    this.setDatasource(new EntitiesTableDataSource(this.dataType, this.measurementsDataService, encapsulatedValidator, options));

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
  }

  ngOnDestroy() {
    super.ngOnDestroy();

    this.measurementsDataService.close();
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

  public trackByFn(index: number, row: TableElement<T>) {
    return this.hasRankOrder ? row.currentData.rankOrder : row.currentData.id;
  }

  protected generateTableId(): string {
    // Append the program, if any
    return super.generateTableId() + (isNotNil(this._program) ? ('-' + this._program) : '');
  }

  protected getDisplayColumns(): string[] {

    const pmfms = this.$pmfms.getValue();
    if (!pmfms) return this.columns;

    const userColumns = this.getUserColumns();

    const pmfmColumnNames = pmfms
      //.filter(p => p.isMandatory || !userColumns || userColumns.includes(p.pmfmId.toString()))
      .filter(p => !p.hidden)
      .map(p => p.pmfmId.toString());

    const startColumns = (this.options && this.options.reservedStartColumns || []).filter(c => !userColumns || userColumns.includes(c));
    const endColumns = (this.options && this.options.reservedEndColumns || []).filter(c => !userColumns || userColumns.includes(c));

    return RESERVED_START_COLUMNS
      .concat(startColumns)
      .concat(pmfmColumnNames)
      .concat(endColumns)
      .concat(RESERVED_END_COLUMNS)
      // Remove columns to hide
      .filter(column => !this.excludesColumns.includes(column));

    //console.debug("[measurement-table] Updating columns: ", this.displayedColumns)
    //if (!this.loading) this.markForCheck();
  }


  setShowColumn(columnName: string, show: boolean) {
    super.setShowColumn(columnName, show, {emitEvent: false});

    if (!this.loading) {
      this.updateColumns();
    }
  }

  public async onReady() {
    // Wait pmfms load, and controls load
    if (isNil(this.$pmfms.getValue())) {
      await firstNotNilPromise(this.$pmfms);
    }
  }

  /**
   * Use in ngFor, for trackBy
   * @param index
   * @param pmfm
   */
  trackPmfm(index: number, pmfm: PmfmStrategy) {
    return pmfm && pmfm.pmfmId || null;
  }

  /* -- protected methods -- */

  protected updateColumns() {
    if (!this.$pmfms.getValue()) return; // skip
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

  protected async onRowCreated(row: TableElement<T>): Promise<void> {
    const data = row.currentData; // if validator enable, this will call a getter function

    await this.onNewEntity(data);

    // Normalize measurement values
    this.normalizeEntityToRow(data, row);

    // Set row data
    row.currentData = data; // if validator enable, this will call a setter function

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
    else {
      this.editedRow = row;
    }

    this.markAsDirty();

    return row;
  }

  protected getI18nColumnName(columnName: string): string {

    // Try to resolve PMFM column, using the cached pmfm list
    if (PMFM_ID_REGEXP.test(columnName)) {
      const pmfmId = parseInt(columnName);
      const pmfm = (this.$pmfms.getValue() || []).find(p => p.pmfmId === pmfmId);
      if (pmfm) return pmfm.name;
    }

    return super.getI18nColumnName(columnName);
  }

  protected normalizeEntityToRow(data: T, row: TableElement<T>) {
    if (!data) return; // skip

    const pmfms = this.measurementsDataService.$pmfms.getValue() || [];

    // Adapt entity measurement values to reactive form
    MeasurementValuesUtils.normalizeEntityToForm(data, pmfms, row.validator);
  }

  getPmfmColumnHeader = getPmfmName;

}

