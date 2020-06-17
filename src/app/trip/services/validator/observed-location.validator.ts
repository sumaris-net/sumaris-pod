import {Injectable} from "@angular/core";
import {FormBuilder, Validators} from "@angular/forms";
import {SharedFormGroupValidators, SharedValidators} from "../../../shared/validator/validators";
import {ObservedLocation} from "../model/observed-location.model";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {DataRootEntityValidatorService} from "../../../data/services/validator/root-data-entity.validator";

@Injectable()
export class ObservedLocationValidatorService extends DataRootEntityValidatorService<ObservedLocation>{

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService) {
    super(formBuilder, settings);
  }

  getFormGroupConfig(data?: ObservedLocation): { [key: string]: any } {
    return Object.assign(
      super.getFormGroupConfig(data),
      {
        __typename: [ObservedLocation.TYPENAME],
        location: [null, Validators.compose([Validators.required, SharedValidators.entity])],
        startDateTime: [null, Validators.required],
        endDateTime: [null],
        measurementValues: this.formBuilder.group({}),
        observers: this.getObserversFormArray(data),
        recorderDepartment: [null, SharedValidators.entity],
        recorderPerson: [null, SharedValidators.entity]
      });
  }

  getFormGroupOptions(data?: any): { [key: string]: any } {
    return {
      validators: [SharedFormGroupValidators.dateRange('startDateTime', 'endDateTime')]
    };
  }
}

