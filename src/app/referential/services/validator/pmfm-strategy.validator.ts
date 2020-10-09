import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {PmfmStrategy} from "../model/pmfm-strategy.model";

import {ValidatorService} from "@e-is/ngx-material-table";
import {SharedValidators} from "../../../shared/validator/validators";
import {isNotNil} from "../../../shared/functions";

@Injectable({providedIn: 'root'})
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
      id: [data && data.id || null],
      acquisitionLevel: [data && data.acquisitionLevel || null, Validators.required],
      rankOrder: [data && data.rankOrder || 1, Validators.compose([Validators.required, SharedValidators.integer, Validators.min(1)])],
      pmfm: [data && data.pmfm || null, Validators.compose([Validators.required, SharedValidators.entity])],
      unit: [data && data.pmfm.unit || null, Validators.compose([Validators.required, SharedValidators.entity])],
      matrix: [data && data.pmfm.matrix || null, Validators.compose([Validators.required, SharedValidators.entity])],
      fraction: [data && data.pmfm.fraction || null, Validators.compose([Validators.required, SharedValidators.entity])],
      method: [data && data.pmfm.method || null, Validators.compose([Validators.required, SharedValidators.entity])],
      isMandatory: [data && data.isMandatory || false, Validators.required],
      acquisitionNumber: [data && data.acquisitionNumber || 1, Validators.compose([Validators.required, SharedValidators.integer, Validators.min(1)])],
      minValue: [data && data.minValue || null, SharedValidators.double()],
      maxValue: [data && data.maxValue || null, SharedValidators.double()],
      defaultValue: [isNotNil(data && data.defaultValue) ? data.defaultValue : null],
      gearIds: [data && data.gearIds || null],
      taxonGroupIds: [data && data.taxonGroupIds || null],
      referenceTaxonIds: [data && data.referenceTaxonIds || null]
    });
  }
}
