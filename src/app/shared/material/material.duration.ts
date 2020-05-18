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
  Validators
} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {Moment} from "moment/moment";
import {DATE_ISO_PATTERN, DEFAULT_PLACEHOLDER_CHAR, KEYBOARD_HIDE_DELAY_MS} from '../constants';
import {SharedValidators} from '../validator/validators';
import {delay, isNil, isNilOrBlank, setTabIndex, toBoolean, toDateISOString, toDuration} from "../functions";
import {Keyboard} from "@ionic-native/keyboard/ngx";
import {first} from "rxjs/operators";
import {InputElement, isFocusableElement} from "./focusable";

export const DEFAULT_VALUE_ACCESSOR: any = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => MatDuration),
  multi: true
};

const HOUR_TIME_PATTERN = /[0-1]\d\d:[0-5]\d/;
const HOUR_MASK = [/[0-1]/, /\d/, /\d/, ':', /[0-5]/, /\d/];
// patterns with _ allowed
const HOUR_TIME_PATTERN2 = /[_0-1][_\d][_\d]:[_0-5][_\d]/;
const HOUR_MASK2 = [/[_0-1]/, /[_\d]/, /[_\d]/, ':', /[_0-5]/, /[_\d]/];

const noop = () => {
};

@Component({
  selector: 'mat-duration',
  templateUrl: 'material.duration.html',
  styleUrls: ['./material.duration.scss'],
  providers: [
    DEFAULT_VALUE_ACCESSOR,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatDuration implements OnInit, ControlValueAccessor, InputElement {
  private _onChangeCallback: (_: any) => void = noop;
  private _onTouchedCallback: () => void = noop;
  protected writing = true;
  protected disabling = false;
  protected _tabindex: number;
  protected keyboardHideDelay: number;

  mobile: boolean;
  form: FormGroup;
  _value: number;
  locale: string;
  hourMask = HOUR_MASK;

  @Input() disabled = false;

  @Input() formControl: FormControl;

  @Input() formControlName: string;

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

  @Input() clearable = false;

  @ViewChildren('matInput') matInputs: QueryList<ElementRef>;

  get value(): any {
    return this._value;
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

    this.required = toBoolean(this.required, this.formControl.validator === Validators.required);

    this.form = this.formBuilder.group({
      duration: [null, this.required ? Validators.compose([Validators.required, Validators.pattern(HOUR_TIME_PATTERN)]) : Validators.pattern(HOUR_TIME_PATTERN)]
    });

    this.form.valueChanges.subscribe((value) => this.onFormChange(value));

    this.updateTabIndex();

    this.writing = false;
  }

  writeValue(obj: any): void {
    if (this.writing) return;

    console.debug('[mat-duration] writeValue:', obj);

    if (isNil(obj)) {
      this.writing = true;
      this.form.patchValue({duration: null}, {emitEvent: false});
      this._value = undefined;
      if (this.formControl.value) {
        this.formControl.patchValue(null, {emitEvent: false});
        this._onChangeCallback(null);
      }
      this.writing = false;
      this.markForCheck();
      return;
    }

    this._value = obj;
    if (isNaN(this._value)) { // invalid value
      return;
    }

    this.writing = true;

    // Format
    const duration = toDuration(this._value, "hours"); // FIXME assume is a decimal hour
    const hour = Math.floor(duration.asHours()).toString().padStart(3, "0");
    const minute = duration.minutes().toString().padStart(2, "0");
    const formattedValue = hour + ':' + minute;
    console.debug(`[mat-duration] Formatted hour:${this._value} to ${formattedValue}`);
    // Set form value
    this.form.patchValue({
      duration: formattedValue
    }, {emitEvent: false});

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

  private onFormChange(json): void {
    if (this.writing) return; // Skip if call by self
    this.writing = true;

    console.debug('[mat-duration] onFormChange:', json);

    if (this.form.invalid) {
      this.formControl.markAsPending();
      const errors = {};
      Object.assign(errors, this.form.controls.duration.errors);
      this.formControl.setErrors(errors);
      this.writing = false;
      return;
    }

    let duration = json.duration || '';
    // Make to remove placeholder chars
    while (duration.indexOf(this.placeholderChar) !== -1) {
      duration = duration.replace(this.placeholderChar, '');
    }

    const durationParts = duration.split(':');
    const hour = parseInt(durationParts[0] || 0);
    const minute = parseInt(durationParts[1] || 0);

    // fixme assume unit is decimal hours
    this._value = hour + minute / 60;

    console.debug("[mat-duration] Setting duration: ", this._value);
    this.formControl.patchValue(this._value, {emitEvent: false});
    //this.formControl.updateValueAndValidity();
    this.writing = false;
    this.markForCheck();

    this._onChangeCallback(this._value);
  }

  public checkIfTouched() {
    if (this.form.touched) {
      this.markForCheck();
      this._onTouchedCallback();
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

