import {EventEmitter, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {AbstractControl, FormGroup} from "@angular/forms";
import {Moment} from 'moment/moment';
import {DATE_ISO_PATTERN} from '../constants';
import {DateAdapter} from "@angular/material";
import {Subscription} from 'rxjs';
import {DateFormatPipe} from "../../shared/pipes/date-format.pipe";

export abstract class AppForm<T> implements OnInit, OnDestroy {

  private _subscriptions: Subscription[];
  protected _enable = false;
  protected _implicitValues: { [key: string]: any } = {};
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
    return this.form.dirty;
  }

  get invalid(): boolean {
    return this.form.invalid;
  }

  get valid(): boolean {
    return this.form.valid;
  }

  get empty(): boolean {
    return !this.form.dirty && !this.form.touched;
  }

  get untouched(): boolean {
    return this.form.untouched;
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

  @Output()
  onCancel: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  onSubmit: EventEmitter<any> = new EventEmitter<any>();

  protected constructor(
    protected dateAdapter: DateAdapter<Moment> | DateFormatPipe,
    public form?: FormGroup
  ) {
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

  public cancel() {
    this.onCancel.emit();
  }

  public doSubmit(event: any, data?: any) {
    if (!this.form && this.form.invalid) return;
    this.onSubmit.emit(event);
  }

  public setForm(form: FormGroup) {
    this.form = form;
  }

  public setValue(data: T) {
    if (!data) return;

    // Convert object to json
    let json = this.toJsonFormValue(this.form, data);
    if (this.debug) console.debug("[form] Updating form... ", json);

    // Apply to form
    this.form.setValue(json, {emitEvent: false});

    this.markForCheck();
  }

  /**
   * Transform an object (e.g. an entity) into a json compatible with the given form
   * @param form
   * @param data
   */
  protected toJsonFormValue(form: FormGroup, data: any): Object {
    let value = {};
    form = form || this.form;
    for (let key in form.controls) {
      if (form.controls[key] instanceof FormGroup) {
        value[key] = this.toJsonFormValue(form.controls[key] as FormGroup, data[key]);
      } else {
        if (data[key] && typeof data[key] == "object" && data[key]._isAMomentObject) {
          value[key] = this.dateAdapter.format(data[key], DATE_ISO_PATTERN);
        } else {
          value[key] = data[key] || (data[key] === 0 ? 0 : null); // Do NOT replace 0 by null
        }
      }
    }
    return value;
  }

  protected registerSubscription(sub: Subscription) {
    this._subscriptions = this._subscriptions || [];
    this._subscriptions.push(sub);
    if (this.debug) console.debug(`[form] Registering a new subscription ${this.constructor.name}#${this._subscriptions.length}`);
  }

  public markAsPristine() {
    this.form.markAsPristine();
    this.markForCheck();
  }

  public markAsUntouched() {
    this.form.markAsUntouched();
    this.markForCheck();
  }

  public markAsTouched() {
    this.form.markAsTouched();
    this.markForCheck();
  }

  public markAsDirty() {
    this.form.markAsDirty();
    this.markForCheck();
  }

  public updateImplicitValue(name: string, res: any[]) {
    this._implicitValues[name] = res && res.length === 1 ? res[0] : undefined;
  }

  public applyImplicitValue(name: string, control?: AbstractControl) {
    control = control || this.form.get(name);
    const value = control && this._implicitValues[name];
    if (control && value !== undefined && value !== null) {
      control.patchValue(value, {emitEvent: false});
      control.markAsDirty();
      this._implicitValues[name] = null;
    }
  }

  protected markForCheck() {
    // Should be override by subclasses
  }
}
