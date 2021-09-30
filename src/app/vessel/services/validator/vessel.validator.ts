import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Vessel} from "../model/vessel.model";
import {SharedValidators} from "@sumaris-net/ngx-components";
import {VesselFeaturesValidatorService} from "./vessel-features.validator";
import {VesselRegistrationValidatorService} from "./vessel-registration.validator";
import {toNumber} from "@sumaris-net/ngx-components";

@Injectable({providedIn: 'root'})
export class VesselValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder,
    private vesselFeaturesValidator: VesselFeaturesValidatorService,
    private vesselRegistrationPeriodValidator: VesselRegistrationValidatorService) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Vessel, opts?: any): FormGroup {
    return this.formBuilder.group({
      __typename: [Vessel.TYPENAME],
      id: [toNumber(data?.id, null)],
      updateDate: [ data?.updateDate || null],
      creationDate: [ data?.creationDate || null],
      vesselFeatures: this.vesselFeaturesValidator.getFormGroup(data?.vesselFeatures),
      vesselRegistrationPeriod: this.vesselRegistrationPeriodValidator.getFormGroup(data?.vesselRegistrationPeriod, {required: true}),
      statusId: [toNumber(data?.statusId, null), Validators.required],
      vesselType: [data?.vesselType || null, Validators.compose([Validators.required, SharedValidators.entity])],
    });
  }
}
