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
} from "@angular/core";
import {ControlValueAccessor, FormControl, FormGroupDirective, NG_VALUE_ACCESSOR} from "@angular/forms";
import {BehaviorSubject, isObservable, merge, Observable, Subscription} from "rxjs";
import {debounceTime, distinctUntilChanged, filter, map, startWith, switchMap, takeWhile, tap} from "rxjs/operators";
import {SuggestFn, SuggestionDataService} from "../../services/data-service.class";
import {
  changeCaseToUnderscore,
  focusInput,
  getPropertyByPath,
  isNil,
  isNilOrBlank,
  isNotNil,
  isNotNilOrBlank,
  joinPropertiesPath,
  selectInputContent,
  suggestFromArray,
  toBoolean
} from "../../functions";
import {InputElement} from "../focusable";
import {firstNotNilPromise} from "../../observables";
import {DisplayFn} from "../../form/field.model";
import {FloatLabelType} from "@angular/material/form-field";

export const DEFAULT_VALUE_ACCESSOR: any = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => MatAutocompleteField),
  multi: true
};

export declare interface MatAutocompleteFieldConfig<T = any, F = any> {
  attributes: string[];
  suggestFn?: (value: any, options?: any) => Promise<T[]>;
  filter?: F;
  items?: Observable<T[]> | T[];
  columnSizes?: (number|'auto'|undefined)[];
  columnNames?: (string|undefined)[];
  displayWith?: DisplayFn;
  showAllOnFocus?: boolean;
  showPanelOnFocus?: boolean;
  mobile?: boolean;
}

export declare interface MatAutocompleteFieldAddOptions<T = any, F = any> extends Partial<MatAutocompleteFieldConfig<T, F>> {
  service?: SuggestionDataService<T, F>;
}

export class MatAutocompleteConfigHolder {
  fields: {
    [key: string]: MatAutocompleteFieldConfig
  } = {};

  getUserAttributes: (fieldName: string, defaultAttributes?: string[]) => string[];

  constructor(private options?: {
    getUserAttributes: (fieldName: string, defaultAttributes?: string[]) => string[];
  }) {
    // Store the function from options (e.g. get from user settings)
    // or create a default function
    this.getUserAttributes = options && options.getUserAttributes || 
      function (fieldName, defaultAttributes): string[] {
        return defaultAttributes || ['label', 'name'];
      };
  }

  add<T = any, F = any>(fieldName: string, options?: MatAutocompleteFieldAddOptions<T, F>): MatAutocompleteFieldConfig<T, F> {
    if (!fieldName) {
      throw new Error("Unable to add config, with name: " + (fieldName || 'undefined'));
    }
    options = options || <MatAutocompleteFieldAddOptions>{};
    const suggestFn: SuggestFn<T, F> = options.suggestFn
        || (options.service && ((v, f) => options.service.suggest(v, f)))
        || undefined;
    const attributes = this.getUserAttributes(fieldName, options.attributes) || ['label', 'name'];
    const attributesOrFn = attributes.map((a, index) => typeof a === "function" && options.attributes[index] || a);
    const filter = <F>{
      searchAttribute: attributes.length === 1 ? attributes[0] : undefined,
      searchAttributes: attributes.length > 1 ? attributes : undefined,
      ...options.filter
    };
    const displayWith = options.displayWith || ((obj) => obj && joinPropertiesPath(obj, attributesOrFn));

    const config: MatAutocompleteFieldConfig = {
      attributes: attributesOrFn,
      suggestFn,
      items: options.items,
      filter,
      displayWith,
      columnSizes: options.columnSizes,
      columnNames: options.columnNames,
      showAllOnFocus: options.showAllOnFocus,
      showPanelOnFocus: options.showPanelOnFocus,
      mobile: options.mobile
    };
    this.fields[fieldName] = config;
    return config;
  }

  get<T = any>(fieldName: string): MatAutocompleteFieldConfig<T> {
    if (!fieldName) {
      throw new Error("Unable to add config, with name: " + (fieldName || 'undefined'));
    }
    return this.fields[fieldName] || this.add(fieldName) as MatAutocompleteFieldConfig<T>;
  }
}

@Component({
  selector: 'mat-autocomplete-field',
  styleUrls: ['./material.autocomplete.scss'],
  templateUrl: './material.autocomplete.html',
  providers: [DEFAULT_VALUE_ACCESSOR],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatAutocompleteField implements OnInit, InputElement, OnDestroy, ControlValueAccessor  {

  private _onChangeCallback = (_: any) => {};
  private _onTouchedCallback = () => {};
  private _implicitValue: any;
  private _subscription = new Subscription();
  private _itemsSubscription: Subscription;
  private _$filter = new BehaviorSubject<any>(undefined);
  private _itemCount: number;

  _tabindex: number;
  matSelectItems$: Observable<any[]>;
  $inputItems = new BehaviorSubject<any[]>(undefined);
  filteredItems$: Observable<any[]>;
  onDropButtonClick = new EventEmitter<UIEvent>(true);
  searchable: boolean;

  get itemCount(): number {
    return this._itemCount;
  }

  @Input() compareWith: (o1: any, o2: any) => boolean;

  @Input() logPrefix = "[mat-autocomplete] ";

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() floatLabel: FloatLabelType;

  @Input() placeholder: string;

  @Input() suggestFn: SuggestFn<any, any>;

  @Input() set filter(value: any) {
    // DEBUG
    //console.debug(this.logPrefix + " Setting filter:", value);
    if (value !== this._$filter.getValue()) {
      this._$filter.next(value);
    }
  }

  get filter(): any {
    return this._$filter.getValue();
  }

  @Input() required = false;

  @Input() mobile: boolean;

  @Input() readonly = false;

  @Input() clearable = false;

  @Input() debounceTime = 250;

  @Input() displayWith: DisplayFn | null;

  @Input() displayAttributes: string[];

  @Input() displayColumnSizes: (number|'auto'|undefined)[];

  @Input() displayColumnNames: string[];

  @Input() showAllOnFocus: boolean;

  @Input() showPanelOnFocus: boolean;

  @Input() appAutofocus: boolean;

  @Input() config: MatAutocompleteFieldConfig;

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
    }
    else {
      if (value !== this.$inputItems.getValue()) {
        this.$inputItems.next(value as any[]);
      }
    }
  }

  get items(): Observable<any[]> | any[] {
    return this.$inputItems;
  }

  @Output('click') onClick = new EventEmitter<MouseEvent>();

  @Output('blur') onBlur = new EventEmitter<FocusEvent>();

  @Output('focus') onFocus = new EventEmitter<FocusEvent>();

  @ViewChild('matSelect') matSelect: ElementRef;

  @ViewChild('matInputText') matInputText: ElementRef;

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
    if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-autocomplete-field>.");

    // Configuration from config object
    if (this.config) {
      this.suggestFn = this.suggestFn || this.config.suggestFn;
      if (!this.suggestFn && this.config.items) {
        this.items = this.config.items;
      }
      this.filter = this.filter || this.config.filter;
      this.displayAttributes = this.displayAttributes || this.config.attributes;
      this.displayColumnSizes = this.displayColumnSizes || this.config.columnSizes;
      this.displayColumnNames = this.displayColumnNames || this.config.columnNames;
      this.displayWith = this.displayWith || this.config.displayWith;
      this.mobile = toBoolean(this.mobile, this.config.mobile);
      this.showAllOnFocus = toBoolean(this.showAllOnFocus, toBoolean(this.config.showAllOnFocus, true));
      this.showPanelOnFocus = toBoolean(this.showPanelOnFocus, toBoolean(this.config.showPanelOnFocus, true));
    }

    // Default values
    this.displayAttributes = this.displayAttributes || (this.filter && this.filter.attributes) || ['label', 'name'];
    this.displayWith = this.displayWith || ((obj) => obj && joinPropertiesPath(obj, this.displayAttributes));
    this.displayColumnSizes = this.displayColumnSizes || this.displayAttributes.map(attr => (
        // If label then col size = 2
        attr && attr.endsWith('label')) ? 2 :
        // If rankOrder then col size = 1
        (attr && attr.endsWith('rankOrder') ? 1 :
          // Else, auto size
          undefined));
    this.displayColumnNames = this.displayAttributes.map((attr, index) => {
      return this.displayColumnNames && this.displayColumnNames[index] ||
        (this.i18nPrefix + changeCaseToUnderscore(attr).toUpperCase());
    });
    this.mobile = toBoolean(this.mobile, false);
    this.searchable = !this.mobile;
    // Default comparator (only need when using mat-select)
    if (!this.searchable && !this.compareWith) this.compareWith = (o1: any, o2: any) => o1 && o2 && o1.id === o2.id;

    // No suggestFn: filter on the given items
    if (!this.suggestFn) {
      const suggestFromArrayFn: SuggestFn<any, any> = async (value, filter) => {
        const res  = await suggestFromArray(this.$inputItems.getValue(), value, {
          searchAttributes: this.displayAttributes,
          ...filter
        });
        this._itemCount = res && res.length || 0;
        return res;
      }
      // Wait (once) that items are loaded, then call suggest from array fn
      this.suggestFn = async (value, filter) => {
        if (isNil(this.$inputItems.getValue())) {
          //console.debug("[mat-autocomplete] Waiting items to be set...");
          await firstNotNilPromise(this.$inputItems);
          //console.debug("[mat-autocomplete] Received items:", this.$inputItems.getValue());
        }
        this.suggestFn = suggestFromArrayFn;
        return this.suggestFn(value, filter); // Loop
      };
    }
    else if (!this.searchable) {
      // Manually fill input items, from the given suggest function
      this._subscription.add(
        this._$filter.pipe(
          startWith<any, any>(this._$filter.getValue()),
          debounceTime(250),
          takeWhile((_) => !this.searchable) // Close subscription, when enabling search (no more mat-select)
        )
          .subscribe(async (filter) => {
            const res = await this.suggestFn('*', filter);
            this.$inputItems.next(res);
            this._itemCount = res && res.length || 0;
          })
      )
    }

    this.matSelectItems$ = this.$inputItems.asObservable()
      .pipe(
        takeWhile((_) => !this.searchable), // Close subscription, when enabling search (no more mat-select)
        filter(isNotNil),
        map((items) => {
          // Make sure control value is inside
          const value = this.formControl.value;
          // If form value is missing: add it to the list
          if (isNotNil(value) && typeof value === 'object' && items.findIndex(item => this.compareWith(item, value)) === -1) {
            // console.debug(this.logPrefix + " Add form value to items, because missed in items", value);
            return items.concat(value);
          }
          return items;
        })
      );


    const updateFilteredItemsEvents$ = merge(
      // Focus or click => Load all
      merge(this.onFocus, this.onClick)
      .pipe(
         filter(_ => this.searchable && this.enabled),
         map((_) => this.showAllOnFocus ? '*' : this.formControl.value)
      ),
        this.onDropButtonClick
          .pipe(
            filter(event => (!event || !event.defaultPrevented) && this.formControl.enabled),
            map((_) => "*")
          ),
        this.formControl.valueChanges
          .pipe(
            startWith<any, any>(this.formControl.value),
            filter(value => isNotNil(value)),
            //tap((value) => console.debug(this.logPrefix + " valueChanges:", value)),
            debounceTime(this.debounceTime)
          ),
        this.$inputItems.asObservable()
          .pipe(
            map(items => this.formControl.value)
          ),
        this._$filter.asObservable()
          .pipe(
            map(items => this.formControl.value)
          )
    );


    this.filteredItems$ = updateFilteredItemsEvents$
      .pipe(
        distinctUntilChanged(),
        //tap(value => console.debug(this.logPrefix + " Received update event: ", value)),
        switchMap(async (value) => {
          const res = await this.suggestFn(value, this.filter);
          // console.debug(this.logPrefix + " Filtered items by suggestFn:", value, res);
          this._itemCount = res && res.length || 0;
          return res;
        }),
        // Store implicit value (will use it onBlur if not other value selected)
        tap(res => this.updateImplicitValue(res)),
    );

    // Applying implicit value, on blur
    this._subscription.add(
      this.onBlur
       .pipe(
         // Skip if no implicit value, or has already a value
         filter(_ => this.searchable && this._implicitValue)
       )
       .subscribe( (_) => {
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

  /**
   * Allow to reload content. Useful when filter has been changed but not detected
   */
  reloadItems() {
    if (!this.searchable) {
      // Re sent the filter, to force a refresh
      this._$filter.next(this._$filter.getValue());
    }
    else {
      this.onDropButtonClick.emit();
    }
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
  getPropertyByPath = getPropertyByPath;

  focus() {
    if (this.searchable) {
      focusInput(this.matInputText);
    }
    else {
      focusInput(this.matSelect);
    }
  }

  filterInputTextFocusEvent(event: FocusEvent) {
    if (!event || event.defaultPrevented) return;

    // Ignore event from mat-option
    if (event.relatedTarget instanceof HTMLElement && event.relatedTarget.tagName === 'MAT-OPTION') {
      event.preventDefault();
      if (event.stopPropagation) event.stopPropagation();
      event.returnValue = false;
      //DEBUG console.debug(this.logPrefix + " Cancelling focus event");
      return false;
    }

    const hasContent = selectInputContent(event);
    if (!hasContent || (this.showPanelOnFocus && this.showAllOnFocus) ) {
      //DEBUG console.debug(this.logPrefix + " Emit focus event");
      this.onFocus.emit(event);
      return true;
    }
    return false;
  }

  filterMatSelectFocusEvent(event: FocusEvent) {
    if (!event || event.defaultPrevented) return;
    // DEBUG
    // console.debug(this.logPrefix + " Received <mat-select> focus event", event);
    this.onFocus.emit(event);
  }

  filterMatSelectBlurEvent(event: FocusEvent) {
    if (!event || event.defaultPrevented) return;

    // DEBUG
    // console.debug(this.logPrefix + " Received <mat-select> blur event", event);

    // Ignore event from mat-option
    if (event.relatedTarget instanceof HTMLElement && event.relatedTarget.tagName === 'MAT-OPTION') {
      event.preventDefault();
      if (event.stopPropagation) event.stopPropagation();
      event.returnValue = false;
      // DEBUG
      // console.debug(this.logPrefix + " Cancelling <mat-select> blur event");
      return false;
    }

    this.onBlur.emit(event);
    return true;
  }

  toggleSearch(event: UIEvent) {
    const searchable = !this.searchable;
    if (searchable && this.searchable && this.matInputText) {
      this.focus();
    }
    else {
      this.searchable = searchable;
      if (searchable) {
        this.appAutofocus = true;
        this.showPanelOnFocus = false;
      }
      this.markForCheck();
    }
  }

  clearValue(event: UIEvent) {
    this.writeValue(null);
    event.stopPropagation();
  }

  /* -- protected method -- */

  private updateImplicitValue(res: any[]) {
    if (this.searchable) {
      // Store implicit value (will use it onBlur if not other value selected)
      if (res && res.length === 1) {
        this._implicitValue = res[0];
        //this.formControl.setErrors(null);
      } else {
        this._implicitValue = undefined;
      }
    }
  }

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
