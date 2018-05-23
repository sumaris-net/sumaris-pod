import {Component, Optional, Input, Inject, Output, EventEmitter,OnInit,Directive, forwardRef, InjectionToken} from '@angular/core';
import { DateFormatPipe } from '../../pipes/date-format.pipe';
import { Platform } from 'ionic-angular';
import {MatFormFieldControl, MatFormFieldBase, DateAdapter} from '@angular/material'
import {FormGroup, FormControl, FormBuilder, Validators, FormGroupDirective, NG_VALUE_ACCESSOR, ControlValueAccessor, FormControlName} from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import {Moment} from 'moment/moment';
import * as moment from 'moment/moment';
import {DATE_ISO_PATTERN} from '../constants';

@Component({
    selector: 'mat-date-time',
    templateUrl: 'material.datetime.html',
    providers: [
        { 
          provide: NG_VALUE_ACCESSOR,
          multi: true,
          useExisting: forwardRef(() => MatDateTime),
        }
      ]
})
export class MatDateTime implements OnInit, ControlValueAccessor{
    private writing: boolean = false;
    private touchUi: boolean = false;
    private mobile: boolean = false;
    private _onChange = (_: any) => {};
    private _onTouched = () => {};

    requiredError: boolean = false;
    form: FormGroup;
    datePattern: string;

    @Input() disabled: boolean = false

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() displayTime: boolean = true

    @Input() placeholder: string;

    @Input() floatPlaceholder: string;

    @Input() readonly: boolean = false;    

    @Input() required: boolean = false;    

    constructor(
        platform: Platform,
        private dateAdapter: DateAdapter<Moment>,
        private translate: TranslateService,
        private formBuilder: FormBuilder,
        @Optional() private formGroupDir: FormGroupDirective
    ) {
      this.touchUi = !platform.is('core');
      this.mobile = this.touchUi && platform.is('mobile');
      
    }

    ngOnInit() {
        this.form = this.formBuilder.group({
            day: (this.required ? ['', Validators.required] : ['']),
            hour: ['', Validators.compose([Validators.min(0), Validators.max(23)])],
            minute : ['', Validators.compose([Validators.min(0), Validators.max(59)])]
          });

        this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;

        const patterns = this.translate.instant(['COMMON.DATE_PATTERN', 'COMMON.DATE_TIME_PATTERN']);
        this.datePattern = (this.displayTime) ?
            (patterns['COMMON.DATE_TIME_PATTERN'] != 'COMMON.DATE_TIME_PATTERN' ? patterns['COMMON.DATE_TIME_PATTERN'] : 'L LT') :
            (this.datePattern = patterns['COMMON.DATE_PATTERN'] != 'COMMON.DATE_PATTERN' ? patterns['COMMON.DATE_PATTERN'] : 'L');
        this.form.valueChanges.subscribe((value) => this.onFormChange(value));
    }

    writeValue(obj: any) : void{
        if (this.writing) return;

        var date = this.dateAdapter.parse(obj, DATE_ISO_PATTERN);
        if (date) {
            this.writing = true;
            this.form.setValue({
                day: date.clone().startOf('day').format(DATE_ISO_PATTERN),
                hour: date.hour(),
                minute: date.minutes()
            });
            this.writing = false;
        }
    }

    registerOnChange(fn: any) : void {
        this._onChange = fn;
    }
    registerOnTouched(fn: any) : void {
        this._onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        if (this.writing) return;

        this.writing = true;
        this.disabled = isDisabled;
        if (isDisabled) {
            this.formControl.disable();
            this.form.disable();
        }
        else {
            this.formControl.enable();
            this.form.enable();
        }
        this.writing = false;
    }

    private onFormChange(value: any): void {
        if (this.writing) return; // Skip if call by self

        var date = value ? this.dateAdapter.parse(value.day, DATE_ISO_PATTERN) : undefined;
        if (date) {
            date = date.hour(value.hour||0).minute(value.minute||0);
        }
        let dateStr = date ? date.format(DATE_ISO_PATTERN) : undefined;

        this.writing = true;
        this.formControl.setValue(dateStr);
        this.writing = false;

        this._onChange(dateStr);

    }

    private markAsTouched() {
        this.requiredError = this.formControl && this.formControl.hasError('required');
        //console.log("has error ?", this.formControl.hasError('required'));
        if (this.form.touched) {
            this._onTouched();
        }
    }
}

