import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {toNumber} from "../../../shared/functions";
import {PhysicalGear} from "../model/trip.model";

@Injectable()
export class PhysicalGearValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: PhysicalGear): FormGroup {
    return this.formBuilder.group({
      __typename: [PhysicalGear.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      rankOrder: [toNumber(data && data.rankOrder, null), Validators.compose([Validators.required, SharedValidators.integer, Validators.min(1)])],
      creationDate: [data && data.creationDate || null],
      gear: [data && data.gear || null, Validators.compose([Validators.required, SharedValidators.entity])],
      measurementValues: this.formBuilder.group({}),
      comments: [data && data.comments || null, Validators.maxLength(2000)]
    });
  }
}
