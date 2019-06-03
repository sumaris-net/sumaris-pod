import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";
import {ObservedLocation} from "./model/observed-location.model";
import {DataRootEntityValidatorService} from "./validator/base.validator";

@Injectable()
export class ObservedLocationValidatorService extends DataRootEntityValidatorService<ObservedLocation>{

  constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormConfig(data?: ObservedLocation): { [key: string]: any } {
    return Object.assign(
      super.getFormConfig(data),
      {
        location: ['', Validators.compose([Validators.required, SharedValidators.entity])],
        startDateTime: ['', Validators.required],
        endDateTime: [''],
        measurementValues: this.formBuilder.group({}),
        observers: this.getObserversArray(data)
      });
  }

  getFormOptions(data?: any): { [key: string]: any } {
    return {
      validators: [SharedValidators.dateIsAfter('startDateTime', 'endDateTime')]
    };
  }
}

