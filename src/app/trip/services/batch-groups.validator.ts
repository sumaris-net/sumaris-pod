import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { SharedValidators } from "../../shared/validator/validators";
import { PmfmStrategy } from "../services/trip.model";

@Injectable()
export class BatchGroupsValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  getRowValidator(options?: {
    pmfms: PmfmStrategy[]
  }): FormGroup {
    return this.getFormGroup(null, options);
  }

  getFormGroup(
    data?: any,
    options?: {
      pmfms: PmfmStrategy[]
    }): FormGroup {


    return this.formBuilder.group({
      id: [''],
      updateDate: [''],
      rankOrder: ['1', Validators.required],
      label: [''],
      individualCount: ['', Validators.compose([Validators.min(0), SharedValidators.integer])],
      samplingRatio: [''],
      samplingRatioText: [''],
      taxonGroup: ['', SharedValidators.entity],
      taxonName: ['', SharedValidators.entity],
      comments: [''],
      parent: ['', SharedValidators.entity]
    });
  }

}
