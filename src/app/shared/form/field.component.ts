import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  forwardRef,
  Input,
  OnInit,
  Optional,
  Output,
  ViewChild
} from '@angular/core';
import {ControlValueAccessor, FormControl, FormGroupDirective, NG_VALUE_ACCESSOR} from '@angular/forms';
import {FloatLabelType} from "@angular/material";
import {
  filterNumberInput,
  isNotNilOrBlank,
  joinPropertiesPath,
  selectInputContent,
  toDateISOString
} from "../../shared/functions";
import {FormFieldDefinition} from "./field.model";
import {DisplayFn} from "../material/material.autocomplete";
import {TranslateService} from "@ngx-translate/core";
import {getColorContrast} from "../graph/colors.utils";

const noop = () => {
};

@Component({
  selector: 'app-form-field',
  styleUrls: ['./field.component.scss'],
  templateUrl: './field.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AppFormField),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppFormField implements OnInit, ControlValueAccessor {

  private _onChangeCallback: (_: any) => void = noop;
  private _onTouchedCallback: () => void = noop;
  protected disabling = false;
  private _definition: FormFieldDefinition;

  type: string;

  @Input()
  set definition(value: FormFieldDefinition) {
    if (this._definition === value) return;
    this._definition = value;
    this.type = value && value.type;
    this.cd.markForCheck();
  }

  get definition(): FormFieldDefinition {
    return this._definition;
  }

  @Input() required: boolean;

  @Input() disabled = false;

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() placeholder: string;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() tabindex: number;

  @Output('keypress.enter')
  onKeypressEnter: EventEmitter<any> = new EventEmitter<any>();

  get value(): any {
    return this.formControl.value;
  }

  @ViewChild('matInput') matInput: ElementRef;

  constructor(
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef,
    @Optional() private formGroupDir: FormGroupDirective
  ) {

  }

  ngOnInit() {

    if (!this._definition) throw new Error("Missing mandatory attribute 'definition' in <app-form-field>.");
    if (typeof this._definition !== 'object') throw new Error("Invalid attribute 'definition' in <app-form-field>. Should be an object.");
    if (!this.type) throw new Error("Invalid attribute 'definition' in <app-form-field>. Missing type !");

    this.checkAndResolveFormControl();

    this.placeholder = this.placeholder || (this._definition.label && this.translate.instant(this._definition.label));

    this.updateTabIndex();

  }

  writeValue(obj: any): void {
    if ((this.type === 'integer' || this.type === 'double') && Number.isNaN(obj)) {
      //console.log("WARN: trying to set NaN value, in a config option field ! " + this.constructor.name);
      obj = null;
    }
    else if (this.type === 'boolean' && typeof obj === "string") {
      obj = (obj !== "false");
    }
    else if (this.type === 'date') {
      obj = toDateISOString(obj);
    }
    if (obj !== this.formControl.value) {
      //console.debug("Set config value ", this.formControl.value, obj);
      this.formControl.patchValue(obj, {emitEvent: false});
      this._onChangeCallback(obj);
    }

    this.cd.markForCheck();
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
      //this.formControl.disable({ onlySelf: true, emitEvent: false });
    } else {
      //this.formControl.enable({ onlySelf: true, emitEvent: false });
    }
    this.disabling = false;
    this.cd.markForCheck();
  }

  public markAsTouched() {
    if (this.formControl.touched) {
      this.cd.markForCheck();
      this._onTouchedCallback();
    }
  }

  filterNumberInput(event: KeyboardEvent, allowDecimals: boolean) {
    if (event.keyCode === 13 /*=Enter*/ && this.onKeypressEnter.observers.length) {
      this.onKeypressEnter.emit(event);
      return;
    }
    filterNumberInput(event, allowDecimals);
  }

  focus() {
    if (this.matInput) this.matInput.nativeElement.focus();
  }

  selectInputContent = selectInputContent;

  getDisplayValueFn(definition: FormFieldDefinition): DisplayFn {
    if (definition.autocomplete && definition.autocomplete.displayWith) {
      return (obj: any) => definition.autocomplete.displayWith(obj);
    }
    const attributes = definition.autocomplete && definition.autocomplete.attributes || ['label', 'name'];
    return (obj: any) => obj && joinPropertiesPath(obj, attributes);
  }

  getColorContrast(color: string): string | undefined {
    return isNotNilOrBlank(color) ? getColorContrast(color, true) : undefined;
  }

  /* -- protected method -- */

  protected checkAndResolveFormControl() {
    if (this.formControl) return;
    if (this.formGroupDir && this.formControlName) {
      const formControlName = (this.formGroupDir.directives || []).find(d => this.formControlName && d.name === this.formControlName);
      this.formControl = formControlName && formControlName.control;
      if (!this.formControl) {
        this.formControl = this.formGroupDir.form.get(this.formControlName) as FormControl;
      }
    }
    if (!this.formControl) {
      throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-config-option-form-field>.");
    }
  }

  protected updateTabIndex() {
    if (this.tabindex && this.tabindex !== -1) {
      setTimeout(() => {
        if (this.matInput) {
          this.matInput.nativeElement.tabIndex = this.tabindex;
        }
        this.cd.markForCheck();
      });
    }
  }
}
