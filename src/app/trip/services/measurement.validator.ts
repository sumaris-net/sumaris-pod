import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AbstractControl, FormBuilder, FormGroup, ValidatorFn, Validators} from "@angular/forms";
import {PmfmStrategy} from "./trip.model";
import {SharedValidators} from "../../shared/validator/validators";

import {isNil, isNotNil} from '../../shared/shared.module';

const REGEXP_INTEGER = /^[0-9]+$/;
const REGEXP_DOUBLE = /^[0-9]+(\.[0-9]+)?$/;

@Injectable()
export class MeasurementsValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  public getRowValidator(options?: any): FormGroup {
    return this.getFormGroup(options && options.pmfms || []);
  }

  public getFormGroup(pmfms: PmfmStrategy[]): FormGroup {
    const config = this.getFormGroupConfig(pmfms);
    return this.formBuilder.group(config);
  }

  public getFormGroupConfig(pmfms: PmfmStrategy[]): { [key: string]: any } {
    return pmfms.reduce((res, pmfm) => {
      const validator = this.getValidator(pmfm);
      if (validator) {
        res[pmfm.pmfmId] = ['', validator];
      }
      else {
        res[pmfm.pmfmId] = [''];
      }
      return res;
    }, {});
  }

  public updateFormGroup(form: FormGroup, pmfms: PmfmStrategy[], options?: {
    protectedAttributes?: string[];
  }) {
    options = options || { protectedAttributes: ['id', 'rankOrder', 'comments', 'updateDate'] };
    let controlNamesToRemove: string[] = [];
    for (let controlName in form.controls) {
      controlNamesToRemove.push(controlName);
    }
    pmfms.forEach(pmfm => {
      const controlName = pmfm.pmfmId.toString();
      let formControl: AbstractControl = form.get(controlName);
      // If new pmfm: add as control
      if (!formControl) {

        formControl = this.formBuilder.control(pmfm.defaultValue || '', this.getValidator(pmfm));
        form.addControl(controlName, formControl);
      }

      // Remove from the remove list
      let index = controlNamesToRemove.indexOf(controlName);
      if (index >= 0) controlNamesToRemove.splice(index, 1);

    });

    // Remove unused controls
    controlNamesToRemove
      .filter(controlName => !options.protectedAttributes || !options.protectedAttributes.includes(controlName)) // Keep protected columns
      .forEach(controlName => form.removeControl(controlName));
  }

  public getValidator(pmfm: PmfmStrategy): ValidatorFn {
    const validatorFns: ValidatorFn[] = [];
    if (pmfm.isMandatory) {
      validatorFns.push(Validators.required);
    }
    if (pmfm.type === 'string') {
      validatorFns.push(Validators.maxLength(40));
    }
    else if (pmfm.type === 'integer' || pmfm.type === 'double') {

      if (isNotNil(pmfm.minValue)) {
        validatorFns.push(Validators.min(pmfm.minValue));
      }
      if (isNotNil(pmfm.maxValue)) {
        validatorFns.push(Validators.max(pmfm.maxValue));
      }

      // Pattern validation
      let pattern: RegExp;
      // Integer or double with 0 decimals
      if (pmfm.type === 'integer' || pmfm.maximumNumberDecimals === 0) {
        validatorFns.push(Validators.pattern(REGEXP_INTEGER));
      }
      // Double without maximal decimals
      else if (pmfm.type === 'double' && isNil(pmfm.maximumNumberDecimals)) {
        validatorFns.push(Validators.pattern(REGEXP_DOUBLE));
      }
      // Double with a N decimal
      else if (pmfm.maximumNumberDecimals >= 1) {
        validatorFns.push(SharedValidators.double({maxDecimals: pmfm.maximumNumberDecimals}));
      }
    }
    else if (pmfm.type === 'qualitative_value') {
      validatorFns.push(SharedValidators.entity);
    }

    return validatorFns.length > 1 ? Validators.compose(validatorFns) : (validatorFns.length === 1 ? validatorFns[0] : undefined);
  }
}
