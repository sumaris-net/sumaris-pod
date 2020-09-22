import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Vessel} from "../model/vessel.model";
import {SharedValidators} from "../../../shared/validator/validators";
import {VesselFeaturesValidatorService} from "./vessel-features.validator";
import {VesselRegistrationValidatorService} from "./vessel-registration.validator";

@Injectable({providedIn: 'root'})
export class VesselValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder,
    private featuresValidator: VesselFeaturesValidatorService,
    private registrationValidator: VesselRegistrationValidatorService) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Vessel): FormGroup {
    return this.formBuilder.group({
      __typename: ['VesselVO'],
      id: [null],
      updateDate: [null],
      creationDate: [null],
      features: this.featuresValidator.getFormGroup(null),
      registration: this.registrationValidator.getFormGroup(null, {required: true}), // TODO add config option ?
      statusId: [null, Validators.required],
      vesselType: ['', Validators.compose([Validators.required, SharedValidators.entity])],
    });
  }
}
