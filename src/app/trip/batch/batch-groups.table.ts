import {ChangeDetectionStrategy, Component, Injector} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {Batch, PmfmStrategy} from "../services/trip.model";
import {BatchGroupValidatorService} from "../services/trip.validators";
import {FormGroup, Validators} from "@angular/forms";
import {BATCH_RESERVED_END_COLUMNS, BATCH_RESERVED_START_COLUMNS, BatchesTable, BatchFilter} from "./batches.table";
import {isNil, isNilOrBlank, isNotNil, toFloat, toInt} from "../../shared/shared.module";
import {MethodIds} from "../../referential/services/model";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {environment} from "../../../environments/environment";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {ModalController} from "@ionic/angular";
import {BatchWeight} from "../services/model/batch.model";
import {TableSelectColumnsComponent} from "../../core/table/table-select-columns.component";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS, SETTINGS_DISPLAY_COLUMNS} from "../../core/table/table.class";
import {IReferentialRef} from "../../core/services/model";
import {isNotNilOrNaN} from "../../shared/functions";
import {BatchModal} from "./batch.modal";
import {BatchGroupModal} from "./batch-group.modal";

const DEFAULT_USER_COLUMNS =["weight", "individualCount"];

@Component({
  selector: 'app-batch-groups-table',
  templateUrl: 'batch-groups.table.html',
  styleUrls: ['batch-groups.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: BatchGroupValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchGroupsTable extends BatchesTable {

  protected modalCtrl: ModalController;

  weightMethodForm: FormGroup;
  estimatedWeightPmfm: PmfmStrategy;


  constructor(
    injector: Injector
  ) {
    super(injector,
      injector.get(ValidatorService),
      new InMemoryTableDataService<Batch, BatchFilter>(Batch, {
        onLoad: (data) => this.onLoad(data),
        onSave: (data) => this.onSave(data),
      })
    );
    this.modalCtrl = injector.get(ModalController);
    this.inlineEdition = !this.mobile;
    this.allowRowDetail = !this.inlineEdition;
    this.detailModal = BatchGroupModal;

    // Set default values
    this.showCommentsColumn = false;

  }

  async ngOnInit(): Promise<void> {
    // -- For DEV only
    this.debug = !environment.production;

    await super.ngOnInit();

    // Taxon group combo
    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options)
    });

    // Taxon name combo
    this.registerAutocompleteField('taxonName', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonNames(value, options)
    });
  }

  onLoad(data: Batch[]): Batch[] {
    if (isNil(this.qvPmfm) || !this.qvPmfm.qualitativeValues) return data; // Skip (pmfms not loaded)

    if (this.debug) console.debug("[batch-group-table] Preparing data to be loaded as table rows...");

    const pmfms = this._initialPmfms;

    let weightMethodValues = this.qvPmfm.qualitativeValues.reduce((res, qv, qvIndex) => {
      res[qvIndex] = false;
      return res;
    }, {});

    // Transform entities into object array
    data = data.map(batch => {
      const measurementValues = {};
      // For each group (one by qualitative value)
      this.qvPmfm.qualitativeValues.forEach((qv, qvIndex) => {
        const child = (batch.children || []).find(c => c.label === `${batch.label}.${qv.label}`);
        if (child) {

          this.getFakeMeasurementValuesFromQvChild(child, measurementValues, qvIndex);

          // Remember method used for the weight (estimated or not)
          if (!weightMethodValues[qvIndex]) {
            if (child.weight && child.weight.estimated) {
              weightMethodValues[qvIndex] = true;
            }
            else if (child.children && child.children.length === 1) {
              const samplingChild = child.children[0];
              weightMethodValues[qvIndex] = samplingChild.weight && samplingChild.weight.estimated;
            }
          }
        }
      });

      // Make entity compatible with reactive form
      batch.measurementValues = measurementValues;
      MeasurementValuesUtils.normalizeEntityToForm(batch, pmfms);

      return batch;
    });

    // Set weight is estimated ?
    if (this.weightMethodForm) {
      this.weightMethodForm.patchValue(weightMethodValues);
    }

    return data;
  }


  async onSave(data: Batch[]): Promise<Batch[]> {
    if (isNil(this.qvPmfm) || !this.qvPmfm.qualitativeValues) return data; // Skip (pmfms not loaded)

    if (this.debug) console.debug("[batch-group-table] Preparing data to be saved...");
    data = data.map(batch => {
      this.prepareEntityToSave(batch);
      return batch;
    });

    return data;
  }

  async onSubBatchesClick(event: UIEvent, row: TableElement<Batch>, qvIndex?: number): Promise<void> {
    if (event) event.preventDefault();

    let parentBatch = row.validator ? Batch.fromObject(row.currentData) : row.currentData;

    const defaultBatch = new Batch();
    defaultBatch.parent = parentBatch;
    defaultBatch.parentId = parentBatch.id;

    if (isNotNil(qvIndex) && this.qvPmfm) {
      const qv = this.qvPmfm.qualitativeValues[qvIndex];
      const qvPmfmId = this.qvPmfm.pmfmId.toString();
      defaultBatch.measurementValues[qvPmfmId] = qv.id.toString();
    }

    await this.openSubBatchesModal(defaultBatch);
  }

  /* -- protected methods -- */

  protected async suggestTaxonGroups(value: any, options?: any): Promise<IReferentialRef[]> {
    //if (isNilOrBlank(value)) return [];
    return this.programService.suggestTaxonGroups(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute
      });
  }

  protected async suggestTaxonNames(value: any, options?: any): Promise<IReferentialRef[]> {
    const taxonGroup = this.editedRow && this.editedRow.validator.get('taxonGroup').value;

    // IF taxonGroup column exists: taxon group must be filled first
    if (this.showTaxonGroupColumn && isNilOrBlank(value) && isNil(parent)) return [];

    return this.programService.suggestTaxonNames(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute,
        taxonGroupId: taxonGroup && taxonGroup.id || undefined
      });
  }

  protected normalizeEntityToRow(data: Batch, row: TableElement<Batch>) {
    // When batch has the QV value
    if (this.qvPmfm) {

      const measurementValues = Object.assign({}, row.currentData.measurementValues);
      // For each group (one by qualitative value)
      this.qvPmfm.qualitativeValues.forEach((qv, qvIndex) => {
        const child = (data.children || []).find(c => c.label === `${data.label}.${qv.label}` || c.measurementValues[this.qvPmfm.pmfmId] == qv.id);
        if (child) {

          // Replace measurement values inside a new map, based on fake pmfms
          this.getFakeMeasurementValuesFromQvChild(child, measurementValues, qvIndex);
        }
        else {
          console.warn("Unable to find child for QV value: " + (qv.label || qv.name));
        }
      });
      data.measurementValues = measurementValues;
    }

    // Inherited method
    super.normalizeEntityToRow(data, row);

  }

  protected getFakeMeasurementValuesFromQvChild(data: Batch, measurementValues?: {[key: string]: any}, qvIndex?: number): {[key: string]: any} {
    if (!data) return measurementValues; // skip

    if (isNil(qvIndex)) {
      const qvId = this.qvPmfm && data.measurementValues[this.qvPmfm.pmfmId];
      qvIndex = isNotNil(qvId) && this.qvPmfm.qualitativeValues.findIndex(qv => qv.id === +qvId);
      if (qvIndex === -1) throw Error("Invalid batch: no QV value");
    }

    measurementValues = measurementValues || {};
    let i = qvIndex * 5;

    // Column: total weight
    data.weight = this.getWeight(data.measurementValues) || undefined;
    measurementValues[i++] = data.weight && !data.weight.calculated && data.weight.value || null;

    // Column: individual count
    measurementValues[i++] = isNotNil(data.individualCount) ? data.individualCount : null;

    // Sampling batch
    if (data.children && data.children.length === 1) {
      const samplingChild = data.children[0];
      // Column: sampling ratio
      measurementValues[i++] = isNotNil(samplingChild.samplingRatio) ? samplingChild.samplingRatio * 100 : null;

      // Column: sampling weight
      samplingChild.weight = this.getWeight(samplingChild.measurementValues);
      measurementValues[i++] = samplingChild.weight && !samplingChild.weight.calculated && samplingChild.weight.value;

      // Column: sampling individual count
      measurementValues[i++] = isNotNil(samplingChild.individualCount) ? samplingChild.individualCount : null;
    }
    return measurementValues;
  }

  protected prepareEntityToSave(batch: Batch) {
    const groupColumnValues = batch.measurementValues;
    batch.measurementValues = {};

    batch.children = (this.qvPmfm && this.qvPmfm.qualitativeValues || []).reduce((res, qv, qvIndex: number) => {
      let i = qvIndex * 5;
      const weight = toFloat(groupColumnValues[i++]);
      const individualCount = toInt(groupColumnValues[i++]);
      const samplingRatio = toInt(groupColumnValues[i++]);
      const samplingWeight = toFloat(groupColumnValues[i++]);
      const samplingIndividualCount = toFloat(groupColumnValues[i++]);

      // TODO: compute total weight and nb indiv ?

      const isEstimatedWeight = this.weightMethodForm && this.weightMethodForm.controls[qvIndex].value || false;
      const weightPmfmId = isEstimatedWeight ? this.estimatedWeightPmfm.pmfmId : this.defaultWeightPmfm.pmfmId;

      const childLabel = `${batch.label}.${qv.label}`;
      const child: Batch = batch.id && (batch.children || []).find(b => b.label === childLabel) || new Batch();
      child.rankOrder = qvIndex + 1;
      child.measurementValues = {};
      child.measurementValues[this.qvPmfm.pmfmId.toString()] = qv.id.toString();
      child.measurementValues[weightPmfmId.toString()] = isNotNilOrNaN(weight) ? weight : undefined;
      child.individualCount = individualCount;
      child.label = childLabel;

      // If sampling
      if (isNotNil(samplingRatio) || isNotNil(samplingIndividualCount) || isNotNil(samplingWeight)) {
        const samplingLabel = childLabel + Batch.SAMPLE_BATCH_SUFFIX;
        const samplingChild: Batch = child.id && (child.children || []).find(b => b.label === samplingLabel) || new Batch();
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

      return res.concat(child);
    }, []);
  }

  // Override parent function
  protected mapPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    if (!pmfms || !pmfms.length) return pmfms; // Skip (no pmfms)

    super.mapPmfms(pmfms);

    // Check PMFM
    if (isNil(this.qvPmfm)) {
      throw new Error(`[batch-group-table] table not ready without a root qualitative PMFM`);
    }
    if (this.debug) console.debug('[batch-group-table] First qualitative PMFM found: ' + this.qvPmfm.label);

    if (isNil(this.defaultWeightPmfm) || this.defaultWeightPmfm.rankOrder < this.qvPmfm.rankOrder) {
      throw new Error(`[batch-group-table] Unable to construct the table. First qualitative value PMFM must be define BEFORE any weight PMFM (by rankOrder in PMFM strategy - acquisition level ${this.acquisitionLevel})`);
    }

    // If estimated weight is allow, init a form for weight methods
    if (!this.weightMethodForm && this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER]) {

      // Create the form, for each QV value
      this.weightMethodForm = this.formBuilder.group(this.qvPmfm.qualitativeValues.reduce((res, qv, index) => {
        res[index] = [false, Validators.required];
        return res;
      }, {}));

      // Listening changes
      this.registerSubscription(
        this.weightMethodForm.valueChanges.subscribe(json => {
          this._dirty = true;
        }));
    }

    this.estimatedWeightPmfm = this.weightPmfmsByMethod && this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER] || this.defaultWeightPmfm;

    const translations = this.translate.instant([
      'TRIP.BATCH.TABLE.TOTAL_WEIGHT',
      'TRIP.BATCH.TABLE.TOTAL_INDIVIDUAL_COUNT',
      'TRIP.BATCH.TABLE.SAMPLING_RATIO',
      'TRIP.BATCH.TABLE.SAMPLING_WEIGHT',
      'TRIP.BATCH.TABLE.SAMPLING_INDIVIDUAL_COUNT']);
    const columnPmfms: PmfmStrategy[] = this.qvPmfm.qualitativeValues.reduce((res, qv, index) => {
      return res.concat(
        [
          // Column on total (weight, nb indiv)
          Object.assign({}, this.defaultWeightPmfm, {
            type: 'double', label: qv.label + '_TOTAL_WEIGHT', id: index,
            name: translations['TRIP.BATCH.TABLE.TOTAL_WEIGHT'],
            minValue: 0,
            maxValue: 10000,
            maximumNumberDecimals: 1
          }),
          {
            type: 'integer', label: qv.label + '_TOTAL_INDIVIDUAL_COUNT', id: index,
            name: translations['TRIP.BATCH.TABLE.TOTAL_INDIVIDUAL_COUNT'],
            minValue: 0,
            maxValue: 10000,
            maximumNumberDecimals: 0
          },

          // Column on sampling (ratio, nb indiv, weight)
          {
            type: 'integer', label: qv.label + '_SAMPLING_RATIO', id: index,
            name: translations['TRIP.BATCH.TABLE.SAMPLING_RATIO'],
            unit: '%',
            minValue: 0,
            maxValue: 100,
            maximumNumberDecimals: 0
          },
          Object.assign({}, this.defaultWeightPmfm, {
            type: 'double', label: qv.label + '_SAMPLING_WEIGHT', id: index,
            name: translations['TRIP.BATCH.TABLE.SAMPLING_WEIGHT'],
            minValue: 0,
            maxValue: 1000,
            maximumNumberDecimals: 1
          }),
          {
            type: 'integer', label: qv.label + '_SAMPLING_INDIVIDUAL_COUNT', id: index,
            name: translations['TRIP.BATCH.TABLE.SAMPLING_INDIVIDUAL_COUNT'],
            minValue: 0,
            maxValue: 1000,
            maximumNumberDecimals: 0
          }
        ]
      );
    }, [])
      .map((pmfm, index) => {
        // Set fake pmfmId (as index in array)
        pmfm.pmfmId = index;
        return PmfmStrategy.fromObject(pmfm);
      });

    return columnPmfms;
  }

  protected isEven(pmfm: PmfmStrategy) {
    const qvIndex = Math.trunc(pmfm.pmfmId / 5);
    return (qvIndex % 2 === 0);
  }

  protected isOdd(pmfm: PmfmStrategy) {
    const qvIndex = Math.trunc(pmfm.pmfmId / 5);
    return (qvIndex % 2 !== 0);
  }

  protected getWeight(measurementValues: { [key: string]: any }): BatchWeight | undefined {
    // Use try default method
    let value = measurementValues[this.defaultWeightPmfm.pmfmId];
    if (isNotNil(value)) {
      return {
        value: value,
        estimated: false,
        calculated: false,
        methodId: this.defaultWeightPmfm.methodId
      };
    }
    if (!this.weightPmfmsByMethod) return undefined;

    // Else, try to get estimated
    let weightPmfm = this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER];
    value = weightPmfm && measurementValues[weightPmfm.pmfmId];
    if (isNotNil(value)) {
      return {
        value: value,
        estimated: true,
        calculated: false,
        methodId: MethodIds.ESTIMATED_BY_OBSERVER
      };
    }

    // Else, try to get calculated
    weightPmfm = this.weightPmfmsByMethod[MethodIds.CALCULATED];
    value = weightPmfm && measurementValues[weightPmfm.pmfmId];
    if (isNotNil(value)) {
      return {
        value: value,
        estimated: false,
        calculated: true,
        methodId: MethodIds.CALCULATED
      };
    }

    return undefined;
  }

  disable() {
    super.disable();
    if (this.weightMethodForm) this.weightMethodForm.disable({onlySelf: true, emitEvent: false});
  }

  enable() {
    super.enable();
    if (this.weightMethodForm) this.weightMethodForm.enable({onlySelf: true, emitEvent: false});
  }

  markAsPristine() {
    super.markAsPristine();
    if (this.weightMethodForm) this.weightMethodForm.markAsPristine({onlySelf: true});
  }

  markAsTouched() {
    super.markAsTouched();
    if (this.weightMethodForm) this.weightMethodForm.markAsTouched({onlySelf: true});
  }

  markAsUntouched() {
    super.markAsUntouched();
    if (this.weightMethodForm) this.weightMethodForm.markAsUntouched({onlySelf: true});
  }

  // Override default pmfms
  updateColumns(pmfms?: PmfmStrategy[]) {
    pmfms = pmfms || this.$pmfms.getValue();
    if (!pmfms) return; // Pmfm not loaded: skip

    this.displayedColumns = this.getDisplayColumns();
    if (!this.loading) this.markForCheck();
  }

  protected getUserColumns(): string[] {
    const userColumns = this.settings.getPageSettings(this.settingsId, SETTINGS_DISPLAY_COLUMNS);
    // No user override: use defaults
    return userColumns || DEFAULT_USER_COLUMNS.slice(0);
  }
  protected getDisplayColumns(userColumns?: string[]): string[] {
    userColumns = userColumns || this.getUserColumns();

    const weightIndex = userColumns.findIndex(c => c === 'weight');

    let individualCountIndex = userColumns.findIndex(c => c === 'individualCount');
    individualCountIndex = (individualCountIndex !== -1 && weightIndex === -1 ? 0 : individualCountIndex);

    const pmfmColumns = (this.qvPmfm && this.qvPmfm.qualitativeValues || []).reduce((res, qv, index) => {
      let offset = index * 5;

      return res.concat([
        weightIndex !== -1 ? (offset + weightIndex) : -1,
        individualCountIndex !== -1 ? (offset + individualCountIndex) : -1,
        offset + 2,
        weightIndex !== -1 ? (offset + 3 + weightIndex) : -1,
        individualCountIndex !== -1 ? (offset + 3 + individualCountIndex) : -1
      ]);
    }, [])
      // Remove hidden column
      .filter(c => c !== -1)
      .map(colPmfmId => colPmfmId.toString());

    return RESERVED_START_COLUMNS
      .concat(BATCH_RESERVED_START_COLUMNS)
      .concat(pmfmColumns)
      //.concat(this.qvPmfm && this.qvPmfm.qualitativeValues ? ['totalWeight-' + this.qvPmfm.qualitativeValues[0].id] : [])
      .concat(BATCH_RESERVED_END_COLUMNS)
      .concat(RESERVED_END_COLUMNS)
      .filter(name => !this.excludesColumns.includes(name));
  }

  async openSelectColumnsModal() {

    const userColumns = this.getUserColumns();
    const hiddenColumns = DEFAULT_USER_COLUMNS.slice(0)
      .filter(name => userColumns.indexOf(name) == -1);
    const columns = userColumns
      .concat(hiddenColumns)
      .map(name => {
        const label = (name === 'individualCount') ? 'TRIP.BATCH.TABLE.INDIVIDUAL_COUNT' :
          ((name === 'weight') ? 'TRIP.BATCH.TABLE.WEIGHT' : '');
        return {
          name,
          label,
          visible: userColumns.indexOf(name) !== -1
        };
      });

    const modal = await this.modalCtrl.create({
      component: TableSelectColumnsComponent,
      componentProps: {columns: columns}
    });

    // On dismiss
    modal.onDidDismiss()
      .then(async (res) => {
        if (!res || !res.data) return; // CANCELLED
        const columns = res.data;

        // Update columns
        const userColumns = columns && columns.filter(c => c.visible).map(c => c.name) || [];

        // Update user settings
        await this.settings.savePageSetting(this.settingsId, userColumns, SETTINGS_DISPLAY_COLUMNS);

        this.displayedColumns = this.getDisplayColumns(userColumns);

        this.markForCheck();
      });
    return modal.present();
  }
}

