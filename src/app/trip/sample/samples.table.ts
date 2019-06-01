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
import {debounceTime, filter, first, switchMap, tap} from "rxjs/operators";
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
  getPmfmName,
  MeasurementUtils,
  PmfmStrategy,
  referentialToString,
  Sample,
  TaxonGroupIds
} from "../services/trip.model";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {ProgramService, ReferentialRefService, TaxonomicLevelIds} from "../../referential/referential.module";
import {SampleValidatorService} from "../services/sample.validator";
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from '@ngx-translate/core';
import {MeasurementsValidatorService} from "../services/trip.validators";
import {isNotNil, LoadResult} from "../../shared/shared.module";

const PMFM_ID_REGEXP = /\d+/;
const SAMPLE_RESERVED_START_COLUMNS: string[] = ['taxonName', 'sampleDate'];
const SAMPLE_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'table-samples',
  templateUrl: 'samples.table.html',
  styleUrls: ['samples.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: SampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplesTable extends AppTable<Sample, { operationId?: number }>
  implements OnInit, OnDestroy, ValidatorService, TableDataService<Sample, any> {

  private _program: string;
  private _acquisitionLevel: string;
  private _dataSubject = new BehaviorSubject<LoadResult<Sample>>({data: []});
  private _onRefreshPmfms = new EventEmitter<any>();

  loading = false;
  loadingPmfms = true;
  pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  measurementValuesFormGroupConfig: { [key: string]: any };
  data: Sample[];
  taxonGroups: Observable<ReferentialRef[]>;
  taxonNames: Observable<ReferentialRef[]>;
  excludesColumns = new Array<String>();

  set value(data: Sample[]) {
    if (this.data !== data) {
      this.data = data;
      if (!this.loading) this.onRefresh.emit();
    }
  }

  get value(): Sample[] {
    return this.data;
  }

  protected get dataSubject(): BehaviorSubject<LoadResult<Sample>> {
    return this._dataSubject;
  }

  @Input()
  set program(value: string) {
    if (this._program !== value && isNotNil(value)) {
      //if (this.debug) console.debug("[samples-table] Setting program:" + value);
      this._program = value;
      if (!this.loading) this._onRefreshPmfms.emit();
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

  @Input() showCommentsColumn = true;
  @Input() showTaxonGroupColumn = true;
  @Input() showTaxonNameColumn = true;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: SampleValidatorService,
    protected measurementsValidatorService: MeasurementsValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected programService: ProgramService,
    protected translate: TranslateService,
    protected formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
      RESERVED_START_COLUMNS
        .concat(SAMPLE_RESERVED_START_COLUMNS)
        .concat(SAMPLE_RESERVED_END_COLUMNS)
        .concat(RESERVED_END_COLUMNS)
    );
    this.i18nColumnPrefix = 'TRIP.SAMPLE.TABLE.';
    this.autoLoad = false;
    this.inlineEdition = true;
    this.setDatasource(new AppTableDataSource<any, { operationId?: number }>(
      Sample, this, this, {
        prependNewElements: false,
        suppressErrors: true,
        onRowCreated: (row) => this.onRowCreated(row)
      }));
    //this.debug = true;
  };

  async ngOnInit(): Promise<void> {
    super.ngOnInit();

    if (!this.showCommentsColumn) this.excludesColumns.push('comments');
    if (!this.showTaxonGroupColumn) this.excludesColumns.push('taxonGroup');
    if (!this.showTaxonNameColumn) this.excludesColumns.push('taxonName');

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
            .concat(SAMPLE_RESERVED_START_COLUMNS)
            .concat(pmfmColumns)
            .concat(SAMPLE_RESERVED_END_COLUMNS)
            .concat(RESERVED_END_COLUMNS)
            // Remove columns to hide
            .filter(column => !this.excludesColumns.includes(column));

          this.loading = false;
          if (this.data) this.onRefresh.emit();
        }));

    // Taxon group combo
    this.taxonGroups = this.registerCellValueChanges('taxonGroup')
      .pipe(
        debounceTime(250),
        switchMap((value) => this.referentialRefService.suggest(value, {
              entityName: 'TaxonGroup',
              levelId: TaxonGroupIds.FAO,
              searchAttribute: 'label'
            })),
          // Remember implicit value
          tap(items => this._implicitValues['taxonGroup'] = (items.length === 1) && items[0] || undefined)
      );

    // Taxon name combo
    this.taxonNames = this.registerCellValueChanges('taxonName')
      .pipe(
        debounceTime(250),
        switchMap((value) => this.referentialRefService.suggest(value, {
              entityName: 'TaxonName',
              levelId: TaxonomicLevelIds.SPECIES,
              searchAttribute: 'label'
            })),
        // Remember implicit value
        tap(items => this._implicitValues['taxonName'] = (items.length === 1) && items[0] || undefined)
      );
  }

  getRowValidator(): FormGroup {
    let formGroup = this.validatorService.getRowValidator();
    if (this.measurementValuesFormGroupConfig) {
      if (formGroup.contains('measurementValues')) {
        formGroup.removeControl('measurementValues');
      }
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
  ): Observable<LoadResult<Sample>> {
    if (!this.data) {
      if (this.debug) console.debug("[sample-table] Unable to load rows: no value!");
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
      if (this.debug) console.debug("[sample-table] Loading rows..", this.data);

      this.pmfms
        .pipe(
          filter(isNotNil),
          first()
        )
        .subscribe(pmfms => {
          // Transform entities into object array
          const data = this.data.map(sample => {
            const json = sample.asObject();
            json.measurementValues = MeasurementUtils.normalizeFormValues(sample.measurementValues, pmfms);
            return json;
          });

          // Sort
          this.sortSamples(data, sortBy, sortDirection);
          if (this.debug) console.debug(`[sample-table] Rows loaded in ${Date.now() - now}ms`, data);

          this._dataSubject.next({data: data, total: data.length});
        });
    }

    return this._dataSubject;
  }

  async saveAll(data: Sample[], options?: any): Promise<Sample[]> {
    if (!this.data) throw new Error("[sample-table] Could not save table: value not set (or not started)");

    if (this.debug) console.debug("[sample-table] Updating data from rows...");

    const pmfms = this.pmfms.getValue() || [];
    this.data = data.map(json => {
      const sample = Sample.fromObject(json);
      sample.measurementValues = MeasurementUtils.toEntityValues(json.measurementValues, pmfms);
      return sample;
    });

    return this.data;
  }

  deleteAll(dataToRemove: Sample[], options?: any): Promise<any> {
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
    this.data.push(Sample.fromObject(row.currentData));
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

  public trackByFn(index: number, row: TableElement<Sample>) {
    return row.currentData.rankOrder;
  }

  /* -- protected methods -- */

  protected async getMaxRankOrder(): Promise<number> {
    const rows = await this.dataSource.getRows();
    return rows.reduce((res, row) => Math.max(res, row.currentData.rankOrder || 0), 0);
  }

  protected async onRowCreated(row: TableElement<Sample>): Promise<void> {
    const data = row.currentData;
    await this.onNewSample(data);
    row.currentData = data;
  }

  protected async onNewSample(sample: Sample, rankOrder?: number): Promise<void> {
    // Set computed values
    sample.rankOrder = isNotNil(rankOrder) ? rankOrder : ((await this.getMaxRankOrder()) + 1);
    sample.label = this._acquisitionLevel + "#" + sample.rankOrder;

    // Set default values
    (this.pmfms.getValue() || [])
      .filter(pmfm => isNotNil(pmfm.defaultValue))
      .forEach(pmfm => {
        sample.measurementValues[pmfm.pmfmId] = MeasurementUtils.normalizeFormValue(pmfm.defaultValue, pmfm);
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

  protected sortSamples(data: Sample[], sortBy?: string, sortDirection?: string): Sample[] {
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
      console.debug(`[sample-table] No pmfm found (program=${this.program}, acquisitionLevel=${this._acquisitionLevel}). Please fill program's strategies !`);
    }

    this.loadingPmfms = false;

    this.pmfms.next(pmfms);

    this.markForCheck();

    return pmfms;
  }

  referentialToString = referentialToString;
  getPmfmColumnHeader = getPmfmName;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

