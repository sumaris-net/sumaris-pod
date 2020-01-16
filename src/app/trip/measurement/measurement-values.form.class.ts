import {ChangeDetectorRef, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {isNil, isNotNil, PmfmStrategy} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {DateAdapter, FloatLabelType} from "@angular/material";
import {BehaviorSubject, Observable} from 'rxjs';
import {AppForm} from '../../core/core.module';
import {ProgramService} from "../../referential/referential.module";
import {FormBuilder, FormGroup} from '@angular/forms';
import {MeasurementsValidatorService} from '../services/measurement.validator';
import {filter, first, throttleTime} from "rxjs/operators";
import {IEntityWithMeasurement, MeasurementValuesUtils} from "../services/model/measurement.model";
import {filterNotNil, firstNotNilPromise} from "../../shared/observables";
import {LocalSettingsService} from "../../core/services/local-settings.service";

export interface MeasurementValuesFormOptions<T extends IEntityWithMeasurement<T>> {
  mapPmfms?: (pmfms: PmfmStrategy[]) => PmfmStrategy[] | Promise<PmfmStrategy[]>;
  onUpdateControls?: (formGroup: FormGroup) => void | Promise<void>;
}

export abstract class MeasurementValuesForm<T extends IEntityWithMeasurement<T>> extends AppForm<T> implements OnInit {

  protected _onValueChanged = new EventEmitter<T>();
  protected _onRefreshPmfms = new EventEmitter<any>();
  protected _program: string;
  protected _gear: string = null;
  protected _acquisitionLevel: string;
  protected _ready = false;
  protected data: T;

  loading = false; // Important, must be false
  loadingPmfms = true; // Important, must be true
  $loadingControls = new BehaviorSubject<boolean>(true);
  loadingValue = false;

  $pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);

  @Input() compact = false;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() requiredGear = false;

  @Output()
  valueChanges: EventEmitter<any> = new EventEmitter<any>();

  @Input()
  set program(value: string) {
    if (this._program !== value && isNotNil(value)) {
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

  @Input()
  set gear(value: string) {
    if (this._gear !== value && isNotNil(value)) {
      this._gear = value;
      if (!this.loading || this.requiredGear) this._onRefreshPmfms.emit();
    }
  }

  get gear(): string {
    return this._gear;
  }

  @Input()
  public set value(value: T) {
    this.safeSetValue(value);
  }

  public get value(): T {
    return this.getValue();
  }

  @Input() set pmfms(pmfms: Observable<PmfmStrategy[]> | PmfmStrategy[]) {
    this.loading = true;
    this.setPmfms(pmfms);
  }

  protected constructor(protected dateAdapter: DateAdapter<Moment>,
                        protected measurementValidatorService: MeasurementsValidatorService,
                        protected formBuilder: FormBuilder,
                        protected programService: ProgramService,
                        protected settings: LocalSettingsService,
                        protected cd: ChangeDetectorRef,
                        form?: FormGroup,
                        protected options?: MeasurementValuesFormOptions<T>
  ) {
    super(dateAdapter, form, settings);

    // TODO: DEV only
    //this.debug = true;

    this.registerSubscription(
      this._onRefreshPmfms
        .subscribe(() => this.refreshPmfms('constructor'))
    );

    // Auto update the view, when pmfms are filled
    this.registerSubscription(
      filterNotNil(this.$pmfms)
        .subscribe((pmfms) => this.updateControls('constructor', pmfms))
    );
  }

  ngOnInit() {
    super.ngOnInit();

    // Listen form changes
    this.registerSubscription(
      this.form.valueChanges
        .pipe(
          filter(() => !this.loading && !this.loadingPmfms && this.valueChanges.observers.length > 0)
        )
        .subscribe((_) => this.valueChanges.emit(this.value))
    );

    if (this.data) {
      this._onValueChanged.emit(this.data);
    }
  }

  setValue(data: T, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any; }) {
    if (!this.isReady() || !this.data) {
      this.safeSetValue(data, opts); // Loop
      return;
    }

    // Adapt measurement values to form (if not skip)
   // if (!opts || opts.normalizeEntityToForm !== false) {
      MeasurementValuesUtils.normalizeEntityToForm(data, this.$pmfms.getValue(), this.form);
    //}

    super.setValue(data, opts);

    // Restore form status
    this.restoreFormStatus({onlySelf: true, emitEvent: opts && opts.emitEvent});
  }

  reset(data?: T, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; [key: string]: any;}) {
    if (!this.isReady() || !this.data) {
      this.safeSetValue(data, opts); // Loop
      return;
    }

    // Adapt measurement values to form (if not skip)
    //if (!opts || opts.normalizeEntityToForm !== false) {
      MeasurementValuesUtils.normalizeEntityToForm(data, this.$pmfms.getValue(), this.form);
    //}

    super.reset(data, opts);

    // Restore form status
    this.restoreFormStatus({onlySelf: true, emitEvent: opts && opts.emitEvent});
    // // Reuse safeSetValue, to avoid code duplication
    // this.safeSetValue(data, opts)
    //   // Then mark the form as pristine
    //   .then(() => this.markAsPristine());
  }

  isReady(): boolean {
    return this._ready || (!this.$loadingControls.getValue()  && !this.loadingPmfms);
  }

  async ready(): Promise<void> {
    // Wait pmfms load, and controls load
    if (this.$loadingControls.getValue() !== false || this.loadingPmfms !== false) {
      this._ready = false;
      if (this.debug) console.debug(`${this.logPrefix} waiting form to be ready...`);
      await firstNotNilPromise(this.$loadingControls
        .pipe(
          filter((loadingControls) => loadingControls === false && this.loadingPmfms === false)
        ));
    }
    this._ready = true;
  }

  /* -- protected methods -- */

  /**
   * Wait form is ready, before setting the value to form
   * @param data
   */
  protected async safeSetValue(data: T, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; }) {
    if (this.data === data) return; // skip if same

    // Will avoid data to be set inside function updateControls()
    this.loadingValue = true;

    // This line is required by physical gear modal
    this.data = data;
    this._onValueChanged.emit(data);

    // Wait form controls ready, if need
    if (!this._ready) await this.ready();

    this.setValue(this.data, {...opts, emitEvent: true});

    this.loadingValue = false;
    this.loading = false;
  }

  protected getValue(): T {
    const json = this.form.value;

    const pmfmForm = this.form.get('measurementValues');
    if (pmfmForm && pmfmForm instanceof FormGroup) {
      // Find dirty pmfms, to avoid full update
      const dirtyPmfms = (this.$pmfms.getValue() || []).filter(pmfm => pmfmForm.controls[pmfm.pmfmId].dirty);
      if (dirtyPmfms.length) {
        json.measurementValues = Object.assign({}, this.data.measurementValues, MeasurementValuesUtils.normalizeValuesToModel(pmfmForm.value, dirtyPmfms));
      }
    }

    this.data.fromObject(json);

    return this.data;
  }

  protected async refreshPmfms(event?: any): Promise<PmfmStrategy[]> {
    // Skip if missing: program, acquisition (or gear, if required)
    if (isNil(this._program) || isNil(this._acquisitionLevel) || (this.requiredGear && isNil(this._gear))) {
      return undefined;
    }

    if (this.debug) console.debug(`${this.logPrefix} refreshPmfms(${event})`);

    this.loading = true;
    this.loadingPmfms = true;

    this.$pmfms.next(null);

    // Load pmfms
    let pmfms = (await this.programService.loadProgramPmfms(
      this._program,
      {
        acquisitionLevel: this._acquisitionLevel,
        gear: this._gear
      })) || [];

    if (!pmfms.length && this.debug) {
      console.debug(`${this.logPrefix} No pmfm found (program=${this._program}, acquisitionLevel=${this._acquisitionLevel}, gear='${this._gear}'. Please fill program's strategies !`);
    }

    pmfms = await this.setPmfms(pmfms);

    if (this.enabled) this.loading = false;

    this.markForCheck();

    return pmfms;
  }

  protected async updateControls(event?: string, pmfms?: PmfmStrategy[]) {
    //if (isNil(this.data)) return; // not ready
    pmfms = pmfms || this.$pmfms.getValue();

    // Waiting end of pmfm load
    if (!pmfms || this.loadingPmfms || !this.form) {
      if (this.debug) console.debug(`${this.logPrefix} updateControls(${event}): waiting pmfms...`);
      pmfms = await firstNotNilPromise(
        // groups pmfms updates event, if many updates in few duration
        this.$pmfms.pipe(throttleTime(100))
      );
    }

    let measFormGroup = this.form.controls['measurementValues'];
    if (measFormGroup && measFormGroup.enabled) {
      measFormGroup.disable({onlySelf: true, emitEvent: false});
    }

    if (this.$loadingControls.getValue() !== true) {
      this._ready = false;
      this.$loadingControls.next(true);
    }
    this.loading = true;

    if (event) if (this.debug) console.debug(`${this.logPrefix} updateControls(${event})...`);

    // No pmfms (= empty form)
    if (!pmfms.length) {
      // Reset form
      if (measFormGroup && measFormGroup instanceof FormGroup) {
        this.measurementValidatorService.updateFormGroup(measFormGroup, {pmfms: []});
        measFormGroup.reset({}, {onlySelf: true, emitEvent: false});
      }
    }

    else {
      if (this.debug) console.debug(`${this.logPrefix} Updating form controls, using pmfms:`, pmfms);

      // Create measurementValues form group
      if (!measFormGroup) {
        measFormGroup = this.measurementValidatorService.getFormGroup(null, {pmfms});
        this.form.addControl('measurementValues', measFormGroup);
        measFormGroup.disable({onlySelf: true, emitEvent: false});
      }

      // Or update if already exist
      else {
        this.measurementValidatorService.updateFormGroup(measFormGroup as FormGroup, {pmfms});
      }
    }

    // Call options function
    if (this.options && this.options.onUpdateControls) {
      const res = this.options.onUpdateControls(this.form);
      if (res instanceof Promise) {
        await res;
      }
    }

    this.loading = this.loadingValue; // Keep loading status if data not apply fully
    this.$loadingControls.next(false);

    if (this.debug) console.debug(`${this.logPrefix} Form controls updated`);

    // If data has already been set, apply it again
    if (!this.loadingValue) {
      if (this.data && pmfms.length && this.form) {
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

  async setPmfms(pmfms: PmfmStrategy[] | Observable<PmfmStrategy[]>): Promise<PmfmStrategy[]> {
    if (!pmfms) return undefined; // skip

    // Wait loaded
    if (pmfms instanceof Observable) {
      //console.log("Form: waiting pmfms before emit $pmfms")
      pmfms = await pmfms.pipe(filter(isNotNil), first()).toPromise();
    }

    // Map
    if (this.options && this.options.mapPmfms) {
      const res = this.options.mapPmfms(pmfms);
      pmfms = (res instanceof Promise) ? await res : res;
    }

    if (pmfms instanceof Array && pmfms !== this.$pmfms.getValue()) {

      // Apply
      this.loadingPmfms = false;
      this.$pmfms.next(pmfms);
    }

    return pmfms;
  }

  protected restoreFormStatus(opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {
    const measFormGroup = this.form.get('measurementValues');
    // Restore enable state (because form.setValue() can change it !)
    if (this._enable) {
      measFormGroup.enable(opts);
    } else if (measFormGroup.enabled) {
      measFormGroup.disable(opts);
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
