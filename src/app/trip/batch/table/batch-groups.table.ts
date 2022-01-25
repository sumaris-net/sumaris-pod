import { ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output, ViewChild } from '@angular/core';
import { TableElement, ValidatorService } from '@e-is/ngx-material-table';
import { FormGroup, Validators } from '@angular/forms';
import { BATCH_RESERVED_END_COLUMNS, BATCH_RESERVED_START_COLUMNS, BatchesTable, BatchFilter } from './batches.table';
import {
  changeCaseToUnderscore,
  ColumnItem,
  FormFieldDefinition,
  InMemoryEntitiesService,
  isNil,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrNaN,
  LoadResult,
  PlatformService,
  propertiesPathComparator,
  ReferentialRef,
  ReferentialUtils,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  SETTINGS_DISPLAY_COLUMNS,
  TableSelectColumnsComponent,
  toBoolean,
  toFloat,
  toInt,
  toNumber,
} from '@sumaris-net/ngx-components';
import {AcquisitionLevelCodes, MethodIds} from '@app/referential/services/model/model.enum';
import {DenormalizedPmfmStrategy} from '@app/referential/services/model/pmfm-strategy.model';
import {MeasurementFormValues, MeasurementValuesUtils} from '../../services/model/measurement.model';
import {Batch, BatchUtils, BatchWeight} from '../../services/model/batch.model';
import {BatchGroupModal, IBatchGroupModalOptions} from '../modal/batch-group.modal';
import {BatchGroup} from '../../services/model/batch-group.model';
import {SubBatch} from '../../services/model/subbatch.model';
import {defer, Observable, Subject, Subscription} from 'rxjs';
import {filter, map, takeUntil, tap} from 'rxjs/operators';
import {ISubBatchesModalOptions, SubBatchesModal} from '../modal/sub-batches.modal';
import {TaxonGroupRef} from '@app/referential/services/model/taxon-group.model';
import {MatMenuTrigger} from '@angular/material/menu';
import {BatchGroupValidators, BatchGroupValidatorService} from '../../services/validator/batch-group.validator';
import {IPmfm, PmfmUtils} from '@app/referential/services/model/pmfm.model';
import {PmfmForm} from '@app/trip/services/validator/operation.validator';
import {TaxonNameRef} from '@app/referential/services/model/taxon-name.model';

const DEFAULT_USER_COLUMNS = ['weight', 'individualCount'];

declare type BaseColumnKeyType = 'totalWeight' | 'totalIndividualCount' | 'samplingRatio' | 'samplingWeight' | 'samplingIndividualCount';

declare interface ColumnDefinition extends FormFieldDefinition {
  key: BaseColumnKeyType;
  computed: boolean;
  hidden: boolean;
  unitLabel?: string;
  rankOrder: number;
  qvIndex: number;
  classList?: string;
  path?: string;

  // Describe column
  isWeight?: boolean;
  isIndividualCount?: boolean;
  isSampling?: boolean;

  // Column from pmfm
  id?: number;
}

declare interface GroupColumnDefinition {
  key: string;
  name: string;
  qvIndex: number;
  colSpan?: number;
}

@Component({
  selector: 'app-batch-groups-table',
  templateUrl: 'batch-groups.table.html',
  styleUrls: ['batch-groups.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: BatchGroupValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchGroupsTable extends BatchesTable<BatchGroup> {

  static BASE_DYNAMIC_COLUMNS: Partial<ColumnDefinition>[] = [
    // Column on total (weight, nb indiv)
    {
      type: 'double',
      key: 'totalWeight',
      label: 'TRIP.BATCH.TABLE.TOTAL_WEIGHT',
      minValue: 0,
      maxValue: 10000,
      maximumNumberDecimals: 1,
      isWeight: true,
      classList: 'total mat-column-weight',
      path: 'weight.value'
    },
    {
      type: 'double',
      key: 'totalIndividualCount',
      label: 'TRIP.BATCH.TABLE.TOTAL_INDIVIDUAL_COUNT',
      minValue: 0,
      maxValue: 10000,
      maximumNumberDecimals: 2,
      isIndividualCount: true,
      classList: 'total',
      path: 'individualCount'
    },

    // Column on sampling (ratio, nb indiv, weight)
    {
      type: 'integer',
      key: 'samplingRatio',
      label: 'TRIP.BATCH.TABLE.SAMPLING_RATIO',
      unitLabel: '%',
      minValue: 0,
      maxValue: 100,
      maximumNumberDecimals: 2,
      isSampling: true,
      path: 'children.0.samplingRatio'
    },
    {
      type: 'double',
      key: 'samplingWeight',
      label: 'TRIP.BATCH.TABLE.SAMPLING_WEIGHT',
      minValue: 0,
      maxValue: 1000,
      maximumNumberDecimals: 1,
      isWeight: true,
      isSampling: true,
      path: 'children.0.weight.value'
    },
    {
      type: 'string',
      key: 'samplingIndividualCount',
      label: 'TRIP.BATCH.TABLE.SAMPLING_INDIVIDUAL_COUNT',
      computed: true,
      isIndividualCount: true,
      isSampling: true,
      path: 'children.0.individualCount'
    }
  ];

  private _defaultTaxonGroups: string[];
  private _showSamplingBatchColumns = true;
  private _showWeightColumns = true;
  private _batchGroupRowSubscription: Subscription;

  readonly qvColumnCount: number = BatchGroupsTable.BASE_DYNAMIC_COLUMNS.length;
  weightMethodForm: FormGroup;
  estimatedWeightPmfm: IPmfm;
  dynamicColumns: ColumnDefinition[];
  modalOptions: Partial<IBatchGroupModalOptions>;

  showToolbar = true; // False only if no group columns AND mobile
  groupColumns: GroupColumnDefinition[];
  groupColumnNames: string[];
  groupColumnStartColSpan: number;

  disable(opts?: { onlySelf?: boolean; emitEvent?: boolean; }) {
    super.disable(opts);
    if (this.weightMethodForm) this.weightMethodForm.disable(opts);
  }

  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean; }) {
    super.enable(opts);
    if (this.weightMethodForm) this.weightMethodForm.enable(opts);
  }

  markAsPristine(opts?: { onlySelf?: boolean; emitEvent?: boolean; }) {
    super.markAsPristine(opts);
    if (this.weightMethodForm) this.weightMethodForm.markAsPristine(opts);
  }

  markAsTouched(opts?: { onlySelf?: boolean; emitEvent?: boolean; }) {
    super.markAsTouched(opts);
    if (this.weightMethodForm) this.weightMethodForm.markAsTouched(opts);
  }

  markAllAsTouched(opts?: { onlySelf?: boolean; emitEvent?: boolean; }) {
    super.markAllAsTouched(opts);
    if (this.weightMethodForm) this.weightMethodForm.markAllAsTouched();
  }

  markAsUntouched(opts?: { onlySelf?: boolean; emitEvent?: boolean; }) {
    super.markAsUntouched(opts);
    if (this.weightMethodForm) this.weightMethodForm.markAsUntouched(opts);
  }

  get dirty(): boolean {
    return this.dirtySubject.value || (this.weightMethodForm && this.weightMethodForm.dirty);
  }

  @Input() useSticky = false;
  @Input() availableSubBatches: SubBatch[] | Observable<SubBatch[]>;
  @Input() availableTaxonGroups: TaxonGroupRef[];

  @Input() set showSamplingBatchColumns(value: boolean) {
    if (this._showSamplingBatchColumns !== value) {
      this._showSamplingBatchColumns = value;
      this.setModalOption('showSamplingBatch', value);
      // updateColumns only if pmfms are ready
      if (!this.loading && this._initialPmfms) {
        this.computeDynamicColumns(this.qvPmfm, {forceCompute: true});
        this.updateColumns();
        if (!this._showSamplingBatchColumns) {
          this.clearSamplingColumns();
        }
      }
    }
  }

  get showSamplingBatchColumns(): boolean {
    return this._showSamplingBatchColumns;
  }

  @Input() set showWeightColumns(value: boolean) {
    if (this._showWeightColumns !== value) {
      this._showWeightColumns = value;
      // updateColumns only if pmfms are ready
      if (!this.loading && this._initialPmfms) {
        this.computeDynamicColumns(this.qvPmfm, {forceCompute: true});
        this.updateColumns();
      }
    }
  }

  get showWeightColumns(): boolean {
    return this._showWeightColumns;
  }

  @Input() showIndividualCountColumns: boolean;
  @Input() showError = true;

  get additionalPmfms(): IPmfm[] {
    return this._initialPmfms.filter(pmfm => (!this.qvPmfm || pmfm.id !== this.qvPmfm.id) && pmfm.id !== this.defaultWeightPmfm.id);
  }

  @Input() allowSubBatches = true;
  @Input() defaultHasSubBatches = false;
  @Input() taxonGroupsNoWeight: string[];
  @Input() mobile: boolean;

  @Output() onSubBatchesChanges = new EventEmitter<SubBatch[]>();


  @ViewChild(MatMenuTrigger) rowMenuTrigger: MatMenuTrigger;

  constructor(
    injector: Injector,
    protected platform: PlatformService
  ) {
    super(injector,
      // Force no validator (readonly mode, if mobile)
      platform.mobile ? null : injector.get(ValidatorService),
      new InMemoryEntitiesService<BatchGroup, BatchFilter>(BatchGroup, BatchFilter, {
        onLoad: (data) => this.onLoad(data),
        onSave: (data) => this.onSave(data),
        equals: Batch.equals
      }),
      BatchGroup
    );


    // Set default values
    this.confirmBeforeDelete = this.mobile;
    this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
    this.keepEditedRowOnSave = !this.mobile;
    // this.showCommentsColumn = false; // Already set in batches-table
    // this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH; // Already set in batches-table

    // -- For DEV only
    //this.debug = !environment.production;


    // If init form callback exists, apply it when start row edition
    this.registerSubscription(
      this.onStartEditingRow
        .pipe(
          filter(row => row && row.validator && true),
          map(row => ({form: row.validator, pmfms: this.pmfms, markForCheck: () => this.markForCheck()})),
          tap(event => {
            // DEBUG
            //console.debug('[samples-table] will sent onPrepareRowForm event:', event)
            // this.onPrepareRowForm.emit(event);
            this._batchGroupRowSubscription?.unsubscribe();
            this._batchGroupRowSubscription = this.computeBatchGroupRowValidator(event);

            // Force update of the form validity
            event.form?.updateValueAndValidity({emitEvent: true, onlySelf: false});
          })
        )
        .subscribe());
  }

  protected computeBatchGroupRowValidator(pmfmForm: PmfmForm): Subscription {
    return BatchGroupValidators.addSamplingRowValidators(pmfmForm, {qvPmfm: this.qvPmfm});
  }


  ngOnInit() {
    this.inlineEdition = this.validatorService && !this.mobile;
    this.allowRowDetail = !this.inlineEdition;
    this.showIndividualCountColumns = toBoolean(this.showIndividualCountColumns, !this.mobile);

    // in DEBUG only: force validator = null
    if (this.debug && this.mobile) this.setValidatorService(null);

    super.ngOnInit();
  }

  setModalOption(key: keyof IBatchGroupModalOptions, value: IBatchGroupModalOptions[typeof key]) {
    this.modalOptions = this.modalOptions || {};
    this.modalOptions[key as any] = value;
  }

  onLoad(data: BatchGroup[]): BatchGroup[] {
    // if (isNil(this.qvPmfm) || !this.qvPmfm.qualitativeValues) return data; // Skip (pmfms not loaded)

    if (this.debug) console.debug('[batch-group-table] Preparing data to be loaded as table rows...');

    // Pmfms to normalize (intialPmfms + fake additionnal pmfms)
    const pmfms = this._initialPmfms.concat(this.pmfms.filter(p => p.id < 0));

    const weightMethodValues = this.qvPmfm ? this.qvPmfm.qualitativeValues.reduce((res, qv, qvIndex) => {
        res[qvIndex] = false;
        return res;
      }, {})
      : {0: false};

    // Transform entities into object array
    data = data.map(batch => {
      const measurementValues = {};

      if (isNotEmptyArray(batch.children) && this.qvPmfm) {
        // For each group (one by qualitative value)
        this.qvPmfm.qualitativeValues.forEach((qv, qvIndex) => {
          const childLabel = `${batch.label}.${qv.label}`;
          // tslint:disable-next-line:triple-equals
          const child = batch.children.find(c => c.label === childLabel || c.measurementValues[this.qvPmfm.id] == qv.id);
          if (child) {

            // Replace measurement values inside a new map, based on fake pmfms
            this.getFakeMeasurementValuesFromQvChild(child, measurementValues, qvIndex);

            // Remember method used for the weight (estimated or not)
            if (!weightMethodValues[qvIndex]) {
              if (child.weight && child.weight.estimated) {
                weightMethodValues[qvIndex] = true;
              } else if (child.children && child.children.length === 1) {
                const samplingChild = child.children[0];
                weightMethodValues[qvIndex] = samplingChild.weight && samplingChild.weight.estimated;
              }
            }

            // Should have sub batches, when sampling batch exists
            const hasSubBatches = this._showSamplingBatchColumns || isNotNil(BatchUtils.getSamplingChild(child));

            // Make sure to create a sampling batch, if has sub bacthes
            if (hasSubBatches) {
              BatchUtils.getOrCreateSamplingChild(child);
            }
          }
        });
      } else if (!this.qvPmfm && batch) {
        // Replace measurement values inside a new map, based on fake pmfms
        this.getFakeMeasurementValuesFromQvChild(batch, measurementValues, 0);

        // Remember method used for the weight (estimated or not)
        if (!weightMethodValues[0]) {
          if (batch.weight && batch.weight.estimated) {
            weightMethodValues[0] = true;
          } else if (batch.children && batch.children.length === 1) {
            const samplingChild = batch.children[0];
            weightMethodValues[0] = samplingChild.weight && samplingChild.weight.estimated;
          }
        }
      }

      // Make entity compatible with reactive form
      batch.measurementValues = measurementValues;

      MeasurementValuesUtils.normalizeEntityToForm(batch, pmfms, null, {keepOtherExistingPmfms: true});

      return batch;
    });

    // Set weight is estimated ?
    if (this.weightMethodForm) {
      console.debug('[batch-group-table] Set weight form values (is estimated ?)')
      this.weightMethodForm.patchValue(weightMethodValues);
    }

    return data;
  }


  async onSave(data: BatchGroup[]): Promise<BatchGroup[]> {

    if (this.debug) console.debug('[batch-group-table] Preparing data to be saved...');
    data = data.map(batch => {
      this.prepareEntityToSave(batch);
      return batch;
    });

    return data;
  }


  /**
   * Allow to fill table (e.g. with taxon groups found in strategies) - #176
   *
   * @params opts.includeTaxonGroups : include taxon label
   */
  async autoFillTable(opts?: { forceIfDisabled?: boolean; }) {
    // Wait table ready and loaded
    await Promise.all([this.ready(), this.waitIdle()]);

    // Skip when disabled
    if ((!opts || opts.forceIfDisabled !== true) && this.disabled) {
      console.warn('[batch-group-table] Skipping autofill as table is disabled');
      return;
    }

    // Skip when editing a row
    if (!this.confirmEditCreate()) {
      console.warn('[batch-group-table] Skipping autofill, as table still editing a row');
      return;
    }

    this.markAsLoading();

    try {
      console.debug('[batch-group-table] Auto fill table, using options:', opts);

      // Read existing taxonGroup
      const rowsTaxonGroups = (await this.dataSource.getRows() || []).map(r => r.currentData)
        .map(batch => batch.taxonGroup)
        .filter(isNotNil);

      const taxonGroups = this.availableTaxonGroups
        // Exclude species that already exists in table
        .filter(taxonGroup => !rowsTaxonGroups.some(tg => ReferentialUtils.equals(tg, taxonGroup)));

      for (const taxonGroup of taxonGroups) {
        const batch = new BatchGroup();
        batch.taxonGroup = TaxonGroupRef.fromObject(taxonGroup);
        await this.addEntityToTable(batch);
      }

    } catch (err) {
      console.error(err && err.message || err);
      this.error = err && err.message || err;
    } finally {
      this.markAsLoaded();
    }
  }

  /**
   * Use in ngFor, for trackBy
   *
   * @param index
   * @param column
   */
  trackColumnDef(index: number, column: ColumnDefinition) {
    return column.rankOrder;
  }

  // FIXME check if need by any program
  async hideUnusedColumns() {
    // DEBUG
    console.debug('[batch-groups-table] hideUnusedColumns()');
    const availableTaxonGroups = this.availableTaxonGroups;
    if (isNotEmptyArray(availableTaxonGroups) && isNotEmptyArray(this.taxonGroupsNoWeight)) {
      const allTaxonHasNoWeight = availableTaxonGroups
        .every(tg => this.taxonGroupsNoWeight.findIndex(tgNw => tgNw.startsWith(tg.label)) !== -1);
      this.showWeightColumns = !allTaxonHasNoWeight;
    } else {
      this.showWeightColumns = true;
    }
  }

  /* -- protected methods -- */

  protected normalizeEntityToRow(batch: BatchGroup, row: TableElement<BatchGroup>) {
    // When batch has the QV value
    if (this.qvPmfm) {
      const measurementValues = {...(row.currentData.measurementValues)}; // Copy existing measurements

      if (isNotEmptyArray(batch.children)) {
        // For each group (one by qualitative value)
        this.qvPmfm.qualitativeValues.forEach((qv, qvIndex) => {
          const childLabel = `${batch.label}.${qv.label}`;
          // tslint:disable-next-line:triple-equals
          const child = batch.children.find(c => c.label === childLabel || c.measurementValues[this.qvPmfm.id] == qv.id);
          if (child) {

            // Replace measurement values inside a new map, based on fake pmfms
            this.getFakeMeasurementValuesFromQvChild(child, measurementValues, qvIndex);
          }
        });
      }
      batch.measurementValues = measurementValues;
    }

    // Inherited method
    super.normalizeEntityToRow(batch, row);

  }

  protected getFakeMeasurementValuesFromQvChild(data: Batch, measurementValues?: MeasurementFormValues, qvIndex?: number): MeasurementFormValues {
    if (!data) return measurementValues; // skip

    if (isNil(qvIndex)) {
      const qvId = this.qvPmfm && data.measurementValues[this.qvPmfm.id];
      qvIndex = isNotNil(qvId) && this.qvPmfm.qualitativeValues.findIndex(qv => qv.id === +qvId);
      if (qvIndex === -1) throw Error('Invalid batch: no QV value');
    }

    measurementValues = measurementValues || {};
    let i = qvIndex * this.qvColumnCount;

    // Column: total weight
    data.weight = this.getWeight(data.measurementValues) || undefined;
    measurementValues[i++] = toNumber(data.weight && !data.weight.computed && data.weight.value, null);

    // Column: individual count
    const individualCount = toNumber(data.individualCount, null);
    /*if (data.qualityFlagId === QualityFlagIds.BAD){
      //console.log('TODO Invalid individual count !', individualCount);
    }*/
    measurementValues[i++] = individualCount;

    // Sampling batch
    const samplingChild = BatchUtils.getSamplingChild(data);
    if (samplingChild) {
      // Column: sampling ratio
      measurementValues[i++] = isNotNil(samplingChild.samplingRatio) ? samplingChild.samplingRatio * 100 : null;

      // Column: sampling weight
      samplingChild.weight = this.getWeight(samplingChild.measurementValues);
      measurementValues[i++] = samplingChild.weight && !samplingChild.weight.computed && samplingChild.weight.value;

      // Column: sampling individual count
      const samplingIndividualCount: any = toNumber(samplingChild.individualCount, null);
      /*if (samplingChild.qualityFlagId === QualityFlagIds.BAD) {
        //console.log('TODO Invalid sampling individual count !', samplingIndividualCount);
        //samplingIndividualCount = '~' + samplingIndividualCount;
      }*/
      measurementValues[i++] = samplingIndividualCount;
    }
    // No sampling batch: clean values
    else {
      measurementValues[i++] = undefined; // Column: sampling ratio
      measurementValues[i++] = undefined; // Column: sampling weight
      measurementValues[i++] = undefined; // sampling individual count
    }

    // this.additionalPmfms.forEach(pmfm => {
    //   measurementValues[this.getPmfmFakeIds(pmfm.id, qvIndex)] = data.measurementValues[pmfm.id] || pmfm.defaultValue || undefined;
    // });

    return measurementValues;
  }

  protected prepareEntityToSave(batch: BatchGroup) {
    const groupColumnValues = batch.measurementValues;
    batch.measurementValues = {};

    if (this.qvPmfm) {
      batch.children = (this.qvPmfm && this.qvPmfm.qualitativeValues || []).reduce((res, qv, qvIndex: number) => {
        return res.concat(this.newPrepareChildToSave(batch, groupColumnValues, qv, qvIndex));
      }, []);
    } else {
      this.newPrepareChildToSave(batch, groupColumnValues);
    }
  }

  protected newPrepareChildToSave(batch: BatchGroup, groupColumnValues: { [key: number]: any }, qv?: ReferentialRef, qvIndex?: number): Batch {

    qvIndex = qvIndex || 0;
    let i = qvIndex * this.qvColumnCount;

    const isEstimatedWeight = this.weightMethodForm && this.weightMethodForm.controls[qvIndex].value || false;
    const weightPmfmId = isEstimatedWeight ? this.estimatedWeightPmfm.id : this.defaultWeightPmfm.id;

    const childLabel = qv ? `${batch.label}.${qv.label}` : batch.label;

    // If qv, add sub level at sorting batch for each qv value
    // If no qv, keep measurements in sorting batch level
    const child: Batch = !qv ? batch : isNotNil(batch.id) && (batch.children || []).find(b => b.label === childLabel) || new Batch();

    const weight = child.weight?.value || null;

    child.rankOrder = qvIndex + 1;
    // child.measurementValues = {};
    if (qv) {
      child.measurementValues[this.qvPmfm.id.toString()] = qv.id.toString();
    }
    child.measurementValues[weightPmfmId.toString()] = isNotNilOrNaN(weight) ? weight : undefined;

    // If sampling
    if (isNotEmptyArray(child.children)) {
      const samplingLabel = childLabel + Batch.SAMPLING_BATCH_SUFFIX;
      const samplingChild: Batch = isNotNil(child.id) && (child.children || []).find(b => b.label === samplingLabel) || new Batch();
      samplingChild.rankOrder = 1;
      samplingChild.label = samplingLabel;
      // samplingChild.measurementValues = {};
      samplingChild.measurementValues[weightPmfmId.toString()] = isNotNilOrNaN(samplingChild.weight?.value) ? samplingChild.weight?.value : undefined;
      child.children = [samplingChild];
    }
    // Remove children
    else {
      child.children = [];
    }

    //Additional pmfms
    this.additionalPmfms.forEach(pmfm => {
      child.measurementValues[pmfm.id] = groupColumnValues[this.getPmfmFakeIds(pmfm.id, qvIndex)] || pmfm.defaultValue;
    });

    return child;
  }

  protected prepareChildToSave(batch: BatchGroup, groupColumnValues: { [key: number]: any }, qv?: ReferentialRef, qvIndex?: number): Batch {

    qvIndex = qvIndex || 0;
    let i = qvIndex * this.qvColumnCount;
    const weight = toFloat(groupColumnValues[i++], null);
    const individualCount = toInt(groupColumnValues[i++], null);
    const samplingRatio = toInt(groupColumnValues[i++], null);
    const samplingWeight = toFloat(groupColumnValues[i++], null);
    const samplingIndividualCount = toFloat(groupColumnValues[i++], null);

    // TODO: compute total weight and nb indiv ?

    const isEstimatedWeight = this.weightMethodForm && this.weightMethodForm.controls[qvIndex].value || false;
    const weightPmfmId = isEstimatedWeight ? this.estimatedWeightPmfm.id : this.defaultWeightPmfm.id;

    const childLabel = qv ? `${batch.label}.${qv.label}` : batch.label;

    // If qv, add sub level at sorting batch for each qv value
    // If no qv, keep measurements in sorting batch level
    const child: Batch = !qv ? batch : isNotNil(batch.id) && (batch.children || []).find(b => b.label === childLabel) || new Batch();

    child.rankOrder = qvIndex + 1;
    child.measurementValues = {};
    if (qv) {
      child.measurementValues[this.qvPmfm.id.toString()] = qv.id.toString();
    }
    child.measurementValues[weightPmfmId.toString()] = isNotNilOrNaN(weight) ? weight : undefined;
    child.individualCount = individualCount;
    child.label = childLabel;

    // If sampling
    if (isNotNil(samplingRatio) || isNotNil(samplingIndividualCount) || isNotNil(samplingWeight)) {
      const samplingLabel = childLabel + Batch.SAMPLING_BATCH_SUFFIX;
      const samplingChild: Batch = isNotNil(child.id) && (child.children || []).find(b => b.label === samplingLabel) || new Batch();
      samplingChild.rankOrder = 1;
      samplingChild.label = samplingLabel;
      samplingChild.samplingRatio = isNotNil(samplingRatio) ? samplingRatio / 100 : undefined;
      samplingChild.samplingRatioText = isNotNil(samplingRatio) ? `${samplingRatio}%` : undefined;
      samplingChild.measurementValues = {};
      samplingChild.measurementValues[weightPmfmId.toString()] = isNotNilOrNaN(samplingWeight) ? samplingWeight : undefined;
      samplingChild.individualCount = samplingIndividualCount;
      child.children = [samplingChild];
    }
    // Remove children
    else {
      child.children = [];
    }

    //Additional pmfms
    this.additionalPmfms.forEach(pmfm => {
      child.measurementValues[pmfm.id] = groupColumnValues[this.getPmfmFakeIds(pmfm.id, qvIndex)] || pmfm.defaultValue;
    });

    return child;
  }

  isQvEven(column: ColumnDefinition) {
    return (column.qvIndex % 2 === 0);
  }

  isQvOdd(column: ColumnDefinition) {
    return (column.qvIndex % 2 !== 0);
  }

  async onSubBatchesClick(event: UIEvent,
                          row: TableElement<BatchGroup>,
                          opts?: { showParent?: boolean; emitLoaded?: boolean; }) {
    if (event) event.preventDefault();

    // Loading spinner
    this.markAsLoading();

    try {

      const selectedParent = this.toEntity(row);
      const subBatches = await this.openSubBatchesModal(selectedParent, opts);

      if (isNil(subBatches)) return; // User cancelled

      // Update the batch group, from subbatches (e.g. observed individual count)
      this.updateBatchGroupRow(row, subBatches);

    } finally {
      // Hide loading
      if (!opts || opts.emitLoaded !== false) {
        this.markAsLoaded();
      }
    }
  }

  /* -- protected functions -- */

  // Override parent function
  protected mapPmfms(pmfms: DenormalizedPmfmStrategy[]): DenormalizedPmfmStrategy[] {
    if (!pmfms || !pmfms.length) return pmfms; // Skip (no pmfms)

    super.mapPmfms(pmfms); // Will find the qvPmfm

    if (this.validatorService) {
      (this.validatorService as BatchGroupValidatorService).qvPmfm = this.qvPmfm;
    }

    // Init dynamic columns
    this.computeDynamicColumns(this.qvPmfm);

    // Convert dynamic column to PMFM (to use compatibility with TableMeasurements)
    const fakePmfms = (this.dynamicColumns || []).map(col => DenormalizedPmfmStrategy.fromObject({
      ...col,
      name: col.label,
      id: col.rankOrder,
      methodId: col.computed && MethodIds.CALCULATED
    }));

    this.additionalPmfms.forEach(p => {
      this.qvPmfm.qualitativeValues.forEach((qv, index) => {
          const fakePmfm = p.clone();
          fakePmfm.id = this.getPmfmFakeIds(p.id, index);
          fakePmfms.push(DenormalizedPmfmStrategy.fromObject(fakePmfm));
      })
    });
    return fakePmfms;
  }

  protected computeDynamicColumns(qvPmfm: IPmfm, opts?: { forceCompute: boolean }): ColumnDefinition[] {
    if ((!opts || opts.forceCompute !== true) && this.dynamicColumns) return this.dynamicColumns; // Already init

    if (this.qvPmfm && this.debug) console.debug('[batch-group-table] Using a qualitative PMFM, to group columns: ' + qvPmfm.label);

    if (isNil(this.defaultWeightPmfm)
      || (PmfmUtils.isDenormalizedPmfm(this.defaultWeightPmfm)
        && (qvPmfm && PmfmUtils.isDenormalizedPmfm(qvPmfm)
          && qvPmfm.rankOrder > this.defaultWeightPmfm.rankOrder))) {
      throw new Error(`[batch-group-table] Unable to construct the table. First qualitative value PMFM must be define BEFORE any weight PMFM (by rankOrder in PMFM strategy - acquisition level ${this.acquisitionLevel})`);
    }

    // If estimated weight is allow, init a form for weight methods
    if (!this.weightMethodForm && this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER]) {

      // Create the form, for each QV value
      this.weightMethodForm = this.formBuilder.group(qvPmfm.qualitativeValues.reduce((res, qv, index) => {
        res[index] = [false, Validators.required];
        return res;
      }, {}));
    }

    this.estimatedWeightPmfm = this.weightPmfmsByMethod && this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER] || this.defaultWeightPmfm;
    this.groupColumnStartColSpan = RESERVED_START_COLUMNS.length
      + (this.showTaxonGroupColumn ? 1 : 0)
      + (this.showTaxonNameColumn ? 1 : 0);
    if (qvPmfm) {
      const groupColumns = [];
      this.dynamicColumns = qvPmfm.qualitativeValues.reduce((res, qv, qvIndex) => {
        const cols = this.computeDynamicColumnsByQv(qv, qvIndex);
        // Create the group column
        const visibleColumnCount = cols.filter(c => !c.hidden).length;
        const groupKey = `group-${qv.label}`;
        groupColumns.push({
          key: groupKey,
          name: qv.name,
          qvIndex,
          colSpan: visibleColumnCount
        });
        // Append to dynamic columns
        return res.concat(cols);
      }, []);

      // DEBUG
      // console.debug('[batch-groups-table] Dynamic columns: ' + qvColumns.map(c => c.key).join(','));

      this.groupColumns = groupColumns;
      this.showToolbar = true;
    } else {
      this.dynamicColumns = this.computeDynamicColumnsByQv();
      this.groupColumnStartColSpan += this.dynamicColumns.length;
      this.showToolbar = !this.mobile;
    }
  }

  protected computeDynamicColumnsByQv(qvGroup?: ReferentialRef, qvIndex?: number): ColumnDefinition[] {
    qvIndex = qvIndex || 0;
    const offset = qvIndex * BatchGroupsTable.BASE_DYNAMIC_COLUMNS.length;
    const hideWeightColumns = !this.showWeightColumns;
    const hideIndividualCountColumns = !this.showIndividualCountColumns;
    const hideSamplingColumns = !this._showSamplingBatchColumns;

    const qvColumns = BatchGroupsTable.BASE_DYNAMIC_COLUMNS
      .reduce((res, columnDef, index) => {
        const key = qvGroup ? `${qvGroup.label}_${columnDef.key}` : columnDef.key;
        const rankOrder = offset + index;
        const hidden = (hideWeightColumns && columnDef.isWeight)
          || (hideIndividualCountColumns && (columnDef.isIndividualCount || columnDef.key === 'samplingRatio'))
          || (hideSamplingColumns && columnDef.isSampling);
        return res.concat({
          ...(columnDef.isWeight && this.defaultWeightPmfm || {}),
          ...columnDef,
          key,
          qvIndex,
          rankOrder,
          hidden,
          path: 'children.' + qvIndex + '.' + columnDef.path
        });
      }, []);

    return qvColumns;
  }

  protected getWeight(measurementValues: { [key: number]: any }): BatchWeight | undefined {
    // Use try default method
    let value = measurementValues[this.defaultWeightPmfm.id];
    if (isNotNil(value)) {
      return {
        value,
        estimated: false,
        computed: false,
        methodId: this.defaultWeightPmfm.methodId
      };
    }
    if (!this.weightPmfmsByMethod) return undefined;

    // Else, try to get estimated
    let weightPmfm = this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER];
    value = weightPmfm && measurementValues[weightPmfm.id];
    if (isNotNil(value)) {
      return {
        value,
        estimated: true,
        computed: false,
        methodId: MethodIds.ESTIMATED_BY_OBSERVER
      };
    }

    // Else, try to get calculated
    weightPmfm = this.weightPmfmsByMethod[MethodIds.CALCULATED];
    value = weightPmfm && measurementValues[weightPmfm.id];
    if (isNotNil(value)) {
      return {
        value,
        estimated: false,
        computed: true,
        methodId: MethodIds.CALCULATED
      };
    }

    return undefined;
  }

  protected getUserColumns(userColumns?: string[]): string[] {
    userColumns = userColumns || this.settings.getPageSettings(this.settingsId, SETTINGS_DISPLAY_COLUMNS);

    // Exclude OLD user columns (fix issue on v0.16.2)
    userColumns = userColumns && userColumns.filter(c => c === 'weight' || c === 'individualCount');

    return isNotEmptyArray(userColumns) && userColumns.length === 2 ? userColumns :
      // If not user column override (or if bad format), then use defaults
      DEFAULT_USER_COLUMNS.slice(0);
  }

  protected updateColumns() {
    if (!this.dynamicColumns) return; // skip
    this.displayedColumns = this.getDisplayColumns();

    if (!this.loading) this.markForCheck();
  }

  protected getDisplayColumns(): string[] {
    if (!this.dynamicColumns) return this.columns;

    const userColumns = this.getUserColumns();

    const weightIndex = userColumns.findIndex(c => c === 'weight');
    let individualCountIndex = userColumns.findIndex(c => c === 'individualCount');
    individualCountIndex = (individualCountIndex !== -1 && weightIndex === -1 ? 0 : individualCountIndex);
    const inverseOrder = individualCountIndex < weightIndex;

    const dynamicColumnKeys = (this.dynamicColumns || [])
      .map(c => ({
        key: c.key,
        hidden: c.hidden,
        rankOrder: c.rankOrder + (inverseOrder &&
          ((c.isWeight && 1) || (c.isIndividualCount && -1)) || 0),
      }))
      .sort((c1, c2) => c1.rankOrder - c2.rankOrder)
      .filter(c => !c.hidden)
      .map(c => c.key);

    this.groupColumnNames = ['top-start']
      .concat(this.groupColumns.map(c => c.key))
      .concat(['top-end']);

    return RESERVED_START_COLUMNS
      .concat(BATCH_RESERVED_START_COLUMNS)
      .concat(dynamicColumnKeys)
      .concat(BATCH_RESERVED_END_COLUMNS)
      .concat(RESERVED_END_COLUMNS)
      .filter(name => !this.excludesColumns.includes(name));
  }

  protected async openSubBatchesModalFromParentModal(parent: BatchGroup): Promise<BatchGroup> {

    // Make sure the row exists
    this.editedRow = (this.editedRow && BatchGroup.equals(this.editedRow.currentData, parent) && this.editedRow)
      || (await this.findRowByEntity(parent))
      // Or add it to table, if new
      || (await this.addEntityToTable(parent, {confirmCreate: false}));

    const subBatches = await this.openSubBatchesModal(parent, {
      showParent: false // action triggered from the parent batch modal, so the parent field can be hidden
    });

    if (isNil(subBatches)) return; // User cancelled

    const children = subBatches.filter(b => BatchGroup.equals(parent, b.parentGroup));

    // Update the parent observed individual count
    parent.observedIndividualCount = BatchUtils.sumObservedIndividualCount(children);

    // Return the updated parent
    return parent;
  }


  protected async openSubBatchesModal(parentGroup?: BatchGroup, opts?: {
    showParent?: boolean;
  }): Promise<SubBatch[] | undefined> {

    // DEBUG
    //if (this.debug) console.debug('[batches-table] Open individual measures modal...');

    const showParentGroup = !opts || opts.showParent !== false; // True by default

    // Define a function to add new parent
    const onNewParentClick = showParentGroup ? async () => {
      const newParent = await this.openDetailModal();
      if (newParent) {
        await this.addEntityToTable(newParent, {confirmCreate: false});
      }
      return newParent;
    } : undefined;

    // Define available parent, as an observable (if new parent can added)
    // - If mobile, create an observable, linked to table rows
    // - else (if desktop), create a copy
    const onModalDismiss = new Subject<any>();
    const availableParents = (showParentGroup ? this.dataSource.connect(null) : defer(() => this.dataSource.getRows()))
      .pipe(
        takeUntil(onModalDismiss),
        map((res: TableElement<BatchGroup>[]) => res.map(row => this.toEntity(row)))
      );

    const hasTopModal = !!(await this.modalCtrl.getTop());
    const modal = await this.modalCtrl.create({
      component: SubBatchesModal,
      componentProps: <ISubBatchesModalOptions>{
        programLabel: this.programLabel,
        acquisitionLevel: AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL,
        usageMode: this.usageMode,
        showParentGroup,
        parentGroup,
        qvPmfm: this.qvPmfm,
        disabled: this.disabled,
        // Scientific species is required, only not already set in batch groups
        showTaxonNameColumn: !this.showTaxonNameColumn,
        // If on field mode: use individualCount=1 on each sub-batches
        showIndividualCount: !this.settings.isOnFieldMode(this.usageMode),
        availableParents,
        data: this.availableSubBatches,
        onNewParentClick,
        // Override using input options
        maxVisibleButtons: this.modalOptions?.maxVisibleButtons
      },
      backdropDismiss: false,
      keyboardClose: true,
      cssClass: hasTopModal ? 'modal-large stack-modal' : 'modal-large'
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();

    onModalDismiss.next(); // disconnect observables

    // User cancelled
    if (isNil(data)) {
      if (this.debug) console.debug('[batches-table] Sub-batches modal: user cancelled');
    } else {
      // DEBUG
      //if (this.debug) console.debug('[batches-table] Sub-batches modal result: ', data);

      this.onSubBatchesChanges.emit(data);
    }

    return data;
  }

  protected async openDetailModal(batchGroup?: BatchGroup): Promise<BatchGroup | undefined> {
    const isNew = !batchGroup && true;
    if (isNew) {
      batchGroup = new BatchGroup();
      await this.onNewEntity(batchGroup);
    }

    this.markAsLoading();

    const modal = await this.modalCtrl.create({
      component: BatchGroupModal,
      backdropDismiss: false,
      componentProps: <IBatchGroupModalOptions>{
        acquisitionLevel: this.acquisitionLevel,
        pmfms: this._initialPmfms,
        qvPmfm: this.qvPmfm,
        disabled: this.disabled,
        data: batchGroup,
        isNew,
        showTaxonGroup: this.showTaxonGroupColumn,
        showTaxonName: this.showTaxonNameColumn,
        availableTaxonGroups: this.availableTaxonGroups,
        taxonGroupsNoWeight: this.taxonGroupsNoWeight,
        showSamplingBatch: this.showSamplingBatchColumns,
        allowSubBatches: this.allowSubBatches,
        defaultHasSubBatches: this.defaultHasSubBatches,
        openSubBatchesModal: (batchGroup) => this.openSubBatchesModalFromParentModal(batchGroup),
        onDelete: (event, batchGroup) => this.deleteEntity(event, batchGroup),
        // Override using given options
        ...this.modalOptions
      },
      cssClass: 'modal-large',
      keyboardClose: true
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug('[batch-group-table] Batch group modal result: ', JSON.stringify(data));
    this.markAsLoaded();

    return data instanceof BatchGroup ? data : undefined;
  }

  async deleteEntity(event: UIEvent, data: BatchGroup): Promise<boolean> {
    const row = await this.findRowByEntity(data);

    // Row not exists: OK
    if (!row) return true;

    const canDeleteRow = await this.canDeleteRows([row]);
    if (canDeleteRow === true) {
      this.cancelOrDelete(event, row, {interactive: false /*already confirmed*/});
    }
    return canDeleteRow;
  }

  async openSelectColumnsModal(event?: UIEvent) {

    let userColumns = this.getUserColumns();
    const hiddenColumns = DEFAULT_USER_COLUMNS.slice(0)
      .filter(name => userColumns.indexOf(name) === -1);
    let columns = (userColumns || [])
      .concat(hiddenColumns)
      .map(name => {
        const label = this.i18nColumnPrefix + changeCaseToUnderscore(name).toUpperCase();
        return {
          name,
          label,
          visible: userColumns.indexOf(name) !== -1
        } as ColumnItem;
      });

    const modal = await this.modalCtrl.create({
      component: TableSelectColumnsComponent,
      componentProps: {
        columns,
        canHideColumns: false
      }
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res || !res.data) return; // CANCELLED
    columns = res.data as ColumnItem[];

    // Update columns
    userColumns = columns.filter(c => c.visible).map(c => c.name) || [];

    // Update user settings
    await this.settings.savePageSetting(this.settingsId, userColumns, SETTINGS_DISPLAY_COLUMNS);

    this.updateColumns();
  }

  protected async findRowByEntity(batchGroup: BatchGroup): Promise<TableElement<BatchGroup>> {
    return batchGroup && (await this.dataSource.getRows()).find(r => BatchGroup.equals(r.currentData, batchGroup));
  }

  /**
   * Update the batch group row (e.g. observed individual count), from subbatches
   * @param row
   * @param subBatches
   */
  protected updateBatchGroupRow(row: TableElement<BatchGroup>, subBatches: SubBatch[]): BatchGroup {
    const parent: BatchGroup = row && row.currentData;
    if (!parent) return; // skip

    const updatedParent = this.prepareBatchGroupToRow(parent, subBatches || []);

    if (row.validator) {
      row.validator.patchValue(updatedParent, {emitEvent: false});
    } else {
      row.currentData = updatedParent;
    }

    return updatedParent;
  }

  /**
   * Update the batch group row (e.g. observed individual count), from subbatches
   * @param parent
   * @param subBatches
   */
  protected prepareBatchGroupToRow(parent: BatchGroup, subBatches: SubBatch[]): BatchGroup {
    if (!parent) return parent; // skip

    const children = (subBatches || []).filter(b => Batch.equals(parent, b.parentGroup));

    if (this.debug) console.debug('[batch-group-table] Computing individual count...');

    if (!this.qvPmfm) {
      console.warn('TODO: check this implementation (computing individual count when NO QV pmfm)');
      parent.observedIndividualCount = BatchUtils.sumObservedIndividualCount(children);
    } else {
      let observedIndividualCount = 0;
      this.qvPmfm.qualitativeValues.forEach((qv, qvIndex) => {

        const qvChildren = children.filter(c => {
          const qvValue = c.measurementValues[this.qvPmfm.id];
          // WARN: use '==' a NOT '===' because id can be serialized as string
          // tslint:disable-next-line:triple-equals
          return qvValue && (qvValue == qv.id || qvValue.id == qv.id);
        });
        const samplingIndividualCount = BatchUtils.sumObservedIndividualCount(qvChildren);
        const qvOffset = (qvIndex * BatchGroupsTable.BASE_DYNAMIC_COLUMNS.length);
        const hasSampling = !!(parent.measurementValues[qvOffset + 2] || parent.measurementValues[qvOffset + 3]);
        parent.measurementValues[qvOffset + 4] = hasSampling || samplingIndividualCount ? samplingIndividualCount : undefined;
        observedIndividualCount += (samplingIndividualCount || 0);
      });
      parent.observedIndividualCount = observedIndividualCount;
    }

    return parent;
  }

  protected async loadAvailableTaxonGroups(opts?: { defaultTaxonGroups?: string[] }): Promise<TaxonGroupRef[]> {
    if (!this.programLabel) return;
    const defaultTaxonGroups = opts && opts.defaultTaxonGroups || this._defaultTaxonGroups || null;
    console.debug('[batch-group-table] Loading available taxon groups, using options:', opts);

    const sortAttributes = this.autocompleteFields.taxonGroup && this.autocompleteFields.taxonGroup.attributes || ['label', 'name'];
    const taxonGroups = ((await this.programRefService.loadTaxonGroups(this.programLabel)) || [])
      // Filter on expected labels (as prefix)
      .filter(taxonGroup => !defaultTaxonGroups || taxonGroup.label && defaultTaxonGroups.findIndex(label => taxonGroup.label.startsWith(label)) !== -1)
      // Sort using order configure in the taxon group column
      .sort(propertiesPathComparator(sortAttributes));

    this.availableTaxonGroups = isNotEmptyArray(taxonGroups) ? taxonGroups : undefined;

    return taxonGroups;
  }

  protected getPmfmFakeIds(id: number, qvIndex: number): number {
    // use negative id to be sure it's not a real pmfm id's
    return id * (-qvIndex - 1);
  }

  protected async clearSamplingColumns() {
    if (!this.dynamicColumns) return;

    const rows = await this.dataSource.getRows();

    if (rows && rows.length) {
      const values = {};
      this.dynamicColumns.forEach((col, index) => {
        if (col.isSampling) {
          values[index] = undefined;
        }
      });

      rows.forEach(r => {
        r.validator.get('measurementValues').patchValue(values);
      });

    }
  }

  protected async onNewEntity(data: BatchGroup): Promise<void> {
    console.debug('[sample-table] Initializing new row data...');

    await super.onNewEntity(data);

    // generate label
    data.label = `${this.acquisitionLevel}#${data.rankOrder}`;

    // Default taxon name
    if (isNotNil(this.defaultTaxonName)) {
      data.taxonName = TaxonNameRef.fromObject(this.defaultTaxonName);
    }
    // Default taxon group
    if (isNotNil(this.defaultTaxonGroup)) {
      data.taxonGroup = TaxonGroupRef.fromObject(this.defaultTaxonGroup);
    }

    if (this.qvPmfm) {
      data.children = (this.qvPmfm && this.qvPmfm.qualitativeValues || []).reduce((res, qv, qvIndex: number) => {

        const childLabel = qv ? `${data.label}.${qv.label}` : data.label;

        // If qv, add sub level at sorting batch for each qv value
        // If no qv, keep measurements in sorting batch level
        const child: Batch = !qv ? data : isNotNil(data.id) && (data.children || []).find(b => b.label === childLabel) || new Batch();

        child.rankOrder = qvIndex + 1;
        child.measurementValues = {};
        child.label = childLabel;

        // If sampling
        if (this.showSamplingBatchColumns) {
          const samplingLabel = childLabel + Batch.SAMPLING_BATCH_SUFFIX;
          const samplingChild: Batch = new Batch();
          samplingChild.rankOrder = 1;
          samplingChild.label = samplingLabel;
          samplingChild.measurementValues = {};
          child.children = [samplingChild];
        }
        // Remove children
        else {
          child.children = [];
        }

        return res.concat(child);
      }, []);
    }
  }
}

