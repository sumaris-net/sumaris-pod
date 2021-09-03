import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedFormArrayValidators, SharedValidators} from '@sumaris-net/ngx-components';
import {toBoolean, toNumber} from "@sumaris-net/ngx-components";
import {ProgramProperties} from "../../../referential/services/config/program.config";
import {MeasurementsValidatorService} from "./measurement.validator";
import {Landing} from "../model/landing.model";
import {DataRootEntityValidatorOptions} from "../../../data/services/validator/root-data-entity.validator";
import {DataRootVesselEntityValidatorService} from "../../../data/services/validator/root-vessel-entity.validator";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import {PmfmValidators} from "../../../referential/services/validator/pmfm.validators";
import {Strategy} from "../../../referential/services/model/strategy.model";
import {IWithObserversEntity} from '@app/data/services/model/model.utils';

export interface LandingValidatorOptions extends DataRootEntityValidatorOptions {
  withMeasurements?: boolean;
  withStrategy?: boolean;
  strategy: Strategy;
}

@Injectable({providedIn: 'root'})
export class LandingValidatorService<O extends LandingValidatorOptions = LandingValidatorOptions>
  extends DataRootVesselEntityValidatorService<Landing, O> {

  constructor(
    formBuilder: FormBuilder,
    protected measurementsValidatorService: MeasurementsValidatorService
  ) {
    super(formBuilder);
  }

  getRowValidator(): FormGroup {
    return super.getRowValidator();
  }

  getFormGroup(data?: Landing, opts?: O): FormGroup {

    const form = super.getFormGroup(data, opts);

    // Add measurement form
    if (opts && opts.withMeasurements) {
      const measForm = form.get('measurementValues') as FormGroup;
      const pmfms = (opts.strategy && opts.strategy.denormalizedPmfms)
        || (opts.program && opts.program.strategies[0] && opts.program.strategies[0].denormalizedPmfms)
        || [];
      pmfms
        .filter(p => p.acquisitionLevel === AcquisitionLevelCodes.LANDING)
        .forEach(p => {
          const key = p.pmfmId.toString();
          const value = data && data.measurementValues && data.measurementValues[key];
          measForm.addControl(key, this.formBuilder.control(value, PmfmValidators.create(p)));
        });
    }

    return form;
  }

  getFormGroupConfig(data?: Landing, opts?: O): { [p: string]: any } {

    const formConfig = Object.assign(super.getFormGroupConfig(data), {
      __typename: [Landing.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      location: [data && data.location || null, SharedValidators.entity],
      dateTime: [data && data.dateTime || null],
      rankOrder: [toNumber(data && data.rankOrder, null), Validators.compose([SharedValidators.integer, Validators.min(1)])],
      rankOrderOnVessel: [toNumber(data && data.rankOrderOnVessel, null), Validators.compose([SharedValidators.integer, Validators.min(1)])],
      measurementValues: this.formBuilder.group({}),
      observedLocationId: [toNumber(data && data.observedLocationId, null)],
      tripId: [toNumber(data && data.tripId, null)],
      comments: [data && data.comments || null],

      // Computed values (e.g. for BIO-PARAM program)
      samplesCount: [data && data.samplesCount, null]
    });

    // Add observers
    if (opts.withObservers) {
      formConfig.observers = this.getObserversFormArray(data);
    }

    return formConfig;
  }

  protected fillDefaultOptions(opts?: O): O {
    opts = super.fillDefaultOptions(opts);

    opts.withObservers = toBoolean(opts.withObservers,
      opts.program && opts.program.getPropertyAsBoolean(ProgramProperties.LANDING_OBSERVERS_ENABLE) || false);

    opts.withStrategy = toBoolean(opts.withStrategy,
      opts.program && opts.program.getPropertyAsBoolean(ProgramProperties.LANDING_STRATEGY_ENABLE) || false);

    opts.withMeasurements = toBoolean(opts.withMeasurements,  toBoolean(!!opts.program, false));

    return opts;
  }

}
