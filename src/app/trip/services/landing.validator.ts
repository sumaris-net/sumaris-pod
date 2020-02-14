import {Injectable} from "@angular/core";
import {FormBuilder, FormControl, Validators} from "@angular/forms";
import {Landing} from "./trip.model";
import {SharedValidators} from "../../shared/validator/validators";
import {DataRootVesselEntityValidatorService} from "./validator/base.validator";
import {toNumber} from "../../shared/functions";

@Injectable()
export class LandingValidatorService extends DataRootVesselEntityValidatorService<Landing> {

  constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormGroupConfig(data?: Landing): { [p: string]: any } {

    return Object.assign(super.getFormGroupConfig(data), {
      __typename: [Landing.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      location: [data && data.location || null, SharedValidators.entity],
      dateTime: [data && data.dateTime || null],
      rankOrder: [toNumber(data && data.rankOrder, null), Validators.compose([SharedValidators.integer, Validators.min(1)])],
      measurementValues: this.formBuilder.group({}),
      observers: this.getObserversFormArray(data),
      observedLocationId: [toNumber(data && data.observedLocationId, null)],
      tripId: [toNumber(data && data.tripId, null)],
      comments: [data && data.comments || null]
    });
  }

  getObserverControl(observer?: any): FormControl {
    return this.formBuilder.control(observer || '', [Validators.required, SharedValidators.entity]);
  }
}
