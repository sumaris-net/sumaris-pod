import { Component, Optional, OnInit, Input, forwardRef } from '@angular/core';
import { PmfmStrategy, getPmfmName } from "../services/trip.model";
import { NG_VALUE_ACCESSOR, ControlValueAccessor, FormControl, FormGroupDirective } from '@angular/forms';
import { FloatLabelType } from "@angular/material";
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

    private _onChange: (_: any) => void = noop;
    private _onTouched: () => void = noop;
    protected disabling: boolean = false;

    @Input() pmfm: PmfmStrategy;

    @Input() disabled: boolean = false

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() placeholder: string;

    @Input() compact: boolean = false;

    @Input() floatLabel: FloatLabelType = "auto";

    @Input('ngModel') model: any;

    get value(): any {
        return this.formControl.value;
    }

    writeValue(obj: any): void {

        if (obj !== this.formControl.value) {
            if (this.pmfm.type == 'boolean') console.debug("[mat-form-field-measurement] Replace value", obj);
            this.formControl.setValue(obj);
            this._onChange(this.value);
        }
        else {
            if (this.pmfm.type == 'boolean') console.debug("[mat-form-field-measurement] Same value, skipping", obj);
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
        this.placeholder = this.placeholder || getPmfmName(this.pmfm, { withUnit: !this.compact });

        if (this.model) {
            console.warn("[mat-form-field-measurement] Replace value (by ngModel)", this.formControl.value, this.model);
            this.formControl.setValue(this.model);
            //this.writeValue(this.model);
        }
    }

    registerOnChange(fn: any): void {
        this._onChange = fn;
    }
    registerOnTouched(fn: any): void {
        this._onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (this.disabling) return;

        this.disabling = true;

        this.disabled = isDisabled;
        if (isDisabled) {
            this.formControl.disable();
        }
        else {
            this.formControl.enable();
        }
        this.disabling = false;
    }

    public markAsTouched() {
        if (this.formControl.touched) {
            this._onTouched();
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
}
