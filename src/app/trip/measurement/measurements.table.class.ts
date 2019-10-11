import {Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {BehaviorSubject, Observable} from 'rxjs';
import {TableElement, ValidatorService} from "angular4-material-table";
import {
  AppTable,
  AppTableDataSource, environment,
  isNil,
  LocalSettingsService,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  TableDataService
} from "../../core/core.module";
import {getPmfmName, PmfmStrategy} from "../services/trip.model";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {ProgramService} from "../../referential/referential.module";
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from '@ngx-translate/core';
import {MeasurementsValidatorService} from "../services/trip.validators";
import {isNotNil} from "../../shared/shared.module";
import {IEntityWithMeasurement, MeasurementValuesUtils, PMFM_ID_REGEXP} from "../services/model/measurement.model";
import {MeasurementsDataService} from "./measurements.service";
import {AppTableDataSourceOptions} from "../../core/table/table-datasource.class";
import {filterNotNil, firstNotNilPromise} from "../../shared/observables";
import {AcquisitionLevelType} from "../../referential/services/model";


export interface AppMeasurementsTableOptions<T extends IEntityWithMeasurement<T>> extends AppTableDataSourceOptions<T> {
  reservedStartColumns?: string[];
  reservedEndColumns?: string[];
  mapPmfms?: (pmfms: PmfmStrategy[]) => PmfmStrategy[] | Promise<PmfmStrategy[]>;
}

export abstract class AppMeasurementsTable<T extends IEntityWithMeasurement<T>, F> extends AppTable<T, F>
  implements OnInit, OnDestroy, ValidatorService {

  private _program: string;

  protected _acquisitionLevel: AcquisitionLevelType;

  protected measurementsDataService: MeasurementsDataService<T, F>;
  protected measurementsValidatorService: MeasurementsValidatorService;

  protected programService: ProgramService;
  protected translate: TranslateService;
  protected formBuilder: FormBuilder;

  measurementValuesFormGroupConfig: { [key: string]: any };
  hasRankOrder = false;

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

  protected constructor(
    protected injector: Injector,
    protected dataType: new() => T,
    protected dataService: TableDataService<T, F>,
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
    this.pageSize = 10000; // Do not use paginator
    this.hasRankOrder = Object.getOwnPropertyNames(new dataType()).findIndex(key => key === 'rankOrder') !== -1;
    this.autoLoad = false; // must wait pmfms to be load
    this.loading = false;

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
    this.setDatasource(new AppTableDataSource(this.dataType, this.measurementsDataService, encapsulatedValidator, options));

    // For DEV only
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      filterNotNil(this.$pmfms)
        .subscribe(pmfms => {
          this.measurementValuesFormGroupConfig = this.measurementsValidatorService.getFormGroupConfig(pmfms);


          // Update the settings id, as program could have changed
          this.settingsId = this.generateTableId();

          // Add pmfm columns
          this.updateColumns();

          // Load the table
          this.onRefresh.emit();
        }));

    // Make sure to copy acquisition level to the data service
    if (this._acquisitionLevel && !this.measurementsDataService.acquisitionLevel) {
      this.measurementsDataService.acquisitionLevel = this._acquisitionLevel;
    }
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
      //.filter(p => p.isMandatory || !userColumns || userColumns.includes(p.pmfmId.toString()))
      .map(p => p.pmfmId.toString());

    const startColumns = (this.options && this.options.reservedStartColumns || []).filter(c => !userColumns || userColumns.includes(c));
    const endColumns = (this.options && this.options.reservedEndColumns || []).filter(c => !userColumns || userColumns.includes(c));

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
    super.setShowColumn(columnName, show);

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
    this.displayedColumns = this.getDisplayColumns();
    if (!this.loading) this.markForCheck();
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

  protected async onRowCreated(row: TableElement<T>): Promise<void> {
    const data = row.currentData; // if validator enable, this will call a getter function

    await this.onNewEntity(data);

    // Normalize measurement values
    this.normalizeEntityToRow(data, row);

    // Set row data
    row.currentData = data; // if validator enable, this will call a setter function

    this.markForCheck();
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

