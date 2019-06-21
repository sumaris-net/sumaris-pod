import {ChangeDetectorRef, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {isNil, isNotNil, MeasurementUtils, PmfmStrategy} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {DateAdapter, FloatLabelType} from "@angular/material";
import {BehaviorSubject, merge, timer} from 'rxjs';
import {AppForm, AppFormUtils} from '../../core/core.module';
import {ProgramService} from "../../referential/referential.module";
import {FormBuilder, FormGroup} from '@angular/forms';
import {MeasurementsValidatorService} from '../services/measurement.validator';
import {debounce, debounceTime, filter, first, startWith, throttleTime} from "rxjs/operators";
import {IEntityWithMeasurement} from "../services/model/measurement.model";

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
    this.setValue(value);
  }

  public get value(): T {

    const json = this.form.value;

    const pmfmForm = this.form.get('measurementValues');
    if (pmfmForm && pmfmForm instanceof FormGroup) {
      // Find dirty pmfms, to avoid full update
      const dirtyPmfms = (this.$pmfms.getValue() || []).filter(pmfm => pmfmForm.controls[pmfm.pmfmId].dirty);
      if (dirtyPmfms.length) {
        json.measurementValues = Object.assign({}, this.data.measurementValues, MeasurementUtils.toEntityValues(pmfmForm.value, dirtyPmfms));
      }
    }

    this.data.fromObject(json);

    return this.data;
  }

  protected constructor(protected dateAdapter: DateAdapter<Moment>,
              protected measurementValidatorService: MeasurementsValidatorService,
              protected formBuilder: FormBuilder,
              protected programService: ProgramService,
              protected cd: ChangeDetectorRef,
              form: FormGroup
  ) {
    super(dateAdapter, form);

    // TODO: DEV only
    //this.debug = true;

    this.registerSubscription(
      this._onRefreshPmfms
        .subscribe(() => this.refreshPmfms('ngOnInit'))
    );
  }

  ngOnInit() {
    super.ngOnInit();

    // Update the form group
    this.registerSubscription(
      merge(
        this._onValueChanged,
        this.$pmfms.pipe(filter(isNotNil)),
      )
        //.pipe(throttleTime(100)) // Avoid redundant call
        .subscribe((_) => this.updateControls('merge', this.$pmfms.getValue())));

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
    this.form.markAsTouched();
    const pmfms = this.$pmfms.getValue();
    if (pmfms && this.form && this.form.controls['measurementValues']) {
      const pmfmForm = this.form.controls['measurementValues'] as FormGroup;
      pmfms.forEach(pmfm => {
        pmfmForm.controls[pmfm.pmfmId].markAsTouched({onlySelf: true});
      });
    }
    super.markForCheck();
  }

  /* -- protected methods -- */

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
    const pmfms = (await this.programService.loadProgramPmfms(
      this._program,
      {
        acquisitionLevel: this._acquisitionLevel,
        gear: this._gear
      })) || [];

    if (!pmfms.length && this.debug) {
      console.debug(`${this.logPrefix} No pmfm found (program=${this._program}, acquisitionLevel=${this._acquisitionLevel}, gear='${this._gear}'. Please fill program's strategies !`);
    }

    this.loadingPmfms = false;

    this.$pmfms.next(pmfms);

    if (this.enabled) this.loading = false;

    this.markForCheck();

    return pmfms;
  }

  public updateControls(event?: string, pmfms?: PmfmStrategy[]) {
    if (isNil(this.data)) return; // not ready
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

    let measurementValues = AppFormUtils.getFormValueFromEntity(this.data.measurementValues || {}, formGroup as FormGroup);
    measurementValues = MeasurementUtils.normalizeFormValues(measurementValues, pmfms);
    formGroup.patchValue(measurementValues, {
      onlySelf: true,
      emitEvent: false
    });
    //this.form.updateValueAndValidity();

    if (this.debug) console.debug(`${this.logPrefix} Form controls updated`);

    formGroup.markAsUntouched();
    formGroup.markAsPristine();

    // Restore enable state (because form.setValue() can change it !)
    if (this._enable) {
      formGroup.enable({onlySelf: true, emitEvent: false});
    } else if (formGroup.enabled) {
      formGroup.disable({onlySelf: true, emitEvent: false});
    }

    // if (this.enabled)
    this.loading = false;
    this.loadingControls = false;

    this.markForCheck();

    return true;
  }

  /** -- protected methods  -- */

  public setValue(data: T) {
    if (this.data === data) return; // skip
    this.data = data;
    this._onValueChanged.emit(data);

    // Wait pmfms load
    const pmfms = this.$pmfms.getValue();
    if (!pmfms || this.loadingPmfms || this.loadingControls) {
      if (this.debug) console.debug(`${this.logPrefix} setValue(): waiting pmfms or form...`);
      this.$pmfms
        .pipe(
          filter(isNotNil),
          throttleTime(100), // groups pmfms updates event, if many updates in few duration
          first()
        )
        .subscribe((pmfms) => this.setValue(data));
      return;
    }

    // Apply to the form
    super.setValue(data);

  }

  protected get logPrefix(): string {
    const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
    return `[meas-values-form-${acquisitionLevel}]`;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
