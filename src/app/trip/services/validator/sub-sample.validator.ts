import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {Sample} from "../model/sample.model";
import {toNumber} from "../../../shared/functions";

@Injectable()
export class SubSampleValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Sample): FormGroup {
    return this.formBuilder.group({
      __typename: [Sample.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      rankOrder: [toNumber(data && data.rankOrder, null), Validators.required],
      label: [data && data.label || null, Validators.required],
      parent: [data && data.parent || null, Validators.compose([Validators.required, SharedValidators.object])],
      comments: [data && data.comments || null]
    });
  }
}
