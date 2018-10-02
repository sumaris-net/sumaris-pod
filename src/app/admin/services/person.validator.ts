import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormControl, FormGroup, Validators } from "@angular/forms";
import { Person } from "./model";
import { AccountFieldDef, AccountService } from "../../core/core.module";
import { getMainProfile, StatusIds } from "../../core/services/model";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class PersonValidatorService implements ValidatorService {

  constructor(
    private accountService: AccountService
  ) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Person): FormGroup {
    const formDef = {
      'id': new FormControl(),
      'lastName': new FormControl(data && data.lastName || null, Validators.required),
      'firstName': new FormControl(data && data.firstName || null, Validators.required),
      'email': new FormControl(data && data.email || null, Validators.compose([Validators.required, Validators.email])),
      'mainProfile': new FormControl(data && (data.mainProfile || getMainProfile(data.profiles)) || 'GUEST', Validators.required),
      'statusId': new FormControl(data && data.statusId || StatusIds.TEMPORARY, Validators.required),
      'pubkey': new FormControl(data && data.pubkey || null, Validators.compose([Validators.required, SharedValidators.pubkey]))
    };

    // Add additional fields
    this.accountService.additionalAccountFields.forEach(field => {
      //console.debug("[register-form] Add additional field {" + field.name + "} to form", field);
      formDef[field.name] = new FormControl(null, this.accountService.getValidators(field));
    });

    return new FormGroup(formDef);
  }


}
