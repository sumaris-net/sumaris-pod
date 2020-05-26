import {MeasurementsValidatorOptions, MeasurementsValidatorService} from "../measurement.validator";
import {Injectable} from "@angular/core";
import {FormGroup, Validators} from "@angular/forms";
import {Measurement} from "../model/measurement.model";
import {SharedValidators} from "../../../shared/validator/validators";

@Injectable()
export class ExpenseValidatorService extends MeasurementsValidatorService {

  getFormGroupConfig(data: Measurement[], opts?: MeasurementsValidatorOptions): { [p: string]: any } {
    return Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        totalCalculated: [null],
        iceAmount: [null, Validators.compose([SharedValidators.double({maxDecimals: 2}), Validators.min(0)])],
        iceType: [null, SharedValidators.entity]
      }
    );
  }

  getFormGroupOptions(data?: Measurement[], opts?: MeasurementsValidatorOptions): { [p: string]: any } {
    return Object.assign(
      super.getFormGroupOptions(data, opts),
      {
        validators: [
          SharedValidators.requiredIf('iceType', 'iceAmount'),
          SharedValidators.requiredIf('iceAmount', 'iceType'),
        ]
      }
    );
  }

  updateFormGroup(form: FormGroup, opts?: MeasurementsValidatorOptions) {
    super.updateFormGroup(form, opts);


  }

  protected fillDefaultOptions(opts?: MeasurementsValidatorOptions): MeasurementsValidatorOptions {
    opts = super.fillDefaultOptions(opts);

    // add expense fields as protected attributes
    opts.protectedAttributes.push('iceAmount', 'iceType');

    return opts;
  }
}
