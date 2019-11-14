import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { VesselFeatures } from "./model";
import { SharedValidators } from "../../shared/validator/validators";

const decimalPattern = new RegExp('^[0-9]+(\.[0-9]{1,2})?$');

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
      'endDate': [null],
      'name': ['', Validators.required],
      'exteriorMarking': ['', Validators.required],
      'registrationCode': ['', Validators.required],
      'registrationStartDate': [null, Validators.required],
      'registrationEndDate': [null],
      'administrativePower': ['', Validators.compose([Validators.min(0), SharedValidators.integer])],
      'lengthOverAll': ['', Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
      'grossTonnageGrt': ['', Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
      'grossTonnageGt': ['', Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
      'basePortLocation': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'registrationLocation': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'comments': ['', Validators.maxLength(2000)],
      'vesselStatusId': [null, Validators.required],
      'vesselType': ['', Validators.compose([Validators.required, SharedValidators.entity])],
    });
  }
}
