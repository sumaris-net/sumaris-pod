import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class IndividualMonitoringService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: any): FormGroup {
    return this.formBuilder.group({
      'id': [''],
      'parent': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'rankOrder': ['', Validators.required],
      'label': ['', Validators.required]
    });
  }
}
