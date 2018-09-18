import { Component, Optional, Input, Output, EventEmitter, OnInit, forwardRef, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Platform } from '@ionic/angular';
import { MatRadioButton, MatRadioChange, MatCheckbox, MatCheckboxChange } from '@angular/material';
import { FormControl, FormBuilder, FormGroupDirective, NG_VALUE_ACCESSOR, ControlValueAccessor } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";

const noop = () => {
};


@Component({
    selector: 'mat-boolean-field',
    templateUrl: 'material.boolean.html',
    styleUrls: ['./material.boolean.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            multi: true,
            useExisting: forwardRef(() => MatBooleanField),
        }
    ]
})
export class MatBooleanField implements OnInit, ControlValueAccessor {
    protected writing: boolean = false;
    protected touchUi: boolean = false;
    private onTouchedCallback: () => void = noop;
    private onChangeCallback: (_: any) => void = noop;

    mobile: boolean;
    requiredError: boolean = false;
    _value: boolean;
    showInput: boolean = true;
    showRadio: boolean = false;

    @Input() disabled: boolean = false

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() placeholder: string;

    @Input() floatLabel: string;

    @Input() readonly: boolean = false;

    @Input() required: boolean = false;

    @Input() compact: boolean = false;

    @Output()
    onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

    @ViewChild('yesButton') yesButton: MatRadioButton;

    @ViewChild('checkboxButton') checkboxButton: MatCheckbox;

    //get accessor
    get value(): any {
        return this._value;
    };

    //set accessor including call the onchange callback
    set value(v: any) {
        if (v !== this._value) {
            this._value = v;
            this.onChangeCallback(v);
        }
    }

    constructor(
        platform: Platform,
        private translate: TranslateService,
        private formBuilder: FormBuilder,
        private cd: ChangeDetectorRef,
        @Optional() private formGroupDir: FormGroupDirective
    ) {
        this.mobile = this.touchUi && platform.is('mobile');
        this.touchUi = !platform.is('desktop');
    }

    ngOnInit() {
        this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
        if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-boolean-field>.");

        this.showRadio = this.formControl.value != null;
    }

    writeValue(value: any): void {
        if (this.writing) return;

        this.writing = true;
        if (value !== this._value) {
            //console.debug("[mat-boolean-field] Setting value:", value);
            this._value = value;
        }
        this.writing = false;
    }

    registerOnChange(fn: any): void {
        this.onChangeCallback = fn;
    }
    registerOnTouched(fn: any): void {
        this.onTouchedCallback = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (this.writing) return;

        this.writing = true;
        this.disabled = isDisabled;
        if (isDisabled) {
            this.formControl.disable();
        }
        else {
            this.formControl.enable();
        }
        this.writing = false;
    }

    private onRadioValueChanged(event: MatRadioChange): void {
        if (this.writing) return; // Skip if call by self
        this.writing = true;
        this._value = event.value;
        this.markAsTouched();
        this.onChangeCallback(event.value);
        this.writing = false;
    }

    private onCheckboxValueChanged(event: MatCheckboxChange): void {
        if (this.writing) return; // Skip if call by self
        this.writing = true;
        this._value = event.checked;
        this.markAsTouched();
        this.onChangeCallback(event.checked);
        this.writing = false;
    }


    public markAsTouched() {
        this.requiredError = this.formControl && this.formControl.hasError('required');
        //console.log("has error ?", this.formControl.hasError('required'));
        if (this.formControl.touched) {
            this.onTouchedCallback();
        }
    }

    public _onBlur(event: FocusEvent) {
        this.markAsTouched();
        this.onBlur.emit(event);
    }


    public _onFocus(event) {
        event.preventDefault();
        event.target.classList.add('hidden');
        this.showRadio = true;
        setTimeout(() => {
            this.yesButton && this.yesButton.focus();
            this.checkboxButton && this.checkboxButton.focus();
        });
    }
}

