import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { PhysicalGear } from "./trip.model";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class PhysicalGearValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: PhysicalGear): FormGroup {
    return this.formBuilder.group({
      __typename: ['PhysicalGearVO'],
      id: [''],
      updateDate: [''],
      rankOrder: ['', Validators.compose([Validators.required, SharedValidators.integer, Validators.min(1)])],
      creationDate: [''],
      gear: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      measurementValues: this.formBuilder.group({}),
      comments: ['', Validators.maxLength(2000)]
    });
  }
}
