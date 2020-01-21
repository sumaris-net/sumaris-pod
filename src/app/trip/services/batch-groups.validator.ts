import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";
import {Batch} from "./model/batch.model";
import {isNotNil} from "./trip.model";

@Injectable()
export class BatchGroupValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Batch): FormGroup {
    return this.formBuilder.group({
      __typename: ['BatchVO'],
      id: [null],
      updateDate: [null],
      rankOrder: ['1', Validators.required],
      label: [data && data.label || ''],
      individualCount: [data && isNotNil(data.individualCount) ? data.individualCount : null , Validators.compose([Validators.min(0), SharedValidators.integer])],
      samplingRatio: [data && isNotNil(data.samplingRatio) ? data.samplingRatio : null, SharedValidators.double()],
      samplingRatioText: [data && data.samplingRatioText || null],
      taxonGroup: [data && data.taxonGroup || null, SharedValidators.entity],
      taxonName: [data && data.taxonName || null, SharedValidators.entity],
      comments: [data && data.comments || null],
      parent: [data && data.parent || null, SharedValidators.entity],
      measurementValues: this.formBuilder.group({}),
      children: this.formBuilder.array([])
    });
  }
}
