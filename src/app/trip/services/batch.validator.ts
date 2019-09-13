import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";
import {Batch, BatchWeight} from "./model/batch.model";

@Injectable()
export class BatchValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Batch): FormGroup {
    return this.formBuilder.group(this.getFormGroupConfig(data));
  }

  protected getFormGroupConfig(data?: Batch): { [key: string]: any } {
    return {
      id: [''],
      updateDate: [''],
      rankOrder: ['1', Validators.required],
      label: [data && data.label || ''],
      individualCount: ['', Validators.compose([Validators.min(0), SharedValidators.integer])],
      samplingRatio: ['', SharedValidators.double()],
      samplingRatioText: [''],
      taxonGroup: ['', SharedValidators.entity],
      taxonName: ['', SharedValidators.entity],
      comments: [''],
      parent: ['', SharedValidators.entity],
      measurementValues: this.formBuilder.group({}),
      children: this.formBuilder.array([])
    };
  }

  getWeightFormGroup(data?: BatchWeight): FormGroup {
    return this.formBuilder.group({
      methodId: ['', SharedValidators.integer],
      estimated: [''],
      calculated: [''],
      value: ['', SharedValidators.double]
    });
  }
}
