import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class SampleValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: any): FormGroup {
    return this.formBuilder.group({
      id: [''],
      updateDate: [''],
      creationDate: [''],
      rankOrder: ['', Validators.required],
      label: ['', Validators.required],
      sampleDate: ['', Validators.required],
      taxonGroup: [null, Validators.compose([Validators.required, SharedValidators.entity])],
      comments: ['']
    });
  }
}
