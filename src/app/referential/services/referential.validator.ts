import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { Referential } from "./model";

@Injectable()
export class ReferentialValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Referential): FormGroup {
    return this.formBuilder.group({
      id: [''],
      updateDate: [''],
      creationDate: [''],
      statusId: ['', Validators.required],
      levelId: [''],
      label: ['', Validators.required],
      name: ['', Validators.required],
      description: ['', Validators.maxLength(255)],
      comments: ['', Validators.maxLength(2000)],
      entityName: ['', Validators.required],
      dirty: ['']
    });
  }
}
