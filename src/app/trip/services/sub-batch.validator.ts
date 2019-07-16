import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class SubBatchValidatorService implements ValidatorService {

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
      individualCount: ['1', Validators.compose([Validators.min(1), SharedValidators.integer])],
      samplingRatio: [null, SharedValidators.empty], // Make no sense to have sampling ratio
      samplingRatioText: [null, SharedValidators.empty], // Make no sense to have sampling ratio
      taxonGroup: [null, SharedValidators.entity],
      taxonName: [null, SharedValidators.entity],
      comments: [''],
      parent: ['', Validators.compose([Validators.required, SharedValidators.object])],
      measurementValues: this.formBuilder.group({})
    });
  }
}
