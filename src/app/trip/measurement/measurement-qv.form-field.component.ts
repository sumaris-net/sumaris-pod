import {
  Component,
  OnInit,
  Input,
  EventEmitter,
  Output,
  forwardRef,
  Optional,
  ChangeDetectionStrategy, OnDestroy
} from '@angular/core';
import {Referential, PmfmStrategy} from "../services/trip.model";
import {merge, Observable, Subject} from 'rxjs';
import {startWith, map, tap, takeUntil, filter} from 'rxjs/operators';
import {referentialToString, EntityUtils, ReferentialRef} from '../../referential/referential.module';
import {NG_VALUE_ACCESSOR, ControlValueAccessor, Validators, FormControl, FormGroupDirective} from '@angular/forms';
import {FloatLabelType} from "@angular/material";


import {SharedValidators} from '../../shared/validator/validators';
import {Platform} from "@ionic/angular";

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

  @Output()
  onBlur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

  constructor(
    platform: Platform,
    @Optional() private formGroupDir: FormGroupDirective
  ) {
    this.touchUi = platform.is('tablet') || platform.is('mobile');
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
              // Not too long on compact mode
              //debounceTime(this.compact ? 0 : 250),
              map(value => {
                //if (EntityUtils.isNotEmpty(value)) return undefined;
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
                }
                else {
                  this._implicitValue = undefined;
                }
              })
            )
        );
      }
    }
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

  public markAsTouched() {
    if (this.formControl.touched) {
      this._onTouchedCallback();
    }
  }

  public computePlaceholder(pmfm: PmfmStrategy): string {
    if (!pmfm) return undefined;
    if (!pmfm.qualitativeValues) return pmfm.name;
    return pmfm.qualitativeValues
      .reduce((res, qv) => (res + "/" + (qv.label || qv.name)), "").substr(1);
  }

  public _onBlur(event: FocusEvent) {
    // When leave component without object, use implicit value if stored
    if (typeof this.formControl.value !== "object" && this._implicitValue) {
      this.writeValue(this._implicitValue);
    }
    this.markAsTouched();
    this.onBlur.emit(event);
  }

  private startsWithUpperCase(input: string, search: string): boolean {
    return input && input.toUpperCase().substr(0, search.length) === search;
  }

  referentialToLabel(obj: Referential | ReferentialRef | any): string {
    return obj && obj.label || '';
  }

  clear() {
    this.formControl.setValue(null);
  }
}
