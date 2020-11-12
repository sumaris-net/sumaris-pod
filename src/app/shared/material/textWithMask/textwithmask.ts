import {
  AfterViewInit,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  Optional,
  Output,
  QueryList,
  ViewChild,
  ViewChildren
} from '@angular/core';
import {Platform} from '@ionic/angular';
import {FloatLabelType} from '@angular/material/form-field';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroupDirective,
  NG_VALUE_ACCESSOR,
  Validators
} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {SharedValidators} from '../../validator/validators';

import {DEFAULT_PLACEHOLDER_CHAR} from '../../constants';
import {filter} from "rxjs/operators";
import {isNil, isNotNil, isNotNilOrBlank} from "../../functions";
import {getCaretPosition, moveInputCaretToSeparator, selectInputContent, selectInputRange} from "../../inputs";
import {Subscription} from "rxjs";
import {TextMaskConfig} from "angular2-text-mask";


import {
  DEFAULT_MAX_DECIMALS,
  formatSampleRowCode,
  TextWithMaskFormatFn,
  TextWithMaskFormatOptions,
  TextWithMaskPattern,
  parseTextWithMask
} from './textwithmask.utils';

const MASKS: {
  [key: string] : {
    [pattern: string]: Array<string|RegExp>
  }
} = {
  'sampleRawCode': {
    'sampleRawCode': [' ', /\d/, /\d/, '°', ' ', /\d/, /\d/, '\'', ' ', /\d/, /\d/, '.', /\d/, /\d/, '"']
  }
};

const noop = () => {
};

@Component({
  selector: 'mat-textwithmask-field',
  templateUrl: './textwithmask.html',
  styleUrls: ['./textwithmask.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => MatTextWithMaskField),
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatTextWithMaskField implements OnInit, ControlValueAccessor, AfterViewInit, OnDestroy, ControlValueAccessor {
  private _onChangeCallback: (_: any) => void = noop;
  private _onTouchedCallback: () => void = noop;
  private _subscription = new Subscription();
  protected _disabled: boolean;
  protected disabling = false;
  protected writing = false;

  formatFn: TextWithMaskFormatFn;
  formatFnOptions: TextWithMaskFormatOptions;
  textMaskConfig: TextMaskConfig;
  textFormControl: FormControl;
  signFormControl: FormControl;
  mask: Array<string | RegExp> | ((raw: string) => Array<string | RegExp>) | false;
  value: number;
  inputPlaceholder: string;
  showSignControl : boolean;

  @Input() mobile: boolean;

  get disabled(): any {
    //return this.readonly || this.formControl.disabled;
    return false;
  }

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() placeholder: string;

  @Input() type: 'sampleRowCode';

  @Input("textWithMaskPattern") pattern: TextWithMaskPattern;

  @Input() defaultSign: '-' | '+';

  @Input() maxDecimals: number = DEFAULT_MAX_DECIMALS;

  @Input() placeholderChar: string = DEFAULT_PLACEHOLDER_CHAR;

  @Input() floatLabel: FloatLabelType = 'auto';

  @Input() readonly = false;

  @Input() required = false;

  @Input() tabindex: number;

  @Output()
  onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

  @Output()
  onFocus: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

  @ViewChild('inputElement') inputElement: ElementRef;

  @ViewChild('suffix', {static: false}) suffixDiv: ElementRef;

  @ViewChildren('injectMatSuffix') suffixInjections: QueryList<ElementRef>;

  constructor(
    private platform: Platform,
    private translate: TranslateService,
    private formBuilder: FormBuilder,
    private cd: ChangeDetectorRef,
    @Optional() private formGroupDir: FormGroupDirective
  ) {
    this.mobile = this.platform.is('mobile');
  }

  ngOnInit() {
    this.type = this.type;
    this.pattern = this.pattern;
    this.mask = MASKS[this.type] && MASKS[this.type][this.pattern];
    if (!this.mask) {
      console.error("Invalid attribute value. Expected: type: 'sampleRowCode or any other mask attribute' and latlongPattern: 'SampleRowCode  or any other mask attribute pattern'");
      this.type = 'sampleRowCode';
      //this.pattern = 'sampleRowCode';
      this.mask = MASKS[this.type][this.pattern];
    }
    
    this.formatFn = formatSampleRowCode;
    this.formatFnOptions = {pattern: this.pattern};

    //this.inputPlaceholder = 'COMMON.LAT_LONG.' + (this.type === 'longitude' && 'D' || '') + this.pattern + '_PLACEHOLDER';

    this.textFormControl = this.formBuilder.control(
      this.required ? [null, Validators.required] : [null]
    );
    this.textMaskConfig = {mask: this.mask, keepCharPositions: true, placeholderChar: this.placeholderChar};

    

    this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
    if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-latlong-field>.");

    //this.formControl.setValidators(Validators.compose([
    //  this.formControl.validator,
    //  this.type === 'latitude' ? SharedValidators.latitude : SharedValidators.longitude
    //]));

    this._subscription.add(
      this.textFormControl.valueChanges
        //.pipe(debounceTime(250))
        .subscribe((value) => this.onFormChange(value))
    );
    this._subscription.add(
      this.signFormControl.valueChanges
        //.pipe(debounceTime(250))
        .subscribe((value) => this.onFormChange(this.textFormControl.value))
    );

    // Listen status changes (when done outside the component  - e.g. when setErrors() is calling on the formControl)
    this._subscription.add(
      this.formControl.statusChanges
        .pipe(
          filter((_) => !this.readonly && !this.writing && !this.disabling) // Skip
        )
        .subscribe((_) => this.markForCheck())
    );
  }

  ngAfterViewInit() {
    // Inject suffix elements, into the first injection point found
    if (this.suffixDiv) {
      this.suffixInjections.find(item => {
        item.nativeElement.append(this.suffixDiv.nativeElement);
        this.suffixDiv.nativeElement.classList.remove('cdk-visually-hidden');
        return true; // take only the first injection point
      });
    }
  }

  ngOnDestroy() {
    this._subscription.unsubscribe();
  }

  writeValue(obj: any): void {
    if (this.writing) return;

    this.value = (typeof obj === "string") ? parseFloat(obj.replace(/,/g, '.')) : obj;
    this.writing = true;
    const strValue = this.formatFn(
      this.value,
      {
        ...this.formatFnOptions,
        placeholderChar: this.placeholderChar
      });

    // DEBUG
    // console.debug("strValue: " + strValue)
    // console.debug("sign: " + sign)

    this.textFormControl.patchValue(strValue, {emitEvent: false});
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
    this.writing = true;
    this._disabled = isDisabled;
    if (isDisabled) {
      this.textFormControl.disable({onlySelf: true, emitEvent: false});
      this.signFormControl.disable({onlySelf: true, emitEvent: false});
    } else {
      this.textFormControl.enable({onlySelf: true, emitEvent: false});
      this.signFormControl.enable({onlySelf: true, emitEvent: false});
    }
    this.writing = false;
    this.disabling = false;
    this.markForCheck();
  }

  private onFormChange(strValue): void {
    if (this.writing) return; // Skip if call by self
    this.writing = true;

    if (this.textFormControl.invalid || this.signFormControl.invalid) {
      this.formControl.markAsPending();
      this.formControl.setErrors({...this.textFormControl.errors, ...this.signFormControl.errors});
      this.writing = false;
      return;
    }

    // DEBUG
    //console.debug('parsedValue=', parsedValue);


    // Get the model value
    //console.debug("[mat-latlon] Setting value {" + this.value + "} parsed from {" + strValue + "}");
    this.formControl.patchValue(this.value, {emitEvent: false});
    this.writing = false;
    this.markForCheck();

    this._onChangeCallback(this.value);
  }

  checkIfTouched(): boolean {
    if (this.formControl.touched || this.textFormControl.touched) {
      this.markForCheck();
      this._onTouchedCallback();
      return true;
    }
    return false
  }

  _onFocus(event: FocusEvent) {

    // Apply the default sign, when pattern is DD and field is empty
    if (isNil(this.value)) {
      if (this.defaultSign) {
        // Compute the text value, using the default sign
        const defaultSign = this.defaultSign === '-' ? -1 : 1;

        if (this.pattern === 'SampleRowCode') {
          let valueStr = this.formatFn(defaultSign, {...this.formatFnOptions, placeholderChar: this.placeholderChar})
          valueStr = valueStr && valueStr.replace('1', this.placeholderChar);

          // Wait end of focus animation (label should move to top)
          setTimeout(() => {
            // Set the value
            this.inputElement.nativeElement.value = valueStr;

            // Move cursor after the sign
            const caretIndex = (this.type === 'sampleRowCode') ? 2 : 1;
            selectInputRange(event.target, caretIndex);
          }, 250);
        }
        else {
          this.signFormControl.setValue(defaultSign);
        }
      }
    }

    // Select the content (if there is a value)
    else {
      selectInputContent(event);
    }

    this.onFocus.emit(event);
    return true;
  }

  _onBlur(event: FocusEvent) {
    const touched = this.checkIfTouched();
    if (touched && isNil(this.value)) {
      this.inputElement.nativeElement.value = '';
    }
    this.onBlur.emit(event);
  }

  moveCaretToSeparator(event: any, forward: boolean): boolean {
    // Move to the space separator
    return moveInputCaretToSeparator(event, ' ', forward);
  }

  onKeypress(event: KeyboardEvent) {
    // Number entered or one of the 4 direction up, down, left and right
    if ((event.which >= 48 && event.which <= 57) || (event.which >= 37 && event.which <= 40)) {
      //console.debug('input number entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
      // OK
    }
    // Decimal separator entered
    else if (event.key === '.' || event.key === ',') {
      // DEBUG
      // console.debug('input decimal separator entered :' + event.code);

      // Move caret after point
      moveInputCaretToSeparator(event, '.');

      // OK
    } else {
      // Command entered (delete, backspace or one of the 4 direction up, down, left and right)
      if ((event.keyCode >= 37 && event.keyCode <= 40) || event.keyCode == 46 || event.which == 8 || event.keyCode == 9) {
        // DEBUG
        //console.debug('input command entered:' + event.which + ' ' + event.keyCode + ' ' + event.charCode);

        // OK
      }
      // Sign entered ('+' or '-')
      else if (event.keyCode === 43 || event.keyCode === 45) {
        // DEBUG
        // console.debug('input sign entered:' + event.which + ' ' + event.keyCode + ' ' + event.charCode);
        const caretPosition = getCaretPosition(event.target);
        if (this.pattern === 'SampleRowCode' && caretPosition > 0) {
          const sign = event.key;
          // Wait key apply
          const value = this.inputElement.nativeElement.value;
          if (value && !value.startsWith(sign)) {
            const newValue = sign + (value.length > 1 ? value.substr(1) : '');

            // DEBUG
            // console.debug("sign entered. Updating input value to:", newValue);

            this.inputElement.nativeElement.value = newValue;
            selectInputRange(this.inputElement.nativeElement, caretPosition);
          }
          event.preventDefault();
        }

        // OK
      }
      // Direction char (capitalized)
      else if (event.key === 'N' || event.key === 'S' || event.key === 'E' || event.key === 'W') {
        // DEBUG
        // console.debug('input direction entered:' + event.which + ' ' + event.keyCode + ' ' + event.charCode);

        // OK
      }
      // Direction char
      else if (event.key === 'n' || event.key === 's' || event.key === 'e' || event.key === 'w') {
        // DEBUG
        // console.debug('input direction entered:' + event.which + ' ' + event.keyCode + ' ' + event.charCode);

        // Capitalize (with a delay, to be sure the key has been applied to input text)
        setTimeout(() => {
          this.inputElement.nativeElement.value = this.inputElement.nativeElement.value.toUpperCase();
        }, 10);

        // OK
      }

      // Any other keyboard events
      else {
        // DEBUG
        //console.debug('input not number entered :' + event.which + ' ' + event.keyCode + ' ' + event.charCode + ' ' + event.code );

        // KO: cancel the event
        event.preventDefault();
      }
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

