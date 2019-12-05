import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormGroup, Validators, FormBuilder} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {OperationGroup} from "./model/trip.model";

@Injectable()
export class OperationGroupValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder,
    protected settings: LocalSettingsService) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: OperationGroup): FormGroup {

    return this.formBuilder.group({
        __typename: ['OperationVO'],
        id: [''],
        updateDate: [''],
        rankOrderOnPeriod: [''],
        comments: ['', Validators.maxLength(2000)],
        metier: ['', Validators.compose([Validators.required, SharedValidators.entity])],
        physicalGear: ['', Validators.compose([Validators.required, SharedValidators.entity])],
        targetSpecies: ['', Validators.compose([Validators.required, SharedValidators.entity])]
      });
  }

}
