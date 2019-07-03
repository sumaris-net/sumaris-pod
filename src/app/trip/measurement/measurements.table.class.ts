import {Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {BehaviorSubject} from 'rxjs';
import {filter, first} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {
  AppTable,
  AppTableDataSource,
  environment,
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


export interface AppMeasurementsTableOptions<T extends IEntityWithMeasurement<T>> extends AppTableDataSourceOptions<T> {
  reservedStartColumns?: string[];
  reservedEndColumns?: string[];
  mapPmfms?: (pmfms: PmfmStrategy[]) => PmfmStrategy[] | Promise<PmfmStrategy[]>;
}

export abstract class AppMeasurementsTable<T extends IEntityWithMeasurement<T>, F> extends AppTable<T, F>
  implements OnInit, OnDestroy, ValidatorService {

  private _program: string;

  protected _acquisitionLevel: string;

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
  set acquisitionLevel(value: string) {
    this._acquisitionLevel = value;
    if (this.measurementsDataService) {
      this.measurementsDataService.acquisitionLevel = value;
    }
  }

  get acquisitionLevel(): string {
    return this._acquisitionLevel;
  }

  @Input()
  set showCommentsColumn(value: boolean) {
    this.setShowColumn('comments', value);
  }

  get showCommentsColumn(): boolean {
    return this.getShowColumn('comments');
  }

  get pmfms(): BehaviorSubject<PmfmStrategy[]> {
    return this.measurementsDataService.$pmfms;
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
    options = options || {prependNewElements: false, suppressErrors: true};
    if (!options.onRowCreated) {
      options.onRowCreated = (row) => this.onRowCreated(row);
    }

    const encapsulatedValidator = this.validatorService ? this : null;
    this.setDatasource(new AppTableDataSource(dataType, this.measurementsDataService, encapsulatedValidator, options));

    // For DEV only
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this.pmfms
        .pipe(filter(isNotNil))
        .subscribe(pmfms => {
          this.measurementValuesFormGroupConfig = this.measurementsValidatorService.getFormGroupConfig(pmfms);
          this.updateColumns(pmfms);

          // Load the table
          this.onRefresh.emit();
        }));

  }

  getRowValidator(): FormGroup {
    const formGroup = this.validatorService.getRowValidator();
    if (this.measurementValuesFormGroupConfig) {
      if (formGroup.contains('measurementValues')) {
        formGroup.removeControl('measurementValues');
      }
      formGroup.addControl('measurementValues', this.formBuilder.group(this.measurementValuesFormGroupConfig));
    } else {
      console.warn('NO measurementValuesFormGroupConfig !');
    }
    return formGroup;
  }

  setFilter(filter: F, opts?: { emitEvent: boolean }) {
    opts = opts || {emitEvent: !this.loading};
    super.setFilter(filter, opts);
  }

  // addRow(): boolean {
  //   if (this.debug) console.debug("[meas-table] Calling addRow()");
  //
  //   // Create new row
  //   const result = super.addRow();
  //   if (!result) return result;
  //
  //   const row = this.dataSource.getRow(-1);
  //   const obj = new this.dataType() as T;
  //   obj.fromObject(row.currentData);
  //   this.data.push(obj);
  //   this.editedRow = row;
  //   return true;
  // }

  public trackByFn(index: number, row: TableElement<T>) {
    return this.hasRankOrder ? row.currentData.rankOrder : row.currentData.id;
  }

  public updateColumns(pmfms?: PmfmStrategy[]) {

    pmfms = pmfms || this.pmfms.getValue();
    if (!pmfms) return;

    const pmfmColumnNames = pmfms.map(p => p.pmfmId.toString());

    this.displayedColumns = RESERVED_START_COLUMNS
      .concat(this.options && this.options.reservedStartColumns || [])
      .concat(pmfmColumnNames)
      .concat(this.options && this.options.reservedEndColumns || [])
      .concat(RESERVED_END_COLUMNS)
      // Remove columns to hide
      .filter(column => !this.excludesColumns.includes(column));

    if (!this.loading) this.markForCheck();
  }

  public setShowColumn(columnName: string, show: boolean) {
    super.setShowColumn(columnName, show);

    if (!this.loading) this.updateColumns();
  }

  public async onPmfmsLoaded() {
    // Wait pmfms load, and controls load
    if (isNil(this.pmfms.getValue())) {
      await this.pmfms
        .pipe(
          filter(isNotNil),
          first()
        ).toPromise();
    }
  }

  /* -- protected abstract methods -- */

  // Can be override by subclass
  protected async onNewEntity(data: T): Promise<void> {
    if (this.hasRankOrder) {
      data.rankOrder = (await this.getMaxRankOrder()) + 1;
    }
  }

  /* -- protected methods -- */

  protected async getMaxRankOrder(): Promise<number> {
    const rows = await this.dataSource.getRows();
    return rows.reduce((res, row) => Math.max(res, row.currentData.rankOrder || 0), 0);
  }

  protected async onRowCreated(row: TableElement<T>): Promise<void> {
    const data = row.currentData; // if validator enable, this will call a getter function

    await this.onNewEntity(data);

    // Normalize measurement values
    this.normalizeRowMeasurementValues(data, row);

    // Set row data
    row.currentData = data; // if validator enable, this will call a setter function

    this.markForCheck();
  }

  protected getI18nColumnName(columnName: string): string {

    // Try to resolve PMFM column, using the cached pmfm list
    if (PMFM_ID_REGEXP.test(columnName)) {
      const pmfmId = parseInt(columnName);
      const pmfm = (this.pmfms.getValue() || []).find(p => p.pmfmId === pmfmId);
      if (pmfm) return pmfm.name;
    }

    return super.getI18nColumnName(columnName);
  }

  protected normalizeRowMeasurementValues(data: T, row: TableElement<T>) {
    if (!data) return; // skip

    const pmfms = this.measurementsDataService.$pmfms.getValue() || [];

    // Adapt entity measurement values to reactive form
    MeasurementValuesUtils.normalizeFormEntity(data, pmfms, row.validator);
  }

  getPmfmColumnHeader = getPmfmName;

}

