import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {EntityUtils, Program} from "../model";

import {ValidatorService} from "angular4-material-table";
import {ConfigOptionValidatorService} from "./config-option.validator";

@Injectable()
export class ProgramValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder,
    protected configOptionValidator: ConfigOptionValidatorService
  ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Program): FormGroup {
    return this.formBuilder.group({
      id: [''],
      updateDate: [''],
      creationDate: [''],
      statusId: ['', Validators.required],
      label: ['', Validators.required],
      name: ['', Validators.required],
      description: ['', Validators.maxLength(255)],
      comments: ['', Validators.maxLength(2000)],
      properties: this.getPropertiesArray(data && data.properties)
    });
  }

  getPropertiesArray(array?: any) {
    const properties = (array && array instanceof Array) ? array : EntityUtils.getObjectAsArray(array || {});
    return this.formBuilder.array(
      properties.map(item => this.configOptionValidator.getFormGroup(item))
    );
  }

  getPropertyFormGroup(data?: {key: string; value: string; }) {
    return this.configOptionValidator.getFormGroup(data);
  }
}
