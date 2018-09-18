import { Component, Optional, OnInit, Input, forwardRef } from '@angular/core';
import { PmfmStrategy, Measurement } from "../services/trip.model";
import { NG_VALUE_ACCESSOR, ControlValueAccessor, FormBuilder, Validators, FormControl, FormGroupDirective } from '@angular/forms';

import { MeasurementsValidatorService } from '../services/measurement.validator';

const noop = () => {
};

@Component({
    selector: 'mat-form-field-measurement',
    templateUrl: './measurement.form-field.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MeasurementFormField),
            multi: true
        }
    ]
})
export class MeasurementFormField implements OnInit, ControlValueAccessor {

    private _onChangeCallback = (_: any) => { };
    private _onTouchedCallback = () => { };


    @Input() pmfm: PmfmStrategy;

    @Input() disabled: boolean = false

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() placeholder: string;

    @Input() compact: boolean = false;

    @Input() floatLabel: string;

    @Input() ngModel: any;

    get value(): any {
        return this.formControl.value;
    }

    writeValue(obj: any): void {

        if (obj !== this.formControl.value) {
            this.formControl.setValue(obj);
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

        this.formControl.setValidators(this.measurementValidatorService.getValidators(this.pmfm));
        this.placeholder = this.placeholder || this.computePlaceholder(this.pmfm);

    }

    registerOnChange(fn: any): void {
        this._onChangeCallback = fn;
    }
    registerOnTouched(fn: any): void {
        this._onTouchedCallback = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (this.disabled != isDisabled) {
            this.disabled = isDisabled;
            if (isDisabled) {
                this.formControl.disable();
            }
            else {
                this.formControl.enable();
            }
        }
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

    computePlaceholder(pmfm: PmfmStrategy): string {
        if (!pmfm) return undefined;
        if (pmfm.type == 'integer' || pmfm.type == 'double') {
            return pmfm.name + (pmfm.unit ? (' (' + pmfm.unit + ')') : '')
        }
        return pmfm.name;
    }

}
