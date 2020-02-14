import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import {Moment} from "moment";
import {VesselPosition} from "./trip.model";
import {toNumber} from "../../shared/functions";

@Injectable()
export class PositionValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: {id?: number; dateTime: Date|Moment; latitude: number; longitude: number; updateDate?: Date|Moment}, opts?: {required: boolean;}): FormGroup {
    return this.formBuilder.group({
      __typename: [VesselPosition.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      dateTime: [data && data.dateTime || null],
      latitude: [toNumber(data && data.latitude, null), opts && opts.required ? Validators.required : null],
      longitude: [toNumber(data && data.longitude, null), opts && opts.required ? Validators.required : null]
    });
  }
}
