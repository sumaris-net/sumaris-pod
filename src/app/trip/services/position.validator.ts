import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import {Moment} from "moment";
import {VesselPosition} from "./trip.model";

@Injectable()
export class PositionValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: {dateTime: Date|Moment; latitude: number; longitude: number; }, opts?: {required: boolean;}): FormGroup {
    return this.formBuilder.group({
      __typename: [VesselPosition.TYPENAME],
      id: [null],
      updateDate: [null],
      dateTime: [null],
      latitude: [null, opts && opts.required ? Validators.required : null],
      longitude: [null, opts && opts.required ? Validators.required : null]
    });
  }
}
