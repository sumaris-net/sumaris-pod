import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";

@Injectable()
export class PositionValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Position): FormGroup {
    return this.formBuilder.group({
      'id': [''],
      'updateDate': [''],
      'dateTime': [''],
      'latitude': ['', Validators.required],
      'longitude': ['', Validators.required]
    });
  }
}
