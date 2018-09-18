import { Component, Optional, Input, Output, EventEmitter, OnInit, forwardRef, ContentChild, ElementRef, AfterContentInit } from '@angular/core';
import { Platform } from '@ionic/angular';
import { MatFormFieldControl, MatFormField } from '@angular/material';
import { FormGroup, FormControl, FormBuilder, Validators, FormGroupDirective, NG_VALUE_ACCESSOR, ControlValueAccessor, ValidationErrors } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { merge } from "rxjs/observable/merge";
import { SharedValidators } from '../validator/validators';
import { formatLatitude, formatLongitude, parseLatitudeOrLongitude } from '../pipes/latlong-format.pipe';
import { DEFAULT_PLACEHOLDER_CHAR } from '../constants';

const MASKS = {
    'latitude': {
        'DDMMSS': [' ', /\d/, /\d/, '°', ' ', /\d/, /\d/, '\'', ' ', /\d/, /\d/, '"', ' ', /N|S/],
        'DDMM': [' ', /\d/, /\d/, '°', ' ', /\d/, /\d/, '.', /\d/, /\d/, /\d/, /\d/, /\d/, /\d/, '\'', ' ', /N|S/],
        'DD': [' ', /\d/, /\d/, '.', /\d/, /\d/, /\d/, /\d/, /\d/, /\d/, '°']
    },
    'longitude': {
        'DDMMSS': [/\d/, /\d/, /\d/, '°', ' ', /\d/, /\d/, '\'', ' ', /\d/, /\d/, '"', ' ', /E|W/],
        'DDMM': [/\d/, /\d/, /\d/, '°', ' ', /\d/, /\d/, '.', /\d/, /\d/, /\d/, /\d/, /\d/, /\d/, '\'', ' ', /E|W/],
        'DD': [/\d/, /\d/, /\d/, '.', /\d/, /\d/, /\d/, /\d/, /\d/, /\d/, '°']
    }
};

@Component({
    selector: 'mat-latlong',
    templateUrl: 'material.latlong.html',
    styleUrls: ['./material.latlong.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            multi: true,
            useExisting: forwardRef(() => MatLatLong),
        }
    ]
})
export class MatLatLong implements OnInit, ControlValueAccessor {
    protected writing: boolean = false;
    protected touchUi: boolean = false;
    private _onChange = (_: any) => { };
    private _onTouched = () => { };

    mobile: boolean;
    requiredError: boolean = false;
    textFormControl: FormControl;
    mask: (string | RegExp)[];
    value: number;

    @Input() disabled: boolean = false

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() placeholder: string;

    @Input() type: string;

    @Input() latLongPattern: 'DDMMSS' | 'DDMM' | 'DD';

    @Input() maxDecimals: number;

    @Input() placeholderChar: string = DEFAULT_PLACEHOLDER_CHAR;

    @Input() floatLabel: string;

    @Input() readonly: boolean = false;

    @Input() required: boolean = false;

    @Output()
    onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

    constructor(
        platform: Platform,
        private translate: TranslateService,
        private formBuilder: FormBuilder,
        @Optional() private formGroupDir: FormGroupDirective
    ) {
        this.mobile = this.touchUi && platform.is('mobile');
        this.touchUi = !platform.is('desktop');
    }

    ngOnInit() {

        this.type = this.type || 'latitude';
        this.latLongPattern = this.latLongPattern || 'DDMM';
        this.mask = MASKS[this.type] && MASKS[this.type][this.latLongPattern];
        if (!this.mask) {
            console.error("Invalid attribute value. Expected: type='latitude|longitude' latlongPattern='DD|DDMM|DDMMSS'");
            this.type = 'latitude';
            this.latLongPattern = 'DDMM';
            this.mask = MASKS[this.type][this.latLongPattern];
        }
        if (this.maxDecimals && this.maxDecimals < 0) {
            console.error("Invalid attribute 'maxDecimals'. Must a positive value.");
            this.maxDecimals = undefined;
        }

        this.textFormControl = this.formBuilder.control(
            this.required ? ['', Validators.required] : ['']
        );

        this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
        if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-latlong>.");

        this.formControl.setValidators(Validators.compose([
            this.formControl.validator,
            this.type === 'latitude' ? SharedValidators.latitude : SharedValidators.longitude
        ]));

        this.textFormControl.valueChanges
            .subscribe((value) => this.onFormChange(value));
    }

    writeValue(obj: any): void {
        if (this.writing) return;

        this.value = (typeof obj == "string") ? parseFloat(obj.replace(/,/g, '.')) : obj;
        this.writing = true;
        const strValue = (this.type === 'latitude' ? formatLatitude : formatLongitude)(
            this.value,
            {
                pattern: this.latLongPattern,
                maxDecimals: this.maxDecimals,
                placeholderChar: this.placeholderChar
            });
        this.textFormControl.setValue(strValue, { emitEvent: false });
        this.writing = false;
    }

    registerOnChange(fn: any): void {
        this._onChange = fn;
    }
    registerOnTouched(fn: any): void {
        this._onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (this.writing) return;

        this.writing = true;
        this.disabled = isDisabled;
        if (isDisabled) {
            this.formControl.disable();
            this.textFormControl.disable();
        }
        else {
            this.formControl.enable();
            this.textFormControl.enable();
        }
        this.writing = false;
    }

    private onFormChange(strValue): void {
        if (this.writing) return; // Skip if call by self
        this.writing = true;

        if (this.textFormControl.invalid) {
            this.formControl.markAsPending();
            this.formControl.setErrors(Object.assign({}, this.textFormControl.errors));
            this.writing = false;
            return;
        }

        this.value = strValue && parseLatitudeOrLongitude(strValue, this.latLongPattern, this.maxDecimals, this.placeholderChar);

        if (isNaN(this.value)) {
            this.formControl.markAsPending();
            this.formControl.setErrors(this.type === 'latitude' ? { invalidLatitude: true } : { invalidLongitude: true });
            this.writing = false;
            return;
        }

        // Get the model value
        console.debug("[mat-latlon] Setting value {" + this.value + "} parsed from {" + strValue + "}");
        this.formControl.setValue(this.value);
        this.writing = false;

        this._onChange(this.value);
    }


    public markAsTouched() {
        this.requiredError = this.formControl && this.formControl.hasError('required');
        //console.log("has error ?", this.formControl.hasError('required'));
        if (this.textFormControl.touched) {
            this._onTouched();
        }
    }

    public _onBlur(event: FocusEvent) {
        this.markAsTouched();
        this.onBlur.emit(event);
    }
}

