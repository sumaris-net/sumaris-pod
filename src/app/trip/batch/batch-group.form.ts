import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, QueryList, ViewChildren} from "@angular/core";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {AbstractControl, FormBuilder, FormControl} from "@angular/forms";
import {ProgramService} from "../../referential/services/program.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {AcquisitionLevelCodes, isNotNil, PmfmStrategy, PmfmUtils} from "../../referential/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {AppFormUtils} from "../../core/core.module";
import {BatchGroupValidatorService} from "../services/batch-group.validator";
import {BehaviorSubject} from "rxjs";
import {BatchForm} from "./batch.form";
import {filter, switchMap} from "rxjs/operators";
import {PlatformService} from "../../core/services/platform.service";
import {firstNotNilPromise} from "../../shared/observables";
import {fadeInAnimation} from "../../shared/shared.module";
import {BatchGroup} from "../services/model/batch-group.model";
import {MeasurementsValidatorService} from "../services/measurement.validator";

@Component({
  selector: 'app-batch-group-form',
  templateUrl: 'batch-group.form.html',
  styleUrls: ['batch-group.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [fadeInAnimation]
})
export class BatchGroupForm extends BatchForm<BatchGroup> {

  $childrenPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  hasIndividualMeasureControl: AbstractControl;

  @Input() qvPmfm: PmfmStrategy;

  @Input() showChildrenSampleBatch = true;

  @Input() showChildrenWeight = true;

  @ViewChildren('childForm') childrenForms !: QueryList<BatchForm>;

  get invalid(): boolean {
    return this.form.invalid || this.hasIndividualMeasureControl.invalid ||
      ((this.childrenForms || []).find(child => child.invalid) && true) || false;
  }

  get valid(): boolean {
    // Important: Should be not invalid AND not pending, so use '!valid' (and NOT 'invalid')
    return this.form.valid && this.hasIndividualMeasureControl.valid &&
      (!this.childrenForms || !this.childrenForms.find(child => !child.valid)) || false;
  }

  get pending(): boolean {
    return this.form.pending || this.hasIndividualMeasureControl.pending ||
       (this.childrenForms && this.childrenForms.find(child => child.pending) && true) || false;
  }

  get dirty(): boolean {
    return this.form.dirty || this.hasIndividualMeasureControl.dirty ||
      (this.childrenForms && this.childrenForms.find(child => child.dirty) && true) || false;
  }

  markAsTouched(opts?: {onlySelf?: boolean; emitEvent?: boolean; }) {
    super.markAsTouched(opts);
    (this.childrenForms || []).forEach(child => child.markAsTouched(opts));
    this.hasIndividualMeasureControl.markAsTouched(opts);
  }

  markAsPristine(opts?: {onlySelf?: boolean; }) {
    super.markAsPristine(opts);
    (this.childrenForms || []).forEach(child => child.markAsPristine(opts));
    this.hasIndividualMeasureControl.markAsPristine(opts);
  }

  markAsUntouched(opts?: {onlySelf?: boolean; }) {
    super.markAsUntouched(opts);
    (this.childrenForms || []).forEach(child => child.markAsUntouched(opts));
    this.hasIndividualMeasureControl.markAsUntouched(opts);
  }

  markAsDirty(opts?: {
    onlySelf?: boolean;
  }) {
    super.markAsDirty(opts);
    (this.childrenForms && []).forEach(child => child.markAsDirty(opts));
    this.hasIndividualMeasureControl.markAsDirty(opts);
  }

  disable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }) {
    super.disable(opts);
    (this.childrenForms || []).forEach(child => child.disable(opts));
    if (this._enable || (opts && opts.emitEvent)) {
      this._enable = false;
      this.markForCheck();
    }
    this.hasIndividualMeasureControl.disable(opts);
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }) {
    super.enable(opts);
    (this.childrenForms || []).forEach(child => child.enable(opts));
    if (!this._enable || (opts && opts.emitEvent)) {
      this._enable = true;
      this.markForCheck();
    }
  }

  get hasIndividualMeasure(): boolean {
    return this.hasIndividualMeasureControl.value === true;
  }

  @Input()
  set hasIndividualMeasure(value: boolean) {
    this.hasIndividualMeasureControl.setValue(value);
  }

  constructor(
    protected measurementValidatorService: MeasurementsValidatorService,
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef,
    protected validatorService: BatchGroupValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService
  ) {
    super(dateAdapter,
      measurementValidatorService,
      formBuilder,
      programService,
      platform,
      cd,
      validatorService,
      referentialRefService,
      settings);

    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;
    this.hasIndividualMeasureControl = new FormControl(false);
  }

  ngOnInit() {
    super.ngOnInit();

    // Set isSampling on each child forms, when has indiv. measure changed
    this.hasIndividualMeasureControl.valueChanges
      .pipe(filter(() => !this.loadingValue && !this.loading))
      .subscribe(value => {
        (this.childrenForms || []).forEach((childForm, index) => {
          childForm.setIsSampling(value, {emitEvent: true}/*Important, to force async validator*/);
        });
        this.markForCheck();
      });
  }

  setValue(data: BatchGroup, opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {
    if (!this.isReady() || !this.data) {
      this.safeSetValue(data, opts);
      return;
    }

    if (this.debug) console.debug("[batch-group-form] setValue() with value:", data);
    let hasIndividualMeasure = data.observedIndividualCount > 0;

    if (!this.qvPmfm) {
      super.setValue(data);

      // Check if measure
      hasIndividualMeasure = hasIndividualMeasure || isNotNil(BatchUtils.getSamplingChild(data));
    } else {

      // Prepare data array, for each qualitative values
      data.children = this.qvPmfm.qualitativeValues.map((qv, index) => {

        // Find existing child, or create a new one
        const child = (data.children || []).find(c => +(c.measurementValues[this.qvPmfm.pmfmId]) === qv.id)
          || new Batch();

        // Make sure label and rankOrder are correct
        child.label = `${data.label}.${qv.label}`;
        child.measurementValues[this.qvPmfm.pmfmId] = qv;
        child.rankOrder = index + 1;

        // Check there is a sampling batch
        hasIndividualMeasure = hasIndividualMeasure || isNotNil(BatchUtils.getSamplingChild(child));

        if (hasIndividualMeasure) {
          BatchUtils.getOrCreateSamplingChild(child);
        }

        return child;
      });

      // Set value (batch group)
      super.setValue(data);

      // Then set value of each child form
      this.childrenForms.forEach((childForm, index) => {
        const childBatch = data.children[index] || new Batch();
        childForm.requiredSampleWeight = this.showChildrenWeight && this.hasIndividualMeasure;
        childForm.setIsSampling(hasIndividualMeasure, {emitEvent: true});
        childForm.setValue(childBatch);
        if (this.enabled) {
          childForm.enable();
        } else {
          childForm.disable();
        }
      });


    }

    // Apply computed value of 'has indiv. measure'
    this.hasIndividualMeasureControl.setValue(hasIndividualMeasure, {emitEvent: false});

    // If there is already some measure
    // Not allow to change 'has measure' field
    if (data.observedIndividualCount > 0) {
      this.hasIndividualMeasureControl.disable();
    }
  }

  logFormErrors(logPrefix: string) {
    AppFormUtils.logFormErrors(this.form, logPrefix);
    if (this.childrenForms) this.childrenForms.forEach((childForm, index) => {
        AppFormUtils.logFormErrors(childForm.form, logPrefix, `children#${index}`);
      });
  }

  protected mapPmfms(pmfms: PmfmStrategy[]) {
    this.qvPmfm = this.qvPmfm || PmfmUtils.getFirstQualitativePmfm(pmfms);
    if (this.qvPmfm) {

      // Create a copy, to keep original pmfm unchanged
      this.qvPmfm = this.qvPmfm.clone();

      // Hide for children form, and change it as required
      this.qvPmfm.hidden = true;
      this.qvPmfm.isMandatory = true;

      // Replace in the list
      this.$childrenPmfms.next(pmfms.map(p => p.pmfmId === this.qvPmfm.pmfmId ? this.qvPmfm : p));

      // Do not display PMFM in the root batch
      pmfms = [];
    }

    return super.mapPmfms(pmfms);
  }

  /* -- protected methods -- */

  // Wait form controls ready
  public async ready(): Promise<void> {
    if (this._ready) return;

    // Wait root form to be ready
    await super.ready();
    this._ready = false;

    // Wait all children forms are ready
    if (this.qvPmfm) {
      await firstNotNilPromise(this.childrenForms.changes
        .pipe(
          // Wait children component created
          filter(() => this.childrenForms.length > 0),
          // Then wait children forms are all ready
          switchMap((childrenForms) => Promise.all(childrenForms.map(c => c.ready())))
        )
      );
    }

    this._ready = true;
  }

  protected getValue(): BatchGroup {
    const data = super.getValue();

    // If has children form
    if (this.qvPmfm) {
      data.children = this.childrenForms.map((form, index) => {
        const qv = this.qvPmfm.qualitativeValues[index];
        const child = form.value;
        child.rankOrder = index + 1;
        child.label = `${data.label}.${qv.label}`;
        child.measurementValues = child.measurementValues || {};
        child.measurementValues[this.qvPmfm.pmfmId.toString()] = '' + qv.id;
        return child;
      });
    }

    if (this.debug) console.debug("[batch-group-form] getValue():", data);

    return data;
  }


}
