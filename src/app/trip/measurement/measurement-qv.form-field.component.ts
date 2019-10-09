import {
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
  ViewChild
} from '@angular/core';
import {entityToString, isNil, isNotNil, PmfmStrategy, Referential} from "../services/trip.model";
import {combineLatest, merge, Observable, of} from 'rxjs';
import {filter, map, takeUntil, tap, throttleTime} from 'rxjs/operators';
import {EntityUtils, ReferentialRef, referentialToString} from '../../referential/referential.module';
import {ControlValueAccessor, FormControl, FormGroupDirective, NG_VALUE_ACCESSOR, Validators} from '@angular/forms';
import {FloatLabelType, MatAutocomplete, MatSelect} from "@angular/material";


import {SharedValidators} from '../../shared/validator/validators';
import {PlatformService} from "../../core/services/platform.service";
import {focusInput, isNotEmptyArray, joinPropertiesPath, suggestFromArray, toBoolean} from "../../shared/functions";
import {AppFormUtils} from "../../core/core.module";
import {sort} from "../../shared/functions";
import {asInputElement, InputElement} from "../../shared/material/focusable";
import {LocalSettingsService} from "../../core/services/local-settings.service";

@Component({
  selector: 'mat-form-field-measurement-qv',
  templateUrl: './measurement-qv.form-field.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MeasurementQVFormField),
      multi: true
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MeasurementQVFormField implements OnInit, OnDestroy, ControlValueAccessor, InputElement {

  private _onChangeCallback = (_: any) => {
  };
  private _onTouchedCallback = () => {
  };
  private _implicitValue: ReferentialRef | any;
  private _onDestroy = new EventEmitter(true);
  private _sortedQualitativeValues: ReferentialRef[];

  items: Observable<ReferentialRef[]>;
  onShowDropdown = new EventEmitter<UIEvent>(true);
  mobile = false;
  selectedIndex = -1;
  disabled: boolean;
  _tabindex: number;

  get nativeElement(): any {
    return this.matInput && this.matInput.nativeElement;
  }

  @Input()
  displayWith: (obj: ReferentialRef | any) => string;

  @Input() pmfm: PmfmStrategy;

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() placeholder: string;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() required: boolean;

  @Input() readonly = false;

  @Input() compact = false;

  @Input() clearable = false;

  @Input() style: 'autocomplete' | 'select' | 'button';

  @Input() searchAttributes: string[];

  @Input() sortAttribute: string;

  @Input() set tabindex(value: number) {
    this._tabindex = value;
    this.markForCheck();
  }

  get tabindex(): number {
    return this._tabindex;
  }

  @Output('keypress.enter')
  onKeypressEnter: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

  @ViewChild('matInput', { static: false }) matInput: ElementRef;

  constructor(
    private platform: PlatformService,
    private settings: LocalSettingsService,
    private cd: ChangeDetectorRef,
    @Optional() private formGroupDir: FormGroupDirective
  ) {
    this.mobile = platform.mobile;

    // Set default style
    this.style = this.mobile ? 'select' : 'autocomplete';
  }

  ngOnInit() {

    this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
    if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-form-field-measurement-qv>.");
    this.disabled = !this.formControl.enabled;

    if (!this.pmfm) throw new Error("Missing mandatory attribute 'pmfm' in <mat-qv-field>.");
    this.pmfm.qualitativeValues = this.pmfm.qualitativeValues || [];
    this.required = toBoolean(this.required, this.pmfm.isMandatory);

    this.formControl.setValidators(this.required ? [Validators.required, SharedValidators.entity] : SharedValidators.entity);

    const attributes = this.settings.getFieldDisplayAttributes('qualitativeValue', ['label', 'name']);
    const displayAttributes = this.compact && attributes.length > 1 ? ['label'] : attributes;
    this.searchAttributes = isNotEmptyArray(this.searchAttributes) && this.searchAttributes || attributes;
    this.sortAttribute =  isNotNil(this.sortAttribute) ? this.sortAttribute : (attributes[0]);

    // Sort values
    this._sortedQualitativeValues = sort(this.pmfm.qualitativeValues, this.sortAttribute);

    this.placeholder = this.placeholder || this.pmfm.name || this.computePlaceholder(this.pmfm, this._sortedQualitativeValues);
    this.displayWith = this.displayWith || ((obj) => referentialToString(obj, displayAttributes));
    this.clearable = this.compact ? false : this.clearable;

    if (!this.mobile) {
      if (!this._sortedQualitativeValues.length) {
        this.items = of([]);
      } else {
        this.items = merge(
          this.onShowDropdown
            .pipe(
              takeUntil(this._onDestroy),
              filter(event => !event.defaultPrevented),
              map((_) => this._sortedQualitativeValues)
            ),
          this.formControl.valueChanges
            .pipe(
              takeUntil(this._onDestroy),
              filter(EntityUtils.isEmpty),
              map(value => suggestFromArray(this._sortedQualitativeValues, value, {
                searchAttributes: this.searchAttributes
              })),
              tap(res => this.updateImplicitValue(res))
            )
        );
      }
    }


    // If button, listen enable/disable changes (hack using statusChanges)
    if (this.style === 'button') {

      this.formControl.statusChanges
        .pipe(
          takeUntil(this._onDestroy)
        )
        .subscribe(() => {
          if (this.disabled !== this.formControl.disabled) {
            this.disabled = !this.disabled;
            this.markForCheck();
          }
        });
    }
  }

  ngOnDestroy(): void {
    this._onDestroy.emit();
  }

  get value(): any {
    return this.formControl.value;
  }

  writeValue(obj: any): void {
    if (obj !== this.formControl.value) {
      this.formControl.patchValue(obj, {emitEvent: false});
      this._onChangeCallback(obj);
    }

    if (this.style === 'button') {
      const index = (obj && isNotNil(obj.id)) ? this.pmfm.qualitativeValues.findIndex(qv => qv.id === obj.id) : -1;
      if (this.selectedIndex !== index) {
        this.selectedIndex = index;
        this.markForCheck();
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

  computePlaceholder(pmfm: PmfmStrategy, sortedQualitativeValues: ReferentialRef[]): string {
    if (!sortedQualitativeValues || !sortedQualitativeValues.length) return pmfm && pmfm.name;
    return sortedQualitativeValues.reduce((res, qv) => (res + "/" + (qv.label || qv.name)), "").substr(1);
  }

  _onBlur(event: FocusEvent) {
    if (event.relatedTarget instanceof HTMLElement && event.relatedTarget.tagName === 'MAT-OPTION') {
      event.preventDefault();
      return;
    }

    // When leave component without object, use implicit value if stored
    if (this._implicitValue && typeof this.formControl.value !== "object") {
      this.writeValue(this._implicitValue);
    }
    this._implicitValue = null;
    this.checkIfTouched();
    this.onBlur.emit(event);
  }

  clear() {
    this.formControl.setValue(null);
    this.markForCheck();
  }

  focus() {
    focusInput(this.matInput);
  }

  getQvId(item: ReferentialRef) {
    return item.id;
  }

  selectInputContent = AppFormUtils.selectInputContent;
  entityToString = entityToString;

  /* -- protected methods -- */

  protected updateImplicitValue(res: any[]) {
    // Store implicit value (will use it onBlur if not other value selected)
    if (res && res.length === 1) {
      this._implicitValue = res[0];
      this.formControl.setErrors(null);
    } else {
      this._implicitValue = undefined;
    }
  }

  protected checkIfTouched() {
    if (this.formControl.touched) {
      this.markForCheck();
      this._onTouchedCallback();
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  private match(qv: ReferentialRef, search: string): boolean {
    return this.searchAttributes.findIndex(attr => this.startsWithUpperCase(qv[attr], search)) !== -1;
  }

  private startsWithUpperCase(input: string, search: string): boolean {
    return input && input.toUpperCase().startsWith(search);
  }

}
