import { Component, Optional, Input, EventEmitter, OnInit, forwardRef, ViewChild } from '@angular/core';
import { DateFormatPipe } from '../../pipes/date-format.pipe';
import { Platform } from 'ionic-angular';
import { MatFormFieldControl, DateAdapter, MatDatepicker } from '@angular/material'
import { FormGroup, FormControl, FormBuilder, Validators, FormGroupDirective, NG_VALUE_ACCESSOR, ControlValueAccessor } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { Moment } from 'moment/moment';
import * as moment from 'moment/moment';
import { DATE_ISO_PATTERN } from '../constants';
import { merge } from "rxjs/observable/merge";

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
export class MatDateTime implements OnInit, ControlValueAccessor {
    protected writing: boolean = false;
    protected touchUi: boolean = false;
    protected mobile: boolean = false;
    private _onChange = (_: any) => { };
    private _onTouched = () => { };

    requiredError: boolean = false;
    form: FormGroup;
    displayPattern: string;
    dayPattern: string;
    date: Moment;
    locale: string;

    @Input() disabled: boolean = false

    @Input() formControl: FormControl;

    @Input() formControlName: string;

    @Input() displayTime: boolean = true;

    @Input() placeholder: string;

    @Input() floatPlaceholder: string;

    @Input() readonly: boolean = false;

    @Input() required: boolean = false;

    @ViewChild(MatDatepicker) datePicker: MatDatepicker<Moment>;

    constructor(
        platform: Platform,
        private dateAdapter: DateAdapter<Moment>,
        private translate: TranslateService,
        private formBuilder: FormBuilder,
        @Optional() private formGroupDir: FormGroupDirective
    ) {
        this.touchUi = !platform.is('core');
        this.mobile = this.touchUi && platform.is('mobile');
        this.locale = translate.currentLang.substr(0, 2);
    }

    ngOnInit() {
        this.form = this.formBuilder.group({
            //date: (this.required ? ['', Validators.required] : ['']),
            day: (this.required ? ['', Validators.required] : ['']),
            hour: ['', Validators.compose([Validators.min(0), Validators.max(23)])],
            minute: ['', Validators.compose([Validators.min(0), Validators.max(59)])]
        });

        this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;

        const patterns = this.translate.instant(['COMMON.DATE_PATTERN', 'COMMON.DATE_TIME_PATTERN']);
        this.displayPattern = (this.displayTime) ?
            (patterns['COMMON.DATE_TIME_PATTERN'] != 'COMMON.DATE_TIME_PATTERN' ? patterns['COMMON.DATE_TIME_PATTERN'] : 'L LT') :
            (this.displayPattern = patterns['COMMON.DATE_PATTERN'] != 'COMMON.DATE_PATTERN' ? patterns['COMMON.DATE_PATTERN'] : 'L');
        this.dayPattern = (patterns['COMMON.DATE_PATTERN'] != 'COMMON.DATE_PATTERN' ? patterns['COMMON.DATE_PATTERN'] : 'L');
        this.form.valueChanges
            .subscribe((value) => this.onFormChange(value));
        /*this.form.controls.date.valueChanges
            .subscribe((date) => this.onDatePickerChange(date))*/
    }

    writeValue(obj: any): void {
        if (this.writing) return;

        this.date = this.dateAdapter.parse(obj, DATE_ISO_PATTERN);
        if (this.date) {
            this.writing = true;
            console.log(this.date);

            this.form.setValue({
                day: this.date.clone().startOf('day').format(this.dayPattern),
                hour: this.date.hour(),
                minute: this.date.minutes()
            }, { emitEvent: false });
            this.writing = false;
        }
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
            this.form.disable();
        }
        else {
            this.formControl.enable();
            this.form.enable();
        }
        this.writing = false;
    }

    private onFormChange(json): void {
        if (this.writing) return; // Skip if call by self
        this.writing = true;

        let date = json.day && this.dateAdapter.parse(json.day, this.dayPattern);
        // If time
        if (this.displayTime) {
            date = date && date
                // set as time as locale time
                .locale(this.locale)
                .hour(json.hour || 0)
                .minute(json.minute || 0)
                .seconds(0).millisecond(0)
                // then change in UTC, to avoid TZ offset in final string
                .utc();
        }
        else {
            date = date && date.utc(true).hour(0).minute(0).seconds(0).millisecond(0);
        }

        // update date picker
        this.date = date && this.dateAdapter.parse(date.clone(), DATE_ISO_PATTERN);

        // Get the model value
        const dateStr = date && date.format(DATE_ISO_PATTERN).replace('+00:00', 'Z');

        this.formControl.setValue(dateStr);
        this.writing = false;

        this._onChange(dateStr);
    }

    private onDatePickerChange(date: any): void {
        if (this.writing) return; // Skip if call by self
        this.writing = true;

        date = date && typeof date === 'string' && this.dateAdapter.parse(date, DATE_ISO_PATTERN) || date;
        if (this.displayTime) {
            date = date && date && date
                // set as time as locale time
                .locale(this.locale)
                .hour(this.form.controls.hour.value || 0)
                .minute(this.form.controls.minute.value || 0)
                .seconds(0).millisecond(0)
                // then change in UTC, to avoid TZ offset in final string
                .utc();
        }
        else {
            // avoid to have TZ offset
            date = date && date.utc(true).hour(0).minute(0).seconds(0).millisecond(0);
        }

        // update day value
        this.form.controls.day.setValue(date && date.clone().startOf('day').format(this.dayPattern), { emitEvent: false });

        // Get the model value
        const dateStr = date && date.format(DATE_ISO_PATTERN).replace('+00:00', 'Z');
        console.log("onDatePickerChange - Date changed: " + dateStr);
        this.formControl.setValue(dateStr);
        this.writing = false;

        this._onChange(dateStr);
    }

    public markAsTouched() {
        this.requiredError = this.formControl && this.formControl.hasError('required');
        //console.log("has error ?", this.formControl.hasError('required'));
        if (this.form.touched) {
            this._onTouched();
        }
    }

    public onKeyDown(event: KeyboardEvent) {
        if (event.key == 'ArrowDown') {
            this.datePicker.open();
            event.preventDefault();
            return;
        }
    }
}

