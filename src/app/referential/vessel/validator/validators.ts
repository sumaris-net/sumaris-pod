import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { VesselFeatures } from "../../services/model";

@Injectable()
export class VesselValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: VesselFeatures): FormGroup {
    return this.formBuilder.group({
      'id': [null],
      'vesselId': [null],
      'updateDate': [null],
      'creationDate': [null],
      'startDate': [null, Validators.required],
      'name': ['', Validators.required],
      'exteriorMarking': ['', Validators.required],
      'basePortLocation': ['', Validators.required],
      'comments': ['', Validators.maxLength(2000)]
    });
  }
}
