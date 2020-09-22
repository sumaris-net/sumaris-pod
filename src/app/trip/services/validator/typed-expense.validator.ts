import {MeasurementsValidatorOptions, MeasurementsValidatorService} from "./measurement.validator";
import {Injectable} from "@angular/core";
import {FormGroup, ValidatorFn, Validators} from "@angular/forms";
import {Measurement} from "../model/measurement.model";
import {SharedFormGroupValidators, SharedValidators} from "../../../shared/validator/validators";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";

interface TypedExpenseValidatorOptions extends MeasurementsValidatorOptions {
  typePmfm?: PmfmStrategy;
  totalPmfm?: PmfmStrategy;
}

@Injectable({providedIn: 'root'})
export class TypedExpenseValidatorService extends MeasurementsValidatorService<Measurement, TypedExpenseValidatorOptions> {

  getFormGroupConfig(data: Measurement[], opts?: TypedExpenseValidatorOptions): { [p: string]: any } {
    return Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        amount: [null, Validators.compose([SharedValidators.double({maxDecimals: 2}), Validators.min(0)])],
        packaging: [null, SharedValidators.entity]
      }
    );
  }

  getFormGroupOptions(data?: Measurement[], opts?: TypedExpenseValidatorOptions): { [p: string]: any } {
    return Object.assign(
      super.getFormGroupOptions(data, opts),
      {
        validators: this.getDefaultValidators()
      }
    );
  }

  getDefaultValidators(): ValidatorFn[] {
    return [
      SharedFormGroupValidators.requiredIf('packaging', 'amount'),
      SharedFormGroupValidators.requiredIf('amount', 'packaging'),
    ];
  }

  updateFormGroup(form: FormGroup, opts?: TypedExpenseValidatorOptions) {
    super.updateFormGroup(form, opts);

    // add formGroup validator for type requirement
    const additionalValidators: ValidatorFn[] = [];
    if (opts.typePmfm) {
      additionalValidators.push(SharedFormGroupValidators.requiredIf(opts.typePmfm.pmfmId.toString(), 'amount'));
      if (opts.totalPmfm) {
        additionalValidators.push(SharedFormGroupValidators.requiredIf(opts.typePmfm.pmfmId.toString(), opts.totalPmfm.pmfmId.toString()));
      }
    }
    if (additionalValidators.length) {
      form.setValidators(Validators.compose(this.getDefaultValidators().concat(additionalValidators)));
    }
  }

  protected fillDefaultOptions(opts?: TypedExpenseValidatorOptions): TypedExpenseValidatorOptions {
    opts = super.fillDefaultOptions(opts);

    // add expense fields as protected attributes
    opts.protectedAttributes.push('amount', 'packaging');

    return opts;
  }
}
