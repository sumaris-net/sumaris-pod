import { OnInit, OnDestroy, EventEmitter, Output, Input } from '@angular/core';
import { FormGroup } from "@angular/forms";
import { Platform } from '@ionic/angular';
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Subscription } from 'rxjs';
import { AppFormUtils } from "./form.utils";

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
  get enabled(): boolean {
    return this._enable;
  }
  get disabled(): boolean {
    return !this._enable;
  }
  get untouched(): boolean {
    return this.form.untouched;
  }

  disable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    this.form && this.form.disable(opts);
    this._enable = false;
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    this.form && this.form.enable(opts);
    this._enable = true;
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
  }

  ngOnDestroy() {
    if (this._subscriptions) {
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

  public setValue(data: T) {
    if (!data) return;
    AppFormUtils.copyEntity2Form(data, this.form, { emitEvent: false });
  }


  protected registerSubscription(sub: Subscription) {
    this._subscriptions = this._subscriptions || [];
    this._subscriptions.push(sub);
  }

  public markAsPristine() {
    this.form.markAsPristine();
  }

  public markAsUntouched() {
    this.form.markAsUntouched();
  }

  public markAsTouched() {
    this.form.markAsTouched();
  }
}
