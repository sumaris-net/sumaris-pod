import { Component, OnInit, Input, EventEmitter, Output, forwardRef, Optional } from '@angular/core';
import { Referential, PmfmStrategy } from "../services/trip.model";
import { Observable, Subject } from 'rxjs';
import { startWith, debounceTime, map } from 'rxjs/operators';
import { referentialToString, EntityUtils, ReferentialRef } from '../../referential/referential.module';
import { NG_VALUE_ACCESSOR, ControlValueAccessor, Validators, FormControl, FormGroupDirective } from '@angular/forms';
import { FloatLabelType } from "@angular/material";


import { SharedValidators } from '../../shared/validator/validators';

@Component({
    selector: 'mat-form-field-measurement-qv',
    templateUrl: './measurement-qv.form-field.component.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MeasurementQVFormField),
            multi: true
        }
    ]
})
export class MeasurementQVFormField implements OnInit, ControlValueAccessor {

    private _onChangeCallback = (_: any) => { };
    private _onTouchedCallback = () => { };
    private _implicitValue: ReferentialRef | any;

    items: Observable<ReferentialRef[]>;
    onValueChange = new Subject<any>();

    displayWithFn: (obj: ReferentialRef | any) => string;

    @Input() pmfm: PmfmStrategy;

    @Input() disabled: boolean = false

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() placeholder: string;

    @Input() floatLabel: FloatLabelType = "auto";

    @Input() required: boolean = false;

    @Input() readonly: boolean = false;

    @Input() compact: boolean = false;

    @Input() clearable: boolean = false;

    @Output()
    onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

    constructor(
        @Optional() private formGroupDir: FormGroupDirective
    ) {
    }

    ngOnInit() {

        this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
        if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-form-field-measurement-qv>.");

        if (!this.pmfm) throw new Error("Missing mandatory attribute 'pmfm' in <mat-qv-field>.");
        this.formControl.setValidators(this.required || this.pmfm.isMandatory ? [Validators.required, SharedValidators.entity] : SharedValidators.entity);

        this.placeholder = this.placeholder || this.computePlaceholder(this.pmfm);

        this.displayWithFn = this.compact ? this.referentialToLabel : referentialToString;

        this.clearable = this.compact ? false : this.clearable;

        this.items = this.onValueChange
            .pipe(
                startWith(this.formControl.value),
                debounceTime(this.compact ? 100 : 250), // Not too long on compact mode
                map(value => {
                    if (EntityUtils.isNotEmpty(value)) return [value];
                    if (!this.pmfm.qualitativeValues) return [];
                    value = (typeof value == "string") && (value as string).toUpperCase() || undefined;
                    if (!value || value === '*') return this.pmfm.qualitativeValues;

                    // Filter by label and name
                    //console.debug(`[mat-qv-field] Searching on text '${value}'...`);
                    const res: ReferentialRef[] = this.pmfm.qualitativeValues.filter((qv) => ((this.startsWithUpperCase(qv.label, value)) || (!this.compact && this.startsWithUpperCase(qv.name, value))));

                    // Store implicit value (will use it onBlur if not other value selected)
                    this._implicitValue = (res.length === 1) ? res[0] : undefined;
                    return res;
                })
            );
    }

    get value(): any {
        return this.formControl.value;
    }

    writeValue(obj: any): void {
        if (obj !== this.formControl.value) {
            this.formControl.setValue(obj, { emitEvent: false });
            this._onChangeCallback(this.value);
        }
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
                //this.formControl.disable({ onlySelf: true, emitEvent: false });
            }
            else {
                //this.formControl.enable({ onlySelf: true, emitEvent: false });
            }
        }
    }

    public markAsTouched() {
        if (this.formControl.touched) {
            this._onTouchedCallback();
        }
    }

    public computePlaceholder(pmfm: PmfmStrategy): string {
        if (!pmfm) return undefined;
        if (!pmfm.qualitativeValues) return pmfm.name;
        return pmfm.qualitativeValues
            .reduce((res, qv) => (res + "/" + (qv.label || qv.name)), "").substr(1);
    }

    public _onBlur(event: FocusEvent) {
        // When leave component without object, use implicit value if stored
        if (typeof this.formControl.value !== "object" && this._implicitValue) {
            this.writeValue(this._implicitValue);
        }
        this.markAsTouched();
        this.onBlur.emit(event);
    }

    private startsWithUpperCase(input: string, search: string): boolean {
        return input && input.toUpperCase().substr(0, search.length) === search;
    }

    referentialToLabel(obj: Referential | ReferentialRef | any): string {
        return obj && obj.label || '';
    }

    clear() {
        this.formControl.setValue(null);
    }
}
