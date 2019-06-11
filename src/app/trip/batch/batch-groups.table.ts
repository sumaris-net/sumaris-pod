import {ChangeDetectionStrategy, Component, Injector} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {Batch, MeasurementUtils, PmfmStrategy} from "../services/trip.model";
import {PmfmLabelPatterns,} from "../../referential/referential.module";
import {BatchGroupsValidatorService} from "../services/trip.validators";
import {FormGroup, Validators} from "@angular/forms";
import {BatchesTable, BatchFilter} from "./batches.table";
import {isNil, isNotNil, toFloat, toInt} from "../../shared/shared.module";
import {MethodIds} from "../../referential/services/model";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {environment} from "../../../environments/environment";
import {PMFM_ID_REGEXP} from "../services/model/measurement.model";


@Component({
  selector: 'table-batch-groups',
  templateUrl: 'batch-groups.table.html',
  styleUrls: ['batch-groups.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: BatchGroupsValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchGroupsTable extends BatchesTable {

  private _initialPmfms: PmfmStrategy[];

  qvPmfm: PmfmStrategy;
  defaultWeightPmfm: PmfmStrategy;
  weightPmfmsByMethod: { [key: string]: PmfmStrategy };
  weightMethodForm: FormGroup;

  constructor(
    injector: Injector
  ) {
    super(injector,
      new InMemoryTableDataService<Batch, BatchFilter>(Batch, {
        onLoad: (data) => this.onLoad(data),
        onSave: (data) => this.onSave(data),
      })
    );

    // Set default values
    this.showCommentsColumn = false;

  }

  async ngOnInit(): Promise<void> {
    // -- For DEV only
    this.debug = !environment.production;

    await super.ngOnInit();

  }

  onLoad(data: Batch[]): Batch[] {
    if (isNil(this.qvPmfm) || !this.qvPmfm.qualitativeValues) return data; // Skip (pmfms not loaded)

    if (this.debug) console.debug("[batch-group-table] Preparing data to be loaded as table rows...");

    const pmfms = this.pmfms.getValue();

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
          // Use as the column index
          let i = qvIndex * 5;

          // Column: individual count
          measurementValues[i++] = child && isNotNil(child.individualCount) ? child.individualCount : null;

          // Column: total weight
          const totalWeight = child && this.getWeight(child.measurementValues);
          measurementValues[i++] = totalWeight && !totalWeight.calculated && totalWeight.value || null;

          // Remember method used for the total weight (estimated or not)
          weightMethodValues[qvIndex] = weightMethodValues[qvIndex] || (totalWeight && totalWeight.estimated);

          if (child.children && child.children.length == 1) {
            const samplingChild = child.children[0];
            // Column: sampling ratio
            measurementValues[i++] = isNotNil(samplingChild.samplingRatio) ? samplingChild.samplingRatio * 100 : null;

            // Column: sampling individual count
            measurementValues[i++] = isNotNil(samplingChild.individualCount) ? samplingChild.individualCount : null;
            const samplingWeightWeight = this.getWeight(samplingChild.measurementValues);

            // Column: sampling weight
            measurementValues[i++] = samplingWeightWeight && !samplingWeightWeight.calculated && samplingWeightWeight.value;

            // Remember method used for the sampling weight (estimated or not)
            weightMethodValues[qvIndex] = weightMethodValues[qvIndex] || (samplingWeightWeight && samplingWeightWeight.estimated);
          }
        }
      });
      batch.measurementValues = MeasurementUtils.normalizeFormValues(measurementValues, pmfms);

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
    const estimatedWeightPmfm = this.weightPmfmsByMethod && this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER] || this.defaultWeightPmfm;

    data = data.map(batch => {
      const groupColumnValues = batch.measurementValues;
      batch.measurementValues = {};

      batch.children = this.qvPmfm.qualitativeValues.reduce((res, qv, qvIndex: number) => {
        let i = qvIndex * 5;
        const individualCount = toInt(groupColumnValues[i++]);
        const weight = toFloat(groupColumnValues[i++]);
        const samplingRatio = toInt(groupColumnValues[i++]);
        const samplingIndividualCount = toFloat(groupColumnValues[i++]);
        const samplingWeight = toFloat(groupColumnValues[i++]);

        // TODO: compute total weight and nb indiv ?


        const isEstimatedWeight = this.weightMethodForm && this.weightMethodForm.controls[qvIndex].value || false;

        const childLabel = `${batch.label}.${qv.label}`;
        const child: Batch = batch.id && (batch.children || []).find(b => b.label === childLabel) || new Batch();
        child.rankOrder = qvIndex + 1;
        child.measurementValues = {};
        child.measurementValues[this.qvPmfm.pmfmId] = qv.id.toString();
        child.measurementValues[isEstimatedWeight ? estimatedWeightPmfm.pmfmId : this.defaultWeightPmfm.pmfmId] = weight;
        child.individualCount = individualCount;
        child.label = childLabel;

        // If sampling
        if (isNotNil(samplingRatio) || isNotNil(samplingIndividualCount) || isNotNil(samplingWeight)) {
          const samplingLabel = `${childLabel}.%`;
          const samplingChild: Batch = child.id && (child.children || []).find(b => b.label === samplingLabel) || new Batch();
          samplingChild.rankOrder = 1;
          samplingChild.label = samplingLabel;
          samplingChild.samplingRatio = isNotNil(samplingRatio) ? samplingRatio / 100 : undefined;
          samplingChild.samplingRatioText = isNotNil(samplingRatio) ? `${samplingRatio}%` : undefined;
          samplingChild.measurementValues = {};
          samplingChild.measurementValues[isEstimatedWeight ? estimatedWeightPmfm.pmfmId : this.defaultWeightPmfm.pmfmId] = samplingWeight;
          samplingChild.individualCount = samplingIndividualCount;
          child.children = [samplingChild];
        }
        // Remove children
        else {
          child.children = [];
        }

        return res.concat(child);
      }, []);

      return batch;
    });

    return data;
  }

  /* -- protected methods -- */

  protected mapPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {
    if (!pmfms || !pmfms.length) return pmfms; // Skip (no pmfms)

    if (this.debug) console.debug('[batch-group-table] Grouping PMFMs...');
    this._initialPmfms = pmfms;

    let weightMinRankOrder: number = undefined;
    let defaultWeightPmfm: PmfmStrategy = undefined;
    this.weightPmfmsByMethod = pmfms.reduce((res, p) => {
      const matches = PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label);
      if (matches) {
        const methodId = p.methodId;
        res[methodId] = p;
        if (isNil(weightMinRankOrder)) weightMinRankOrder = p.rankOrder;
        if (isNil(defaultWeightPmfm)) defaultWeightPmfm = p;
      }
      return res;
    }, {});
    this.defaultWeightPmfm = defaultWeightPmfm;

    // Find the first qualitative PMFM
    this.qvPmfm = pmfms.find(p => p.type === 'qualitative_value');
    if (isNil(this.qvPmfm)) {
      throw new Error(`[batch-group-table] table not ready without a root qualitative PMFM`);
    }
    if (this.debug) console.debug('[batch-group-table] First qualitative PMFM found: ' + this.qvPmfm.label);

    if (isNil(weightMinRankOrder) || weightMinRankOrder < this.qvPmfm.rankOrder) {
      throw new Error(`[batch-group-table] Unable to construct the table. No qualitative value found (before weight) in program PMFMs on acquisition level ${this.acquisitionLevel}`);
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

    const translations = this.translate.instant([
      'TRIP.BATCH.TABLE.TOTAL_INDIVIDUAL_COUNT',
      'TRIP.BATCH.TABLE.TOTAL_WEIGHT',
      'TRIP.BATCH.TABLE.SAMPLING_RATIO',
      'TRIP.BATCH.TABLE.SAMPLING_INDIVIDUAL_COUNT',
      'TRIP.BATCH.TABLE.SAMPLING_WEIGHT']);
    const columnPmfms: PmfmStrategy[] = this.qvPmfm.qualitativeValues.reduce((res, qv, index) => {
      return res.concat(
        [
          // Column on total (nb indiv, weight)
          {
            type: 'integer', label: qv.label + '_TOTAL_INDIVIDUAL_COUNT', id: index,
            name: translations['TRIP.BATCH.TABLE.TOTAL_INDIVIDUAL_COUNT'],
            minValue: 0,
            maxValue: 10000,
            maximumNumberDecimals: 0
          },
          Object.assign({}, this.defaultWeightPmfm, {
            type: 'double', label: qv.label + '_TOTAL_WEIGHT', id: index,
            name: translations['TRIP.BATCH.TABLE.TOTAL_WEIGHT'],
            minValue: 0,
            maxValue: 10000,
            maximumNumberDecimals: 1
          }),
          // Column on sampling (ratio, nb indiv, weight)
          {
            type: 'integer', label: qv.label + '_SAMPLING_RATIO', id: index,
            name: translations['TRIP.BATCH.TABLE.SAMPLING_RATIO'],
            unit: '%',
            minValue: 0,
            maxValue: 100,
            maximumNumberDecimals: 0
          },
          {
            type: 'integer', label: qv.label + '_SAMPLING_INDIVIDUAL_COUNT', id: index,
            name: translations['TRIP.BATCH.TABLE.SAMPLING_INDIVIDUAL_COUNT'],
            minValue: 0,
            maxValue: 1000,
            maximumNumberDecimals: 0
          },
          Object.assign({}, this.defaultWeightPmfm, {
            type: 'double', label: qv.label + '_SAMPLING_WEIGHT', id: index,
            name: translations['TRIP.BATCH.TABLE.SAMPLING_WEIGHT'],
            minValue: 0,
            maxValue: 1000,
            maximumNumberDecimals: 1
          })
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

  protected getWeight(measurementValues: { [key: string]: any }): { methodId: number; estimated: boolean; calculated: boolean; value: any } | undefined {
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

  protected getI18nColumnName(columnName: string): string {
    if (!this.qvPmfm) return super.getI18nColumnName(columnName); // Skip

    // Try to resolve PMFM column
    if (PMFM_ID_REGEXP.test(columnName)) {
      const pmfmIndex = parseInt(columnName);
      const pmfm = (this.pmfms.getValue() || []).find(p => p.pmfmId === pmfmIndex);
      const qvIndex = pmfm.id;
      return `${this.translate.instant(this.qvPmfm.qualitativeValues[qvIndex].name)} > ${this.translate.instant(pmfm.name)}`;

    }

    return super.getI18nColumnName(columnName);
  }
}

