import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators, ValidatorFn} from "@angular/forms";
import {EntityUtils, Program} from "../model";

import {ValidatorService} from "angular4-material-table";
import {ConfigOption} from "../../../core/services/model";

@Injectable()
export class ConfigOptionValidatorService implements ValidatorService {

  constructor(protected formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: {key: string; value: string; }): FormGroup {
    return this.formBuilder.group({
      key: [data && data.key || null, Validators.compose([Validators.required, Validators.max(50)])],
      value: [data && data.value || null, Validators.compose([Validators.required, Validators.max(100)])]
    });
  }

  getValidator(option: ConfigOption): ValidatorFn | ValidatorFn[] {
    return [];
  }
}
