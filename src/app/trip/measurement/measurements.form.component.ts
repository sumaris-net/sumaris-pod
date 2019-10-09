import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import {Measurement, MeasurementUtils, PmfmStrategy} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {DateAdapter, FloatLabelType} from "@angular/material";
import {BehaviorSubject} from 'rxjs';
import {filter, throttleTime} from "rxjs/operators";
import {AppForm, LocalSettingsService} from '../../core/core.module';
import {ProgramService} from "../../referential/referential.module";
import {FormBuilder} from '@angular/forms';
import {MeasurementsValidatorService} from '../services/measurement.validator';
import {isNil, isNotNil} from '../../shared/shared.module';
import {MeasurementType, MeasurementValuesUtils} from "../services/model/measurement.model";
import {filterNotNil, firstNotNilPromise} from "../../shared/observables";

@Component({
  selector: 'form-measurements',
  templateUrl: './measurements.form.component.html',
  styleUrls: ['./measurements.form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MeasurementsForm extends AppForm<Measurement[]> implements OnInit {

  private _onRefreshPmfms = new EventEmitter<any>();
  private _program: string;
  private _gear: string;
  private _acquisitionLevel: string;
  protected data: Measurement[];

  loading = false; // Important, must be false
  loadingPmfms = true; // Important, must be true
  $loadingControls = new BehaviorSubject<boolean>(true);
  loadingValue = false;

  $pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);

  @Input() showError = false;

  @Input() compact = false;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() requiredGear = false;

  @Input() entityName: MeasurementType;

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
  set value(value: Measurement[]) {
    this.safeSetValue(value);
  }

  get value(): Measurement[] {
    return this.getValue();
  }

  constructor(protected dateAdapter: DateAdapter<Moment>,
              protected measurementValidatorService: MeasurementsValidatorService,
              protected formBuilder: FormBuilder,
              protected programService: ProgramService,
              protected settings: LocalSettingsService,
              protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, formBuilder.group({}), settings);

    // TODO: DEV only
    //this.debug = true;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this._onRefreshPmfms
        .subscribe(() => this.refreshPmfms('constructor'))
    );

    // Auto update the view, when pmfms are filled
    this.registerSubscription(
      filterNotNil(this.$pmfms)
        .subscribe((pmfms) => this.updateControls('constructor', pmfms))
    );

    // Listen form changes
    this.registerSubscription(
      this.form.valueChanges
        .pipe(
          filter(() => !this.loading && !this.loadingPmfms && this.valueChanges.observers.length > 0)
        )
        .subscribe((_) => this.valueChanges.emit(this.value))
    );
  }

  setValue(data: Measurement[], opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {
    if (this.$loadingControls.getValue()) {
      throw Error("Form not ready yet. Please use safeSetValue() instead!");
    }

    const pmfms = this.$pmfms.getValue();
    this.data = MeasurementUtils.initAllMeasurements(data, pmfms, this.entityName);

    const json = MeasurementValuesUtils.normalizeValuesToForm(MeasurementUtils.toMeasurementValues(this.data), pmfms);

    this.form.patchValue(json, opts);

    this.markAsUntouched(opts);
    this.markAsPristine(opts);

    // Restore form status
    this.restoreFormStatus({onlySelf: true, emitEvent: opts && opts.emitEvent});
  }

  async ready(): Promise<void> {
    // Wait pmfms load, and controls load
    if (this.$loadingControls.getValue() !== false || this.loadingPmfms !== false) {
      if (this.debug) console.debug(`${this.logPrefix} waiting form to be ready...`);
      await firstNotNilPromise(this.$loadingControls
        .pipe(
          filter((loadingControls) => loadingControls === false && this.loadingPmfms === false)
        ));
    }
  }

  /* -- protected methods -- */

  /**
   * Wait form is ready, before setting the value to form
   * @param data
   */
  protected async safeSetValue(data: Measurement[]) {
    if (this.data === data) return; // skip if same

    // Will avoid data to be set inside function updateControls()
    this.loadingValue = true;
    this.data = data;

    // Wait form controls ready
    await this.ready();

    this.setValue(this.data, {emitEvent: true});

    this.loadingValue = false;
    this.loading = false;
  }

  protected getValue(): Measurement[] {

    if (this.loading) return this.data; // Avoid to return not loading data

    // Find dirty pmfms, to avoid full update
    const dirtyPmfms = (this.$pmfms.getValue() || []).filter(pmfm => this.form.controls[pmfm.pmfmId].dirty);
    if (dirtyPmfms.length) {

      // Update measurements value
      const json = this.form.value;
      MeasurementUtils.setValuesByFormValues(this.data, json, dirtyPmfms);
    }

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
    const pmfms = (await this.programService.loadProgramPmfms(
      this._program,
      {
        acquisitionLevel: this._acquisitionLevel,
        gear: this._gear
      })) || [];

    if (!pmfms.length && this.debug) {
      console.debug(`${this.logPrefix} No pmfm found (program=${this._program}, acquisitionLevel=${this._acquisitionLevel}, gear='${this._gear}'. Please fill program's strategies !`);
    }

    // Apply
    this.loadingPmfms = false;
    this.$pmfms.next(pmfms);

    if (this.enabled) this.loading = false;

    this.markForCheck();

    return pmfms;
  }

  protected async updateControls(event?: string, pmfms?: PmfmStrategy[]) {
    //if (isNil(this.data)) return; // not ready
    pmfms = pmfms || this.$pmfms.getValue();

    if (this.form.enabled) {
      this.form.disable();
    }

    // Waiting end of pmfm load
    if (!pmfms || this.loadingPmfms || !this.form) {
      if (this.debug) console.debug(`${this.logPrefix} updateControls(${event}): waiting pmfms...`);
      pmfms = await firstNotNilPromise(
        // groups pmfms updates event, if many updates in few duration
        this.$pmfms.pipe(throttleTime(100))
      );
    }

    if (this.$loadingControls.getValue() !== true) this.$loadingControls.next(true);
    this.loading = true;

    if (event) if (this.debug) console.debug(`${this.logPrefix} updateControls(${event})...`);

    // No pmfms (= empty form)
    if (!pmfms.length) {
      // Reset form
      this.measurementValidatorService.updateFormGroup(this.form, []);
      this.form.reset({}, {onlySelf: true, emitEvent: false});
      this.loading = this.loadingValue;
      return true;
    }

    else {
      if (this.debug) console.debug(`${this.logPrefix} Updating form controls, using pmfms:`, pmfms);

      // Update the existing form
      this.measurementValidatorService.updateFormGroup(this.form, pmfms);
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

  protected restoreFormStatus(opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {
    // Restore enable state (because form.setValue() can change it !)
    if (this._enable) {
      this.form.enable(opts);
    } else if (this.form.enabled) {
      this.form.disable(opts);
    }
  }

  protected get logPrefix(): string {
    const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
    return `[meas-form-${acquisitionLevel}]`;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
