import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { VesselValidatorService } from "../validator/validators";
import { FormGroup } from "@angular/forms";
import { VesselFeatures, Referential } from "../../../services/model";
import { Platform } from 'ionic-angular';
import { Moment } from 'moment/moment';
import { DATE_ISO_PATTERN } from '../../../app/constants';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap, startWith } from 'rxjs/operators';
import { VesselService } from '../../../services/vessel-service';
import { ReferentialService } from '../../../services/referential-service';


@Component({
  selector: 'form-vessel',
  templateUrl: './form-vessel.html'
})
export class VesselForm implements OnInit {

  form: FormGroup;
  data: VesselFeatures;

  touchUi: boolean = false;
  mobile: boolean = false;
  locations: Observable<Referential[]>;

  public error: string = null;

  public get value(): any {
    return this.form.value;
  }

  public disable(opts?: {
      onlySelf?: boolean;
      emitEvent?: boolean;
  }): void {
    this.form.disable(opts);
  }

  public enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }): void {
    this.form.enable(opts);
  }
  
  @Output()
  onCancel:EventEmitter<any> = new EventEmitter<any>();
  
  @Output()
  onSubmit: EventEmitter<any> = new EventEmitter<any>();

  constructor(
    private dateAdapter: DateAdapter<Moment>,
    private platform: Platform,
    private vesselValidatorService: VesselValidatorService,
    private referentialService: ReferentialService,
    private vesselService: VesselService) {

      this.touchUi = !platform.is('core');
      this.mobile = platform.is('mobile');
      if (this.touchUi) {
       console.debug("[vessel] Enabling touch UI");
      }
  }

  ngOnInit() {
    this.form = this.vesselValidatorService.getFormGroup();
    this.locations = this.form.controls['basePortLocation']
      .valueChanges
      .pipe(
        mergeMap(value => {
          if (!value) return Observable.empty();
          if (typeof value == "object") return Observable.of([value]);
          return this.referentialService.loadAll(0,50, undefined, undefined,
            {
              entityName: 'Location'/*searchText: value as string*/,
              levelId: 2 /* Port */
            }
          );
        }));
  }

  cancel() {
    this.onCancel.emit();
  }

  doSubmit(event:any, data: any) {
    if (this.form.invalid) return;    
    this.onSubmit.emit(data);
  }

  displayReferentialFn(ref?: Referential | any): string | undefined {
    return ref ? (ref.label + " - " + ref.name) : undefined;
  }

  public setValue(data: VesselFeatures) {
    this.data = data;

    // Convert object to json
    let json = this.getValue(this.form, data);
    console.debug("[vessel-form] Updating form... ", json);

    // Appply to form
    this.form.setValue(json);
  }

  getValue(form: FormGroup, data: any): Object {
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

  markAsPristine() {
    this.form.markAsPristine();
  }
}
