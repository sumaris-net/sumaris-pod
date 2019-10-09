import {Injectable} from "@angular/core";
import {FormBuilder, FormControl, Validators} from "@angular/forms";
import {Landing} from "./trip.model";
import {SharedValidators} from "../../shared/validator/validators";
import {DataRootVesselEntityValidatorService} from "./validator/base.validator";

@Injectable()
export class LandingValidatorService extends DataRootVesselEntityValidatorService<Landing> {

  constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormConfig(data?: Landing): { [p: string]: any } {

    return Object.assign(super.getFormConfig(data), {
      __typename: ['LandingVO'],
      location: ['', SharedValidators.entity],
      dateTime: [''],
      measurementValues: this.formBuilder.group({}),
      observers: this.getObserversArray(data),
      observedLocationId: [''],
      tripId: ['']
    });
  }

  getObserverControl(observer?: any): FormControl {
    return this.formBuilder.control(observer || '', [Validators.required, SharedValidators.entity]);
  }
}
