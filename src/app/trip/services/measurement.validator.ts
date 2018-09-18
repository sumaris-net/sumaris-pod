import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder, AbstractControl, ValidatorFn } from "@angular/forms";
import { PmfmStrategy } from "./trip.model";
import { SharedValidators } from "../../shared/validator/validators";

@Injectable()
export class MeasurementsValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  public getRowValidator(options?: any): FormGroup {
    return this.getFormGroup(options && options.pmfms || []);
  }

  public getFormGroup(pmfms: PmfmStrategy[]): FormGroup {
    const controlConfig: any = {};
    pmfms.forEach(pmfm => {
      controlConfig[pmfm.id] = this.getValidators(pmfm);
    });

    return this.formBuilder.group(controlConfig);
  }

  public updateFormGroup(form: FormGroup, pmfms: PmfmStrategy[], options?: {
    protectedColumns?: string[]
  }) {
    options = options || { protectedColumns: ['id', 'rankOrder', 'comments'] };
    let controlNamesToRemove: string[] = [];
    for (let controlName in form.controls) {
      controlNamesToRemove.push(controlName);
    }
    const controlConfig: any = {};
    pmfms.forEach(pmfm => {
      const controlName = pmfm.id.toString();
      let formControl: AbstractControl = form.get(controlName);
      // If new pmfm: add as control
      if (!formControl) {

        formControl = this.formBuilder.control('', this.getValidators(pmfm));
        //console.log(formControl);
        form.addControl(controlName, formControl);
      }

      // Remove from the remove list
      let index = controlNamesToRemove.indexOf(controlName);
      if (index >= 0) controlNamesToRemove.splice(index, 1);

    });

    // Remove unused controls
    controlNamesToRemove
      .filter(controlName => !options.protectedColumns || !options.protectedColumns.includes(controlName)) // Keep protected columns
      .forEach(controlName => form.removeControl(controlName));
  }

  public getValidators(pmfm: PmfmStrategy): ValidatorFn | ValidatorFn[] {
    let validatorFns: ValidatorFn[] = [];
    if (pmfm.isMandatory) {
      validatorFns.push(Validators.required);
    }
    if (pmfm.type === 'string') {
      validatorFns.push(Validators.maxLength(40));
    }
    else if (pmfm.type === 'integer' || pmfm.type === 'double') {

      if (pmfm.minValue != undefined) {
        validatorFns.push(Validators.min(pmfm.minValue));
      }
      if (pmfm.maxValue != undefined) {
        validatorFns.push(Validators.max(pmfm.maxValue));
      }

      // Pattern validation
      let pattern;
      if (pmfm.type === 'integer' || pmfm.maximumNumberDecimals !== 0) {
        pattern = '^[0-9]+$';
      }
      else if (pmfm.maximumNumberDecimals > 0) {
        pattern = '^[0-9]+([.,][0-9]{0,' + pmfm.maximumNumberDecimals + '})?$';
      }
      validatorFns.push(Validators.pattern(pattern));
    }
    else if (pmfm.type === 'qualitative_value') {
      validatorFns.push(SharedValidators.entity);
    }

    return validatorFns.length ? Validators.compose(validatorFns) : validatorFns.length == 1 ? validatorFns[0] : undefined;
  }
}
