import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
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
      id: [''],
      program: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      updateDate: [''],
      creationDate: [''],
      location: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      startDateTime: ['', Validators.required],
      endDateTime: [''],
      recorderPerson: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      comments: [''],
      measurementValues: this.formBuilder.group({}),
      observers: this.formBuilder.array(
        (data && data.observers || []).map(this.getObserverControl),
        SharedValidators.requiredArrayMinLength(1)
      )
    }, {
      validator: Validators.compose([SharedValidators.dateIsAfter('startDateTime', 'endDateTime') ])
    });
  }

  getObserverControl(observer?: any): FormControl {
    return this.formBuilder.control(observer || '', [Validators.required, SharedValidators.entity]);
  }
}

