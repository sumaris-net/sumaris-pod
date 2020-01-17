import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { Referential } from "./model";

@Injectable()
export class ReferentialValidatorService implements ValidatorService {

  constructor(protected formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Referential, opts?: {
    withDescription?: boolean;
    withComments?: boolean;
  }): FormGroup {
    return this.formBuilder.group(
      this.getFormConfig(data, opts)
    );
  }

  getFormConfig(data?: Referential, opts?: {
    withDescription?: boolean;
    withComments?: boolean;
  }): {[key: string]: any} {
    const controlsConfig: {[key: string]: any} = {
      id: [data && data.id || null],
      updateDate: [data && data.updateDate || null],
      creationDate: [data && data.creationDate || null],
      statusId: [data && data.statusId || null, Validators.required],
      levelId: [data && data.levelId || null],
      label: [data && data.label || null, Validators.required],
      name: [data && data.name || null, Validators.required],
      entityName: [data && data.entityName || null, Validators.required]
    };

    if (!opts || opts.withDescription !== false) {
      controlsConfig.description = [data && data.description || null, Validators.maxLength(255)];
    }
    if (!opts || opts.withComments !== false) {
      controlsConfig.comments = [data && data.comments || null, Validators.maxLength(2000)];
    }

    return controlsConfig;
  }
}
