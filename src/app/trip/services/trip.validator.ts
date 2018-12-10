import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { Trip } from "./trip.model";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class TripValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Trip): FormGroup {
    return this.formBuilder.group({
      'id': [''],
      'program': ['', Validators.required],
      'updateDate': [''],
      'creationDate': [''],
      'vesselFeatures': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'departureDateTime': ['', Validators.required],
      'departureLocation': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'returnDateTime': [''],
      'returnLocation': ['', SharedValidators.entity],
      'comments': ['', Validators.maxLength(2000)]
    }, {
      validator: Validators.compose([SharedValidators.dateIsAfter('departureDateTime', 'returnDateTime') ])
    });
  }
}
