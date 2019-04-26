import {
  Component,
  Optional,
  OnInit,
  Input,
  forwardRef,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { PmfmStrategy, getPmfmName } from "../services/trip.model";
import {NG_VALUE_ACCESSOR, ControlValueAccessor, FormControl, FormGroupDirective, FormBuilder} from '@angular/forms';
import { FloatLabelType } from "@angular/material";
import { MeasurementsValidatorService } from '../services/measurement.validator';
import {AppFormUtils} from "../../core/core.module";

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
export class MeasurementFormField implements OnInit, ControlValueAccessor {

    private _onChangeCallback: (_: any) => void = noop;
    private _onTouchedCallback: () => void = noop;
    protected disabling: boolean = false;

    type: string;

    @Input() pmfm: PmfmStrategy;

    @Input() readonly: boolean = false;

    @Input() disabled: boolean = false;

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() placeholder: string;

    @Input() compact: boolean = false;

    @Input() floatLabel: FloatLabelType = "auto";

    get value(): any {
        return this.formControl.value;
    }

    writeValue(obj: any): void {
        if (obj !== this.formControl.value) {
          console.log("Settings meas value ", this.formControl.value, obj);
            this.formControl.patchValue(obj, { emitEvent: false });
            this._onChangeCallback(obj);
        }
    }

    constructor(
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
        this.placeholder = this.placeholder || getPmfmName(this.pmfm, { withUnit: !this.compact });

        // Compute the field type (use special case for Latitude/Longitude)
        let type = this.pmfm.type;
        if (type === "double" && this.pmfm.label === "LATITUDE") {
            type = "latitude";
        }
        else if (type === "double" && this.pmfm.label === "LONGITUDE") {
            type = "longitude";
        }
        this.type = type;
    }

    registerOnChange(fn: any): void {
        this._onChangeCallback = fn;
    }
    registerOnTouched(fn: any): void {
        this._onTouchedCallback = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (this.disabling) return;

        this.disabling = true;

        this.disabled = isDisabled;
        if (isDisabled) {
            //this.formControl.disable({ onlySelf: true, emitEvent: false });
        }
        else {
            //this.formControl.enable({ onlySelf: true, emitEvent: false });
        }
        this.cd.markForCheck();
        this.disabling = false;
    }

    public markAsTouched() {
        if (this.formControl.touched) {
            this._onTouchedCallback();
            this.cd.markForCheck();
        }
    }

    public computeNumberInputStep(pmfm: PmfmStrategy): string {

        if (pmfm.maximumNumberDecimals > 0) {
            let step = "0.";
            if (pmfm.maximumNumberDecimals > 1) {
                for (let i = 0; i < pmfm.maximumNumberDecimals - 1; i++) {
                    step += "0";
                }
            }
            step += "1";
            return step;
        }
        else {
            return "1";
        }
    }

    filterNumberInput = AppFormUtils.filterNumberInput;
}
