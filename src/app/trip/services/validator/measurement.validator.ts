import { Injectable } from '@angular/core';
import { ValidatorService } from '@e-is/ngx-material-table';
import { AbstractControl, AbstractControlOptions, FormBuilder, FormGroup } from '@angular/forms';

import { LocalSettingsService, toBoolean } from '@sumaris-net/ngx-components';
import { Measurement, MeasurementUtils, MeasurementValuesUtils } from '../model/measurement.model';
import { PmfmValidators } from '@app/referential/services/validator/pmfm.validators';
import { IPmfm } from '@app/referential/services/model/pmfm.model';

export interface MeasurementsValidatorOptions {
  isOnFieldMode?: boolean;
  pmfms?: IPmfm[];
  protectedAttributes?: string[];
  forceOptional?: boolean;
}

@Injectable({ providedIn: 'root' })
export class MeasurementsValidatorService<T extends Measurement = Measurement, O extends MeasurementsValidatorOptions = MeasurementsValidatorOptions>
  implements ValidatorService
{
  constructor(protected formBuilder: FormBuilder, protected settings: LocalSettingsService) {}

  getRowValidator(opts?: O): FormGroup {
    return this.getFormGroup(null, opts);
  }

  getFormGroup(data: T[], opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    return this.formBuilder.group(this.getFormGroupConfig(data, opts), this.getFormGroupOptions(data, opts));
  }

  getFormGroupConfig(data: T[], opts?: O): { [key: string]: any } {
    opts = this.fillDefaultOptions(opts);

    // Convert the array of Measurement into a normalized map of form values
    const measurementValues =
      (data &&
        MeasurementValuesUtils.normalizeValuesToForm(MeasurementUtils.toMeasurementValues(data as Measurement[]), opts.pmfms, {
          keepSourceObject: true,
          onlyExistingPmfms: false,
        })) ||
      undefined;

    return opts.pmfms.reduce((res, pmfm) => {
      const validator = PmfmValidators.create(pmfm, null, opts);
      if (validator) {
        res[pmfm.id] = [measurementValues ? measurementValues[pmfm.id] : null, validator];
      } else {
        res[pmfm.id] = [measurementValues ? measurementValues[pmfm.id] : null];
      }
      return res;
    }, {});
  }

  getFormGroupOptions(data?: T[], opts?: O): AbstractControlOptions | null {
    return null;
  }

  updateFormGroup(form: FormGroup, opts?: O) {
    opts = this.fillDefaultOptions(opts);

    const controlNamesToRemove: string[] = [];
    // eslint-disable-next-line guard-for-in
    for (const controlName in form.controls) {
      controlNamesToRemove.push(controlName);
    }
    opts.pmfms.forEach((pmfm) => {
      const controlName = pmfm.id.toString();
      let formControl: AbstractControl = form.get(controlName);
      // If new pmfm: add as control
      if (!formControl) {
        formControl = this.formBuilder.control(pmfm.defaultValue || '', PmfmValidators.create(pmfm, null, opts));
        form.addControl(controlName, formControl);
      }

      // Remove from the remove list
      const index = controlNamesToRemove.indexOf(controlName);
      if (index >= 0) controlNamesToRemove.splice(index, 1);
    });

    // Remove unused controls
    controlNamesToRemove
      .filter((controlName) => !opts.protectedAttributes || !opts.protectedAttributes.includes(controlName)) // Keep protected columns
      .forEach((controlName) => form.removeControl(controlName));
  }

  /* -- -- */
  protected fillDefaultOptions(opts?: O): O {
    opts = opts || ({} as O);

    opts.pmfms = opts.pmfms || [];

    opts.forceOptional = toBoolean(opts.forceOptional, false);

    opts.protectedAttributes = opts.protectedAttributes || ['id', 'rankOrder', 'comments', 'updateDate'];

    return opts;
  }
}
