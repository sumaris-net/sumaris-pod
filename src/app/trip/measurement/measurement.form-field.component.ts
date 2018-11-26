import { Component, Optional, OnInit, Input, forwardRef } from '@angular/core';
import { PmfmStrategy, getPmfmName } from "../services/trip.model";
import { NG_VALUE_ACCESSOR, ControlValueAccessor, FormControl, FormGroupDirective } from '@angular/forms';
import { FloatLabelType } from "@angular/material";
import { MeasurementsValidatorService } from '../services/measurement.validator';

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
    ]
})
export class MeasurementFormField implements OnInit, ControlValueAccessor {

    private _onChangeCallback: (_: any) => void = noop;
    private _onTouchedCallback: () => void = noop;
    protected disabling: boolean = false;

    type: string;

    @Input() pmfm: PmfmStrategy;

    @Input() disabled: boolean = false

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
            this.formControl.setValue(obj, { emitEvent: false });
            this._onChangeCallback(this.value);
        }
    }

    constructor(
        protected measurementValidatorService: MeasurementsValidatorService,
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
        if (type == "double" && this.pmfm.label === "LATITUDE") {
            type = "latitude";
        }
        else if (type == "double" && this.pmfm.label === "LONGITUDE") {
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
        this.disabling = false;
    }

    public markAsTouched() {
        if (this.formControl.touched) {
            this._onTouchedCallback();
        }
    }

    computeNumberInputStep(pmfm: PmfmStrategy): string {

        if (pmfm.maximumNumberDecimals > 0) {
            let step = "0.";
            if (pmfm.maximumNumberDecimals > 1) {
                for (let i = 0; i < pmfm.maximumNumberDecimals - 1; i++) {
                    step += "0"
                }
            }
            step += "1";
            return step;
        }
        else {
            return "1";
        }
    }

  filterNumberInput(event: KeyboardEvent, allowDecimals: boolean) {
    console.log(event);
    let numberEntered = false;
    if ((event.which >= 48 && event.which <= 57) || (event.which >= 37 && event.which <= 40)) {
      //input number entered or one of the 4 direction up, down, left and right
      console.log('input number entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
    }
    else if (allowDecimals && event.key == '.' || event.key == ',') {
      console.log('input decimal separator entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
    }
    else {
      //input command entered of delete, backspace or one of the 4 direction up, down, left and right
      if ((event.keyCode >= 37 && event.keyCode <= 40) || event.keyCode == 46 || event.which == 8) {
        console.log('input command entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
      }
      else {
        console.log('input not number entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
        event.preventDefault();
      }
    }
  }
}
