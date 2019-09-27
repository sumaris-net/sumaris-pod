import {EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {AbstractControl, FormGroup} from "@angular/forms";
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {Subscription} from 'rxjs';
import {DateFormatPipe} from "../../shared/pipes/date-format.pipe";
import {AppFormUtils} from "./form.utils";
import {
  MatAutocompleteConfigHolder,
  MatAutocompleteFieldAddOptions,
  MatAutocompleteFieldConfig
} from "../../shared/material/material.autocomplete";
import {LocalSettingsService} from "../services/local-settings.service";

export abstract class AppForm<T> implements OnInit, OnDestroy {

  private _subscriptions: Subscription[];
  protected _enable = false;
  protected _implicitValues: { [key: string]: any } = {};

  autocompleteHelper: MatAutocompleteConfigHolder;
  autocompleteFields: {[key: string]: MatAutocompleteFieldConfig};
  error: string = null;

  @Input() debug = false;

  @Input() tabindex: number;

  get value(): any {
    return this.form.value;
  }

  set value(data: any) {
    this.setValue(data);
  }

  get dirty(): boolean {
    return this.form && this.form.dirty;
  }

  get invalid(): boolean {
    return !this.form || this.form.invalid;
  }

  get pending(): boolean {
    return !this.form || this.form.pending;
  }

  get valid(): boolean {
    return this.form && this.form.valid;
  }

  get empty(): boolean {
    return !this.form || (!this.form.dirty && !this.form.touched);
  }

  get untouched(): boolean {
    return !this.form || this.form.untouched;
  }

  get enabled(): boolean {
    return this._enable;
  }

  get disabled(): boolean {
    return !this._enable;
  }

  disable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    this.form && this.form.disable(opts);
    if (this._enable || (opts && opts.emitEvent)) {
      this._enable = false;
      this.markForCheck();
    }
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    this.form && this.form.enable(opts);
    if (!this._enable || (opts && opts.emitEvent)) {
      this._enable = true;
      this.markForCheck();
    }
  }

  @Input()
  form?: FormGroup;

  @Output()
  onCancel: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  onSubmit: EventEmitter<any> = new EventEmitter<any>();

  protected constructor(
    protected dateAdapter: DateAdapter<Moment> | DateFormatPipe,
    form?: FormGroup,
    protected settings?: LocalSettingsService
  ) {
    this.form = form || this.form;
    this.autocompleteHelper = new MatAutocompleteConfigHolder(settings && {
      getUserAttributes: (a, b) => settings.getFieldDisplayAttributes(a, b)
    });
    this.autocompleteFields = this.autocompleteHelper.fields;
  }

  ngOnInit() {
    this._enable ? this.enable() : this.disable();

    if (this.form) {
      this.form.statusChanges.subscribe(status => {
        this.markForCheck();
      });
    }
  }

  ngOnDestroy() {
    if (this._subscriptions) {
      if (this.debug) console.debug(`[form] Deleting ${this._subscriptions.length} subscriptions ${this.constructor.name}#`);
      this._subscriptions.forEach(s => s.unsubscribe());
      this._subscriptions = undefined;
    }
    this._implicitValues = {};
  }

  cancel() {
    this.onCancel.emit();
  }

  doSubmit(event: any) {
    if (!this.form && this.form.invalid) {
      return;
    }
    this.onSubmit.emit(event);
  }

  setForm(form: FormGroup) {
    this.form = form;
  }

  setValue(data: T, opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {
    if (!data) return;

    // Convert object to json
    if (this.debug) console.debug("[form] Updating form (using entity)", data);

    opts = opts || {};
    if (opts.emitEvent === undefined) opts.emitEvent = false; // default opts value

    // Apply to form
    AppFormUtils.copyEntity2Form(data, this.form, opts);

    this.markForCheck();
  }

  markAsPristine(opts?: {onlySelf?: boolean}) {
    AppFormUtils.markAsPristine(this.form, opts);
    this.markForCheck();
  }

  markAsUntouched(opts?: {onlySelf?: boolean}) {
    this.form.markAsUntouched(opts);
    this.markForCheck();
  }

  markAsTouched(opts?: {onlySelf?: boolean; emitEvent?: boolean; }) {
    AppFormUtils.markAsTouched(this.form, opts);
    this.markForCheck();
  }

  markAsDirty() {
    this.form.markAsDirty();
    this.markForCheck();
  }

  /**
   * @deprecated Use autocomplete field instead
   */
  updateImplicitValue(name: string, res: any[]) {
    this._implicitValues[name] = res && res.length === 1 ? res[0] : undefined;
  }

  /**
   * @deprecated Use autocomplete field instead
   */
  applyImplicitValue(name: string, control?: AbstractControl) {
    control = control || this.form.get(name);
    const value = control && this._implicitValues[name];
    if (control && value !== undefined && value !== null) {
      control.patchValue(value, {emitEvent: true});
      control.markAsDirty();
      this._implicitValues[name] = null;
    }
  }

  /* -- protected methods -- */

  protected registerSubscription(sub: Subscription) {
    this._subscriptions = this._subscriptions || [];
    this._subscriptions.push(sub);
    if (this.debug) console.debug(`[form] Registering a new subscription ${this.constructor.name}#${this._subscriptions.length}`);
  }

  protected registerAutocompleteConfig(fieldName: string, options?: MatAutocompleteFieldAddOptions) {
    return this.autocompleteHelper.add(fieldName, options);
  }

  protected markForCheck() {
    // Should be override by subclasses
  }
}
