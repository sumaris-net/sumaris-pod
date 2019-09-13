import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit
} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {Batch, BatchUtils, BatchWeight} from "../services/model/batch.model";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ProgramService} from "../../referential/services/program.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {
  AcquisitionLevelCodes, EntityUtils,
  IReferentialRef,
  ReferentialRef,
  referentialToString,
  UsageMode
} from "../../core/services/model";
import {debounceTime, filter, first, map, switchMap, tap, throttleTime} from "rxjs/operators";
import {
  isNil,
  isNotNil,
  MethodIds,
  PmfmLabelPatterns,
  PmfmStrategy, PmfmUtils,
  TaxonGroupIds
} from "../../referential/services/model";
import {BehaviorSubject, merge, Observable, Subscription} from "rxjs";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {SpeciesBatchValidatorService} from "../services/validator/species-batch.validator";
import {AppFormUtils, FormArrayHelper, PlatformService} from "../../core/core.module";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {isNilOrBlank, isNotNilOrBlank, isNotNilOrNaN, toBoolean, toInt} from "../../shared/functions";
import {FormFieldValue} from "../../shared/form/field.model";
import {BatchValidatorService} from "../services/batch.validator";

@Component({
  selector: 'app-batch-form',
  templateUrl: 'batch.form.html',
  styleUrls: ['batch.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchForm extends MeasurementValuesForm<Batch>
  implements OnInit, OnDestroy {

  defaultWeightPmfm: PmfmStrategy;
  weightPmfms: PmfmStrategy[];
  weightPmfmsByMethod: { [key: string]: PmfmStrategy };
  isSampling: boolean;
  mobile: boolean;

  @Input() tabindex: number;

  @Input() usageMode: UsageMode;

  @Input() showTaxonGroup = true;

  @Input() showTaxonName = true;

  @Input() showTotalIndividualCount = false;

  @Input() showIndividualCount = false;

  @Input() showSampleBatch: boolean = false;

  @Input() showError = true;

  $allPmfms = new BehaviorSubject<PmfmStrategy[]>(null);

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef,
    protected validatorService: BatchValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd,
      null, // Allow to be set by parent component
      {
        mapPmfms: (pmfms) => this.mapPmfms(pmfms),
        onUpdateControls: (form) => this.onUpdateControls(form)
      });
    this.mobile = platform.mobile;

      // Set default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;
    this._enable = true;

    // for DEV only
    this.debug = !environment.production;
  }

  ngOnInit() {
    this.form = this.form || this.validatorService.getFormGroup();

    super.ngOnInit();

    console.debug("[batch-form] Init form");

    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;

    // Taxon group combo
    this.registerAutocompleteConfig('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options)
    });
    // Taxon name combo
    this.registerAutocompleteConfig('taxonName', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonNames(value, options)
    });
  }

  public setValue(data: Batch) {

    // Fill weight
    if (this.defaultWeightPmfm) {
      const weightPmfm = (this.weightPmfms || []).find(p => isNotNil(data.measurementValues[p.pmfmId.toString()]));
      data.weight = {
        methodId: weightPmfm && weightPmfm.methodId,
        calculated: false,
        estimated: weightPmfm && weightPmfm.methodId === MethodIds.ESTIMATED_BY_OBSERVER,
        value : weightPmfm && data.measurementValues[weightPmfm.pmfmId.toString()],
      };
      console.log("TODO check weight=" + data.weight)

      // Make sure the weight is fill only in the default weight pmfm
      this.weightPmfms.forEach(p => {
        delete data.measurementValues[p.pmfmId.toString()];
        this.form.removeControl(p.pmfmId.toString());
      });
    }

    // Adapt measurement values to form
    MeasurementValuesUtils.normalizeEntityToForm(data, this.$allPmfms.getValue(), this.form);

    const childrenFormHelper = this.getChildrenFormHelper(this.form);

    if (this.showSampleBatch) {

      const samplingBatch = BatchUtils.getOrCreateSamplingChild(data);
      this.setIsSampling(BatchUtils.isSampleNotEmpty(samplingBatch));

      childrenFormHelper.resize(1);
      const samplingFormGroup = childrenFormHelper.at(0) as FormGroup;

      // Adapt measurement values to form
      MeasurementValuesUtils.normalizeEntityToForm(samplingBatch, [], samplingFormGroup);

      if (!samplingFormGroup.get('weight')) {
        samplingFormGroup.addControl('weight', this.validatorService.getWeightFormGroup());
      }

      // Read child weight (use the first one)
      if (this.defaultWeightPmfm) {
        const samplingWeightPmfm = (this.weightPmfms || []).find(p => isNotNil(samplingBatch.measurementValues[p.pmfmId.toString()]));
        samplingBatch.weight = {
          methodId: samplingWeightPmfm && samplingWeightPmfm.methodId,
          calculated: false,
          estimated: samplingWeightPmfm && samplingWeightPmfm.methodId === MethodIds.ESTIMATED_BY_OBSERVER,
          value: samplingWeightPmfm && samplingBatch.measurementValues[samplingWeightPmfm.pmfmId.toString()],
        };
        console.log("TODO check weight=" + samplingBatch.weight)
      }
    }

    console.log(data.label + " TODO Check setValue()", data)

    super.setValue(data);
  }

  protected getValue(): Batch {
    if (!this.dirty) return this.data;

    const json = this.form.value;

    // Convert weight into measurement
    if (this.defaultWeightPmfm && json.weight && isNotNil(json.weight.value)) {
      const weightPmfm = this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER] || this.defaultWeightPmfm;
      json.measurementValues[weightPmfm.pmfmId.toString()] = json.weight.value;
    }

    // Convert measurements
    json.measurementValues = Object.assign({}, this.data.measurementValues, MeasurementValuesUtils.normalizeValuesToModel(json.measurementValues, this.$allPmfms.getValue()));
    this.data.fromObject(json);

    if (this.showSampleBatch) {
      const child = BatchUtils.getOrCreateSamplingChild(this.data);
      this.setIsSampling(BatchUtils.isSampleNotEmpty(child));

      if (this.isSampling) {
        const childJson = json.children && json.children[0] || {};

        // Convert weight into measurement
        if (childJson.weight && isNotNil(childJson.weight.value)) {
          const childWeightPmfm = childJson.weight.estimated && this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER] || this.defaultWeightPmfm;
          childJson.measurementValues[childWeightPmfm.pmfmId.toString()] = childJson.weight.value;
        }

        // Convert measurements
        childJson.measurementValues = Object.assign({}, child.measurementValues, MeasurementValuesUtils.normalizeValuesToModel(childJson.measurementValues, this.weightPmfms));

        // Convert sampling ratio
        if (isNotNilOrBlank(childJson.samplingRatio)) {
          childJson.samplingRatioText = `${childJson.samplingRatio}%`;
          childJson.samplingRatio = +childJson.samplingRatio / 100;
        }

        child.fromObject(childJson);
        this.data.children = [child];
      }
      else {
        this.data.children = [];
      }
    }

    console.log('TODO batch Form getValue() = ', this.data)

    return this.data;
  }

  setIsSampling(enable: boolean, form?: FormGroup) {
    this.isSampling = enable;

    const childrenArray = (form || this.form).get('children') as FormArray;

    if (childrenArray) {
      if (enable && childrenArray.disabled) {
        childrenArray.enable({emitEvent: false});
      } else if (!enable && childrenArray.enabled) {
        childrenArray.disable({emitEvent: false});
      }
    }
  }

  /* -- protected methods -- */

  // Wait form controls ready
  async onReady(): Promise<void> {
    await super.onReady();

    // Wait pmfms to be loaded
    if (isNil(this.$allPmfms.getValue())) {
      await this.$allPmfms.pipe(
        filter(isNotNil),
        first()
      ).toPromise();
    }
  }

  protected async suggestTaxonGroups(value: any, options?: any): Promise<IReferentialRef[]> {
    return this.programService.suggestTaxonGroups(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute
      });
  }

  protected async suggestTaxonNames(value: any, options?: any): Promise<IReferentialRef[]> {
    const taxonGroup = this.form.get('taxonGroup').value;

    // IF taxonGroup column exists: taxon group must be filled first
    if (this.showTaxonGroup && isNilOrBlank(value) && isNil(parent)) return [];

    return this.programService.suggestTaxonNames(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute,
        taxonGroupId: taxonGroup && taxonGroup.id || undefined
      });
  }

  protected mapPmfms(pmfms: PmfmStrategy[]) {

    // Read weight PMFMs
    this.weightPmfms = pmfms.filter(p => PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label));
    this.defaultWeightPmfm = this.weightPmfms.length && this.weightPmfms[0] || undefined;
    this.weightPmfmsByMethod = {};
    this.weightPmfms.forEach(p => this.weightPmfmsByMethod[p.methodId] = p);

    this.showSampleBatch = toBoolean(this.showSampleBatch, true);
    this.$allPmfms.next(pmfms);

    // Exclude hidden and weight PMFMs
    return pmfms.filter(p => !PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label) && !p.hidden);
  }

  protected onUpdateControls(form: FormGroup) {

    const childrenFormHelper = this.getChildrenFormHelper(form);

    // Create weight sub form (if need)
    let weightForm = form.get('weight');
    if (this.defaultWeightPmfm) {
      if (!weightForm) {
        weightForm = this.validatorService.getWeightFormGroup();
        form.addControl('weight', weightForm);
      }
    }
    else if (weightForm) {
      this.form.removeControl('weight');
    }

    // Add pmfms to form
    let measFormGroup = form.get('measurementValues') as FormGroup;
    if (measFormGroup) {
      this.measurementValidatorService.updateFormGroup(measFormGroup, this.$allPmfms.getValue());
    }

    const hasSamplingForm = childrenFormHelper.size() === 1 && this.defaultWeightPmfm && true;

    // If the sample batch exists
    if (this.showSampleBatch) {

      childrenFormHelper.resize(1);
      const samplingForm = childrenFormHelper.at(0) as FormGroup;

      // Create sampling weight sub form (if need)
      let samplingWeightForm = samplingForm.get('weight');
      if (this.defaultWeightPmfm) {
        // Create weight form
        if (!samplingWeightForm) {
          samplingWeightForm = this.validatorService.getWeightFormGroup();
          samplingForm.addControl('weight', samplingWeightForm);
        }
      }
      else if (samplingWeightForm) {
        samplingForm.removeControl('weight');
      }

      // Reset measurementValues (if exists)
      let samplingMeasFormGroup = samplingForm.get('measurementValues');
      if (samplingMeasFormGroup) {
        this.measurementValidatorService.updateFormGroup(samplingMeasFormGroup as FormGroup, []);
      }

      // Adapt exists sampling child, if any
      if (this.data) {
        const samplingChildBatch = BatchUtils.getOrCreateSamplingChild(this.data);

        this.setIsSampling(BatchUtils.isSampleNotEmpty(samplingChildBatch));

        // Adapt measurement values to reactive form
        // MeasurementValuesUtils.normalizeEntityToForm(samplingChildBatch, [], samplingFormGroup);
        //
        // samplingMeasFormGroup.patchValue(samplingChildBatch.measurementValues, {
        //   onlySelf: true,
        //   emitEvent: false
        // });
      } else {
        // No data: disable sampling
        this.setIsSampling(false);
      }

      // TODO: add async validators:
      //  e.g. sampling weight < total weight
    }

    // Remove existing sample, if exists but showSample=false
    else if (hasSamplingForm) {
      childrenFormHelper.resize(0);
    }
  }

  referentialToString = referentialToString;
  selectInputContent = AppFormUtils.selectInputContent;

  protected getChildrenFormHelper(form: FormGroup): FormArrayHelper<Batch> {
    return new FormArrayHelper<Batch>(
      this.formBuilder,
      form,
      'children',
      (value) => this.validatorService.getFormGroup(),
      (v1, v2) => EntityUtils.equals(v1, v2),
      (value) => isNil(value),
      {allowEmptyArray: true}
    );
  }
}
