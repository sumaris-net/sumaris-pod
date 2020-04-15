import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from "@angular/core";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {ProgramService} from "../../referential/services/program.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {
  EntityUtils,
  IReferentialRef,
  referentialToString,
  UsageMode
} from "../../core/services/model";
import {filter, first} from "rxjs/operators";
import {
  AcquisitionLevelCodes,
  isNil,
  isNotNil,
  MethodIds,
  PmfmLabelPatterns,
  PmfmStrategy
} from "../../referential/services/model";
import {BehaviorSubject, Subscription} from "rxjs";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {AppFormUtils, FormArrayHelper} from "../../core/core.module";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {isNilOrBlank, isNotNilOrBlank, isNotNilOrNaN, toBoolean} from "../../shared/functions";
import {BatchValidatorService} from "../services/batch.validator";
import {firstNotNilPromise} from "../../shared/observables";
import {PlatformService} from "../../core/services/platform.service";

@Component({
  selector: 'app-batch-form',
  templateUrl: 'batch.form.html',
  styleUrls: ['batch.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchForm<T extends Batch = Batch> extends MeasurementValuesForm<T>
  implements OnInit, OnDestroy {

  protected $initialized = new BehaviorSubject<boolean>(false);
  protected _requiredSampleBatch = false;

  defaultWeightPmfm: PmfmStrategy;
  weightPmfms: PmfmStrategy[];
  weightPmfmsByMethod: { [key: string]: PmfmStrategy };
  isSampling = false;
  mobile: boolean;
  childrenFormHelper: FormArrayHelper<Batch>;
  samplingFormValidator: Subscription;


  @Input() tabindex: number;

  @Input() usageMode: UsageMode;

  @Input() showTaxonGroup = true;

  @Input() showTaxonName = true;

  @Input() showWeight = true;

  @Input() showTotalIndividualCount = false;

  @Input() showIndividualCount = false;

  @Input() showEstimatedWeight = false;

  @Input() showSampleBatch = false;

  @Input() showError = true;

  @Input() mapPmfmFn: (pmfms: PmfmStrategy[]) => PmfmStrategy[];

  $allPmfms = new BehaviorSubject<PmfmStrategy[]>(null);

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean }): void {
    super.enable(opts);

    // Refresh sampling child form
    if (!this.isSampling) this.setIsSampling(this.isSampling);
  }

  get childrenArray() {
    return this.form.get('children') as FormArray;
  }

  @Input()
  set requiredSampleBatch(required: boolean) {
    if (this._requiredSampleBatch !== required) {
      this._requiredSampleBatch = required;
      // Refresh validator, if need
      if (this.samplingFormValidator) this.addSamplingFormValidators();
    }
  }

  get requiredSampleBatch(): boolean {
    return this._requiredSampleBatch;
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
      validatorService.getFormGroup(null, {
        withWeight: true,
        rankOrderRequired: false, // Allow to be set by parent component
        labelRequired: false, // Allow to be set by parent component
      }),
      {
        mapPmfms: (pmfms) => this.mapPmfms(pmfms),
        onUpdateControls: (form) => this.onUpdateControls(form)
      });
    this.mobile = platform.mobile;

    // Set default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;
    this._enable = true;

    this.childrenFormHelper = this.getChildrenFormHelper(this.form);

    // for DEV only
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;

    // This will cause update controls
    this.$initialized.next(true);

    // Taxon group combo
    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options)
    });
    // Taxon name combo
    this.registerAutocompleteField('taxonName', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonNames(value, options)
    });


  }

  ngOnDestroy() {
    super.ngOnDestroy();
    if (this.samplingFormValidator) this.samplingFormValidator.unsubscribe();
  }

  setValue(data: T, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean}) {

    if (!this._ready || !this.data) {
      this.safeSetValue(data, opts);
      return;
    }

    // Fill weight, if a weight PMFM exists
    if (this.defaultWeightPmfm) {
      const weightPmfm = (this.weightPmfms || []).find(p => isNotNil(data.measurementValues[p.pmfmId.toString()]));
      data.weight = {
        methodId: weightPmfm && weightPmfm.methodId,
        computed: false,
        estimated: weightPmfm && weightPmfm.methodId === MethodIds.ESTIMATED_BY_OBSERVER,
        value : weightPmfm && data.measurementValues[weightPmfm.pmfmId.toString()],
      };

      // Make sure the weight is fill only in the default weight pmfm
      this.weightPmfms.forEach(p => {
        delete data.measurementValues[p.pmfmId.toString()];
        this.form.removeControl(p.pmfmId.toString());
      });
    }

    // No weight PMFM : disable weight form group, if exists (will NOT exists in BatchGroupForm sub classe)
    else {
      // Disable weight, if group exist
      const weightFOrmGroup = this.form.get('weight');
      if (weightFOrmGroup) weightFOrmGroup.disable({onlySelf: true, emitEvent: false});
    }

    // Adapt measurement values to form
    if (!opts || opts.normalizeEntityToForm !== false) {
      // IMPORTANT: applying normalisation of measurement values on ALL pmfms (not only displayed pmfms)
      // This is required by the batch-group-form component, to keep the value of hidden PMFM, such as Landing/Discard Pmfm
      MeasurementValuesUtils.normalizeEntityToForm(data, this.$allPmfms.getValue(), this.form);
    }

    if (this.showSampleBatch) {

      this.childrenFormHelper.resize(1);
      const samplingFormGroup = this.childrenFormHelper.at(0) as FormGroup;

      const samplingBatch = BatchUtils.getOrCreateSamplingChild(data);
      this.setIsSampling(BatchUtils.isSampleNotEmpty(samplingBatch));

      // Read child weight (use the first one)
      if (this.defaultWeightPmfm) {
        const samplingWeightPmfm = (this.weightPmfms || []).find(p => isNotNil(samplingBatch.measurementValues[p.pmfmId.toString()]));
        samplingBatch.weight = {
          methodId: samplingWeightPmfm && samplingWeightPmfm.methodId,
          computed: false,
          estimated: samplingWeightPmfm && samplingWeightPmfm.methodId === MethodIds.ESTIMATED_BY_OBSERVER,
          value: samplingWeightPmfm && samplingBatch.measurementValues[samplingWeightPmfm.pmfmId.toString()],
        };

        // Adapt measurement values to form
        MeasurementValuesUtils.normalizeEntityToForm(samplingBatch, [], samplingFormGroup);
      }

      // Convert sampling ratio
      if (isNotNil(samplingBatch.samplingRatio)) {
        samplingBatch.samplingRatio = samplingBatch.samplingRatio * 100;
      }

      this.addSamplingFormValidators();
    }
    else {
      this.childrenFormHelper.resize((data.children || []).length);
    }

    super.setValue(data, {
      // Always skip normalization (already done)
      normalizeEntityToForm: false
    });
  }

  protected getValue(): T {
    const json = this.form.value;

    // Convert weight into measurement
    const totalWeight = this.defaultWeightPmfm && json.weight && json.weight.value;
    if (isNotNil(totalWeight)) {
      const weightPmfm = this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER] || this.defaultWeightPmfm;
      json.measurementValues[weightPmfm.pmfmId.toString()] = totalWeight;
    }
    json.weight = undefined;

    // Convert measurements
    json.measurementValues = Object.assign({}, this.data.measurementValues, MeasurementValuesUtils.normalizeValuesToModel(json.measurementValues, this.$allPmfms.getValue()));

    if (this.showSampleBatch) {

      if (this.isSampling) {
        const child = BatchUtils.getOrCreateSamplingChild(this.data);
        const childJson = json.children && json.children[0] || {};

        childJson.rankOrder = 1;
        childJson.label = json.label && (json.label  + Batch.SAMPLING_BATCH_SUFFIX) || undefined;

        childJson.measurementValues = childJson.measurementValues || {};

        // Convert weight into measurement
        if (childJson.weight && isNotNil(childJson.weight.value)) {
          const childWeightPmfm = childJson.weight.estimated && this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER] || this.defaultWeightPmfm;
          childJson.measurementValues[childWeightPmfm.pmfmId.toString()] = childJson.weight.value;
        }
        else if (isNotNilOrNaN(childJson.samplingRatio) && json){

        }
        childJson.weight = undefined;

        // Convert measurements
        childJson.measurementValues = Object.assign({},
          child.measurementValues,  // Keep existing extra measurements
          MeasurementValuesUtils.normalizeValuesToModel(childJson.measurementValues, this.weightPmfms));

        // Convert sampling ratio
        if (isNotNilOrBlank(childJson.samplingRatio)) {
          childJson.samplingRatioText = `${childJson.samplingRatio}%`;
          childJson.samplingRatio = +childJson.samplingRatio / 100;
        }

        json.children = [childJson];
      }
      else {
        json.children = [];
      }

      // Update data
      this.data.fromObject(json, {withChildren: true});
    }
    else {
     // Keep existing children
      this.data.fromObject(json);
    }

    if (this.debug) console.debug(this.data.label + " getValue() with data:", this.data);

    return this.data;
  }

  setIsSampling(enable: boolean, opts?: {emitEvent?: boolean}) {
    this.isSampling = enable;

    if (!this.loading) this.form.markAsDirty();

    const childrenArray = this.childrenArray;

    if (childrenArray) {
      if (enable && childrenArray.disabled) {
        childrenArray.enable({emitEvent: toBoolean(opts && opts.emitEvent, false)});
        this.markForCheck();
      } else if (!enable && childrenArray.enabled) {
        childrenArray.disable({emitEvent: toBoolean(opts && opts.emitEvent, false)});
        this.markForCheck();
      }
    }
  }

  /* -- protected methods -- */

  protected async onInitialized(): Promise<void> {
    // Wait end of ngInit()
    if (this.$initialized.getValue() !== true) {
      await this.$initialized
        .pipe(
          filter((initialized) => initialized === true),
          first()
        ).toPromise();
    }
  }

  // Wait form controls ready
  async ready(): Promise<void> {
    await super.ready();

    // Wait all pmfms to be loaded
    if (isNil(this.$allPmfms.getValue())) {
      await firstNotNilPromise(this.$allPmfms);
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

    if (this.mapPmfmFn) {
      pmfms = this.mapPmfmFn(pmfms);
    }

    // Read weight PMFMs
    this.weightPmfms = pmfms.filter(p => PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label));
    this.defaultWeightPmfm = this.weightPmfms.length && this.weightPmfms[0] || undefined;
    this.weightPmfmsByMethod = {};
    this.weightPmfms.forEach(p => this.weightPmfmsByMethod[p.methodId] = p);

    this.showSampleBatch = toBoolean(this.showSampleBatch, isNotNil(this.defaultWeightPmfm));
    this.$allPmfms.next(pmfms);

    // Exclude hidden and weight PMFMs
    return pmfms.filter(p => !PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label) && !p.hidden);
  }

  protected async onUpdateControls(form: FormGroup): Promise<void> {

    // Wait end of ngInit()
    await this.onInitialized();

    const childrenFormHelper = this.getChildrenFormHelper(form);

    // Add pmfms to form
    const measFormGroup = form.get('measurementValues') as FormGroup;
    if (measFormGroup) {
      this.measurementValidatorService.updateFormGroup(measFormGroup, {pmfms: this.$allPmfms.getValue()});
    }

    const hasSamplingForm = childrenFormHelper.size() === 1 && this.defaultWeightPmfm && true;

    // If the sample batch exists
    if (this.showSampleBatch) {

      childrenFormHelper.resize(1);
      const samplingForm = childrenFormHelper.at(0) as FormGroup;

      // Reset measurementValues (if exists)
      const samplingMeasFormGroup = samplingForm.get('measurementValues');
      if (samplingMeasFormGroup) {
        this.measurementValidatorService.updateFormGroup(samplingMeasFormGroup as FormGroup, {pmfms: []});
      }

      // Adapt exists sampling child, if any
      if (this.data) {
        const samplingChildBatch = BatchUtils.getOrCreateSamplingChild(this.data);

        this.setIsSampling(BatchUtils.isSampleNotEmpty(samplingChildBatch));

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
      (value) => this.validatorService.getFormGroup(value, {withWeight: true}),
      (v1, v2) => EntityUtils.equals(v1, v2),
      (value) => isNil(value),
      {allowEmptyArray: true}
    );
  }


  protected addSamplingFormValidators() {
    // Unregister to previous validator
    if (this.samplingFormValidator) this.samplingFormValidator.unsubscribe();

    // Add new a validator
    this.samplingFormValidator = this.validatorService.addSamplingFormValidators(this.form, {
      required: this._requiredSampleBatch
    });
  }
  protected markForCheck() {
    this.cd.markForCheck();
  }
}
