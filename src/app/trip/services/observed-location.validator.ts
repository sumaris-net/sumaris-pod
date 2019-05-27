import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";

@Injectable()
export class ObservedLocationValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  getRowValidator(options?: {}): FormGroup {
    return this.getFormGroup(null);
  }

  getFormGroup(data?: any): FormGroup {

    return this.formBuilder.group({
      'id': [''],
      'program': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'updateDate': [''],
      'creationDate': [''],
      'location': ['', Validators.required],
      'startDateTime': ['', Validators.required],
      'endDateTime': [''],
      'recorderPerson': ['', Validators.required],
      'comments': [''],
      'measurementValues': this.formBuilder.group({})
    }, {
      validator: Validators.compose([SharedValidators.dateIsAfter('startDateTime', 'endDateTime') ])
    });
  }

}

