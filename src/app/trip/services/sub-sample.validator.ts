import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { SharedValidators } from "../../shared/validator/validators";
import {Sample} from "./model/sample.model";

@Injectable()
export class SubSampleValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Sample): FormGroup {
    return this.formBuilder.group({
      __typename: [Sample.TYPENAME],
      id: [''],
      updateDate: [''],
      rankOrder: [null, Validators.required],
      label: ['', Validators.required],
      parent: [null, Validators.compose([Validators.required, SharedValidators.entity])],
      comments: [null]
    });
  }
}
