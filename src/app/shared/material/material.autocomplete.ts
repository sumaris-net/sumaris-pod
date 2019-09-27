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
import {merge, Observable} from "rxjs";
import {debounceTime, distinctUntilChanged, filter, map, switchMap, takeUntil, tap, throttleTime} from "rxjs/operators";
import {SuggestionDataService} from "../services/data-service.class";
import {
  changeCaseToUnderscore,
  focusInput,
  getPropertyByPath,
  isNil,
  isNilOrBlank,
  joinPropertiesPath,
  selectInputContent,
  setTabIndex,
  suggestFromArray,
  toBoolean
} from "../functions";
import {InputElement} from "./focusable";
import {MatAutocomplete} from "@angular/material";

export const DEFAULT_VALUE_ACCESSOR: any = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => MatAutocompleteField),
  multi: true
};

export declare type DisplayFn = (obj: any) => string;

export declare interface  MatAutocompleteFieldConfig<T = any> {
  service?: SuggestionDataService<T>;
  filter?: any;
  items?: Observable<T[]> | T[];
  attributes: string[];
  columnSizes?: (number|'auto'|undefined)[];
  columnNames?: (string|undefined)[];
  displayWith?: DisplayFn;
  showAllOnFocus?: boolean;
  showPanelOnFocus?: boolean;
}

export declare interface  MatAutocompleteFieldAddOptions<T = any> {
  attributes?: string[];
  service?: SuggestionDataService<any>;
  filter?: any;
  displayWith?: DisplayFn;
  suggestFn?: (value: any, options?: any) => Promise<any[]>;
  showAllOnFocus?: boolean;
  showPanelOnFocus?: boolean;
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

  add<T = any>(fieldName: string, options?: MatAutocompleteFieldAddOptions<T>): MatAutocompleteFieldConfig<T> {
    if (!fieldName) {
      throw new Error("Unable to add config, with name: " + (fieldName || 'undefined'));
    }
    options = options || {};
    const service: SuggestionDataService<T> = options.service || (options.suggestFn && {
      suggest: (value: any, filterData?: any) => options.suggestFn(value, filterData)
    }) || undefined;
    const attributes = this.getUserAttributes(fieldName, options.attributes) || ['label', 'name'];
    const attributesOrFn = attributes.map((a, index) => a === "function" && options.attributes[index] || a);
    const filter = Object.assign({
      searchAttribute: attributes.length === 1 ? attributes[0] : undefined
    }, options.filter || {});
    const displayWith = options.displayWith || ((obj) => obj && joinPropertiesPath(obj, attributesOrFn));

    const config: MatAutocompleteFieldConfig = {
      attributes: attributesOrFn,
      service,
      filter,
      displayWith,
      showAllOnFocus: options.showAllOnFocus,
      showPanelOnFocus: options.showPanelOnFocus
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
  templateUrl: 'material.autocomplete.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => MatAutocompleteField),
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatAutocompleteField implements OnInit, InputElement, OnDestroy, ControlValueAccessor  {

  private _onChangeCallback = (_: any) => {};
  private _onTouchedCallback = () => {};
  private _implicitValue: any;
  private _onDestroy = new EventEmitter(true);

  //loading = false;
  $items: Observable<any[]>;

  onDropButtonClick = new EventEmitter<UIEvent>(true);

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() floatLabel: string;

  @Input() placeholder: string;

  @Input() service: SuggestionDataService<any>;

  @Input() filter: any = undefined;

  @Input() required = false;

  @Input() readonly = false;

  @Input() clearable = false;

  @Input() items: Observable<any[]> | any[];

  @Input() debounceTime = 250;

  @Input() displayWith: DisplayFn | null;

  @Input() displayAttributes: string[];

  @Input() displayColumnSizes: (number|'auto'|undefined)[];

  @Input() displayColumnNames: string[];

  @Input() showAllOnFocus: boolean;

  @Input() showPanelOnFocus: boolean;

  @Input() tabindex: number;

  @Input() appAutofocus: boolean;

  @Input() config: MatAutocompleteFieldConfig;

  @Input() i18nPrefix = 'REFERENTIAL.';

  @Input('class') classList: string;

  @Output('click') onClick = new EventEmitter<MouseEvent>();

  @Output('blur') onBlur = new EventEmitter<FocusEvent>();

  @Output('focus') onFocus = new EventEmitter<FocusEvent>();

  @ViewChild('matInput') matInput: ElementRef;

  @ViewChild('autoCombo') matAutocomplete: MatAutocomplete;

  get value(): any {
    return this.formControl.value;
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
      this.service = this.service || this.config.service;
      this.filter = this.filter || this.config.filter;
      this.displayAttributes = this.displayAttributes || this.config.attributes;
      this.displayColumnSizes = this.displayColumnSizes || this.config.columnSizes;
      this.displayColumnNames = this.displayColumnNames || this.config.columnNames;
      this.displayWith = this.displayWith || this.config.displayWith;
      this.showAllOnFocus = toBoolean(this.showAllOnFocus, toBoolean(this.config.showAllOnFocus, false));
      this.showPanelOnFocus = toBoolean(this.showPanelOnFocus, toBoolean(this.config.showPanelOnFocus, false));
    }

    // Default values
    this.displayAttributes = this.displayAttributes || (this.filter && this.filter.attributes) || ['label', 'name'];
    this.displayWith = this.displayWith || ((obj) => obj && joinPropertiesPath(obj, this.displayAttributes));
    this.displayColumnSizes = this.displayColumnSizes || this.displayAttributes.map(attr => (
        // If label then col size = 2
        attr.endsWith('label')) ? 2 :
        // If rankOrder then col size = 1
        (attr.endsWith('rankOrder') ? 1 :
          // Else, auto
          undefined));

    this.displayColumnNames = this.displayAttributes.map((attr, index) => {
      return this.displayColumnNames && this.displayColumnNames[index] ||
        (this.i18nPrefix + changeCaseToUnderscore(attr).toUpperCase());
    });

    const updateEvents$ = merge(
      merge(this.onFocus
          .pipe(
            // Skip when event comes from a mat-option
            filter(event => !event.defaultPrevented && !(event.relatedTarget instanceof HTMLElement && event.relatedTarget.tagName === 'MAT-OPTION'))
          ),
        this.onClick)
         .pipe(
           map((_) => this.showAllOnFocus ? '*' : this.formControl.value),
           //filter(value => isNil(value) || typeof value === "string")
        ),
      this.onDropButtonClick
        .pipe(
          filter(event => !event || !event.defaultPrevented),
          map((_) => "*")
        ),
      this.formControl.valueChanges
        .pipe(
          debounceTime(this.debounceTime),
          distinctUntilChanged()
        )
    )
        .pipe(
          takeUntil(this._onDestroy)
        );

    if (this.service) {
      this.$items = updateEvents$
        .pipe(
          throttleTime(100),
          switchMap((value) => this.service.suggest(value, this.filter)),
          // Store implicit value (will use it onBlur if not other value selected)
          tap(res =>  this.updateImplicitValue(res))
        );
    }
    else if (this.items) {
      let itemsArray: any[];
      if (this.items instanceof Array){
        itemsArray = this.items;
      }
      else if (this.items instanceof Observable){
        this.items
          .pipe(takeUntil(this._onDestroy))
          .subscribe(v => itemsArray = v);
      }
      const searchOptions = Object.assign({searchAttributes: this.displayAttributes}, this.filter);
      this.$items = updateEvents$
        .pipe(
          map(value => suggestFromArray(itemsArray, value, searchOptions)),
          // Store implicit value (will use it onBlur if not other value selected)
          tap(res =>  this.updateImplicitValue(res))
        );
    }

    else {
      console.warn("Missing attribute 'service', 'items' or 'config' in <mat-autocomplete-field>", this);
    }

    this.onBlur
       .pipe(
         // Skip if focus target is a mat-option
         filter(event => !event.defaultPrevented || !(event.relatedTarget instanceof HTMLElement) || event.relatedTarget.tagName !== 'MAT-OPTION'),
         // Wait panel closed
         //mergeMap(() => this.matAutocomplete.closed.pipe(first())),
         takeUntil(this._onDestroy)
       )
       .subscribe( (_) => {
        // When leave component without object, use implicit value if stored
        const existingValue = this.formControl.value;
        if (this._implicitValue && (isNilOrBlank(existingValue) || typeof this.formControl.value !== "object")) {
          this.writeValue(this._implicitValue);
          this.formControl.markAsPending({emitEvent: false, onlySelf: true});
          this.formControl.updateValueAndValidity({emitEvent: false, onlySelf: true});
        }
        this._implicitValue = null; // reset the implicit value
        this.checkIfTouched();
      });

    // Update tab index
    this.updateTabIndex();
  }

  ngOnDestroy(): void {
    this._onDestroy.emit();
  }

  writeValue(value: any): void {
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
  }

  clear() {
    this.formControl.setValue(null);
    this.markForCheck();
  }

  selectInputContent = selectInputContent;
  getPropertyByPath = getPropertyByPath;

  focus() {
    focusInput(this.matInput);
  }

  _onFocus(event: FocusEvent) {
    if (!event || event.defaultPrevented) return;

    // Ignore event from mat-option
    if (event.relatedTarget instanceof HTMLElement && event.relatedTarget.tagName === 'MAT-OPTION') {
      event.preventDefault();
      if (event.stopPropagation) event.stopPropagation();
      event.returnValue = false;
      return false;
    }

    const hasContent = selectInputContent(event);
    if (!hasContent || (this.showPanelOnFocus && this.showAllOnFocus) ) {
      this.onFocus.emit(event);
      return true;
    }
    return false;
  }

  /* -- protected method -- */

  private updateImplicitValue(res: any[]) {
    // Store implicit value (will use it onBlur if not other value selected)
    if (res && res.length === 1) {
      this._implicitValue = res[0];
      //this.formControl.setErrors(null);
    } else {
      this._implicitValue = undefined;
    }
  }

  private checkIfTouched() {
    if (this.formControl.touched) {
      this.markForCheck();
      this._onTouchedCallback();
    }
  }

  private updateTabIndex() {
    if (isNil(this.tabindex) || this.tabindex === -1) return; // skip

    setTimeout(() => {
      setTabIndex(this.matInput, this.tabindex);
      this.markForCheck();
    });
  }

  private markForCheck() {
    this.cd.markForCheck();
  }
}
