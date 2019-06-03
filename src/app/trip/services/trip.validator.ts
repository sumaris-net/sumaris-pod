import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormGroup, Validators, FormBuilder} from "@angular/forms";
import {Trip} from "./trip.model";
import {SharedValidators} from "../../shared/validator/validators";
import {AccountService} from "../../core/services/account.service";

@Injectable()
export class TripValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder,
    private accountService: AccountService
  ) {

  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Trip): FormGroup {
    const isOnFieldMode = this.accountService.isUsageMode('FIELD');

    return this.formBuilder.group({
      id: [''],
      program: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      updateDate: [''],
      creationDate: [''],
      vesselFeatures: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      departureDateTime: ['', Validators.required],
      departureLocation: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      returnDateTime: ['', isOnFieldMode ? undefined : Validators.required],
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


