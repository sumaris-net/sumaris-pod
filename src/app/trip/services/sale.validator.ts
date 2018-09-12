import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { Sale } from "./trip.model";

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
      'vesselFeatures': ['', Validators.required],
      'startDateTime': ['', Validators.required],
      'endDateTime': [''],
      'saleLocation': ['', Validators.required],
      'saleType': ['', Validators.required],
      'comments': ['', Validators.maxLength(2000)]
    });
  }
}
