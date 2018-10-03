import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormControl, FormGroup, Validators, ValidatorFn } from "@angular/forms";
import { Account } from "./model";
import { AccountService } from "./account.service";
import { getMainProfile, StatusIds } from "./model";
import { AccountFieldDef } from "../core.module";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class AccountValidatorService implements ValidatorService {

  constructor(
    protected accountService: AccountService
  ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Account): FormGroup {
    const formDef = {
      'id': new FormControl(),
      'lastName': new FormControl(data && data.lastName || null, Validators.compose([Validators.required, Validators.minLength(2)])),
      'firstName': new FormControl(data && data.firstName || null, Validators.compose([Validators.required, Validators.minLength(2)])),
      'email': new FormControl(data && data.email || null, Validators.compose([Validators.required, Validators.email])),
      'mainProfile': new FormControl(data && (data.mainProfile || getMainProfile(data.profiles)) || 'GUEST', Validators.required),
      'statusId': new FormControl(data && data.statusId || StatusIds.TEMPORARY, Validators.required),
      'pubkey': new FormControl(data && data.pubkey || null, Validators.compose([Validators.required, SharedValidators.pubkey]))
    };

    // Add additional fields
    this.accountService.additionalAccountFields.forEach(field => {
      //console.debug("[register-form] Add additional field {" + field.name + "} to form", field);
      formDef[field.name] = new FormControl(data && data[field.name] || null, this.getValidators(field));
    });

    return new FormGroup(formDef);
  }

  public getValidators(field: AccountFieldDef): ValidatorFn | ValidatorFn[] {
    let validatorFns: ValidatorFn[] = [];
    if (field.required) {
      validatorFns.push(Validators.required);
    }
    if (!!field.dataService) {
      validatorFns.push(SharedValidators.entity);
    }

    return validatorFns.length ? Validators.compose(validatorFns) : validatorFns.length == 1 ? validatorFns[0] : undefined;
  }
}
