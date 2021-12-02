import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "@sumaris-net/ngx-components";
import {Sample} from "../model/sample.model";
import {toNumber} from "@sumaris-net/ngx-components";

@Injectable({providedIn: 'root'})
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
      label: [data && data.label || null],
      parent: [data && data.parent || null, Validators.compose([Validators.required, SharedValidators.object])],
      comments: [data && data.comments || null]
    });
  }
}
