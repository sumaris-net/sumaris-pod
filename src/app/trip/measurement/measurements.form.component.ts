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
import {Platform} from "@ionic/angular";
import {Moment} from 'moment/moment';
import {DateAdapter, FloatLabelType} from "@angular/material";
import {BehaviorSubject, merge} from 'rxjs';
import {filter, first, startWith, throttleTime} from "rxjs/operators";
import {AppForm} from '../../core/core.module';
import {ProgramService} from "../../referential/referential.module";
import {FormBuilder} from '@angular/forms';
import {MeasurementsValidatorService} from '../services/measurement.validator';
import {TranslateService} from '@ngx-translate/core';
import {isNil, isNotNil} from '../../shared/shared.module';

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

  loading = false;
  loadingPmfms = true;

  pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  onLoading = new BehaviorSubject<boolean>(null);

  @Input() showError: boolean = false;

  @Input() compact: boolean = false;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() requiredGear: boolean = false;

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
    //if (this.debug) console.debug(`${this.logPrefix} Set form value`, value);
    if (this.data !== value) {
      // Transform entity into json
      this.data = (value || []).map(m => {
        if (m instanceof Measurement) return m.asObject();
        return m;
      });
      this._onValueChanged.emit();
    }
  }

  get value(): any {
    if (this.loading) return this.data; // Avoid to return not loading data

    // Find dirty pmfms, to avoid full update
    const dirtyPmfms = (this.pmfms.getValue() || []).filter(pmfm => this.form.controls[pmfm.pmfmId].dirty);
    if (dirtyPmfms.length) {

      // Update measurements value
      //if (this.debug) console.debug(`${this.logPrefix} Updating form measurements...`);
      const json = this.form.value;
      MeasurementUtils.updateMeasurementValues(json, this.data, dirtyPmfms);
      //if (this.debug) console.debug(`${this.logPrefix} Updating form measurements [OK]`, this.data);
    }

    return this.data;
  }

  constructor(protected dateAdapter: DateAdapter<Moment>,
              protected measurementValidatorService: MeasurementsValidatorService,
              protected formBuilder: FormBuilder,
              protected programService: ProgramService,
              protected translate: TranslateService,
              protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, formBuilder.group({}));

    // TODO: DEV only
    //this.debug = true;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerSubscription(
      this._onRefreshPmfms.asObservable()
        .subscribe(() => this.refreshPmfms('ngOnInit'))
    );

    // Auto update the view, when value AND pmfms are filled
    this.registerSubscription(
      merge(
        this._onValueChanged
          .pipe(
            startWith('ngOnInit'),
            filter(() => this.data && this.data.length > 0)
          ),
        this.pmfms.filter(isNotNil)
      )
      .subscribe((_) => this.updateControls('merge', this.pmfms.getValue()))
    );

    // Listen form changes
    this.registerSubscription(
      this.form.valueChanges
        .subscribe((_) => {
          if (!this.loading && this.valueChanges.observers.length) {
            this.valueChanges.emit(this.value);
          }
        })
    );
  }

  public markAsTouched() {
    // Force each sub-controls
    (this.pmfms.getValue() || []).forEach(p => {
      const control = this.form.get(p.pmfmId.toString());
      if (control) control.markAsTouched();
    });
    super.markAsTouched();
  }

  /* -- protected methods -- */

  protected async refreshPmfms(event?: any): Promise<PmfmStrategy[]> {
    if (isNil(this._program) || isNil(this._acquisitionLevel) || (this.requiredGear && isNil(this._gear))) {
      return undefined;
    }

    if (this.debug) console.debug(`${this.logPrefix} refreshPmfms(${event})`);

    this.loading = true;
    this.loadingPmfms = true;

    this.pmfms.next(null);

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

    this.pmfms.next(pmfms);

    if (this.enabled) this.loading = false;

    this.markForCheck();

    return pmfms;
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

    this.measurementValidatorService.updateFormGroup(this.form, pmfms);
    const json = MeasurementUtils.toFormValues(this.data, pmfms);
    this.data = MeasurementUtils.initAllMeasurements(this.data, pmfms);
    this.form.patchValue(json, {
      onlySelf: true,
      emitEvent: false
    });

    if (this.debug) console.debug(`${this.logPrefix} Form updated in ${Date.now() - now}ms`, json);

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

  protected get logPrefix(): string {
    const acquisitionLevel = this._acquisitionLevel && this._acquisitionLevel.toLowerCase().replace(/[_]/g, '-') || '?';
    return `[meas-form-${acquisitionLevel}]`;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
