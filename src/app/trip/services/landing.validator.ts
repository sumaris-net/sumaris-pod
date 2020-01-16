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

  getFormGroupConfig(data?: Landing): { [p: string]: any } {

    return Object.assign(super.getFormGroupConfig(data), {
      __typename: ['LandingVO'],
      id: [null],
      updateDate: [null],
      location: [null, SharedValidators.entity],
      dateTime: [null],
      rankOrder: [null, Validators.compose([SharedValidators.integer, Validators.min(1)])],
      measurementValues: this.formBuilder.group({}),
      observers: this.getObserversFormArray(data),
      observedLocationId: [null],
      comments: [null],
      tripId: [null]
    });
  }

  getObserverControl(observer?: any): FormControl {
    return this.formBuilder.control(observer || '', [Validators.required, SharedValidators.entity]);
  }
}
