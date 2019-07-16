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
import {Batch, BatchUtils} from "../services/model/batch.model";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ProgramService} from "../../referential/services/program.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {
  AcquisitionLevelCodes,
  FieldOptions,
  ReferentialRef,
  referentialToString,
  UsageMode
} from "../../core/services/model";
import {debounceTime, filter, map, switchMap, tap, throttleTime} from "rxjs/operators";
import {
  isNil,
  MethodIds,
  PmfmLabelPatterns,
  PmfmStrategy,
  TaxonGroupIds,
  TaxonomicLevelIds
} from "../../referential/services/model";
import {merge, Observable} from "rxjs";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {SpeciesBatchValidatorService} from "../services/validator/species-batch.validator";
import {AppFormUtils, PlatformService} from "../../core/core.module";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {isNotNilOrBlank} from "../../shared/functions";

@Component({
  selector: 'app-batch-form',
  templateUrl: 'batch.form.html',
  providers: [
    {provide: ValidatorService, useClass: SpeciesBatchValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchForm extends MeasurementValuesForm<Batch>
  implements OnInit, OnDestroy {

  protected fieldsOptions: {
    taxonGroup?: FieldOptions,
    taxonName?: FieldOptions
  } = {};

  defaultWeightPmfm: PmfmStrategy;
  weightPmfmsByMethod: { [key: string]: PmfmStrategy };
  estimatedWeightControl: AbstractControl;
  isSampling: boolean;
  mobile: boolean;

  samplingBatchPmfms: PmfmStrategy[];

  onShowTaxonGroupDropdown = new EventEmitter<UIEvent>();
  $taxonGroups: Observable<ReferentialRef[]>;

  onShowTaxonNameDropdown = new EventEmitter<UIEvent>();
  $taxonNames: Observable<ReferentialRef[]>;

  @Input() tabindex: number;

  @Input() usageMode: UsageMode;

  @Input() showTaxonGroup = true;

  @Input() showTaxonName = true;

  @Input() showIndividualCount = true;

  @Input() showSampleBatch: boolean;

  @Input() showError = true;

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
    protected validatorService: ValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, cd,
      validatorService.getRowValidator(),
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

  async ngOnInit() {
    super.ngOnInit();

    console.debug("[batch-form] Init form");

    await this.settings.ready();

    // Read fields options, from settings
    this.fieldsOptions.taxonGroup = this.settings.getFieldOptions('taxonGroup');
    this.fieldsOptions.taxonName = this.settings.getFieldOptions('taxonName');

    // Taxon groups combo
    this.$taxonGroups = merge(
        this.onShowTaxonGroupDropdown
          .pipe(
            filter(e => !e.defaultPrevented),
            map((_) => "*")
          ),
        this.form.get('taxonGroup').valueChanges
          .pipe(debounceTime(250))
      )
      .pipe(
        throttleTime(100),
        switchMap((value) => this.referentialRefService.suggest(value, {
          entityName: 'TaxonGroup',
          levelId: TaxonGroupIds.FAO,
          searchAttribute: this.fieldsOptions.taxonGroup.searchAttribute
        })),
        // Remember implicit value
        tap(res => this.updateImplicitValue('taxonGroup', res))
      );

    // Taxon name combo
    this.$taxonNames =
      merge(
        this.onShowTaxonNameDropdown
          .pipe(
            filter(e => !e.defaultPrevented),
            map((_) => "*")
          ),
        this.form.get('taxonName').valueChanges
          .pipe(debounceTime(250))
      )
      .pipe(
        throttleTime(100),
        switchMap((value) => {
          const taxonGroup = this.form.get('taxonGroup').value;
          return this.programService.suggestTaxonNames(value, {
            program: this.program,
            searchAttribute: this.fieldsOptions.taxonName.searchAttribute,
            taxonGroupId: taxonGroup && taxonGroup.id || undefined
          })
        }),
        // Remember implicit value
        tap(res => this.updateImplicitValue('taxonName', res))
      );
  }


  public setValue(data: Batch) {

    // If a sample batch
    if (this.showSampleBatch) {
      const samplingChildBatch = BatchUtils.getOrCreateSamplingChild(data);
      if (samplingChildBatch) {
        const childrenArray = this.form.get('children') as FormArray;
        const samplingFormGroup = childrenArray.at(0) as FormGroup;

        // Adapt measurement values to form
        MeasurementValuesUtils.normalizeFormEntity(samplingChildBatch, this.samplingBatchPmfms, samplingFormGroup);
      }
    }
    super.setValue(data);
  }

  protected getValue(): Batch {
    const json = this.form.value;

    // Convert measurements
    json.measurementValues = Object.assign({}, this.data.measurementValues, MeasurementValuesUtils.toEntityValues(json.measurementValues, this.$pmfms.getValue()));

    this.data.fromObject(json);

    if (this.isSampling && json.children && json.children.length === 1) {
      const child = this.data.children && this.data.children[0] || new Batch();
      const childJson = json.children[0];
      // Convert measurements
      childJson.measurementValues = Object.assign({}, child.measurementValues, MeasurementValuesUtils.toEntityValues(childJson.measurementValues, this.samplingBatchPmfms));

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

    return this.data;
  }

  setIsSampling(enable: boolean) {
    this.isSampling = enable;
    const childrenArray = this.form.get('children') as FormArray;

    if (childrenArray) {
      if (enable && childrenArray.disabled) {
        childrenArray.enable({emitEvent: false});
      } else if (!enable && childrenArray.enabled) {
        childrenArray.disable({emitEvent: false});
      }
    }
  }

  /* -- protected methods -- */

  protected mapPmfms(pmfms: PmfmStrategy[]) {
    let weightMinRankOrder: number = undefined;

    this.defaultWeightPmfm = undefined;
    this.weightPmfmsByMethod = pmfms.reduce((res, p) => {
      const matches = PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label);
      if (matches) {
        const methodId = p.methodId;
        res[methodId] = p;
        if (isNil(weightMinRankOrder)) weightMinRankOrder = p.rankOrder;
        if (isNil(this.defaultWeightPmfm)) this.defaultWeightPmfm = p;
      }
      return res;
    }, {});

    // If estimated weight is allow, init a form with a check box
    if (this.weightPmfmsByMethod[MethodIds.ESTIMATED_BY_OBSERVER]) {

      // Create the form, for each QV value
      this.estimatedWeightControl = this.formBuilder.control(false, Validators.required);

      // Listening changes
      this.registerSubscription(
        this.estimatedWeightControl.valueChanges.subscribe(value => {
          this.markAsDirty();
        }));
    }

    // Remove weight pmfms
    return pmfms.filter(p =>
      // Keep default weight PMFM
      p === this.defaultWeightPmfm
      // But exclude other weight PMFMs
      || !PmfmLabelPatterns.BATCH_WEIGHT.exec(p.label));
  }

  protected onUpdateControls(form: FormGroup) {

    // Get the children array
    const childrenArray = form.get('children') as FormArray;
    const hasSampleValidator = childrenArray && childrenArray.length === 1 && this.defaultWeightPmfm && true;

    // If not already set (as component Input), set if show sample
    if (isNil(this.showSampleBatch)) {
      this.showSampleBatch = hasSampleValidator;
    }

    // If the sample batch exists
    if (this.showSampleBatch) {

      const samplingFormGroup = childrenArray.at(0) as FormGroup;
      let samplingMeasFormGroup = samplingFormGroup.get('measurementValues');

      this.samplingBatchPmfms = [this.defaultWeightPmfm];

      // Create measurementValues group
      if (!samplingMeasFormGroup) {
        samplingMeasFormGroup = this.measurementValidatorService.getFormGroup(this.samplingBatchPmfms);
        samplingFormGroup.addControl('measurementValues', samplingMeasFormGroup);
        samplingMeasFormGroup.disable({onlySelf: true, emitEvent: false});
      }

      // Or update if already exist
      else {
        this.measurementValidatorService.updateFormGroup(samplingMeasFormGroup as FormGroup, this.samplingBatchPmfms);
      }

      // Adapt exists sampling child, if any
      if (this.data) {
        const samplingChildBatch = BatchUtils.getOrCreateSamplingChild(this.data);

        this.setIsSampling(BatchUtils.isSampleNotEmpty(samplingChildBatch));

        // Adapt measurement values to reactive form
        MeasurementValuesUtils.normalizeFormEntity(samplingChildBatch, this.samplingBatchPmfms, samplingFormGroup);

        samplingMeasFormGroup.patchValue(samplingChildBatch.measurementValues, {
          onlySelf: true,
          emitEvent: false
        });
      }
      else {
        // No data: disable sampling
        this.setIsSampling(false);
      }

      // TODO: add async validators:
      //  e.g. sampling weight < total weight
    }

    // Remove existing sample validator, if exists but showSample=false
    else if (hasSampleValidator) {
      this.form.removeControl('children');
    }
  }

  referentialToString = referentialToString;
  selectInputContent = AppFormUtils.selectInputContent;

}
