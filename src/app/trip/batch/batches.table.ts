import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit
} from "@angular/core";
import {BehaviorSubject, Observable} from 'rxjs';
import {debounceTime, filter, first, mergeMap, switchMap, tap} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {
  AccountService,
  AppTable,
  AppTableDataSource,
  EntityUtils, isNil,
  ReferentialRef,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  TableDataService
} from "../../core/core.module";
import {
  Batch,
  getPmfmName,
  MeasurementUtils,
  PmfmStrategy,
  referentialToString,
  TaxonGroupIds
} from "../services/trip.model";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {ProgramService, ReferentialRefService} from "../../referential/referential.module";
import {BatchValidatorService} from "../services/batch.validator";
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from '@ngx-translate/core';
import {MeasurementsValidatorService} from "../services/trip.validators";
import {TaxonomicLevelIds} from "src/app/referential/services/model";
import {isNotNil, LoadResult} from "../../shared/shared.module";

const PMFM_ID_REGEXP = /\d+/;
const BATCH_RESERVED_START_COLUMNS: string[] = ['taxonGroup', 'taxonName'];
const BATCH_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'table-batches',
  templateUrl: 'batches.table.html',
  styleUrls: ['batches.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: BatchValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchesTable extends AppTable<Batch, { operationId?: number }>
  implements OnInit, OnDestroy, ValidatorService, TableDataService<Batch, any> {

  private _program: string;
  private _acquisitionLevel: string;
  private _implicitValues: { [key: string]: any } = {};
  private _dataSubject = new BehaviorSubject<LoadResult<Batch>>({data: []});
  private _onRefreshPmfms = new EventEmitter<any>();

  loading = false;
  loadingPmfms = true;
  pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  measurementValuesFormGroupConfig: { [key: string]: any };
  data: Batch[];
  taxonGroups: Observable<ReferentialRef[]>;
  taxonNames: Observable<ReferentialRef[]>;

  set value(data: Batch[]) {
    if (this.data !== data) {
      this.data = data;
      if (!this.loading) this.onRefresh.emit();
    }
  }

  get value(): Batch[] {
    return this.data;
  }

  protected get dataSubject(): BehaviorSubject<LoadResult<Batch>> {
    return this._dataSubject;
  }

  @Input()
  set program(value: string) {
    if (this._program !== value && isNotNil(value)) {
      if (this.debug) console.debug("[batch-table] Setting program:" + value);
      this._program = value;
      if (!this.loading) this._onRefreshPmfms.emit('set program');
    }
  }

  get program(): string {
    return this._program;
  }

  @Input()
  set acquisitionLevel(value: string) {
    if (this._acquisitionLevel !== value && isNotNil(value)) {
      this._acquisitionLevel = value;
      if (!this.loading) this._onRefreshPmfms.emit();
    }
  }

  get acquisitionLevel(): string {
    return this._acquisitionLevel;
  }

  @Input() showCommentsColumn: boolean = true;
  @Input() showTaxonGroupColumn: boolean = true;
  @Input() showTaxonNameColumn: boolean = true;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: BatchValidatorService,
    protected measurementsValidatorService: MeasurementsValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected programService: ProgramService,
    protected translate: TranslateService,
    protected formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
      RESERVED_START_COLUMNS.concat(BATCH_RESERVED_START_COLUMNS).concat(BATCH_RESERVED_END_COLUMNS).concat(RESERVED_END_COLUMNS)
    );
    this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
    this.autoLoad = false;
    this.inlineEdition = true;
    this.setDatasource(new AppTableDataSource<any, { operationId?: number }>(
      Batch, this, this, {
        prependNewElements: false,
        suppressErrors: false,
        onNewRow: (row) => this.onNewBatchRow(row)
      }));
    //this.debug = true;
  };

  async ngOnInit() {
    super.ngOnInit();

    let excludesColumns: String[] = new Array<String>();
    if (!this.showCommentsColumn) excludesColumns.push('comments');
    if (!this.showTaxonGroupColumn) excludesColumns.push('taxonGroup');
    if (!this.showTaxonNameColumn) excludesColumns.push('taxonName');

    this.registerSubscription(
      this._onRefreshPmfms.asObservable()
        .subscribe(() => this.refreshPmfms('ngOnInit'))
    );

    this.registerSubscription(
      this.pmfms
        .pipe(filter(isNotNil))
        .subscribe(pmfms => {
          this.measurementValuesFormGroupConfig = this.measurementsValidatorService.getFormGroupConfig(pmfms);
          let pmfmColumns = pmfms.map(p => p.pmfmId.toString());

          this.displayedColumns = RESERVED_START_COLUMNS
            .concat(BATCH_RESERVED_START_COLUMNS)
            .concat(pmfmColumns)
            .concat(BATCH_RESERVED_END_COLUMNS)
            .concat(RESERVED_END_COLUMNS)
            // Remove columns to hide
            .filter(column => !excludesColumns.includes(column));

          this.loading = false;

          if (this.data) this.onRefresh.emit();
        }));

    // Taxon group combo
    this.taxonGroups = this.registerCellValueChanges('taxonGroup')
      .pipe(
        debounceTime(250),
        switchMap((value) => this.referentialRefService.suggest(value,
          {
            entityName: 'TaxonGroup',
            levelId: TaxonGroupIds.FAO,
            searchAttribute: 'label'
          })
        ),
        // Save implicit value, when only one result
        tap(items => this._implicitValues['taxonGroup'] = (items.length === 1) && items[0] || undefined));

    // Taxon name combo
    this.taxonNames = this.registerCellValueChanges('taxonName')
      .pipe(
        debounceTime(250),
        switchMap((value) => this.referentialRefService.suggest(value,
          {
            entityName: 'TaxonName',
            levelId: TaxonomicLevelIds.SPECIES,
            searchAttribute: 'label'
          })
        ),
        // Save implicit value, when only one result
        tap(items => this._implicitValues['taxonName'] = (items.length === 1) && items[0] || undefined));

  }

  getRowValidator(): FormGroup {
    let formGroup = this.validatorService.getRowValidator();
    if (this.measurementValuesFormGroupConfig) {
      formGroup.addControl('measurementValues', this.formBuilder.group(this.measurementValuesFormGroupConfig));
    }
    return formGroup;
  }

  watchAll(
    offset: number,
    size: number,
    sortBy?: string,
    sortDirection?: string,
    selectionFilter?: any,
    options?: any
  ): Observable<LoadResult<Batch>> {
    if (!this.data) {
      if (this.debug) console.debug("[batch-table] Unable to load rows: no value!");
      return Observable.empty(); // Not initialized
    }

    // If dirty: save first
    if (this._dirty) {
      this.save()
        .then(saved => {
          if (saved) {
            this.watchAll(offset, size, sortBy, sortDirection, selectionFilter, options);
            this._dirty = true; // restore previous state
          }
        });
    } else {
      sortBy = (sortBy !== 'id') && sortBy || 'rankOrder'; // Replace id by rankOrder

      const now = Date.now();
      if (this.debug) console.debug("[batch-table] Loading rows..", this.data);

      this.pmfms
        .pipe(
          filter(isNotNil),
          first()
        )
        .subscribe(pmfms => {
          // Transform entities into object array
          const data = this.data.map(batch => {
            const json = batch.asObject();
            json.measurementValues = MeasurementUtils.normalizeFormValues(batch.measurementValues, pmfms);
            return json;
          });

          // Sort
          this.sortBatches(data, sortBy, sortDirection);
          if (this.debug) console.debug(`[batch-table] Rows loaded in ${Date.now() - now}ms`, data);

          this._dataSubject.next({data: data, total: data.length});
        });
    }

    return this._dataSubject;
  }

  async saveAll(data: Batch[], options?: any): Promise<Batch[]> {
    if (!this.data) throw new Error("[batch-table] Could not save table: value not set (or not started)");

    if (this.debug) console.debug("[batch-table] Updating data from rows...");

    const pmfms = this.pmfms.getValue() || [];
    this.data = data.map(json => {
      const batch = Batch.fromObject(json);
      batch.measurementValues = MeasurementUtils.toEntityValues(json.measurementValues, pmfms);
      return batch;
    });

    return this.data;
  }

  deleteAll(dataToRemove: Batch[], options?: any): Promise<any> {
    this._dirty = true;
    // Noting else to do (make no sense to delete in this.data, will be done in saveAll())
    return Promise.resolve();
  }

  addRow(): boolean {
    if (this.debug) console.debug("[survivaltest-table] Calling addRow()");

    // Create new row
    const result = super.addRow();
    if (!result) return result;

    const row = this.dataSource.getRow(-1);
    this.data.push(row.currentData);
    this.editedRow = row;
    return true;
  }

  onCellFocus(event: any, row: TableElement<any>, columnName: string) {
    this.startCellValueChanges(columnName, row);
  }

  onCellBlur(event: FocusEvent, row: TableElement<any>, columnName: string) {
    this.stopCellValueChanges(columnName);
    // Apply last implicit value
    if (row.validator.controls[columnName].hasError('entity') && isNotNil(this._implicitValues[columnName])) {
      row.validator.controls[columnName].setValue(this._implicitValues[columnName]);
    }
    this._implicitValues[columnName] = undefined;
  }

  public trackByFn(index: number, row: TableElement<Batch>) {
    return row.currentData.rankOrder;
  }

  /* -- protected methods -- */

  protected async getMaxRankOrder(): Promise<number> {
    const rows = await this.dataSource.getRows();
    return rows.reduce((res, row) => Math.max(res, row.currentData.rankOrder || 0), 0);
  }

  protected async onNewBatchRow(row: TableElement<Batch>): Promise<void> {
    const batch = row.currentData;
    await this.onNewBatch(batch);
    row.currentData = batch;
  }

  protected async onNewBatch(batch: Batch, rankOrder?: number): Promise<void> {
    // Set computed values
    batch.rankOrder = isNotNil(rankOrder) ? rankOrder : ((await this.getMaxRankOrder()) + 1);
    batch.label = this._acquisitionLevel + "#" + batch.rankOrder;

    // Set default values
    (this.pmfms.getValue() || [])
      .filter(pmfm => isNotNil(pmfm.defaultValue))
      .forEach(pmfm => {
        batch.measurementValues[pmfm.pmfmId] = MeasurementUtils.normalizeFormValue(pmfm.defaultValue, pmfm);
      });
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

  protected sortBatches(data: Batch[], sortBy?: string, sortDirection?: string): Batch[] {
    if (sortBy && PMFM_ID_REGEXP.test(sortBy)) {
      sortBy = 'measurementValues.' + sortBy;
    }
    sortBy = (!sortBy || sortBy === 'id') ? 'rankOrder' : sortBy; // Replace id with rankOrder
    const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
    return data.sort((a, b) => {
      const valueA = EntityUtils.getPropertyByPath(a, sortBy);
      const valueB = EntityUtils.getPropertyByPath(b, sortBy);
      return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
    });
  }

  protected async refreshPmfms(event?: any): Promise<PmfmStrategy[]> {
    if (isNil(this._program) || isNil(this._acquisitionLevel)) return undefined;

    this.loading = true;
    this.loadingPmfms = true;

    // Load pmfms
    const pmfms = (await this.programService.loadProgramPmfms(
      this._program,
      {
        acquisitionLevel: this._acquisitionLevel
      })) || [];

    if (!pmfms.length && this.debug) {
      console.debug(`[batch-table] No pmfm found (program=${this.program}, acquisitionLevel=${this._acquisitionLevel}). Please fill program's strategies !`);
    }

    this.loadingPmfms = false;

    this.pmfms.next(pmfms);

    this.markForCheck();

    return pmfms;
  }

  markForCheck() {
    this.cd.markForCheck();
  }

  referentialToString = referentialToString;
  getPmfmColumnHeader = getPmfmName;
}

