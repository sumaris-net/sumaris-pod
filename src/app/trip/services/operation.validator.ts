import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormGroup, Validators, FormBuilder} from "@angular/forms";
import {Operation} from "./trip.model";
import {PositionValidatorService} from "./position.validator";
import {SharedValidators} from "../../shared/validator/validators";
import {LocalSettingsService} from "../../core/services/local-settings.service";

@Injectable()
export class OperationValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder,
    private positionValidator: PositionValidatorService,
    protected settings: LocalSettingsService) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Operation): FormGroup {
    const isOnFieldMode = this.settings.isUsageMode('FIELD');

    return this.formBuilder.group({
        __typename: ['OperationVO'],
        id: [''],
        updateDate: [''],
        rankOrderOnPeriod: [''],
        startDateTime: ['', Validators.required],
        endDateTime: [''],
        comments: ['', Validators.maxLength(2000)],
        startPosition: this.positionValidator.getFormGroup(null, {required: true}),
        endPosition: this.positionValidator.getFormGroup(null, {required: !isOnFieldMode}),
        metier: ['', Validators.compose([Validators.required, SharedValidators.entity])],
        physicalGear: ['', Validators.compose([Validators.required, SharedValidators.entity])]
      },
      {
        validator: Validators.compose([
          SharedValidators.dateIsAfter('startDateTime', 'endDateTime'),
          SharedValidators.dateMaxDuration('startDateTime', 'endDateTime', 100, 'days')
        ])
      });
  }

}
