import {Injectable} from "@angular/core";
import {
  DataEntityValidatorOptions,
  DataEntityValidatorService
} from "../../../data/services/validator/data-entity.validator";

import {SharedFormArrayValidators} from "../../../shared/validator/validators";


import {FormBuilder, FormGroup, ValidatorFn, Validators} from "@angular/forms";
import {toBoolean} from "../../../shared/functions";
import {SharedFormGroupValidators, SharedValidators} from "../../../shared/validator/validators";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import { Planification } from '../model/planification.model';

export interface PlanificationValidatorOptions extends DataEntityValidatorOptions {
  required?: boolean
}

@Injectable()
export class PlanificationValidatorService<O extends PlanificationValidatorOptions = PlanificationValidatorOptions>
  extends DataEntityValidatorService<Planification, PlanificationValidatorOptions> {

  constructor(
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService) {
    super(formBuilder, settings);
  }

  getFormGroupConfig(data?: Planification, opts?: PlanificationValidatorOptions): { [p: string]: any } {
    // TODO : implémenter le contrôle ici
    const formConfig = Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        __typename: [Planification.TYPENAME],
        comment: [data && data.comment || null,Validators.nullValidator],
        year: [data && data.year || null, Validators.required],
        sampleRowCode: [data && data.sampleRowCode || null, Validators.required],
        taxonName: [data && data.taxonName || null, Validators.compose([Validators.required, SharedValidators.entity])],

        // TODO : update correct types--------------------------------------------------------------------------------------
        eotp: [data && data.eotp || null, Validators.compose([Validators.nullValidator, SharedValidators.entity])],
        laboratory: [data && data.laboratory || null, Validators.compose([Validators.required, SharedValidators.entity])],
        fishingArea: [data && data.fishingArea || null, Validators.compose([Validators.required, SharedValidators.entity])],
        landingArea: [data && data.landingArea || null, Validators.compose([Validators.required, SharedValidators.entity])],
        calcifiedType: [data && data.calcifiedType || null, Validators.compose([Validators.required, SharedValidators.entity])],
        //------------------------------------------------------------------------------------------------------------------

        sex: [data && data.sex || null,Validators.nullValidator],
        age: [data && data.age || null,Validators.nullValidator],
      });


    return formConfig;

  }


  getFormGroupOptions(data?: Planification, opts?: O): { [key: string]: any } {
    /*return {
      validator: Validators.compose([
        SharedFormGroupValidators.dateMinDuration('startDate', 'startDate', 1, 'hours'),
        SharedFormGroupValidators.dateMaxDuration('endDate', 'endDate', 100, 'days'),
        SharedFormGroupValidators.dateMaxDuration('comment', 'endDate', 100, 'days')

      ])
    };*/
    return null;
  }

  updateFormGroup(formGroup: FormGroup, opts?: PlanificationValidatorOptions) {
    opts = this.fillDefaultOptions(opts);

    //ormGroup.get('comment').setValidators(opts.isOnFieldMode ? null : Validators.required);
    //formGroup.get('year').setValidators(opts.isOnFieldMode ? null : Validators.required);
    return formGroup;
  }




}
