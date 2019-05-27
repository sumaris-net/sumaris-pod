import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class BatchValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: any): FormGroup {
    return this.formBuilder.group({
      id: [''],
      updateDate: [''],
      rankOrder: ['1', Validators.required],
      label: [data && data.label || ''],
      individualCount: ['', Validators.compose([Validators.min(0), SharedValidators.integer])],
      samplingRatio: [''],
      samplingRatioText: [''],
      taxonGroup: ['', SharedValidators.entity],
      taxonName: ['', SharedValidators.entity],
      comments: [''],
      parent: ['', SharedValidators.entity],
      dirty: [''],
      measurementValues: this.formBuilder.group({})
    });
  }
}
