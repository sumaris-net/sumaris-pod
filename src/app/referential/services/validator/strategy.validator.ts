import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Strategy} from "../model";

import {ValidatorService} from "angular4-material-table";

@Injectable()
export class StrategyValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder
  ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Strategy): FormGroup {
    return this.formBuilder.group({
      id: [''],
      updateDate: [''],
      creationDate: [''],
      statusId: ['', Validators.required],
      label: ['', Validators.required],
      name: ['', Validators.required],
      description: ['', Validators.maxLength(255)],
      comments: ['', Validators.maxLength(2000)]
    });
  }
}
