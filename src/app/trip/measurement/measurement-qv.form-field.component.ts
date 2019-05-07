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
import {PmfmStrategy, Referential} from "../services/trip.model";
import {merge, Observable, Subject} from 'rxjs';
import {filter, map, takeUntil, tap} from 'rxjs/operators';
import {EntityUtils, ReferentialRef, referentialToString} from '../../referential/referential.module';
import {ControlValueAccessor, FormControl, FormGroupDirective, NG_VALUE_ACCESSOR, Validators} from '@angular/forms';
import {FloatLabelType, MatSelect} from "@angular/material";


import {SharedValidators} from '../../shared/validator/validators';
import {PlatformService} from "../../core/services/platform.service";

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
export class MeasurementQVFormField implements OnInit, OnDestroy, ControlValueAccessor {

  private _onChangeCallback = (_: any) => {
  };
  private _onTouchedCallback = () => {
  };
  private _implicitValue: ReferentialRef | any;
  private _onDestroy = new Subject<any>();

  items: Observable<ReferentialRef[]>;
  onShowDropdown = new Subject<any>();
  touchUi = false;

  @Input()
  displayWith: (obj: ReferentialRef | any) => string;

  @Input() pmfm: PmfmStrategy;

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() placeholder: string;

  @Input() floatLabel: FloatLabelType = "auto";

  @Input() required = false;

  @Input() readonly = false;

  @Input() compact = false;

  @Input() clearable = false;

  @Input() tabindex: number;

  @Output()
  onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

  @ViewChild('matInput') matInput: ElementRef;
  @ViewChild('matSelect') matSelect: MatSelect;

  constructor(
    private platform: PlatformService,
    private cd: ChangeDetectorRef,
    @Optional() private formGroupDir: FormGroupDirective
  ) {
    this.touchUi = platform.touchUi;
  }

  ngOnInit() {

    this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
    if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-form-field-measurement-qv>.");

    if (!this.pmfm) throw new Error("Missing mandatory attribute 'pmfm' in <mat-qv-field>.");
    this.pmfm.qualitativeValues = this.pmfm.qualitativeValues || [];
    this.required = this.required || this.pmfm.isMandatory;

    this.formControl.setValidators(this.required ? [Validators.required, SharedValidators.entity] : SharedValidators.entity);

    this.placeholder = this.placeholder || this.computePlaceholder(this.pmfm);

    this.displayWith = this.displayWith || (this.compact ? this.referentialToLabel : referentialToString);

    this.clearable = this.compact ? false : this.clearable;

    if (!this.touchUi) {
      if (!this.pmfm.qualitativeValues.length) {
        this.items = Observable.of([]);
      } else {
        this.items = merge(
          this.onShowDropdown
            .pipe(
              takeUntil(this._onDestroy),
              map((_) => this.pmfm.qualitativeValues)
            ),
          this.formControl.valueChanges
            .pipe(
              takeUntil(this._onDestroy),
              filter(EntityUtils.isEmpty),
              map(value => {
                value = (typeof value === "string") && (value as string).toUpperCase() || undefined;
                if (!value || value === '*') return this.pmfm.qualitativeValues;

                // Filter by label and name
                return this.pmfm.qualitativeValues.filter((qv) => ((this.startsWithUpperCase(qv.label, value)) || (!this.compact && this.startsWithUpperCase(qv.name, value))));
              }),
              // Store implicit value (will use it onBlur if not other value selected)
              tap(res => {
                if (res && res.length === 1) {
                  this._implicitValue = res[0];
                  this.formControl.setErrors(null);
                } else {
                  this._implicitValue = undefined;
                }
              })
            )
        );
      }
    }

    setTimeout(() => this.updateTabIndex());
  }

  ngOnDestroy(): void {
    this._onDestroy.next();
  }

  get value(): any {
    return this.formControl.value;
  }

  writeValue(obj: any): void {
    if (obj !== this.formControl.value) {
      this.formControl.patchValue(obj, {emitEvent: false});
      this._onChangeCallback(obj);
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

  checkIfTouched() {
    if (this.formControl.touched) {
      this.markForCheck();
      this._onTouchedCallback();
    }
  }

  computePlaceholder(pmfm: PmfmStrategy): string {
    if (!pmfm) return undefined;
    if (!pmfm.qualitativeValues) return pmfm.name;
    return pmfm.qualitativeValues
      .reduce((res, qv) => (res + "/" + (qv.label || qv.name)), "").substr(1);
  }

  _onBlur(event: FocusEvent) {
    // When leave component without object, use implicit value if stored
    if (typeof this.formControl.value !== "object" && this._implicitValue) {
      this.writeValue(this._implicitValue);
    }
    this.checkIfTouched();
    this.onBlur.emit(event);
  }


  referentialToLabel(obj: Referential | ReferentialRef | any): string {
    return obj && obj.label || '';
  }

  clear() {
    this.formControl.setValue(null);
    this.markForCheck();
  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  private startsWithUpperCase(input: string, search: string): boolean {
    return input && input.toUpperCase().substr(0, search.length) === search;
  }

  private updateTabIndex() {
    console.log("Updating tab index !");
    if (this.tabindex && this.tabindex !== -1) {
      if (this.matInput) {
        this.matInput.nativeElement.tabIndex = this.tabindex;
      }
      else if (this.matSelect) {
        this.matSelect.tabIndex = this.tabindex;
      }
      this.markForCheck();
    }
  }
}
