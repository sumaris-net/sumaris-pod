import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {Batch} from "./model/batch.model";
import {BatchValidatorService} from "./batch.validator";
import {BatchGroup} from "./model/batch-group.model";
import {SharedValidators} from "../../shared/validator/validators";

@Injectable()
export class BatchGroupValidatorService extends BatchValidatorService<BatchGroup> {

  constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormGroup(data?: BatchGroup, opts?: {
    withWeight?: boolean;
    rankOrderRequired?: boolean;
    labelRequired?: boolean;
  }): FormGroup {
    return super.getFormGroup(data, opts);
  }

  protected getFormGroupConfig(data?: BatchGroup, opts?: {
    withWeight?: boolean;
    rankOrderRequired?: boolean;
    labelRequired?: boolean;
  }): { [key: string]: any } {
    const config = super.getFormGroupConfig(data, opts);

    config.observedIndividualCount = [data && data.observedIndividualCount, SharedValidators.integer];

    return config;
  }
}
