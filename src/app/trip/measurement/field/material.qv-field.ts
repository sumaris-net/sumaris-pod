import { Component, OnInit, Input, EventEmitter, Output, forwardRef, ViewChild, Optional } from '@angular/core';
import { FormGroup } from "@angular/forms";
import { Referential, PmfmStrategy, Measurement } from "../../services/model";
import { ModalController, Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { startWith, switchMap, map, mergeMap, debounceTime } from 'rxjs/operators';
import { merge } from "rxjs/observable/merge";
import { AppForm } from '../../../core/core.module';
import { VesselModal, ReferentialService, VesselService } from "../../../referential/referential.module";
import { referentialToString } from '../../../referential/services/model';
import { NG_VALUE_ACCESSOR, ControlValueAccessor, FormBuilder, Validators, FormControl, FormGroupDirective } from '@angular/forms';
import { MeasurementsValidatorService } from '../validator/validators';

import { environment } from '../../../../environments/environment';

const noop = () => {
};

@Component({
    selector: 'mat-qv-field',
    templateUrl: './material.qv-field.html',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MatQualitativeValueField),
            multi: true
        }
    ]
})
export class MatQualitativeValueField implements OnInit, ControlValueAccessor {

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

    @Output()
    onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

    constructor(

        @Optional() private formGroupDir: FormGroupDirective
    ) {

    }

    ngOnInit() {

        this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;

        if (!this.pmfm) throw new Error("Missing mandatory attribute 'pmfm' in <mat-qv-field>.");

        this.placeholder = this.placeholder || this.computePlaceholder(this.pmfm);

        this.items = this.formControl.valueChanges
            .pipe(
                startWith(''),
                debounceTime(150),
                switchMap(value => {
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
                this.formControl.disable(); this.formControl.disable();
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
