import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormGroup, Validators, FormBuilder} from "@angular/forms";
import {VesselRegistration} from "./model";
import {SharedValidators} from "../../shared/validator/validators";

@Injectable()
export class VesselRegistrationValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: VesselRegistration, opts?: { required: boolean }): FormGroup {
    return this.formBuilder.group({
      __typename: ['VesselRegistrationVO'],
      id: [null],
      startDate: [null, opts && opts.required ? Validators.required : null],
      endDate: [null],
      registrationCode: ['', opts && opts.required ? Validators.required : null],
      intRegistrationCode: [''],
      registrationLocation: ['', opts && opts.required ? Validators.compose([Validators.required, SharedValidators.entity]) : SharedValidators.entity]
    });
  }
}
