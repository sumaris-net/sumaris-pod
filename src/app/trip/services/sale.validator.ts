import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { Sale } from "./trip.model";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class SaleValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Sale): FormGroup {
    return this.formBuilder.group({
      'id': [''],
      'updateDate': [''],
      'creationDate': [''],
      'vesselFeatures': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'startDateTime': ['', Validators.required],
      'endDateTime': [''],
      'saleLocation': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'saleType': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'comments': ['', Validators.maxLength(2000)]
    });
  }
}
