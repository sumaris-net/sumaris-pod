import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, Input, OnInit, Optional, QueryList, ViewChildren } from '@angular/core';
import { ControlValueAccessor, FormArray, FormBuilder, FormGroupDirective, NG_VALUE_ACCESSOR } from '@angular/forms';
import { FloatLabelType } from '@angular/material/form-field';
import { FormArrayHelper, InputElement, toNumber } from '@sumaris-net/ngx-components';
import { IPmfm, PmfmUtils } from '../services/model/pmfm.model';
import { PmfmValue, PmfmValueUtils } from '@app/referential/services/model/pmfm-value.model';
import { PmfmFormField, PmfmFormFieldStyle } from '@app/referential/pmfm/pmfm.form-field.component';

const noop = () => {};

@Component({
  selector: 'app-pmfm-form-array',
  styleUrls: ['./pmfm.form-array.component.scss'],
  templateUrl: './pmfm.form-array.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PmfmFormArray),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PmfmFormArray implements OnInit, ControlValueAccessor, InputElement {

  private _onChangeCallback: (_: any) => void = noop;
  private _onTouchedCallback: () => void = noop;

  formArrayHelper: FormArrayHelper<PmfmValue>;

  @Input() pmfm: IPmfm;
  @Input() required: boolean;
  @Input() readonly = false;
  @Input() hidden = false;
  @Input() formArray: FormArray;
  @Input() formArrayName: string;
  @Input() placeholder: string;
  @Input() compact = false;
  @Input() floatLabel: FloatLabelType = "auto";
  @Input() tabindex: number;
  @Input() autofocus: boolean;
  @Input() weightDisplayedUnit: string;
  @Input() style: PmfmFormFieldStyle;
  @Input() maxVisibleButtons: number;
  @Input() acquisitionNumber: number;

  // When async validator (e.g. BatchForm), force update when error detected
  @Input() listenStatusChanges: boolean;

  get value(): any {
    return this.formArray.value;
  }

  get disabled(): boolean {
    return this.formArray.disabled;
  }

  @ViewChildren('pmfmField') matInputs: QueryList<PmfmFormField>;

  constructor(
    protected cd: ChangeDetectorRef,
    protected formBuilder: FormBuilder,
    @Optional() private formGroupDir: FormGroupDirective
  ) {

  }

  ngOnInit() {

    if (!this.pmfm) throw new Error("Missing mandatory attribute 'pmfm' in <app-pmfm-form-array>.");
    if (typeof this.pmfm !== 'object') throw new Error("Invalid attribute 'pmfm' in <app-pmfm-form-array>. Should be an object.");
    if (!this.pmfm.isMultiple) throw new Error("Invalid 'pmfm' in <app-pmfm-form-array>. Should habe 'isMutliple=true'. Please use a AppPmfmField instead");

    this.formArray = this.formArray || (this.formArrayName && this.formGroupDir && this.formGroupDir.form.get(this.formArrayName) as FormArray);
    if (!this.formArray) throw new Error("Missing mandatory attribute 'formArray' or 'formArrayName' in <app-pmfm-form-array>.");

    this.acquisitionNumber = toNumber(this.acquisitionNumber, PmfmUtils.isDenormalizedPmfm(this.pmfm) ? this.pmfm.acquisitionNumber : -1);
    this.formArrayHelper = new FormArrayHelper<PmfmValue>(
      this.formArray,
      (value) => this.formBuilder.control(value || null),
      PmfmValueUtils.equals,
      PmfmValueUtils.isEmpty,
      {
        allowEmptyArray: false
      });

    if (this.listenStatusChanges) {
      this.formArray.statusChanges.subscribe((_) => this.cd.markForCheck());
    }
  }

  writeValue(value: any): void {
    // FIXME This is a hack, because some time invalid value are passed
    // Example: in the batch group table (inline edition)
    if (PmfmUtils.isNumeric(this.pmfm) && Number.isNaN(value)) {
      //console.warn("Trying to set NaN value, in a measurement field ! " + this.constructor.name);
      value = null;
      if (value !== this.formArray.value) {
        this.formArray.patchValue(value, {emitEvent: false});
        this._onChangeCallback(value);
      }
    }
  }

  registerOnChange(fn: any): void {
    this._onChangeCallback = fn;
  }

  registerOnTouched(fn: any): void {
    this._onTouchedCallback = fn;
  }

  setDisabledState(isDisabled: boolean): void {
  }

  markAsTouched() {
    if (this.formArray.touched) {
      this.cd.markForCheck();
      this._onTouchedCallback();
    }
  }

  focus() {
    if (this.hidden) {
      console.warn("Cannot focus an hidden field!")
    }
    else {
      this.matInputs.first.focus();
    }
  }

  /* -- protected method -- */

}
