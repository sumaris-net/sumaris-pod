import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Optional,
  Output,
  ViewChild
} from '@angular/core';
import {getPmfmName, isNil, PmfmStrategy} from "../services/trip.model";
import {ControlValueAccessor, FormControl, FormGroupDirective, NG_VALUE_ACCESSOR} from '@angular/forms';
import {FloatLabelType} from "@angular/material";
import {MeasurementsValidatorService} from '../services/measurement.validator';
import {AppFormUtils} from "../../core/core.module";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {focusInput, setTabIndex, toBoolean} from "../../shared/functions";
import {asInputElement, InputElement} from "../../shared/material/focusable";

const noop = () => {
};

@Component({
  selector: 'mat-form-field-measurement',
  styleUrls: ['./measurement.form-field.component.scss'],
  templateUrl: './measurement.form-field.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MeasurementFormField),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MeasurementFormField implements OnInit, ControlValueAccessor, InputElement {

  private _onChangeCallback: (_: any) => void = noop;
  private _onTouchedCallback: () => void = noop;

  type: string;
  numberInputStep: string;

  @Input() pmfm: PmfmStrategy;

  @Input() required: boolean;

  @Input() readonly = false;

  @Input() disabled = false;

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() placeholder: string;

  @Input() compact = false;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() tabindex: number;

  // When async validator (e.g. BatchForm), force update when error detected
  @Input() listenStatusChanges: boolean;

  @Output('keypress.enter')
  onKeypressEnter: EventEmitter<any> = new EventEmitter<any>();

  get value(): any {
    return this.formControl.value;
  }

  get latLongFormat(): string {
    return this.settings.settings.latLongFormat || 'DDMM';
  }

  @ViewChild('matInput', { static: false }) matInput: ElementRef;

  constructor(
    protected settings: LocalSettingsService,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected cd: ChangeDetectorRef,
    @Optional() private formGroupDir: FormGroupDirective
  ) {

  }

  ngOnInit() {

    if (!this.pmfm) throw new Error("Missing mandatory attribute 'pmfm' in <mat-form-field-measurement>.");
    if (typeof this.pmfm !== 'object') throw new Error("Invalid attribute 'pmfm' in <mat-form-field-measurement>. Should be an object.");

    this.formControl = this.formControl || (this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl);
    if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-form-field-measurement>.");

    this.formControl.setValidators(this.measurementValidatorService.getValidator(this.pmfm));

    if (this.listenStatusChanges) {
      this.formControl.statusChanges.subscribe((status) => this.cd.markForCheck());
    }
    this.placeholder = this.placeholder || getPmfmName(this.pmfm, {withUnit: !this.compact});
    this.required = toBoolean(this.required, this.pmfm.required);

    this.updateTabIndex();

    // Compute the field type (use special case for Latitude/Longitude)
    let type = this.pmfm.type;
    if (type === "double") {
      if (this.pmfm.label === "LATITUDE") {
        type = "latitude";
      } else if (this.pmfm.label === "LONGITUDE") {
        type = "longitude";
      }
      else {
        this.numberInputStep = this.computeNumberInputStep(this.pmfm);
      }
    }
    this.type = type;
  }

  writeValue(obj: any): void {
    // FIXME This is a hack, because some tme invalid value are passed
    // Example: in the batch group table (inline edition)
    if (this.pmfm.isNumeric && Number.isNaN(obj)) {
      //console.log("WARN: trying to set NaN value, in a measurement field ! " + this.constructor.name);
      obj = null;
      if (obj !== this.formControl.value) {
        this.formControl.patchValue(obj, {emitEvent: false});
        this._onChangeCallback(obj);
      }
    }
  }

  registerOnChange(fn: any): void {
    this._onChangeCallback = fn;
  }

  registerOnTouched(fn: any): void {
    this._onTouchedCallback = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (this.disabled === isDisabled) return;
    this.disabled = isDisabled;
    this.cd.markForCheck();
  }

  public markAsTouched() {
    if (this.formControl.touched) {
      this.cd.markForCheck();
      this._onTouchedCallback();
    }
  }

  computeNumberInputStep(pmfm: PmfmStrategy): string {

    if (pmfm.maximumNumberDecimals > 0) {
      let step = "0.";
      if (pmfm.maximumNumberDecimals > 1) {
        for (let i = 0; i < pmfm.maximumNumberDecimals - 1; i++) {
          step += "0";
        }
      }
      step += "1";
      return step;
    } else {
      return "1";
    }
  }

  filterNumberInput(event: KeyboardEvent, allowDecimals: boolean) {
    if (event.keyCode === 13 /*=Enter*/ && this.onKeypressEnter.observers.length) {
      this.onKeypressEnter.emit(event);
      return;
    }
    AppFormUtils.filterNumberInput(event, allowDecimals);
  }

  filterAlphanumericalInput(event: KeyboardEvent) {
    if (event.keyCode === 13 /*=Enter*/ && this.onKeypressEnter.observers.length) {
      this.onKeypressEnter.emit(event);
      return;
    }
    // Add features (e.g. check against a pattern)
  }

  focus() {
    focusInput(this.matInput);
  }

  selectInputContent = AppFormUtils.selectInputContent;

  protected updateTabIndex() {
    if (isNil(this.tabindex) || this.tabindex === -1) return;
    setTimeout(() => {
      if(!this.matInput) return;
      setTabIndex(this.matInput, this.tabindex);
      this.cd.markForCheck();
    });
  }
}
