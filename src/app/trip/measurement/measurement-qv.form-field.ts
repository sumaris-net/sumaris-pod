import { Component, OnInit, Input, EventEmitter, Output, forwardRef, Optional } from '@angular/core';
import { Referential, PmfmStrategy } from "../services/trip.model";
import { Observable } from 'rxjs';
import { startWith, mergeMap, debounceTime } from 'rxjs/operators';
import { referentialToString } from '../../referential/services/model';
import { NG_VALUE_ACCESSOR, ControlValueAccessor, Validators, FormControl, FormGroupDirective } from '@angular/forms';

import { SharedValidators } from '../../shared/validator/validators';

@Component({
    selector: 'mat-form-field-measurement-qv',
    templateUrl: './measurement-qv.form-field.html',
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

    items: Observable<Referential[]>;


    @Input() pmfm: PmfmStrategy;

    @Input() disabled: boolean = false

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() placeholder: string;

    @Input() floatLabel: string;

    @Input() required: boolean = false;

    @Input() readonly: boolean = false;

    @Output()
    onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

    constructor(

        @Optional() private formGroupDir: FormGroupDirective
    ) {

    }

    ngOnInit() {

        this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;


        if (!this.pmfm) throw new Error("Missing mandatory attribute 'pmfm' in <mat-qv-field>.");

        this.formControl.setValidators(this.required || this.pmfm.isMandatory ? [Validators.required, SharedValidators.entity] : SharedValidators.entity);

        this.placeholder = this.placeholder || this.computePlaceholder(this.pmfm);

        this.items = this.formControl.valueChanges
            .pipe(
                startWith(''),
                debounceTime(150),
                mergeMap(value => {
                    if (!value) {
                        if (!this.pmfm || !this.pmfm.qualitativeValues) return Observable.empty();
                        return Observable.of(this.pmfm.qualitativeValues);
                    }
                    if (typeof value == "object") return Observable.of([value]);
                    const ucValue = (value as string).toUpperCase();
                    return Observable.of((this.pmfm.qualitativeValues)
                        .filter((qv) => ((this.startsWithUpperCase(qv.label, ucValue)) || (this.startsWithUpperCase(qv.name, ucValue)))));
                })
            )
    }

    referentialToString = referentialToString;

    get value(): any {
        return this.formControl.value;
    }

    writeValue(obj: any): void {

        if (obj !== this.formControl.value) {
            this.formControl.setValue(obj);
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

    public computePlaceholder(pmfm: PmfmStrategy): string {
        if (!pmfm) return undefined;
        if (!pmfm.qualitativeValues) return pmfm.name;
        return pmfm.qualitativeValues
            .reduce((res, qv) => (res + "/" + (qv.label || qv.name)), "").substr(1);
    }

    public _onBlur(event: FocusEvent) {
        this.markAsTouched();
        this.onBlur.emit(event);
    }

    private startsWithUpperCase(input: string, search: string): boolean {
        return input && input.toUpperCase().substr(0, search.length) === search;
    }
}
