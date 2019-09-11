import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import {Moment} from "moment";

@Injectable()
export class PositionValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: {dateTime: Date|Moment; latitude: number; longitude: number; }, opts?: {required: boolean;}): FormGroup {
    return this.formBuilder.group({
      id: [''],
      updateDate: [''],
      dateTime: [''],
      latitude: ['', opts && opts.required ? Validators.required : null],
      longitude: ['', opts && opts.required ? Validators.required : null]
    });
  }
}
