import { OnInit, EventEmitter, Output } from '@angular/core';
import { FormGroup } from "@angular/forms";
import { Platform } from '@ionic/angular';
import { Moment } from 'moment/moment';
import { DATE_ISO_PATTERN } from '../constants';
import { DateAdapter } from "@angular/material";


export abstract class AppForm<T> implements OnInit {

  private _enable: boolean = false;
  public touchUi: boolean = false;
  public mobile: boolean = false;

  public error: string = null;

  public get value(): any {
    return this.form.value;
  }
  public set value(data: any) {
    this.setValue(data);
  }

  public get dirty(): boolean {
    return this.form.dirty;
  }
  public get invalid(): boolean {
    return this.form.invalid;
  }
  public get valid(): boolean {
    return this.form.valid;
  }

  public get empty(): boolean {
    return !this.form.dirty && !this.form.touched;
  }

  public disable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    this.form && this.form.disable(opts);
    this._enable = false;
  }

  public enable(opts?: {
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

    this.mobile = platform.is('mobile');
    this.touchUi = !platform.is('desktop');
    //this.touchUi && console.debug("[form] Enabling touch UI");
  }

  ngOnInit() {
    this._enable ? this.enable() : this.disable();
  }

  public cancel() {
    this.onCancel.emit();
  }

  public doSubmit(event: any, data: any) {
    if (!this.form && this.form.invalid) return;
    this.onSubmit.emit(event);
  }

  public setValue(data: T) {
    if (!data) return;

    // Convert object to json
    let json = this.toJsonFormValue(this.form, data);
    console.debug("[form] Updating form... ", json);

    // Appply to form
    this.form.setValue(json);
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
          value[key] = data[key] || null;
        }
      }
    }
    return value;
  }

  public markAsPristine() {
    this.form.markAsPristine({ onlySelf: false });
  }

  public markAsUntouched() {
    this.form.markAsUntouched();
  }

  public markAsTouched() {
    this.form.markAsTouched();
  }
}
