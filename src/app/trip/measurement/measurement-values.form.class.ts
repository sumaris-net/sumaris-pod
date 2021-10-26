import { ChangeDetectorRef, Directive, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Moment } from 'moment';
import { DateAdapter } from '@angular/material/core';
import { FloatLabelType } from '@angular/material/form-field';
import { BehaviorSubject, isObservable, Observable, Subject, timer } from 'rxjs';
import { FormArray, FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { MeasurementsValidatorService } from '../services/validator/measurement.validator';
import { filter, first, map, takeUntil, tap } from 'rxjs/operators';
import { IEntityWithMeasurement, MeasurementFormValue, MeasurementValuesUtils } from '../services/model/measurement.model';
import {
  AppForm,
  firstNotNilPromise,
  firstTruePromise,
  FormArrayHelper,
  isNil,
  isNotEmptyArray,
  isNotNil,
  LocalSettingsService,
  ReferentialRef,
  ReferentialUtils,
  WaitOptions,
} from '@sumaris-net/ngx-components';
import { ProgramRefService } from '@app/referential/services/program-ref.service';
import { IPmfm } from '@app/referential/services/model/pmfm.model';
import { DenormalizedPmfmStrategy } from '@app/referential/services/model/pmfm-strategy.model';
import Timer = NodeJS.Timer;

export interface MeasurementValuesFormOptions<T extends IEntityWithMeasurement<T>> {
  mapPmfms?: (pmfms: IPmfm[]) => IPmfm[] | Promise<IPmfm[]>;
  onUpdateControls?: (formGroup: FormGroup) => void | Promise<void>;
}

export const MeasurementFormLoadingSteps = Object.freeze({
  STARTING: 0,
  LOADING_PMFMS: 1,
  UPDATING_FORM_GROUP: 2,
  FORM_GROUP_READY: 3
});

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class MeasurementValuesForm<T extends IEntityWithMeasurement<T>> extends AppForm<T>
  implements OnInit, OnDestroy {

  $loadingStep = new BehaviorSubject<number>(MeasurementFormLoadingSteps.STARTING);

  $programLabel = new BehaviorSubject<string>(undefined);
  $strategyLabel = new BehaviorSubject<string>(undefined);
  $pmfms = new BehaviorSubject<IPmfm[]>(undefined);

  formArraysControls: FormArray[];
  formArrayHelpers = new Map<string, FormArrayHelper<ReferentialRef>>();

  protected _onValueChanged = new EventEmitter<T>();
  protected _onRefreshPmfms = new EventEmitter<any>();
  protected _gearId: number = null;
  protected _acquisitionLevel: string;
  protected _forceOptional = false;
  protected _measurementValuesForm: FormGroup;
  protected data: T;
  protected applyingValue = false;

  get forceOptional(): boolean {
    return this._forceOptional;
  }

  get measurementValuesForm(): FormGroup {
    return this._measurementValuesForm || (this.form.controls.measurementValues as FormGroup);
  }

  get loading(): boolean {
    return this.$loadingStep.value < MeasurementFormLoadingSteps.FORM_GROUP_READY;
  }

  get starting(): boolean {
    return this.$loadingStep.value === MeasurementFormLoadingSteps.STARTING;
  }

  @Input() compact = false;
  @Input() floatLabel: FloatLabelType = 'auto';
  @Input() requiredStrategy = false;
  @Input() requiredGear = false;

  @Input()
  set programLabel(value: string) {
    this.setProgramLabel(value);
  }

  get programLabel(): string {
    return this.$programLabel.getValue();
  }

  @Input()
  set strategyLabel(value: string) {
    this.setStrategyLabel(value);
  }

  get strategyLabel(): string {
    return this.$strategyLabel.getValue();
  }

  @Input()
  set acquisitionLevel(value: string) {
    if (this._acquisitionLevel !== value && isNotNil(value)) {
      this._acquisitionLevel = value;
      if (!this.starting) this._onRefreshPmfms.emit();
    }
  }

  get acquisitionLevel(): string {
    return this._acquisitionLevel;
  }

  @Input()
  set gearId(value: number) {
    if (this._gearId !== value && isNotNil(value)) {
      this._gearId = value;
      if (!this.starting || this.requiredGear) this._onRefreshPmfms.emit();
    }
  }

  get gearId(): number {
    return this._gearId;
  }

  @Input()
  set value(value: T) {
    this.safeSetValue(value);
  }

  get value(): T {
    return this.getValue();
  }

  @Input() set pmfms(pmfms: Observable<IPmfm[]> | IPmfm[]) {
    this.setPmfms(pmfms);
  }

  @Input()
  set forceOptional(value: boolean) {
    if (this._forceOptional !== value) {
      this._forceOptional = value;
      if (!this.starting) this._onRefreshPmfms.emit();
    }
  }

  @Output() valueChanges = new EventEmitter<any>();
  @Output() get strategyLabelChanges(): Observable<string> {
    return this.$strategyLabel.asObservable();
  }

  protected constructor(protected dateAdapter: DateAdapter<Moment>,
                        protected measurementValidatorService: MeasurementsValidatorService,
                        protected formBuilder: FormBuilder,
                        protected programRefService: ProgramRefService,
                        protected settings: LocalSettingsService,
                        protected cd: ChangeDetectorRef,
                        form?: FormGroup,
                        protected options?: MeasurementValuesFormOptions<T>
  ) {
    super(dateAdapter, form, settings);

    this.registerSubscription(
      this._onRefreshPmfms.subscribe(() => this.loadPmfms())
    );
    this.registerSubscription(
      this.$pmfms.subscribe(pmfms => this.updateFormGroup(pmfms))
    );

    // TODO: DEV only
    //this.debug = true;
  }

  ngOnInit() {
    super.ngOnInit();

    // Listen form changes
    this.registerSubscription(
      this.form.valueChanges
        .pipe(
          filter(() => !this.loading && isNotEmptyArray(this.valueChanges.observers))
        )
        .subscribe((_) => this.valueChanges.emit(this.value))
    );

    if (this.data) {
      this._onValueChanged.emit(this.data);
    }

    // Try to load pmfms
    this.setLoadingProgression(MeasurementFormLoadingSteps.LOADING_PMFMS);
    this.loadPmfms();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.$loadingStep.unsubscribe();
    this.$pmfms.unsubscribe();
    this.$programLabel.unsubscribe();
    this.$strategyLabel.unsubscribe();
  }

  setValue(data: T, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any; }) {
    if (this.loading || !this.applyingValue) {
      this.safeSetValue(data, opts); // Loop
      return;
    }

    // Adapt measurement values to form (if not skip)
    if (!opts || opts.normalizeEntityToForm !== false) {
      MeasurementValuesUtils.normalizeEntityToForm(data, this.$pmfms.value, this.form);
    }

    // If a program has been filled, always keep it
    const program = this.form.get('program')?.value;
    if (program) {
      data.program = program;
    }

    this.formArrayHelpers.forEach((formArrayHelper, id) => {
      formArrayHelper.resize(Math.max(1, ((data.measurementValues[id] || [])as []).length));
    });

    super.setValue(data, opts);

    // Restore form status
    this.restoreFormStatus({onlySelf: true, emitEvent: opts && opts.emitEvent});
  }

  reset(data?: T, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any; }) {
    if (this.loading || !this.applyingValue) {
      this.safeSetValue(data, opts); // Loop
      return;
    }

    // Adapt measurement values to form (if not skip)
    if (!opts || opts.normalizeEntityToForm !== false) {
      MeasurementValuesUtils.normalizeEntityToForm(data, this.$pmfms.getValue(), this.form);
    }

    super.reset(data, opts);

    // Restore form status
    this.restoreFormStatus({onlySelf: true, emitEvent: opts && opts.emitEvent});
  }

  /**
   * Reset all data to original value. Useful sometimes, to re init the component (e.g. physical gear form).
   * Note: Keep @Input() attributes unchanged
   */
  public unload() {
    this.data = null;
    this._measurementValuesForm = null;
    this.markAsLoading();
    this.$pmfms.next(undefined);
  }

  addFormControlToFormArray(pmfm: DenormalizedPmfmStrategy) {
    const formArrayHelper = this.formArrayHelpers.get(pmfm.id.toString());

    if (formArrayHelper.size() < pmfm.acquisitionNumber){
      this.formArrayHelpers.get(pmfm.id.toString()).add();
    }
  }

  markAsLoading(opts?: {step?: number; emitEvent?: boolean;}) {

    // Emit, if changed
    const step = (opts && opts.step) || MeasurementFormLoadingSteps.STARTING;
    if (this.$loadingStep.value !== step) {
      if (this.debug) console.debug(`${this.logPrefix} Loading step -> ${step}`);
      this.$loadingStep.next(step);
    }
  }

  markAsLoaded() {
    if (this.$loadingStep.value < MeasurementFormLoadingSteps.FORM_GROUP_READY) {
      this.$loadingStep.next(MeasurementFormLoadingSteps.FORM_GROUP_READY);
    }
  }

  waitIdle(opts?: WaitOptions): Promise<boolean> {
    let idle$ = this.$loadingStep
      .pipe(
        map(step => step >= MeasurementFormLoadingSteps.FORM_GROUP_READY)
      );

    // Add timeout
    if (opts && opts.timeout) {
      idle$ = idle$.pipe(
        takeUntil(timer(opts.timeout))
      );
    }

    return firstTruePromise(idle$);
  }

  /* -- protected methods -- */

  protected setProgramLabel(value: string) {
    if (isNotNil(value) && this.$programLabel.value !== value) {

      this.$programLabel.next(value);

      // Reload pmfms
      if (!this.starting) this._onRefreshPmfms.emit();
    }
  }

  protected setStrategyLabel(value: string) {
    if (isNotNil(value) && this.$strategyLabel.value !== value) {

      this.$strategyLabel.next(value);

      // Reload pmfms
      if (!this.loading && this.requiredStrategy) this._onRefreshPmfms.emit();
    }
  }

  /**
   * Wait form is ready, before setting the value to form
   * @param data
   * @param opts
   */
  protected async safeSetValue(data: T, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; }) {
    if (!data) return; // Trying to set undefined value to meas form. Skipping
    if (this.applyingValue) return // Already applying a value. Skipping

    // Propagate the program
    if (data.program?.label) {
      this.programLabel = data.program.label;
    }

    // Will avoid data to be set (see updateFormGroup())
    this.applyingValue = true;

    try {
      // This line is required by physical gear modal
      this.data = data;
      this._onValueChanged.emit(data);

      // Wait form idle (e.g. loading pmfms, update form groups, etc.)
      await this.waitIdle();

      this.setValue(this.data, {...opts, emitEvent: true});

      this.form.markAsPristine();
      this.form.markAsUntouched();
    }
    finally {
      this.markAsLoaded();
      this.applyingValue = false;
    }
  }

  protected getValue(): T {
    const json = this.form.value;

    const measurementValuesForm = this.measurementValuesForm;
    if (measurementValuesForm) {
      // Find dirty pmfms, to avoid full update
      const dirtyPmfms = (this.$pmfms.getValue() || []).filter(pmfm => measurementValuesForm.controls[pmfm.id].dirty);
      if (dirtyPmfms.length) {
        json.measurementValues = Object.assign({}, this.data && this.data.measurementValues || {}, MeasurementValuesUtils.normalizeValuesToModel(measurementValuesForm.value, dirtyPmfms));
      }
    }

    if (this.data && this.data.fromObject) {
      this.data.fromObject(json);
    } else {
      this.data = json;
    }

    return this.data;
  }

  protected setLoadingProgression(step: number) {
    this.markAsLoading({step})
  }

  protected async loadPmfms() {
    // Check if can load (must have: program, acquisition - and gear if required)
    if (isNil(this.programLabel) || (this.requiredStrategy && isNil(this.strategyLabel))
      || isNil(this._acquisitionLevel) || (this.requiredGear && isNil(this._gearId))) {

      // DEBUG
      //console.debug('loadPmfms -> cannot be call');

      return;
    }

    // DEBUG
    //if (this.debug) console.debug(`${this.logPrefix} loadPmfms()`);

    this.setLoadingProgression(MeasurementFormLoadingSteps.LOADING_PMFMS);
    this.$pmfms.next(null);

    try {
      // Load pmfms
      let pmfms = (await this.programRefService.loadProgramPmfms(
        this.programLabel,
        {
          strategyLabel: this.strategyLabel,
          acquisitionLevel: this._acquisitionLevel,
          gearId: this._gearId
        })) || [];

      // If force to optional, create a copy of each pmfms that should be forced
      if (this._forceOptional) {
        pmfms = pmfms.map(pmfm => {
          if (pmfm.required) {
            pmfm = pmfm.clone(); // Keep unchanged the original entity
            pmfm.required = false;
          }
          return pmfm;
        });
      } else {
        pmfms = pmfms.slice(); // Do a copy, to force refresh when comparing using '===' in components
      }

      // Apply
      await this.setPmfms(pmfms);

    } catch (err) {
      console.error(`${this.logPrefix} Error while loading pmfms: ${err && err.message || err}`, err);
      this.markAsLoaded();
      this.$pmfms.next(undefined); // Reset pmfms
    }
  }

  protected async updateFormGroup(pmfms?: IPmfm[]) {
    pmfms = pmfms || this.$pmfms.value;
    if (!pmfms) return; // Skip

    const form = this.form;
    this._measurementValuesForm = form.controls.measurementValues as FormGroup;

    if (this.debug) console.debug(`${this.logPrefix} Updating form controls, force_optional: ${this._forceOptional}}, using pmfms:`, pmfms);

    // Disable the form (if need)
    if (this._measurementValuesForm?.enabled) {
      this._measurementValuesForm.disable({onlySelf: true, emitEvent: false});
    }

    // Mark as loading
    this.setLoadingProgression(MeasurementFormLoadingSteps.UPDATING_FORM_GROUP);

    // No pmfms (= empty form)
    if (!pmfms.length) {
      // Reset measurement form (if exists)
      if (this._measurementValuesForm) {
        this.measurementValidatorService.updateFormGroup(this._measurementValuesForm, {pmfms: []});
        this._measurementValuesForm.reset({}, {onlySelf: true, emitEvent: false});
      }
    } else {
      if (this.debug) console.debug(`${this.logPrefix} Updating form controls, using pmfms:`, pmfms);

      // Create measurementValues form group
      if (!this._measurementValuesForm) {
        this._measurementValuesForm = this.measurementValidatorService.getFormGroup(null, {pmfms});

        form.addControl('measurementValues', this._measurementValuesForm);
        this._measurementValuesForm.disable({onlySelf: true, emitEvent: false});
      }

      // Or update if already exist
      else {
        this.measurementValidatorService.updateFormGroup(this._measurementValuesForm, {pmfms});
        this.initFormArraysHelpers();
      }
    }

    // Call options function
    if (this.options && this.options.onUpdateControls) {
      const res = this.options.onUpdateControls(form);
      if (res instanceof Promise) {
        await res;
      }
    }

    if (this.debug) console.debug(`${this.logPrefix} Form controls updated`);
    this.setLoadingProgression(MeasurementFormLoadingSteps.FORM_GROUP_READY);

    // Keep loading status if data not apply fully
    if (!this.applyingValue) {
      if (this.data && pmfms.length && form) {
        this.setValue(this.data, {onlySelf: true, emitEvent: false});
      }
      // No data defined yet
      else {
        // Restore enable state (because form.setValue() can change it !)
        this.restoreFormStatus({onlySelf: true, emitEvent: false});
      }
    }

    return true;
  }

  async setPmfms(value: IPmfm[] | Observable<IPmfm[]>): Promise<IPmfm[]> {

    // If no pmfms
    if (!value) {
      // Reset pmfms and the loading step
      if (this.$loadingStep.value > MeasurementFormLoadingSteps.LOADING_PMFMS || isNotNil(this.$pmfms.value)) {
        if (this.debug && isNotNil(this.$pmfms.value)) console.warn(`${this.logPrefix} setPmfms(null|undefined): resetting pmfms`);
        this.markAsLoading();
        this.$pmfms.next(undefined);
      }
      return undefined; // break
    }

    // Mark as loading pmfms
    const previousLoadingStep = this.$loadingStep.value;
    this.setLoadingProgression(MeasurementFormLoadingSteps.LOADING_PMFMS);

    // Wait loaded, if observable
    let pmfms: IPmfm[];
    if (isObservable<IPmfm[]>(value)) {
      if (this.debug) console.debug(`${this.logPrefix} setPmfms(): waiting pmfms observable...`);
      pmfms = await firstNotNilPromise(value);
    } else {
      if (this.debug) console.debug(`${this.logPrefix} setPmfms()`);
      pmfms = value;
    }

    // Map
    if (this.options && this.options.mapPmfms) {
      const res = this.options.mapPmfms(pmfms);
      pmfms = (res instanceof Promise) ? await res : res;
    }

    // Apply (if changed)
    if (pmfms !== this.$pmfms.value) {
      // next step
      this.setLoadingProgression(MeasurementFormLoadingSteps.UPDATING_FORM_GROUP);

      this.$pmfms.next(pmfms);
    }
    else {
      // Nothing changes: restoring previous steps
      this.setLoadingProgression(previousLoadingStep);
    }

    return pmfms;
  }

  protected restoreFormStatus(opts?: { emitEvent?: boolean; onlySelf?: boolean; }) {
    const form = this.measurementValuesForm;
    // Restore enable state (because form.setValue() can change it !)
    if (this._enable) {
      form.enable(opts);
    } else if (form.enabled) {
      form.disable(opts);
    }
  }

  protected get logPrefix(): string {
    const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
    return `[meas-values-form-${acquisitionLevel}]`;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected initFormArraysHelpers() {
    // Create helper, if needed
    const formArraysControlsNames = new Array<string>();
    this.formArraysControls = new Array<FormArray>();
    for (const control in this.measurementValuesForm.controls) {
      if (this.measurementValuesForm.get(control) instanceof FormArray) {
        formArraysControlsNames.push(control);
        this.formArraysControls.push(this.measurementValuesForm.get(control) as FormArray);
      }
    }
    formArraysControlsNames.forEach(formArrayName => {
      if (!this.formArrayHelpers || !this.formArrayHelpers[formArrayName]) {
        this.formArrayHelpers.set(formArrayName, new FormArrayHelper<MeasurementFormValue>(
          FormArrayHelper.getOrCreateArray(this.formBuilder, this.measurementValuesForm, formArrayName),
          (measurementFormValue) => this.getArrayControl(measurementFormValue),
          ReferentialUtils.equals,
          ReferentialUtils.isEmpty,
          {
            allowEmptyArray: false
          }
        ));
      }
      // Helper exists: update options
      else {
        this.formArrayHelpers[formArrayName].allowEmptyArray = false;
      }
    });
  }

  protected getArrayControl(measurementFormValue?: MeasurementFormValue): FormControl {
    return this.formBuilder.control(measurementFormValue || null);
  }
}
