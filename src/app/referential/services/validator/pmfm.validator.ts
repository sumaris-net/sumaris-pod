import {FormBuilder, Validators} from "@angular/forms";
import {isNotNil, toNumber} from "../../../shared/functions";
import {SharedFormGroupValidators, SharedValidators} from "../../../shared/validator/validators";
import {Injectable} from "@angular/core";
import {ReferentialValidatorService} from "./referential.validator";
import {Pmfm} from "../model/pmfm.model";


@Injectable()
export class PmfmValidatorService extends ReferentialValidatorService<Pmfm> {

  constructor(
    protected formBuilder: FormBuilder
  ) {
    super(formBuilder);
  }

  getFormGroupConfig(data?: Pmfm, opts?: { withDescription?: boolean; withComments?: boolean }): { [p: string]: any } {
    const config = super.getFormGroupConfig(data, opts);
    return {
      ...config,
      minValue: [toNumber(data && data.minValue, null), SharedValidators.double()],
      maxValue: [toNumber(data && data.maxValue, null), SharedValidators.double()],
      defaultValue: [isNotNil(data && data.defaultValue) ? data.defaultValue : null],
      maximumNumberDecimals: [toNumber(data && data.maximumNumberDecimals, null), SharedValidators.integer],
      signifFiguresNumber: [toNumber(data && data.signifFiguresNumber, null), SharedValidators.integer],
      parameter: [data && data.parameter || null, Validators.compose([Validators.required, SharedValidators.entity])],
      matrix: [data && data.matrix || null, SharedValidators.entity],
      fraction: [data && data.fraction || null, SharedValidators.entity],
      method: [data && data.method || null, SharedValidators.entity],
      unit: [data && data.unit || null, Validators.compose([Validators.required, SharedValidators.entity])]
    } ;
  }

  getFormGroupOptions(data?: Pmfm, opts?: any): { [key: string]: any } {
    return {
      /*validator: Validators.compose([
        SharedFormGroupValidators.requiredIf('fraction', 'matrix')
      ])*/
    };
  }
}
