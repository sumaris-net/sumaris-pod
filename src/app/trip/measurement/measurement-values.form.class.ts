import {ChangeDetectorRef, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {isNil, isNotNil, PmfmStrategy} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {DateAdapter, FloatLabelType} from "@angular/material";
import {BehaviorSubject, Observable} from 'rxjs';
import {AppForm, LocalSettingsService} from '../../core/core.module';
import {ProgramService} from "../../referential/referential.module";
import {FormBuilder, FormGroup} from '@angular/forms';
import {MeasurementsValidatorService} from '../services/measurement.validator';
import {filter, first, throttleTime} from "rxjs/operators";
import {IEntityWithMeasurement, MeasurementValuesUtils} from "../services/model/measurement.model";

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
  protected data: T;

  loading = false; // Important, must be false
  loadingPmfms = true; // Important, must be true
  loadingControls = true; // Important, must be true

  $pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  $loadingControls = new BehaviorSubject<boolean>(true);

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
                        form: FormGroup,
                        protected options?: MeasurementValuesFormOptions<T>
  ) {
    super(dateAdapter, form, settings);

    // TODO: DEV only
    //this.debug = true;

    this.registerSubscription(
      this._onRefreshPmfms
        .subscribe(() => this.refreshPmfms('constructor'))
    );

    this.registerSubscription(
      this.$pmfms.pipe(
        filter(isNotNil)
      )
        .subscribe((pmfms) => this.updateControls('constructor', pmfms))
    );
  }

  ngOnInit() {
    super.ngOnInit();

    // Listen form changes
    this.registerSubscription(
      this.form.valueChanges
        .takeWhile(() => !this.loading)
        .subscribe((_) => {
          if (!this.loading && !this.loadingPmfms && this.valueChanges.observers.length) {
            this.valueChanges.emit(this.value);
          }
        })
    );

    if (this.data) {
      this._onValueChanged.emit(this.data);
    }
  }

  public markAsTouched() {
    super.markAsTouched();

    // Propage to pmfms control
    const pmfms = this.$pmfms.getValue();
    if (pmfms && this.form && this.form.controls['measurementValues']) {
      const pmfmForm = this.form.controls['measurementValues'] as FormGroup;
      pmfms.map(pmfm => pmfmForm.controls[pmfm.pmfmId])
        .forEach(control => control.markAsTouched({onlySelf: true}));
    }
  }

  /* -- protected methods -- */

  protected async asyncPmfms(): Promise<PmfmStrategy[]> {
    return this.$pmfms.getValue() || (await this.$pmfms.pipe(filter(isNotNil), first()).toPromise());
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

  public async updateControls(event?: string, pmfms?: PmfmStrategy[]) {
    //if (isNil(this.data)) return; // not ready
    pmfms = pmfms || this.$pmfms.getValue();

    let formGroup = this.form.get('measurementValues');
    if (formGroup && formGroup.enabled) {
      formGroup.disable({onlySelf: true, emitEvent: false});
    }

    // Waiting end of pmfm load
    if (!pmfms || this.loadingPmfms) {
      if (this.debug) console.debug(`${this.logPrefix} updateControls(${event}): waiting pmfms...`);
      this.$pmfms
        .pipe(
          filter(isNotNil),
          throttleTime(100), // groups pmfms updates event, if many updates in few duration
          first()
        )
        .subscribe((pmfms) => this.updateControls(event, pmfms));
      return;
    }

    this.loadingControls = true;
    this.$loadingControls.next(true);
    this.loading = true;

    if (event) if (this.debug) console.debug(`${this.logPrefix} updateControls(${event})...`);

    // No pmfms (= empty form)
    if (!pmfms.length) {
      // Reset form
      if (formGroup && formGroup instanceof FormGroup) {
        this.measurementValidatorService.updateFormGroup(formGroup, []);
        formGroup.reset({}, {onlySelf: true, emitEvent: false});
      }
      this.loading = false;
      this.loadingControls = false;
      this.$loadingControls.next(false);
      return true;
    }

    if (this.debug) console.debug(`${this.logPrefix} Updating form, using pmfms:`, pmfms);

    // Create measurementValues form group
    if (!formGroup) {
      formGroup = this.measurementValidatorService.getFormGroup(pmfms);
      this.form.addControl('measurementValues', formGroup);
      formGroup.disable({onlySelf: true, emitEvent: false});
    }

    // Or update if already exist
    else {
      this.measurementValidatorService.updateFormGroup(formGroup as FormGroup, pmfms);
    }

    // Call options function
    if (this.options && this.options.onUpdateControls) {
      const res = this.options.onUpdateControls(this.form);
      if (res instanceof Promise) {
        await res;
      }
    }

    // if (this.enabled)
    this.loading = false;
    this.loadingControls = false;
    this.$loadingControls.next(false);

    if (this.debug) console.debug(`${this.logPrefix} Form controls updated`);

    if (this.data) {
      // Adapt measurement values to form
      MeasurementValuesUtils.normalizeFormEntity(this.data, pmfms, this.form);

      formGroup.patchValue(this.data.measurementValues, {
        onlySelf: true,
        emitEvent: false
      });
    }

    this.markAsUntouched();
    this.markAsPristine();

    // Restore enable state (because form.setValue() can change it !)
    if (this._enable) {
      formGroup.enable({onlySelf: true, emitEvent: false});
    } else if (formGroup.enabled) {
      formGroup.disable({onlySelf: true, emitEvent: false});
    }

    this.markForCheck();

    return true;
  }

  public setValue(data: T) {
    if (this.$loadingControls.getValue()) {
      throw Error("Form not ready yet. Please use safeSetValue() instead!");
    }

    // Adapt measurement values to form
    MeasurementValuesUtils.normalizeFormEntity(data, this.$pmfms.getValue(), this.form);

    super.setValue(data);

    this.markAsUntouched();
    this.markAsPristine();

    // Restore enable state (because form.setValue() can change it !)
    const measFormGroup = this.form.get('measurementValues');
    if (this._enable) {
      measFormGroup.enable({onlySelf: true, emitEvent: false});
    } else if (measFormGroup.enabled) {
      measFormGroup.disable({onlySelf: true, emitEvent: false});
    }
  }

  public async onReady() {
    // Wait pmfms load, and controls load
    if (this.$loadingControls.getValue()) {
      //if (this.debug) console.debug(`${this.logPrefix} waiting form to be ready...`);
      await this.$loadingControls
        .pipe(
          filter((loadingControls) => loadingControls === false),
          throttleTime(100), // groups event, if many updates in few duration
          first()
        ).toPromise();
    }
  }

  /** -- protected methods  -- */

  /**
   * Wait form is ready, before setting the value to form
   * @param data
   */
  protected async safeSetValue(data: T) {
    if (this.data === data) return; // skip if same
    this.data = data;
    this._onValueChanged.emit(data);

    // Wait form controls ready
    await this.onReady();

    this.setValue(this.data);
  }

  protected getValue(): T {
    const json = this.form.value;

    const pmfmForm = this.form.get('measurementValues');
    if (pmfmForm && pmfmForm instanceof FormGroup) {
      // Find dirty pmfms, to avoid full update
      const dirtyPmfms = (this.$pmfms.getValue() || []).filter(pmfm => pmfmForm.controls[pmfm.pmfmId].dirty);
      if (dirtyPmfms.length) {
        json.measurementValues = Object.assign({}, this.data.measurementValues, MeasurementValuesUtils.toEntityValues(pmfmForm.value, dirtyPmfms));
      }
    }

    this.data.fromObject(json);

    return this.data;
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

  protected get logPrefix(): string {
    const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
    return `[meas-values-form-${acquisitionLevel}]`;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
