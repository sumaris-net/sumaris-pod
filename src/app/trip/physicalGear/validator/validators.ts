import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { PhysicalGear } from "../../services/model";

@Injectable()
export class PhysicalGearValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: PhysicalGear): FormGroup {
    return this.formBuilder.group({
      'id': [''],
      'updateDate': [''],
      'rankOrder': ['', Validators.required],
      'creationDate': [''],
      'gear': ['', Validators.required],
      'comments': ['', Validators.maxLength(2000)]
    });
  }
}
