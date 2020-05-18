import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  OnInit,
  Optional,
  QueryList,
  ViewChild,
  ViewChildren
} from '@angular/core';
import {Platform} from '@ionic/angular';
import {DateAdapter} from '@angular/material/core';
import {FloatLabelType} from '@angular/material/form-field';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  FormGroupDirective,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  ValidatorFn,
  Validators
} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {Moment} from "moment/moment";
import {DATE_ISO_PATTERN, DEFAULT_PLACEHOLDER_CHAR, KEYBOARD_HIDE_DELAY_MS} from '../constants';
import {SharedValidators} from '../validator/validators';
import {delay, isNil, isNilOrBlank, setTabIndex, toBoolean, toDateISOString} from "../functions";
import {Keyboard} from "@ionic-native/keyboard/ngx";
import {first} from "rxjs/operators";
import {InputElement, isFocusableElement} from "./focusable";
import {BehaviorSubject} from "rxjs";
import {MatDatepicker, MatDatepickerInputEvent} from "@angular/material/datepicker";

export const DEFAULT_VALUE_ACCESSOR: any = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => MatDateTime),
  multi: true
};

const DAY_MASK = [/\d/, /\d/, '/', /\d/, /\d/, '/', /\d/, /\d/, /\d/, /\d/];

const HOUR_TIME_PATTERN = /[0-2]\d:[0-5]\d/;
const HOUR_MASK = [/[0-2]/, /\d/, ':', /[0-5]/, /\d/];

const noop = () => {
};

declare interface NgxTimePicker {
  selectedHour: { time: number };
  selectedMinute: { time: number };

  open();

  close();
}

@Component({
  selector: 'mat-date-time',
  templateUrl: 'material.datetime.html',
  styleUrls: ['./material.datetime.scss'],
  providers: [
    DEFAULT_VALUE_ACCESSOR,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatDateTime implements OnInit, ControlValueAccessor, InputElement {
  private _onChangeCallback: (_: any) => void = noop;
  private _onTouchedCallback: () => void = noop;
  protected writing = true;
  protected disabling = false;
  protected _tabindex: number;
  protected keyboardHideDelay: number;

  mobile: boolean;
  form: FormGroup;
  displayPattern: string;
  dayPattern: string;
  _value: Moment;
  locale: string;
  dayMask = DAY_MASK;
  hourMask = HOUR_MASK;

  @Input() disabled = false;

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() displayTime = true;

  @Input() placeholder: string;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() readonly = false;

  @Input() required: boolean;

  @Input() compact = false;

  @Input() placeholderChar: string = DEFAULT_PLACEHOLDER_CHAR;

  @Input() set tabindex(value: number) {
    if (this._tabindex !== value) {
      this._tabindex = value;
      setTimeout(() => this.updateTabIndex());
    }
  }

  get tabindex(): number {
    return this._tabindex;
  }

  @Input() startDate: Date;

  @Input() clearable = false;

  @ViewChild('datePicker1') datePicker1: MatDatepicker<Moment>;
  @ViewChild('datePicker2') datePicker2: MatDatepicker<Moment>;
  @ViewChild('timePicker') timePicker: NgxTimePicker;

  @ViewChildren('matInput') matInputs: QueryList<ElementRef>;

  get value(): any {
    return toDateISOString(this._value);
  }

  constructor(
    platform: Platform,
    private dateAdapter: DateAdapter<Moment>,
    private translate: TranslateService,
    private formBuilder: FormBuilder,
    private keyboard: Keyboard,
    private cd: ChangeDetectorRef,
    @Optional() private formGroupDir: FormGroupDirective,
  ) {
    // Workaround because ion-datetime has issue (do not returned a ISO date)
    this.mobile = platform.is('mobile');
    this.keyboardHideDelay = this.mobile && KEYBOARD_HIDE_DELAY_MS || 0;

    this.locale = (translate.currentLang || translate.defaultLang).substr(0, 2);
  }

  ngOnInit() {

    this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
    if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-date-time>.");

    // Redirect errors from main control, into day sub control
    const $error = new BehaviorSubject<ValidationErrors>(null);
    const dayValidator: ValidatorFn = (_) => $error.getValue();

    this.required = toBoolean(this.required, this.formControl.validator === Validators.required);
    if (this.displayTime) {
      this.form = this.formBuilder.group({
        day: [dayValidator],
        hour: ['', this.required ? Validators.compose([Validators.required, Validators.pattern(HOUR_TIME_PATTERN)]) : Validators.pattern(HOUR_TIME_PATTERN)]
      });
    } else {
      this.form = this.formBuilder.group({
        day: [dayValidator]
      });
    }

    // Add custom 'validDate' validator
    this.formControl.setValidators(this.required ? Validators.compose([Validators.required, SharedValidators.validDate]) : SharedValidators.validDate);

    // Get patterns to display date and date+time
    const patterns = this.translate.instant(['COMMON.DATE_PATTERN', 'COMMON.DATE_TIME_PATTERN']);
    this.updatePattern(patterns);

    this.form.valueChanges
      .subscribe((value) => this.onFormChange(value));

    // Listen status changes outside the component (e.g. when setErrors() is calling on the formControl)
    this.formControl.statusChanges
      .subscribe((status) => {
        if (this.readonly || this.writing || this.disabling) return; // Skip
        if (status === 'INVALID') {
          $error.next(this.formControl.errors);
        }
        else if (status === 'VALID') {
          $error.next(null);
        }
        this.form.controls.day.updateValueAndValidity({onlySelf: true, emitEvent: false});
        this.markForCheck();
      });

    this.updateTabIndex();

    this.writing = false;
  }

  writeValue(obj: any): void {
    if (this.writing) return;

    if (isNilOrBlank(obj)) {
      this.writing = true;
      if (this.displayTime) {
        this.form.patchValue({day: null, hour: null}, {emitEvent: false});
      } else {
        this.form.patchValue({day: null}, {emitEvent: false});
      }
      this._value = undefined;
      if (this.formControl.value) {
        this.formControl.patchValue(null, {emitEvent: false});
        this._onChangeCallback(null);
      }
      this.writing = false;
      this.markForCheck();
      return;
    }

    this._value = this.dateAdapter.parse(obj, DATE_ISO_PATTERN);
    if (!this._value) { // invalid date
      return;
    }

    this.writing = true;

    // With time
    if (this.displayTime) {

      // Format hh
      let hour: number | string = this._value.hour();
      hour = hour < 10 ? ('0' + hour) : hour;
      // Format mm
      let minutes: number | string = this._value.minutes();
      minutes = minutes < 10 ? ('0' + minutes) : minutes;
      // Set form value
      this.form.patchValue({
        day: this._value.clone().startOf('day').format(this.dayPattern),
        hour: `${hour}:${minutes}`
      }, {emitEvent: false});
    }

    // Without time
    else {
      //console.log("call writeValue()", this.date, this.formControl);
      // Set form value
      this.form.patchValue({
        day: this._value.clone().startOf('day').format(this.dayPattern)
      }, {emitEvent: false});
    }
    this.writing = false;
    this.markForCheck();
  }

  registerOnChange(fn: any): void {
    this._onChangeCallback = fn;
  }

  registerOnTouched(fn: any): void {
    this._onTouchedCallback = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (this.disabling) return;

    this.disabling = true;
    this.disabled = isDisabled;
    if (isDisabled) {
      this.form.disable({onlySelf: true, emitEvent: false});
    } else {
      this.form.enable({onlySelf: true, emitEvent: false});
    }
    this.disabling = false;

    this.markForCheck();
  }

  private updatePattern(patterns: string[]) {
    this.displayPattern = (this.displayTime) ?
      (patterns['COMMON.DATE_TIME_PATTERN'] !== 'COMMON.DATE_TIME_PATTERN' ? patterns['COMMON.DATE_TIME_PATTERN'] : 'L LT') :
      (this.displayPattern = patterns['COMMON.DATE_PATTERN'] !== 'COMMON.DATE_PATTERN' ? patterns['COMMON.DATE_PATTERN'] : 'L');
    this.dayPattern = (patterns['COMMON.DATE_PATTERN'] !== 'COMMON.DATE_PATTERN' ? patterns['COMMON.DATE_PATTERN'] : 'L');
  }

  private onFormChange(json): void {
    if (this.writing) return; // Skip if call by self
    this.writing = true;

    if (this.form.invalid) {
      this.formControl.markAsPending();
      const errors = {};

      if (!this.displayTime) {
        Object.assign(errors, this.form.controls.day.errors);
      } else {
        Object.assign(errors, this.form.controls.day.errors, this.form.controls.hour.errors);
      }
      this.formControl.setErrors(errors);
      this.writing = false;
      return;
    }

    // Make to remove placeholder chars
    while (json.day && json.day.indexOf(this.placeholderChar) !== -1) {
      json.day = json.day.replace(this.placeholderChar, '');
    }

    let date: Moment;

    // Parse day string
    date = json.day && this.dateAdapter.parse(json.day, this.dayPattern) || null;

    // If time
    if (this.displayTime) {

      const hourParts = (json.hour || '').split(':');
      date = date && date
      // set as time as locale time
        .locale(this.locale)
        .hour(parseInt(hourParts[0] || 0))
        .minute(parseInt(hourParts[1] || 0))
        .seconds(0).millisecond(0)
        // then change in UTC, to avoid TZ offset in final string
        .utc();
    } else {
      // Reset time
      date = date && date.utc(true).hour(0).minute(0).seconds(0).millisecond(0);
    }

    // update date picker
    this._value = date && this.dateAdapter.parse(date.clone(), DATE_ISO_PATTERN);

    // Get the model value
    const dateStr = date && date.isValid() && date.format(DATE_ISO_PATTERN).replace('+00:00', 'Z') || date;
    //console.debug("[mat-date-time] Setting date: ", dateStr);
    this.formControl.patchValue(dateStr, {emitEvent: false});
    //this.formControl.updateValueAndValidity();
    this.writing = false;
    this.markForCheck();

    this._onChangeCallback(dateStr);
  }

  onDatePickerChange(event: MatDatepickerInputEvent<Moment>): void {
    if (this.writing || !(event && event.value)) return; // Skip if call by self
    this.writing = true;

    let date = event.value;
    date = typeof date === 'string' && this.dateAdapter.parse(date, DATE_ISO_PATTERN) || date;
    let day;
    if (this.displayTime) {
      // Keep original day (to avoid to have a offset of 1 day - fix #33)
      day = date && date.clone().locale(this.locale).hour(0).minute(0).seconds(0).millisecond(0).utc(true);
      const hourParts = (this.form.controls.hour.value || '').split(':');
      date = date && date
      // set as time as locale time
        .locale(this.locale)
        .hour(parseInt(hourParts[0] || 0))
        .minute(parseInt(hourParts[1] || 0))
        .seconds(0).millisecond(0)
        // then change in UTC, to avoid TZ offset in final string
        .utc();
    } else {
      // avoid to have TZ offset
      date = date && date.utc(true).hour(0).minute(0).seconds(0).millisecond(0);
      day = date && date.clone().startOf('day');
    }

    // update day value
    this.form.controls.day.setValue(day && day.format(this.dayPattern), {emitEvent: false});

    // Get the model value
    const dateStr = date && date.format(DATE_ISO_PATTERN).replace('+00:00', 'Z');
    this.formControl.patchValue(dateStr, {emitEvent: false});
    this.writing = false;
    this.markForCheck();

    this._onChangeCallback(dateStr);
  }

  checkIfTouched() {
    if (this.form.touched) {
      this.markForCheck();
      this._onTouchedCallback();
    }
  }

  async openDatePickerIfMobile(event: UIEvent, datePicker?: MatDatepicker<any>) {
    if (!this.mobile || event.defaultPrevented) return;

    this.preventEvent(event);

    // Make sure the keyboard is closed
    await this.waitKeyboardHide(false);

    // Open the picker
    this.openDatePicker(null, datePicker);
  }

  public openDatePicker(event?: UIEvent, datePicker?: MatDatepicker<any>) {
    datePicker = datePicker || this.datePicker1 || this.datePicker2;
    if (datePicker) {

      if (event) this.preventEvent(event);

      if (!datePicker.opened) {
        datePicker.open();
      }
    }
  }

  async openTimePickerIfMobile(event: UIEvent) {
    if (!this.mobile || event.defaultPrevented) return;

    this.preventEvent(event);

    // Make sure the keyboard is closed
    await this.waitKeyboardHide(true);

    // Open the picker
    this.openTimePicker(null);
  }

  openTimePicker(event: UIEvent) {

    if (this.timePicker) {

      if (event) this.preventEvent(event);

      this.timePicker.open();
    }
  }

  onTimePickerChange(value: string) {
    if (this.form.controls['hour'].value !== value) {
      this.form.controls['hour'].patchValue(value, {emitEvent: false});
      this.markForCheck();
    }
  }

  onTimePickerKeyup(event: KeyboardEvent) {
    if (!this.timePicker) return;
    if (event.key === 'Enter') {
      // Format hour
      let hour: number | string = this.timePicker.selectedHour.time;
      hour = hour < 10 ? ('0' + hour) : hour;
      // Format minutes
      let minutes: number | string = this.timePicker.selectedMinute.time;
      minutes = minutes < 10 ? ('0' + minutes) : minutes;
      // Notify the changes (will update the value)
      this.onTimePickerChange(`${hour}:${minutes}`);
      event.preventDefault();
      event.stopPropagation();
      // Close the picker
      this.timePicker.close();
    } else if (event.key === 'Escape') {
      // Close the picker
      event.preventDefault();
      event.stopPropagation();
      this.timePicker.close();
    }
  }

  preventEvent(event: UIEvent) {
    if (!event) return;
    event.preventDefault();
    if (event.stopPropagation) event.stopPropagation();
    event.returnValue = false;
  }

  focus() {
    if (!this.matInputs.length) return;

    setTimeout(() => {
      const elementRef = this.matInputs[0]; // get the first element
      if (isFocusableElement(elementRef)) {
        elementRef.focus();
      }
    });
  }

  /* -- protected method -- */

  protected async waitKeyboardHide(waitKeyboardDelay: boolean) {

    if (!this.keyboard.isVisible) return; // ok, already hidden

    // Force keyboard to be hide
    this.keyboard.hide();

    // Wait hide occur
    await this.keyboard.onKeyboardHide().pipe(first()).toPromise();

    // Wait an additional delay if need (depending on the OS)
    if (this.keyboardHideDelay > 0 && waitKeyboardDelay) {
      await delay(this.keyboardHideDelay);
    }
  }

  protected updateTabIndex() {
    if (isNil(this._tabindex) || this._tabindex === -1) return; // skip

    // Focus to first input
    setTimeout(() => {
      this.matInputs.forEach((elementRef, index) => {
        setTabIndex(elementRef, this._tabindex + index);
      });
      this.markForCheck();
    });
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

