import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Trip} from "./trip.model";
import {SharedValidators} from "../../shared/validator/validators";
import {LocalSettingsService} from "../../core/services/local-settings.service";

@Injectable()
export class TripValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder,
    private settings: LocalSettingsService
  ) {

  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Trip): FormGroup {
    const isOnFieldMode = this.settings.isUsageMode('FIELD');

    return this.formBuilder.group({
      id: [''],
      program: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      updateDate: [''],
      creationDate: [''],
      vesselFeatures: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      departureDateTime: ['', Validators.required],
      departureLocation: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      returnDateTime: ['', isOnFieldMode ? null : Validators.required],
      returnLocation: ['', isOnFieldMode ? SharedValidators.entity : Validators.compose([Validators.required, SharedValidators.entity])],
      comments: ['', Validators.maxLength(2000)]
    }, {
      validator: Validators.compose([
        SharedValidators.dateIsAfter('departureDateTime', 'returnDateTime'),
        SharedValidators.dateMinDuration('departureDateTime', 'returnDateTime', 1, 'hours'),
        SharedValidators.dateMaxDuration('departureDateTime', 'returnDateTime', 100, 'days')
      ])
    });
  }
}


