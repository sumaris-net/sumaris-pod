import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  forwardRef,
  Input,
  OnDestroy,
  OnInit,
  Optional,
  Output,
  ViewChild
} from "@angular/core";
import {ControlValueAccessor, FormControl, FormGroupDirective, NG_VALUE_ACCESSOR} from "@angular/forms";
import {BehaviorSubject, isObservable, Observable, Subscription} from "rxjs";
import {filter} from "rxjs/operators";
import {
  isNilOrBlank,
  isNotNilOrBlank,
  joinPropertiesPath,
  toBoolean
} from "../../functions";
import {InputElement, selectInputContent} from "../../inputs";
import {DisplayFn} from "../../form/field.model";
import {FloatLabelType} from "@angular/material/form-field";
import {IonSlides} from "@ionic/angular";

export const DEFAULT_VALUE_ACCESSOR: any = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => MatSwipeField),
  multi: true
};

export declare interface MatSwipeFieldConfig<T = any, F = any> {
  attributes?: string[];
  items?: Observable<T[]> | T[];
  displayWith?: DisplayFn;
  class?: string;
  mobile?: boolean;
}

@Component({
  selector: 'mat-swipe',
  styleUrls: ['./material.swipe.scss'],
  templateUrl: './material.swipe.html',
  providers: [DEFAULT_VALUE_ACCESSOR],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatSwipeField implements OnInit, InputElement, OnDestroy, ControlValueAccessor {

  private _onChangeCallback = (_: any) => {
  };
  private _onTouchedCallback = () => {
  };
  private _implicitValue: any;
  private _subscription = new Subscription();
  private _itemsSubscription: Subscription;
  private _itemCount: number;
  previousDisabled: boolean;
  nextDisabled: boolean;

  _tabindex: number;
  $inputItems = new BehaviorSubject<any[]>(undefined);

  get itemCount(): number {
    return this._itemCount;
  }

  @Input() compareWith: (o1: any, o2: any) => boolean;

  @Input() logPrefix = "[mat-swipe] ";

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() floatLabel: FloatLabelType;

  @Input() placeholder: string;

  @Input() required = false;

  @Input() mobile: boolean;

  @Input() readonly = false;

  @Input() clearable = false;

  @Input() debounceTime = 250;

  @Input() displayWith: DisplayFn | null;

  @Input() displayAttributes: string[];

  @Input() showAllOnFocus: boolean;

  @Input() showPanelOnFocus: boolean;

  @Input() appAutofocus: boolean;

  @Input() config: MatSwipeFieldConfig;

  @Input() i18nPrefix = 'REFERENTIAL.';

  @Input() noResultMessage = 'COMMON.NO_RESULT';

  @Input('class') classList: string;

  @Input() set tabindex(value: number) {
    this._tabindex = value;
    this.markForCheck();
  }

  get tabindex(): number {
    return this._tabindex;
  }

  @Input() set items(value: Observable<any[]> | any[]) {
    // Remove previous subscription on items, (if exits)
    if (this._itemsSubscription) {
      console.warn("Items received twice !");
      this._subscription.remove(this._itemsSubscription);
      this._itemsSubscription.unsubscribe();
    }

    if (isObservable<any[]>(value)) {

      this._itemsSubscription = this._subscription.add(
        value.subscribe(v => this.$inputItems.next(v))
      );
    } else {
      if (value !== this.$inputItems.getValue()) {
        this.$inputItems.next(value as any[]);
      }
    }
  }

  // get items(): Observable<any[]> | any[] {
  //   return this.$inputItems;
  // }

  @Output('click') onClick = new EventEmitter<MouseEvent>();

  @Output('blur') onBlur = new EventEmitter<FocusEvent>();

  @Output('focus') onFocus = new EventEmitter<FocusEvent>();

  @ViewChild('ionSlides') ionSlides: IonSlides;

  get value(): any {
    return this.formControl.value;
  }

  get disabled(): any {
    return this.readonly || this.formControl.disabled;
  }

  get enabled(): any {
    return !this.readonly && this.formControl.enabled;
  }

  constructor(
    protected cd: ChangeDetectorRef,
    @Optional() private formGroupDir: FormGroupDirective
  ) {
  }

  ngOnInit() {
    this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
    if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-swipe-field>.");

    // Configuration from config object
    if (this.config) {
      if (this.config.items) {
        this.items = this.config.items;
      }
      this.displayAttributes = this.displayAttributes || this.config.attributes;
      this.displayWith = this.displayWith || this.config.displayWith;
      this.mobile = toBoolean(this.mobile, this.config.mobile);
      this.classList = this.classList || this.config.class;
    }

    // Default values
    this.displayAttributes = this.displayAttributes || ['label', 'name'];
    this.displayWith = this.displayWith || ((obj) => obj && joinPropertiesPath(obj, this.displayAttributes));

    this.mobile = toBoolean(this.mobile, false);

    // Applying implicit value, on blur
    this._subscription.add(
      this.onBlur
        .pipe(
          // Skip if no implicit value, or has already a value
          filter(_ => this._implicitValue)
        )
        .subscribe((_) => {
          // When leave component without object, use implicit value if :
          // - an explicit value
          // - field is not empty (user fill something)
          // - OR field empty but is required
          const existingValue = this.formControl.value;
          if ((this.required && isNilOrBlank(existingValue)) || (isNotNilOrBlank(existingValue) && typeof existingValue !== "object")) {
            this.writeValue(this._implicitValue);
            this.formControl.markAsPending({emitEvent: false, onlySelf: true});
            this.formControl.updateValueAndValidity({emitEvent: false, onlySelf: true});
          }
          this._implicitValue = null; // reset the implicit value
          this.checkIfTouched();
        }));
  }

  ngOnDestroy(): void {
    this._subscription.unsubscribe();
    this._implicitValue = undefined;
    this.$inputItems.complete();
  }

  writeValue(value: any): void {
    //console.debug(this.logPrefix + " Writing value: ", value);
    if (value !== this.formControl.value) {
      this.formControl.patchValue(value, {emitEvent: false});
      this._onChangeCallback(value);
    }
  }

  registerOnChange(fn: any): void {
    this._onChangeCallback = fn;
  }

  registerOnTouched(fn: any): void {
    this._onTouchedCallback = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.cd.markForCheck();
  }

  selectInputContent = selectInputContent;

  focus() {
      // focusInput(this.ionSlides);
  }

  clearValue(event: UIEvent) {
    this.writeValue(null);
    event.stopPropagation();
  }

  previous() {
    this.ionSlides.slidePrev();
  }

  next() {
    this.ionSlides.slideNext();
  }

  reachStart(reached: boolean) {
    this.previousDisabled = reached;
  }

  reachEnd(reached: boolean) {
    this.nextDisabled = reached;
  }

  /* -- protected method -- */


  private checkIfTouched() {
    if (this.formControl.touched) {
      this.cd.markForCheck();
      this._onTouchedCallback();
    }
  }

  private markForCheck() {
    this.cd.markForCheck();
  }
}
