import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {VesselRegistrationPeriod} from "../model/vessel.model";
import {SharedValidators, toNumber} from '@sumaris-net/ngx-components';

@Injectable({providedIn: 'root'})
export class VesselRegistrationValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: VesselRegistrationPeriod, opts?: { required: boolean }): FormGroup {
    return this.formBuilder.group({
      __typename: [VesselRegistrationPeriod.TYPENAME],
      id: [toNumber(data && data.id, null)],
      startDate: [data?.startDate || null, opts && opts.required ? Validators.required : null],
      endDate: [data?.endDate || null],
      registrationCode: [data?.registrationCode || null, opts && opts.required ? Validators.required : null],
      intRegistrationCode: [data?.intRegistrationCode || null],
      registrationLocation: [data?.registrationLocation || null, opts && opts.required ? Validators.compose([Validators.required, SharedValidators.entity]) : SharedValidators.entity]
    });
  }
}
