import { Injectable } from '@angular/core';
import { ValidatorService } from '@e-is/ngx-material-table';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Vessel } from '../model/vessel.model';
import { SharedValidators, toNumber } from '@sumaris-net/ngx-components';
import { VesselFeaturesValidatorService } from './vessel-features.validator';
import { VesselRegistrationValidatorService } from './vessel-registration.validator';

@Injectable({ providedIn: 'root' })
export class VesselValidatorService implements ValidatorService {
  constructor(
    private formBuilder: FormBuilder,
    private featuresValidator: VesselFeaturesValidatorService,
    private registrationValidator: VesselRegistrationValidatorService
  ) {}

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Vessel, opts?: any): FormGroup {
    return this.formBuilder.group({
      __typename: ['VesselVO'],
      id: [toNumber(data && data.id, null)],
      updateDate: [(data && data.updateDate) || null],
      creationDate: [(data && data.creationDate) || null],
      features: this.featuresValidator.getFormGroup(data && data.features),
      registration: this.registrationValidator.getFormGroup(data && data.registration, { required: true }),
      statusId: [null, Validators.required],
      vesselType: ['', Validators.compose([Validators.required, SharedValidators.entity])],
    });
  }
}
