import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {Batch} from "./model/batch.model";
import {BatchValidatorService} from "./batch.validator";
import {BatchGroup} from "./model/batch-group.model";

@Injectable()
export class BatchGroupValidatorService extends BatchValidatorService<BatchGroup> {

  constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormGroup(data?: BatchGroup): FormGroup {
    return super.getFormGroup(data);
  }

  protected getFormGroupConfig(data?: BatchGroup,  opts?: {
    rankOrderRequired?: boolean;
    labelRequired?: boolean;
  }): { [key: string]: any } {
    const config = super.getFormGroupConfig(data, opts);

    config.observedIndividualCount = [data && data.observedIndividualCount];

    return config;
  }
}
