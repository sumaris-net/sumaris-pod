import { ChangeDetectorRef, Directive, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Moment } from 'moment';
import { DateAdapter } from '@angular/material/core';
import { FloatLabelType } from '@angular/material/form-field';
import { BehaviorSubject, isObservable, merge, Observable, timer } from 'rxjs';
import { AbstractControl, FormBuilder, FormGroup } from '@angular/forms';
import { MeasurementsValidatorService } from '../services/validator/measurement.validator';
import { filter, map } from 'rxjs/operators';
import { IEntityWithMeasurement, MeasurementValuesUtils } from '../services/model/measurement.model';
import { AppForm, firstNotNilPromise, isNil, isNotNil, LocalSettingsService, toNumber, WaitForOptions } from '@sumaris-net/ngx-components';
import { ProgramRefService } from '@app/referential/services/program-ref.service';
import { IPmfm } from '@app/referential/services/model/pmfm.model';

export interface MeasurementValuesFormOptions<T extends IEntityWithMeasurement<T>> {
  mapPmfms?: (pmfms: IPmfm[]) => IPmfm[] | Promise<IPmfm[]>;
  onUpdateFormGroup?: (formGroup: FormGroup) => void | Promise<void>;
  allowSetValueBeforePmfms?: boolean; // False by default
}

export const MeasurementFormLoadingSteps = Object.freeze({
  STARTING: 0,
  LOADING_PMFMS: 1,
  SETTING_PMFMS: 2,
  UPDATING_FORM_GROUP: 3,
  FORM_GROUP_READY: 4
});

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class MeasurementValuesForm<T extends IEntityWithMeasurement<T>> extends AppForm<T>
  implements OnInit, OnDestroy {

  $loadingStep = new BehaviorSubject<number>(MeasurementFormLoadingSteps.STARTING);

  $programLabel = new BehaviorSubject<string>(undefined);
  $strategyLabel = new BehaviorSubject<string>(undefined);
  $pmfms = new BehaviorSubject<IPmfm[]>(undefined);

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

  get isNewData(): boolean {
    return isNil(this.data?.id);
  }

  @Input() compact = false;
  @Input() floatLabel: FloatLabelType = 'auto';
  @Input() requiredStrategy = false;
  @Input() requiredGear = false;

  @Input()
  set programLabel(value: string) {
    this.setProgramLabel(value, {emitEvent: !this.starting});
  }

  get programLabel(): string {
    return this.$programLabel.getValue();
  }

  @Input()
  set strategyLabel(value: string) {
    this.setStrategyLabel(value, {emitEvent: !this.starting});
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
    this.setGearId(value, {emitEvent: !this.starting});
  }

  get gearId(): number {
    return this._gearId;
  }

  @Input()
  set value(value: T) {
    this.applyValue(value);
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

  get programControl(): AbstractControl {
    return this.form.get('program');
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
      this.$pmfms
        .pipe(filter(isNotNil))
        .subscribe(pmfms => this.updateFormGroup(pmfms))
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
          filter(() => !this.loading && !this.applyingValue)
        )
        .subscribe((_) => this.valueChanges.emit(this.value))
    );

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

  /**
   * Reset all data to original value. Useful sometimes, to re init the component (e.g. physical gear form).
   * Note: Keep @Input() attributes unchanged
   */
  public unload() {
    this.data = null;
    this.applyingValue = false;
    this._measurementValuesForm = null;
    this.markAsLoading();
    if (this.$pmfms.value) this.$pmfms.next(undefined);
  }


  markAsLoading(opts?: {step?: number; emitEvent?: boolean;}) {

    // /!\ do NOT used STARTING step anymore (only used to avoid to many refresh, BEFORE ngOnInit())
    const step = toNumber(opts && opts.step, MeasurementFormLoadingSteps.LOADING_PMFMS);

    // Emit, if changed
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

  waitIdle(opts?: WaitForOptions): Promise<boolean> {
    let idle$ = this.$loadingStep
      .pipe(
        // DEBUG
        //tap(_ => console.debug(this.logPrefix + 'waiting idle...')),

        filter(step => step >= MeasurementFormLoadingSteps.FORM_GROUP_READY),
        map(_ => true)
      );

    // Add timeout
    if (opts && opts.timeout) {
      idle$ = merge(
        idle$,
        timer(opts.timeout)
          .pipe(
            map(_ => {
              throw new Error(`waitIdle Timeout (after ${opts.timeout}ms)`);
            }))
      );
    }

    return firstNotNilPromise(idle$);
  }


  setValue(data: T, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any; waitIdle?: boolean;}) {
    this.applyValue(data, opts);
  }

  reset(data: T, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any; waitIdle?: boolean;}) {
    this.applyValue(data, opts);
  }

  /* -- protected methods -- */

  /**
   * /!\ should NOT be overwritten by subclasses. Use setFormValue() instead
   * @param data
   * @param opts
   */
  private async applyValue(data: T, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any; waitIdle?: boolean;}) {
    this.applyingValue = true;

    try {
      this.data = data;
      this.onApplyingEntity(data, opts);

      // Wait form is ready, before applying the data
      const waitIdle = (!opts || opts.waitIdle !== false) && this.options?.allowSetValueBeforePmfms !== true;
      if (waitIdle) await this.waitIdle();

      // Applying value to form (that should be ready).
      await this.updateView(this.data, opts);
    }
    catch(err) {
      console.error(err);
      this.error = err && err.message || err;
    }
    finally {
      this.markAsLoaded();
      this.applyingValue = false;
    }
  }

  protected onApplyingEntity(data: T, opts?: {[key: string]: any;}) {
    if (data.program?.label) {
      // Propage program
      this.setProgramLabel(data.program?.label);
    }
  }

  protected async updateView(data: T, opts?: { emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any; }) {
    // Warn is form is NOT ready
    if (this.loading && (this.options?.allowSetValueBeforePmfms !== true)) {
      console.warn(`${this.logPrefix} Trying to set value, but form not ready!`);
    }

    if (this.debug) console.debug(`${this.logPrefix} updateView() with:`, data);

    // Adapt measurement values to form (if not skip)
    if (!opts || opts.normalizeEntityToForm !== false) {
      MeasurementValuesUtils.normalizeEntityToForm(data, this.$pmfms.value, this.form);
    }

    // If a program has been filled, always keep it
    const program = this.programControl?.value;
    if (program) {
      data.program = program;
    }

    this.data = data;

    super.setValue(data, opts);

    if (!opts || opts.emitEvent !== false) {
      this.form.markAsPristine();
      this.form.markAsUntouched();
      this.markForCheck();
    }

    // Restore form status
    this.updateViewState({onlySelf: true, ...opts});

  }

  protected setProgramLabel(value: string, opts?: {emitEvent?: boolean}) {
    if (isNotNil(value) && this.$programLabel.value !== value) {

      this.$programLabel.next(value);

      // Reload pmfms
      if (!opts || opts.emitEvent !== false) this._onRefreshPmfms.emit();
    }
  }

  protected setStrategyLabel(value: string, opts?: {emitEvent?: boolean}) {
    if (isNotNil(value) && this.$strategyLabel.value !== value) {

      this.$strategyLabel.next(value);

      // Reload pmfms
      if (!opts || opts.emitEvent !== false) this._onRefreshPmfms.emit();
    }
  }

  protected setGearId(value: number, opts?: {emitEvent?: boolean}) {
    if (isNotNil(value) && this._gearId !== value) {
      this._gearId = value;

      // Reload pmfms
      if (!opts || opts.emitEvent !== false) this._onRefreshPmfms.emit();
    }
  }

  protected getValue(): T {
    const measurementValuesForm = this.measurementValuesForm;

    const json = this.form.value;

    if (measurementValuesForm) {
      // Find dirty pmfms, to avoid full update
      const dirtyPmfms = (this.$pmfms.value || []).filter(pmfm => measurementValuesForm.controls[pmfm.id]?.dirty);
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

  /**
   * Check if can load (must have: program, acquisition - and gear if required)
   */
  protected canLoadPmfms(): boolean{
    // Check if can load (must have: program, acquisition - and gear if required)
    if (isNil(this.programLabel)
      || isNil(this._acquisitionLevel)
      || (this.requiredStrategy && isNil(this.strategyLabel))
      || (this.requiredGear && isNil(this._gearId))) {

      // DEBUG
      //if (this.debug) console.debug(`${this.logPrefix} cannot load pmfms (missing some inputs)`);

      return false;
    }
    return true;
  }

  protected async loadPmfms() {
    if (!this.canLoadPmfms()) return;

    // DEBUG
    //if (this.debug) console.debug(`${this.logPrefix} loadPmfms()`);

    this.setLoadingProgression(MeasurementFormLoadingSteps.LOADING_PMFMS);
    //if (this.$pmfms.value) this.$pmfms.next(undefined);

    let pmfms;
    try {
      // Load pmfms
      pmfms = (await this.programRefService.loadProgramPmfms(
        this.programLabel,
        {
          strategyLabel: this.strategyLabel,
          acquisitionLevel: this._acquisitionLevel,
          gearId: this._gearId
        })) || [];
    } catch (err) {
      console.error(`${this.logPrefix} Error while loading pmfms: ${err && err.message || err}`, err);
      pmfms = undefined;
    }

    // Apply pmfms
    await this.setPmfms(pmfms);
  }

  resetPmfms() {
    this.markAsLoading();
    if (this.debug && this.$pmfms.value) console.warn(`${this.logPrefix} Reset pmfms`);
    if (this.$pmfms.value) this.$pmfms.next(undefined);
  }

  async setPmfms(value: IPmfm[] | Observable<IPmfm[]>): Promise<IPmfm[]> {

    // If undefined: reset pmfms
    if (!value) {
      this.resetPmfms();
      return undefined; // break
    }

    // DEBUG
    //if (this.debug) console.debug(`${this.logPrefix} setPmfms()`);

    // Mark as settings pmfms
    const previousLoadingStep = this.$loadingStep.value;
    this.setLoadingProgression(MeasurementFormLoadingSteps.SETTING_PMFMS);

    try {
      // Wait loaded, if observable
      let pmfms: IPmfm[];
      if (isObservable<IPmfm[]>(value)) {
        if (this.debug) console.debug(`${this.logPrefix} setPmfms(): waiting pmfms observable...`);
        pmfms = await firstNotNilPromise(value);
      } else {
        pmfms = value;
      }

      // Force all as optional
      if (this._forceOptional) {
        pmfms = pmfms.map(pmfm => {
          if (pmfm.required) {
            // Create a copy of each required pmfms
            // To keep unchanged the original entity
            pmfm = pmfm.clone();
            pmfm.required = false;
          }
          return pmfm;
        });
      }

      // Call the map function
      if (this.options?.mapPmfms) {
        const res = this.options.mapPmfms(pmfms);
        pmfms = (res instanceof Promise) ? await res : res;
      }

      // Apply (if changed)
      if (pmfms !== this.$pmfms.value) {
        // DEBUG log
        if (this.debug) console.debug(`${this.logPrefix} Pmfms changed {acquisitionLevel: '${this._acquisitionLevel}'}`, pmfms);

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
    catch(err) {
      console.error(`${this.logPrefix} Error while applying pmfms: ${err && err.message || err}`, err);
      this.resetPmfms();
      return undefined;
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
      }
    }

    // Call options function
    if (this.options && this.options.onUpdateFormGroup) {
      const res = this.options.onUpdateFormGroup(form);
      if (res instanceof Promise) {
        await res;
      }
    }

    if (this.debug) console.debug(`${this.logPrefix} Form controls updated`);
    this.setLoadingProgression(MeasurementFormLoadingSteps.FORM_GROUP_READY);

    if (!this.applyingValue) {
      // Update data in view
      if (this.data) {
        await this.updateView(this.data, {emitEvent: false});
      }
      // No data defined yet
      else {
        // Restore enable state (because form.setValue() can change it !)
        this.updateViewState({ onlySelf: true, emitEvent: false });
      }
    }

    return true;
  }

  protected updateViewState(opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    if (this._enable) {
      this.enable(opts);
    }
    else {
      this.disable(opts);
    }
  }

  protected get logPrefix(): string {
    const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
    return `[meas-values-form-${acquisitionLevel}]`;
  }


  protected markForCheck() {
    this.cd.markForCheck();
  }

}
