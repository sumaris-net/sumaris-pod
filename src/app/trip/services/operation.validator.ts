import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { Operation } from "./trip.model";
import { PositionValidatorService } from "./position.validator";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class OperationValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder,
    private positionValidator: PositionValidatorService) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Operation): FormGroup {
    return this.formBuilder.group({
      id: [''],
      updateDate: [''],
      rankOrderOnPeriod: [''],
      startDateTime: ['', Validators.required],
      endDateTime: [''],
      comments: ['', Validators.maxLength(2000)],
      startPosition: this.positionValidator.getFormGroup(),
      endPosition: this.positionValidator.getFormGroup(),
      metier: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      physicalGear: ['', Validators.compose([Validators.required, SharedValidators.entity])]
      },
    {
      validator: Validators.compose([SharedValidators.dateIsAfter('startDateTime', 'endDateTime') ])
      });
    }

}
