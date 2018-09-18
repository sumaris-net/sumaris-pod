import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";

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
      'rankOrder': ['', Validators.required],
      'comments': ['']
    });
  }
}
