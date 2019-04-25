import { OnInit, OnDestroy, EventEmitter, Output, Input } from '@angular/core';
import { FormGroup } from "@angular/forms";
import { Platform } from '@ionic/angular';
import { Moment } from 'moment/moment';
import { DATE_ISO_PATTERN } from '../constants';
import { DateAdapter } from "@angular/material";
import { Subscription } from 'rxjs';

export abstract class AppForm<T> implements OnInit, OnDestroy {

  private _subscriptions: Subscription[];
  protected _enable: boolean = false;

  touchUi: boolean = false;
  mobile: boolean = false;
  error: string = null;

  @Input()
  debug: boolean = false;

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

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: Platform,
    public form: FormGroup
  ) {

    this.touchUi = !platform.is('desktop');
    this.mobile = this.touchUi && platform.is('mobile');
    //this.touchUi && console.debug("[form] Enabling touch UI");
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
    this.form.setValue(json, { emitEvent: false });

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
      }
      else {
        if (data[key] && typeof data[key] == "object" && data[key]._isAMomentObject) {
          value[key] = this.dateAdapter.format(data[key], DATE_ISO_PATTERN);
        }
        else {
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

  protected markForCheck() {
    // Should be override by subclasses
  }
}
