import {ChangeDetectionStrategy, Component, forwardRef, Input, OnInit, Optional} from "@angular/core";
import {FormControl, FormGroupDirective, NG_VALUE_ACCESSOR} from "@angular/forms";
import {Observable} from "rxjs";
import {debounceTime, first, map, mergeMap, startWith, switchMap} from "rxjs/operators";
import {SuggestionDataService, TableDataService} from "../services/data-service.class";

export const DEFAULT_VALUE_ACCESSOR: any = {
  provide: NG_VALUE_ACCESSOR,
  useExisting: forwardRef(() => MatAutocompleteField),
  multi: true
};

@Component({
  selector: 'mat-autocomplete-field',
  templateUrl: 'material.autocomplete.html',
  providers: [DEFAULT_VALUE_ACCESSOR],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatAutocompleteField implements OnInit {

  private _onChangeCallback = (_: any) => {
  };
  private _onTouchedCallback = () => {
  };

  items: Observable<any[]>;

  @Input() formControl: FormControl;

  @Input() formControlName: string;

  @Input() floatLabel: string;

  @Input() placeholder: string;

  @Input() service: SuggestionDataService<any>;

  @Input() filter: any = undefined;

  @Input() required = false;

  @Input() displayWith = (_: any) => '';

  constructor(
    @Optional() private formGroupDir: FormGroupDirective
  ) {

  }

  ngOnInit() {
    this.formControl = this.formControl || this.formControlName && this.formGroupDir && this.formGroupDir.form.get(this.formControlName) as FormControl;
    if (!this.formControl) throw new Error("Missing mandatory attribute 'formControl' or 'formControlName' in <mat-autocomplete-field>.");

    if (this.service) {
      this.items = this.formControl.valueChanges
        .pipe(
          startWith('*'),
          debounceTime(250),
          switchMap((value) => this.service.suggest(value as string, this.filter))
        );
    }

    //this.formControl.valueChanges.subscribe(value => this._onChange(value));
  }

  writeValue(value: any): void {
    //console.debug("writeValue", value);
  }

  registerOnChange(fn: any): void {
    this._onChangeCallback = fn;
  }

  registerOnTouched(fn: any): void {
    this._onTouchedCallback = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      //this.formControl.disable({ onlySelf: true, emitEvent: false });
    } else {
      //this.formControl.enable({ onlySelf: true, emitEvent: false });
    }
  }

}
