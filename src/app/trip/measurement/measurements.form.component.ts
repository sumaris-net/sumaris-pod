import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output
} from '@angular/core';
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material/core";
import {FloatLabelType} from "@angular/material/form-field";
import {BehaviorSubject} from 'rxjs';
import {filter, throttleTime} from "rxjs/operators";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {ProgramService} from "../../referential/services/program.service";
import {FormBuilder} from '@angular/forms';
import {MeasurementsValidatorService} from '../services/validator/measurement.validator';
import {sleep, isNil, isNotNil} from '../../shared/functions';
import {
  Measurement,
  MeasurementType,
  MeasurementUtils,
  MeasurementValuesUtils
} from "../services/model/measurement.model";
import {filterNotNil, firstNotNilPromise} from "../../shared/observables";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {AppForm} from "../../core/form/form.class";

@Component({
  selector: 'app-form-measurements',
  templateUrl: './measurements.form.component.html',
  styleUrls: ['./measurements.form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MeasurementsForm extends AppForm<Measurement[]> implements OnInit {

  private _onRefreshPmfms = new EventEmitter<any>();
  private _program: string;
  private _gearId: number;
  private _acquisitionLevel: string;
  private _forceOptional = false;
  protected data: Measurement[];

  loading = false; // Important, must be false
  loadingPmfms = true; // Important, must be true
  $loadingControls = new BehaviorSubject<boolean>(true);
  applyingValue = false;
  keepRankOrder = false;

  $pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);

  @Input() showError = false;

  @Input() compact = false;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() requiredGear = false;

  @Input() entityName: MeasurementType;

  @Input() animated = false;

  @Output()
  valueChanges = new EventEmitter<any>();

  @Input()
  set program(value: string) {
    if (this._program !== value && isNotNil(value)) {
      this._program = value;
      this.loaded().then(() => this._onRefreshPmfms.emit());
    }
  }

  get program(): string {
    return this._program;
  }

  @Input()
  set acquisitionLevel(value: string) {
    if (this._acquisitionLevel !== value && isNotNil(value)) {
      this._acquisitionLevel = value;
      this.loaded().then(() => this._onRefreshPmfms.emit());
    }
  }

  get acquisitionLevel(): string {
    return this._acquisitionLevel;
  }

  @Input()
  set gearId(value: number) {
    if (this._gearId !== value && isNotNil(value)) {
      this._gearId = value;
      if (this.requiredGear) {
        this._onRefreshPmfms.emit();
      }
      else {
        this.loaded().then(() => this._onRefreshPmfms.emit());
      }
    }
  }

  get gearId(): number {
    return this._gearId;
  }

  @Input()
  set value(value: Measurement[]) {
    this.safeSetValue(value);
  }

  get value(): Measurement[] {
    return this.getValue();
  }

  @Input()
  set forceOptional(value: boolean) {
    if (this._forceOptional !== value) {
      this._forceOptional = value;
      this.loaded().then(() => this._onRefreshPmfms.emit());
    }
  }

  get forceOptional(): boolean {
    return this._forceOptional;
  }

  constructor(protected dateAdapter: DateAdapter<Moment>,
              protected measurementValidatorService: MeasurementsValidatorService,
              protected formBuilder: FormBuilder,
              protected programService: ProgramService,
              protected settings: LocalSettingsService,
              protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService.getFormGroup([]), settings);

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
    this.data = MeasurementUtils.initAllMeasurements(data, pmfms, this.entityName, this.keepRankOrder);

    const json = MeasurementValuesUtils.normalizeValuesToForm(MeasurementUtils.toMeasurementValues(this.data), pmfms);

    this.form.patchValue(json, opts);

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
    this.applyingValue = true;
    this.data = data;

    // Wait form controls ready
    await this.ready();

    this.setValue(this.data, {emitEvent: true});

    this.applyingValue = false;
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

  protected async refreshPmfms(event?: any) {
    // Skip if missing: program, acquisition (or gear, if required)
    if (isNil(this._program) || isNil(this._acquisitionLevel) || (this.requiredGear && isNil(this._gearId))) {
      return;
    }

    if (this.debug) console.debug(`${this.logPrefix} refreshPmfms(${event})`);

    this.loading = true;
    this.loadingPmfms = true;

    try {
      // Load pmfms
      let pmfms = (await this.programService.loadProgramPmfms(
        this._program,
        {
          acquisitionLevel: this._acquisitionLevel,
          gearId: this._gearId
        })) || [];

      if (!pmfms.length && this.debug) {
        console.warn(`${this.logPrefix} No pmfm found, for {program: ${this._program}, acquisitionLevel: ${this._acquisitionLevel}, gear: ${this._gearId}}. Make sure programs/strategies are filled`);
      }
      else {

        // If force to optional, create a copy of each pmfms that should be forced
        if (this._forceOptional) {
          pmfms = pmfms.map(pmfm => {
            if (pmfm.required) {
              pmfm = pmfm.clone(); // Keep original entity
              pmfm.required = false;
              return pmfm;
            }
            // Return original pmfm, as not need to be overrided
            return pmfm;
          });
        }
      }

      // Apply
      this.setPmfms(pmfms);
    }
    catch (err) {
      console.error(`${this.logPrefix} Error while loading pmfms: ${err && err.message || err}`, err);
      // Reset pmfms
      this.setPmfms(null);
    }
    finally {
      if (this.enabled) this.loading = false;
      this.markForCheck();
    }

  }

  protected setPmfms(pmfms: PmfmStrategy[]) {
    this.loadingPmfms = false;
    this.$pmfms.next(pmfms);
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
      this.measurementValidatorService.updateFormGroup(this.form, {
        pmfms: []
      });
      this.form.reset({}, {onlySelf: true, emitEvent: false});
      this.loading = this.applyingValue; // Keep loading=true, when data not fully applied
      return true;
    }

    else {

      if (this.debug) console.debug(`${this.logPrefix} Updating form controls {event: ${event}, force_optional: ${this._forceOptional}}, using pmfms:`, pmfms);

      // Update the existing form
      this.measurementValidatorService.updateFormGroup(this.form, {
        pmfms
      });
    }

    this.loading = this.applyingValue; // Keep loading=true, when data not fully applied
    this.$loadingControls.next(false);

    if (this.debug) console.debug(`${this.logPrefix} Form controls updated`);

    // Data already set: apply value again to fill the form
    if (!this.applyingValue) {
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
    const form = this.form;
    // Restore enable state (because form.setValue() can change it !)
    if (this._enable) {
      form.enable(opts);
    } else if (form.enabled) {
      form.disable(opts);
    }
  }

  protected get logPrefix(): string {
    const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
    return `[meas-form-${acquisitionLevel}]`;
  }

  protected async loaded(): Promise<any> {
    if (!this.loading) return true;
    do {
      await sleep(100);
    } while (!this.loading);
    return true;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
