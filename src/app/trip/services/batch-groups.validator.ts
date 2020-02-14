import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {Batch} from "./model/batch.model";
import {BatchValidatorService} from "./batch.validator";

@Injectable()
export class BatchGroupValidatorService extends BatchValidatorService {

  constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormGroup(data?: Batch): FormGroup {
    return super.getFormGroup(data);
  }
}
