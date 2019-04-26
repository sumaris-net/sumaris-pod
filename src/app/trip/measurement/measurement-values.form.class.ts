import {ChangeDetectorRef, EventEmitter, Input, Output} from '@angular/core';
import {isNil, isNotNil, MeasurementUtils, PmfmStrategy} from "../services/trip.model";
import {Platform} from "@ionic/angular";
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {BehaviorSubject, merge, Subject} from 'rxjs';
import {zip} from "rxjs/observable/zip";
import {AppForm, AppFormUtils} from '../../core/core.module';
import {ProgramService} from "../../referential/referential.module";
import {FormBuilder, FormGroup} from '@angular/forms';
import {MeasurementsValidatorService} from '../services/measurement.validator';
import {filter, first, mergeMap, startWith, throttleTime} from "rxjs/operators";

export abstract class MeasurementValuesForm<T extends { measurementValues: { [key: string]: any } }> extends AppForm<T> {

  protected _onValueChanged = new EventEmitter<any>();
  protected _onRefreshPmfms = new EventEmitter<any>();
  protected _program: string;
  protected _gear: string = null;
  protected _acquisitionLevel: string;
  protected data: T;

  loading = false;
  loadingPmfms = true;
  pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);

  @Input() requiredGear: boolean = false;

  get program(): string {
    return this._program;
  }

  @Input()
  set program(value: string) {
    if (this._program === value) return; // Skip if same
    this._program = value;
    if (!this.loading) {
      this._onRefreshPmfms.emit('set program');
    }
  }

  get acquisitionLevel(): string {
    return this._acquisitionLevel;
  }

  @Input()
  set acquisitionLevel(value: string) {
    if (this._acquisitionLevel !== value && isNotNil(value)) {
      this._acquisitionLevel = value;
      if (!this.loading) this._onRefreshPmfms.emit('set acquisitionLevel');
    }
  }

  get gear(): string {
    return this._gear;
  }

  @Input()
  set gear(value: string) {
    if (this._gear !== value && isNotNil(value)) {
      this._gear = value;
      if (!this.loading || this.requiredGear) {
        this._onRefreshPmfms.emit('set gear');
      }
    }
  }

  public set value(value: T) {
    if (this.data !== value) {
      this.data = value;
      this._onValueChanged.emit();
    }
  }

  public get value(): T {

    if (this.form.controls.measurementValues) {
      const pmfmForm = this.form.controls.measurementValues as FormGroup;
      // Find dirty pmfms, to avoid full update
      const dirtyPmfms = (this.pmfms.getValue() || []).filter(pmfm => pmfmForm.controls[pmfm.pmfmId].dirty);
      if (dirtyPmfms.length) {
        this.data.measurementValues = Object.assign({}, this.data.measurementValues, MeasurementUtils.toEntityValues(pmfmForm.value, dirtyPmfms));
      }
    }

    return this.data;
  }

  @Output()
  valueChanges: EventEmitter<any> = new EventEmitter<any>();

  constructor(protected dateAdapter: DateAdapter<Moment>,
              protected platform: Platform,
              protected measurementValidatorService: MeasurementsValidatorService,
              protected formBuilder: FormBuilder,
              protected programService: ProgramService,
              form: FormGroup,
              protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, platform, form);
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this._onRefreshPmfms.asObservable()
        .subscribe(() => this.refreshPmfms('ngOnInit'))
    );

    // Update the form group
    this.registerSubscription(
      merge(
        this._onValueChanged.pipe(startWith('ngOnInit')),
        this.pmfms.pipe(filter(isNotNil)),
      )
        .subscribe((_) => this.updateControls('merge', this.pmfms.getValue())));

    // Listen form changes
    this.form.valueChanges
      .subscribe((_) => {
        if (!this.loading && this.valueChanges.observers.length) {
          this.valueChanges.emit(this.value);
        }
      });
  }

  public markAsTouched() {
    this.form.markAsTouched();
    const pmfms = this.pmfms.getValue();
    if (pmfms && this.form && this.form.controls['measurementValues']) {
      const pmfmForm = this.form.controls['measurementValues'] as FormGroup;
      pmfms.forEach(pmfm => {
        pmfmForm.controls[pmfm.pmfmId].markAsTouched();
      });
    }
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

    // Load pmfms
    const pmfms = (await this.programService.loadProgramPmfms(
      this._program,
      {
        acquisitionLevel: this._acquisitionLevel,
        gear: this._gear
      })) || [];

    if (!pmfms.length && this.debug) {
      console.warn(`${this.logPrefix} No pmfm found for '${this._program}' and gear '${this._gear}'. Please fill program's strategies for acquisition level '${this._acquisitionLevel}'.`);
    }

    this.loadingPmfms = false;

    this.pmfms.next(pmfms);

    if (this.enabled) this.loading = false;

    this.markForCheck();
  }

  public updateControls(event?: string, pmfms?: PmfmStrategy[]) {
    if (isNil(this.data)) return; // not ready
    pmfms = pmfms || this.pmfms.getValue();

    if (this.form.enabled) {
      this.form.disable();
    }

    // Waiting end of pmfm load
    if (!pmfms || this.loadingPmfms) {
      if (this.debug) console.debug(`${this.logPrefix} updateControls(${event}): waiting pmfms...`);
      this.pmfms
        .pipe(
          filter(isNotNil),
          throttleTime(100), // groups pmfms updates event, if many updates in few duration
          first()
        )
        .subscribe((pmfms) => this.updateControls(event, pmfms));
      return;
    }

    this.loading = true;

    if (event) if (this.debug) console.debug(`${this.logPrefix} updateControls(${event})...`);

    // No pmfms (= empty form)
    if (!pmfms.length) {
      // Reset form
      this.measurementValidatorService.updateFormGroup(this.form, []);
      this.form.reset({}, {onlySelf: true, emitEvent: false});
      this.loading = false;
      return true;
    }

    const now = Date.now();
    if (this.debug) console.debug(`${this.logPrefix} Updating form, using pmfms:`, pmfms);

    // Create measurementValues form group
    if (!this.form.controls['measurementValues']) {
      this.form.addControl('measurementValues', this.measurementValidatorService.getFormGroup(pmfms));
    }

    // Or update if already exist
    else {
      const form = this.form.controls['measurementValues'] as FormGroup;
      this.measurementValidatorService.updateFormGroup(form, pmfms);
    }

    const json = AppFormUtils.getFormValueFromEntity(this.data, this.form);
    json.measurementValues = MeasurementUtils.normalizeFormValues(json.measurementValues, pmfms);
    this.form.patchValue(json, {
      onlySelf: true,
      emitEvent: false
    });
    //this.form.updateValueAndValidity();

    if (this.debug) console.debug(`${this.logPrefix} Form controls updated`);

    this.markAsUntouched();
    this.markAsPristine();

    // Restore enable state (because form.setValue() can change it !)
    if (this._enable) {
      this.enable({onlySelf: true, emitEvent: false});
    } else if (this.form.enabled) {
      this.disable({onlySelf: true, emitEvent: false});
    }

    this.loading = false;

    this.markForCheck();

    return true;
  }

  /** -- protected methods  -- */

  protected get logPrefix(): string {
    const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
    return `[meas-values-form-${acquisitionLevel}]`;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
