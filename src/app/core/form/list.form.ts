import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Injector,
  Input,
  OnInit,
  Optional, Output
} from "@angular/core";
import {AbstractControl, FormArray, FormBuilder, FormGroup, FormGroupDirective, Validators} from "@angular/forms";
import {EntityUtils, referentialToString} from "../services/model";
import {FormFieldDefinition, FormFieldDefinitionMap, FormFieldValue} from "../../shared/form/field.model";
import {isEmptyArray, isNil} from "../../shared/functions";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {LocalSettingsService} from "../services/local-settings.service";
import {AppForm} from "./form.class";
import {FormArrayHelper, FormArrayHelperOptions} from "./form.utils";
import {SelectionModel} from "@angular/cdk/collections";
import {TableElement} from "angular4-material-table";

@Component({
  selector: 'app-list-form',
  templateUrl: 'list.form.html',
  styleUrls: ['./list.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppListForm<T = any> extends AppForm<T[]> implements OnInit {

  loading = true;
  helper: FormArrayHelper<T>;
  selection = new SelectionModel<T>(true, []);

  @Input() formArrayName: string;
  @Input() formArray: FormArray;
  @Input() options: FormArrayHelperOptions & { allowMultipleSelection?: boolean };

  @Input('displayWith') displayWithFn: (item: any) => string = referentialToString;
  @Input('equals') equalsFn: (v1: T, v2: T) => boolean;

  @Output() onNewItem = new EventEmitter<UIEvent>();
  @Output() onSelectionChange = new EventEmitter<T[]>(true);


  set value(data: T[]) {
    this.setValue(data);
  }

  get value(): T[] {
    return this.formArray.value as T[];
  }

  get itemControls(): FormGroup[] {
    return this.formArray && this.formArray.controls as FormGroup[];
  }

  get length(): number {
    return this.helper ? this.helper.size() : 0;
  }

  get dirty(): boolean {
    return this.formArray && this.formArray.dirty;
  }

  get invalid(): boolean {
    return !this.formArray || this.formArray.invalid;
  }

  get valid(): boolean {
    return this.formArray && this.formArray.valid;
  }

  get pending(): boolean {
    return !this.formArray || this.formArray.pending;
  }

  constructor(
    protected injector: Injector,
    protected formBuilder: FormBuilder,
    protected dateAdapter: DateAdapter<Moment>,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    @Optional() private formGroupDir: FormGroupDirective
  ) {
    super(dateAdapter,
      null,
      settings);

    //this.debug = !environment.production;
  }

  ngOnInit() {

    this.options = {
      allowEmptyArray: true,
      allowMultipleSelection: true,
      ...this.options};

    // Retrieve the form
    const form = (this.formArray && this.formArray.parent as FormGroup || this.formGroupDir && this.formGroupDir.form || this.formBuilder.group({}));
    this.setForm(form);

    this.formArray = this.formArray || this.formArrayName && form.get(this.formArrayName) as FormArray
    this.formArrayName = this.formArrayName || this.formArray && Object.keys(form.controls).find(key => form.get(key) === this.formArray) || 'properties';
    if (!this.formArray) {
      console.warn(`Missing array control '${this.formArrayName}'. Will create it!`)
      this.formArray = this.formBuilder.array([]);
      this.form.addControl(this.formArrayName, this.formArray);
    }

    this.equalsFn = this.equalsFn || ((v1: any, v2: any) => (isNil(v1) && isNil(v2)) || v1 === v2);
    this.helper = new FormArrayHelper<T>(
      this.formArray,
      (value) => this.createControl(value),
      this.equalsFn,
      (value) => isNil(value),
      this.options
    );

    super.ngOnInit();
  }

  setValue(data: T[] | any) {
    if (!data) return; // Skip

    if (this.helper) {
      this.helper.resize(data.length);
      this.helper.formArray.patchValue(data, {emitEvent: false});
    }

    this.selection.clear();
    this.markAsPristine();
    this.loading = false;
  }

  /** Whether the number of selected elements matches the total number of rows. */
  isAllSelected() {
    return this.selection.selected.length === this.length;
  }

  async masterToggle(opts?: {emitEvent?: boolean}) {
    if (this.loading) return;
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      const items = this.value;
      items.forEach(item => this.selection.select(item));
    }

    if (!opts || opts.emitEvent !== false) {
      this.onSelectionChange.emit(this.selection.selected);
    }
  }

  async onItemClick(event: MouseEvent, item: T, opts?: {emitEvent?: boolean}) {
    if (!item || this.onSelectionChange.observers.length === 0) return;

    // Multiple selection (if ctrl+click)
    if (event.ctrlKey && this.options.allowMultipleSelection) {
      this.selection.toggle(item);
    }
    // Only one selection
    else {
      if (this.selection.isSelected(item)) {
        this.selection.clear();
      }
      else {
        this.selection.clear();
        this.selection.select(item);
      }

    }

    if (!opts || opts.emitEvent !== false) {
      this.onSelectionChange.emit(this.selection.selected);
    }
  }

  hasSelection(): boolean {
    return this.selection.hasValue();
  }

  onNewClick(event: UIEvent) {
    this.onNewItem.emit(event);
  }

  add(value: T) {
    this.helper.add(value, {emitEvent: true});
    this.markForCheck();
  }

  removeAt(index: number) {
    this.helper.removeAt(index);
  }

  displayWith(value: T): string {
    return this.displayWithFn ? this.displayWithFn(value) : (value as any);
  }


  /* -- protected methods -- */

  protected createControl(data?: any): AbstractControl {
    return this.formBuilder.control(data || null, Validators.required);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

