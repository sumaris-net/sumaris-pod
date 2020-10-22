import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {PmfmStrategy} from "../model/pmfm-strategy.model";

import {ValidatorService} from "@e-is/ngx-material-table";
import {SharedValidators} from "../../../shared/validator/validators";
import {isNotNil} from "../../../shared/functions";

@Injectable({providedIn: 'root'})
export class PmfmSimpleStrategyValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder
  ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: PmfmStrategy): FormGroup {
    return this.formBuilder.group({
      id: [data && data.id || null],
      pmfm: [data && data.pmfm || null, Validators.compose([Validators.required, SharedValidators.entity])],
      parameter: [data && data.pmfm.parameter || null, Validators.compose([Validators.required, SharedValidators.entity])],
      matrix: [data && data.pmfm.matrix || null, Validators.compose([Validators.required, SharedValidators.entity])],
      fraction: [data && data.pmfm.fraction || null, Validators.compose([Validators.required, SharedValidators.entity])],
      method: [data && data.pmfm.method || null, Validators.compose([Validators.required, SharedValidators.entity])],
    });
  }
}
