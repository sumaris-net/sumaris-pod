import { Component, OnInit, Input, EventEmitter, Output, forwardRef, Optional } from '@angular/core';
import { Referential, PmfmStrategy } from "../services/trip.model";
import { Observable, Subject } from 'rxjs';
import { startWith, mergeMap, debounceTime, map, distinctUntilChanged, filter } from 'rxjs/operators';
import { referentialToString } from '../../referential/services/model';
import { NG_VALUE_ACCESSOR, ControlValueAccessor, Validators, FormControl, FormGroupDirective } from '@angular/forms';
import { MatInput } from '@angular/material';


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
    private _implicitValue: Referential | any;

    items: Observable<Referential[]>;
    onKeyDown = new Subject<any>();

    displayWithFn: (obj: Referential | any) => string;

    @Input() pmfm: PmfmStrategy;

    @Input() disabled: boolean = false

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() placeholder: string;

    @Input() floatLabel: string;

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


        if (!this.pmfm) throw new Error("Missing mandatory attribute 'pmfm' in <mat-qv-field>.");

        this.formControl.setValidators(this.required || this.pmfm.isMandatory ? [Validators.required, SharedValidators.entity] : SharedValidators.entity);

        this.placeholder = this.placeholder || this.computePlaceholder(this.pmfm);

        this.displayWithFn = this.compact ? this.referentialToLabel : referentialToString;

        this.clearable = this.compact ? false : this.clearable;

        this.items = this.onKeyDown
            .distinctUntilChanged()
            .debounceTime(250)
            .startWith('')
            .map(value => {
                if (!value) {
                    if (!this.pmfm || !this.pmfm.qualitativeValues) return [];
                    return this.pmfm.qualitativeValues;
                }
                if (typeof value == "object") return [value];
                const ucValue = (value as string).toUpperCase();
                const items: Referential[] = (this.pmfm.qualitativeValues)
                    .filter((qv) => ((this.startsWithUpperCase(qv.label, ucValue)) || (!this.compact && this.startsWithUpperCase(qv.name, ucValue))));
                // Store implicit value (will use it onBlur if not other value selected)
                this._implicitValue = (items.length == 1) && items[0] || undefined;
                return items;
            });
    }

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
        // When leave component without object, use implicit value if stored
        if (typeof this.formControl.value != "object" && this._implicitValue) {
            this.writeValue(this._implicitValue);
        }
        this.markAsTouched();
        this.onBlur.emit(event);
    }

    private startsWithUpperCase(input: string, search: string): boolean {
        return input && input.toUpperCase().substr(0, search.length) === search;
    }

    referentialToLabel(obj: Referential | any): string {
        return obj && obj.label || '';
    }

    clear() {
        this.formControl.setValue(null);
    }
}
