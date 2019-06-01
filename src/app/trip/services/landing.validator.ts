import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {Sale} from "./trip.model";
import {SharedValidators} from "../../shared/validator/validators";

@Injectable()
export class LandingValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Sale): FormGroup {

    return this.formBuilder.group({
      id: [''],
      program: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      updateDate: [''],
      creationDate: [''],
      landingLocation: ['', SharedValidators.entity],
      landingDateTime: [''],
      vesselFeatures: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      recorderPerson: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      comments: ['', Validators.maxLength(2000)],
      measurementValues: this.formBuilder.group({}),
      observers: this.formBuilder.array(
        (data && data.observers || []).map(this.getObserverControl),
        SharedValidators.requiredArrayMinLength(1)
      )
    });
  }

  getObserverControl(observer?: any): FormControl {
    return this.formBuilder.control(observer || '', [Validators.required, SharedValidators.entity]);
  }
}
