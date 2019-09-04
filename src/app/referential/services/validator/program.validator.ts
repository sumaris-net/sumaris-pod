import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {EntityUtils, Program} from "../model";

import {ValidatorService} from "angular4-material-table";

@Injectable()
export class ProgramValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder
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
      properties.map(item => this.getPropertyFormGroup(item))
    );
  }

  getPropertyFormGroup(data?: {key: string; value?: string;}): FormGroup {
    return this.formBuilder.group({
      key: [data && data.key || null, Validators.compose([Validators.required, Validators.max(50)])],
      value: [data && data.value || null, Validators.compose([Validators.required, Validators.max(100)])]
    });
  }
}
