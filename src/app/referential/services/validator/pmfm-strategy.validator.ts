import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {PmfmStrategy, Strategy} from "../model";

import {ValidatorService} from "angular4-material-table";
import {SharedValidators} from "../../../shared/validator/validators";

@Injectable()
export class PmfmStrategyValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder
  ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: PmfmStrategy): FormGroup {
    return this.formBuilder.group({
      id: [data && data.id || null],
      acquisitionLevel: [data && data.acquisitionLevel || null, Validators.required],
      rankOrder: [data && data.rankOrder || null, Validators.compose([Validators.required, SharedValidators.integer, Validators.min(1)])],
      pmfm: [data && data.pmfm || null, Validators.compose([Validators.required, SharedValidators.entity])],
      isMandatory: [data && data.isMandatory || null, Validators.required],
      acquisitionNumber: [data && data.acquisitionNumber || null, Validators.compose([Validators.required, SharedValidators.integer, Validators.min(1)])],
      minValue: [data && data.minValue || null, SharedValidators.double()],
      maxValue: [data && data.maxValue || null, SharedValidators.double()]
    });
  }
}
