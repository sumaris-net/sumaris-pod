import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Optional,
  Output,
  ViewChild
} from '@angular/core';
import {Platform} from '@ionic/angular';
import {FloatLabelType, MatCheckbox, MatCheckboxChange, MatRadioButton, MatRadioChange} from '@angular/material';
import {ControlValueAccessor, FormBuilder, FormControl, FormGroupDirective, NG_VALUE_ACCESSOR} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {isNotNil} from '../functions';

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
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatBooleanField implements OnInit, ControlValueAccessor {
  private _onChange: (_: any) => void = noop;
  private _onTouched: () => void = noop;
  private _value: boolean;

  protected disabling = false;
  protected writing = false;

  showRadio = false;

  @Input() disabled = false;

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() placeholder: string;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() readonly = false;

  @Input() required = false;

  @Input() compact = false;

  @Output()
  onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

  get value(): any {
    return this._value;
  }

  @Input()
  set value(v: any) {
    if (v !== this._value) {
      this._value = v;
      this._onChange(v);
    }
  }

  @ViewChild('yesButton') yesButton: MatRadioButton;

  @ViewChild('checkboxButton') checkboxButton: MatCheckbox;

  constructor(
    private translate: TranslateService,
    private formBuilder: FormBuilder,
    private cd: ChangeDetectorRef,
    @Optional() private formGroupDir: FormGroupDirective
  ) { }

  ngOnInit() {
    this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
    if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-boolean-field>.");
  }

  writeValue(value: any): void {
    if (this.writing) return;

    this.writing = true;
    if (value !== this._value) {
      this._value = value;
      this.showRadio = isNotNil(this._value);
    }
    this.writing = false;

    this.markForCheck();
  }

  registerOnChange(fn: any): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this._onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (this.disabling) return;

    this.disabling = true;
    this.disabled = isDisabled;
    if (isDisabled) {
      //this.formControl.disable({ onlySelf: true, emitEvent: false });
    } else {
      //this.formControl.enable({ onlySelf: true, emitEvent: false });
    }
    this.disabling = false;
  }


  public markAsTouched() {
    if (this.formControl.touched) {
      this.markForCheck();
      this._onTouched();
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
      if (this.yesButton) this.yesButton.focus();
      if (this.checkboxButton) this.checkboxButton.focus();
      this.markForCheck();
    });
  }

  /* -- protected method -- */

  private onRadioValueChanged(event: MatRadioChange): void {
    if (this.writing) return; // Skip if call by self
    this.writing = true;
    this._value = event.value;
    this.markAsTouched();
    this._onChange(event.value);
    this.writing = false;
  }

  private onCheckboxValueChanged(event: MatCheckboxChange): void {
    if (this.writing) return; // Skip if call by self
    this.writing = true;
    this._value = event.checked;
    this.markAsTouched();
    this._onChange(event.checked);
    this.writing = false;
  }

  private markForCheck() {
    this.cd.markForCheck();
  }
}

