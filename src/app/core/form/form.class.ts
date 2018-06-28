import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { FormGroup } from "@angular/forms";
import { Platform } from 'ionic-angular';
import { Moment } from 'moment/moment';
import { DATE_ISO_PATTERN } from '../constants';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap, startWith } from 'rxjs/operators';
import { merge } from "rxjs/observable/merge";


export abstract class AppForm<T> implements OnInit {

  private _enable: boolean = false;
  public touchUi: boolean = false;
  public mobile: boolean = false;

  public error: string = null;

  public get value(): any {
    return this.form.value;
  }
  public get dirty(): any {
    return this.form.dirty;
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
    this.touchUi = !platform.is('core');
    this.touchUi && console.debug("[form] Enabling touch UI");
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
    let json = this.getValue(this.form, data);
    console.debug("[form] Updating form... ", json);

    // Appply to form
    this.form.setValue(json);
  }

  protected getValue(form: FormGroup, data: any): Object {
    let value = {};
    form = form || this.form;
    for (let key in form.controls) {
      if (form.controls[key] instanceof FormGroup) {
        value[key] = this.getValue(form.controls[key] as FormGroup, data[key]);
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
    this.form.markAsPristine();
  }

  public markAsUntouched() {
    this.form.markAsUntouched();
  }
}
