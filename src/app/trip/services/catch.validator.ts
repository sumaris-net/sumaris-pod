import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { Operation } from "./model";

@Injectable()
export class CatchValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Operation): FormGroup {
    return this.formBuilder.group(this.getFields());
  }

  getFields(): { [key: string]: any } {
    return {
      id: ['']
    };
  }
}
