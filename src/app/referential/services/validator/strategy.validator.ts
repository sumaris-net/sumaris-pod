import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Strategy} from "../model";

import {ValidatorService} from "angular4-material-table";
import {PmfmStrategyValidatorService} from "./pmfm-strategy.validator";

@Injectable()
export class StrategyValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder,
    protected pmfmStrategyValidatorService: PmfmStrategyValidatorService
  ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Strategy): FormGroup {
    return this.formBuilder.group({
      id: [data && data.id || null],
      updateDate: [data && data.updateDate || null],
      creationDate: [data && data.creationDate || null],
      statusId: [data && data.statusId || null, Validators.required],
      label: [data && data.label || null, Validators.required],
      name: [data && data.name || null, Validators.required],
      description: [data && data.description || null, Validators.maxLength(255)],
      comments: [data && data.comments || null, Validators.maxLength(2000)],
      pmfmStrategies: this.getPmfmStrategiesFormArray(data)
    });
  }

  getPmfmStrategiesFormArray(data?: Strategy) {
    return this.formBuilder.array(
      (data && data.pmfmStrategies || []).map(ps => this.pmfmStrategyValidatorService.getFormGroup(ps))
    );
  }
}
