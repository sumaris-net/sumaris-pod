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
import {BehaviorSubject, merge} from 'rxjs';
import {filter, first, startWith, throttleTime} from "rxjs/operators";
import {AppForm, LocalSettingsService} from '../../core/core.module';
import {ProgramService} from "../../referential/referential.module";
import {FormBuilder} from '@angular/forms';
import {MeasurementsValidatorService} from '../services/measurement.validator';
import {TranslateService} from '@ngx-translate/core';
import {isNil, isNotNil} from '../../shared/shared.module';
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {filterNotNil, firstNotNilPromise} from "../../shared/observables";

@Component({
  selector: 'form-measurements',
  templateUrl: './measurements.form.component.html',
  styleUrls: ['./measurements.form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MeasurementsForm extends AppForm<Measurement[]> implements OnInit {

  private _onValueChanged = new EventEmitter<any>();
  private _onRefreshPmfms = new EventEmitter<any>();
  private _program: string;
  private _gear: string;
  private _acquisitionLevel: string;
  protected data: Measurement[];

  loading = false; // Important, must be false
  loadingPmfms = true; // Important, must be true
  loadingControls = true; // Important, must be true

  $pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  $loadingControls = new BehaviorSubject<boolean>(true);

  @Input() showError = false;

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
  set value(value: any) {
    this.safeSetValue(value);
  }

  get value(): any {
    return this.getValue();
  }

  constructor(protected dateAdapter: DateAdapter<Moment>,
              protected measurementValidatorService: MeasurementsValidatorService,
              protected formBuilder: FormBuilder,
              protected programService: ProgramService,
              protected translate: TranslateService,
              protected cd: ChangeDetectorRef,
              protected settings: LocalSettingsService
  ) {
    super(dateAdapter, formBuilder.group({}), settings);

    // TODO: DEV only
    //this.debug = true;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this._onRefreshPmfms
        .subscribe(() => this.refreshPmfms('ngOnInit'))
    );

    // Auto update the view, when pmfms are filled
    this.registerSubscription(
      filterNotNil(this.$pmfms)
      .subscribe((_) => this.updateControls('merge', this.$pmfms.getValue()))
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

  public markAsTouched() {
    // Force each sub-controls
    (this.$pmfms.getValue() || []).forEach(p => {
      const control = this.form.get(p.pmfmId.toString());
      if (control) control.markAsTouched();
    });
    super.markAsTouched();
  }

  /* -- protected methods -- */
  /**
   * Wait form is ready, before setting the value to form
   * @param data
   */
  protected async safeSetValue(data: Measurement[]) {
    if (this.data === data) return; // skip if same

    this.data = data;
    this._onValueChanged.emit(data);

    // Wait form controls ready
    await this.onReady();

    const pmfms = this.$pmfms.getValue();
    this.data = MeasurementUtils.initAllMeasurements(this.data, pmfms);

    const json = MeasurementValuesUtils.normalizeValuesToForm(MeasurementUtils.toMeasurementValues(this.data), pmfms);

    this.form.patchValue(json, {emitEvent: true});
    //this.markForCheck();
    //this.setValue(json, {emitEvent: true});
  }

  protected getValue(): Measurement[] {

    if (this.loading) return this.data; // Avoid to return not loading data

    // Find dirty pmfms, to avoid full update
    const dirtyPmfms = (this.$pmfms.getValue() || []).filter(pmfm => this.form.controls[pmfm.pmfmId].dirty);
    if (dirtyPmfms.length) {

      // Update measurements value
      //if (this.debug) console.debug(`${this.logPrefix} Updating form measurements...`);
      const json = this.form.value;
      MeasurementUtils.setValuesByFormValues(this.data, json, dirtyPmfms);
      //if (this.debug) console.debug(`${this.logPrefix} Updating form measurements [OK]`, this.data);
    }

    return this.data;
  }

  protected async refreshPmfms(event?: any): Promise<PmfmStrategy[]> {
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

  public updateControls(event?: string, pmfms?: PmfmStrategy[]) {
    //if (isNil(this.data)) return; // not ready
    pmfms = pmfms || this.$pmfms.getValue();

    if (this.form.enabled) {
      this.form.disable();
    }

    // Waiting end of pmfm load
    if (!pmfms || this.loadingPmfms || !this.form) {
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

    this.measurementValidatorService.updateFormGroup(this.form, pmfms);

    this.loading = false;
    this.loadingControls = false;
    this.$loadingControls.next(false);

    if (this.debug) console.debug(`${this.logPrefix} Form controls updated in ${Date.now() - now}ms`);

    this.markAsUntouched();
    this.markAsPristine();

    // Restore enable state (because form.setValue() can change it !)
    if (this._enable) {
      this.enable({onlySelf: true, emitEvent: false});
    } else if (this.form.enabled) {
      this.disable({onlySelf: true, emitEvent: false});
    }

    return true;
  }

  async onReady() {
    // Wait pmfms load, and controls load
    if (this.$loadingControls.getValue() !== false) {
      if (this.debug) console.debug(`${this.logPrefix} waiting form to be ready...`);
      await firstNotNilPromise(this.$loadingControls
        .pipe(
          filter((loadingControls) => loadingControls === false),
          throttleTime(100), // groups event, if many updates in few duration
        ));
    }
  }

  /** -- protected methods  -- */

  protected get logPrefix(): string {
    const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
    return `[meas-form-${acquisitionLevel}]`;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
