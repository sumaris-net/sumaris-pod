import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, FormBuilder } from "@angular/forms";
import { Person } from "../../../core/services/model/person.model";
import { AccountValidatorService } from "../../../core/core.module";
import { Account } from "../../../core/services/model/account.model";
import { SharedValidators } from "../../../shared/validator/validators";

@Injectable()
export class PersonValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder,
    protected accountValidatorService: AccountValidatorService
  ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Person): FormGroup {

    // Use account validator as base form group definition
    // BUT add more flexibility (e.g. 'pubkey' become optional)
    // This is need to be able to store person that are not using SUMARiS tools (e.g. onboard obsevers)
    const formConfig = this.accountValidatorService.getFormGroupConfig(data && Account.fromObject(data.asObject));
    formConfig.pubkey = [data && data.pubkey || null, SharedValidators.pubkey];
    formConfig.avatar = [''];

    return this.formBuilder.group(formConfig);
  }


}
