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
        __typename: [ObservedLocation.TYPENAME],
        location: [null, Validators.compose([Validators.required, SharedValidators.entity])],
        startDateTime: [null, Validators.required],
        endDateTime: [null],
        measurementValues: this.formBuilder.group({}),
        observers: this.getObserversArray(data),
        recorderDepartment: [null, SharedValidators.entity],
        recorderPerson: [null, SharedValidators.entity]
      });
  }

  getFormOptions(data?: any): { [key: string]: any } {
    return {
      validators: [SharedValidators.dateRange('startDateTime', 'endDateTime')]
    };
  }
}

