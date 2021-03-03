import {Injectable} from "@angular/core";
import {AbstractControlOptions, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedFormGroupValidators, SharedValidators} from "../../../shared/validator/validators";
import {ObservedLocation} from "../model/observed-location.model";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {
  DataRootEntityValidatorOptions,
  DataRootEntityValidatorService
} from "../../../data/services/validator/root-data-entity.validator";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import {PmfmValidators} from "../../../referential/services/validator/pmfm.validators";
import {toBoolean} from "../../../shared/functions";
import {ProgramProperties} from "../../../referential/services/config/program.config";

export interface ObservedLocationValidatorOptions extends DataRootEntityValidatorOptions {
  withMeasurements?: boolean;
}

@Injectable({providedIn: 'root'})
export class ObservedLocationValidatorService
  extends DataRootEntityValidatorService<ObservedLocation, ObservedLocationValidatorOptions>{

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService) {
    super(formBuilder, settings);
  }

  getFormGroup(data?: ObservedLocation, opts?: ObservedLocationValidatorOptions): FormGroup {
    opts = this.fillDefaultOptions(opts);

    const form = super.getFormGroup(data, opts);

    // Add measurement form
    if (opts.withMeasurements) {
      const measForm = form.get('measurementValues') as FormGroup;
      // TODO: find strategy from date and location
      (opts.program && opts.program.strategies[0] && opts.program.strategies[0].denormalizedPmfms || [])
        .filter(p => p.acquisitionLevel === AcquisitionLevelCodes.OBSERVED_LOCATION)
        .forEach(p => {
          const key = p.pmfmId.toString();
          const value = data && data.measurementValues && data.measurementValues[key];
          measForm.addControl(key, this.formBuilder.control(value, PmfmValidators.create(p)));
        });
    }

    return form;
  }

  getFormGroupConfig(data?: ObservedLocation, opts?: ObservedLocationValidatorOptions): { [key: string]: any } {
    return {
      ...super.getFormGroupConfig(data),
      __typename: [ObservedLocation.TYPENAME],
      location: [data && data.location || null, Validators.compose([Validators.required, SharedValidators.entity])],
      startDateTime: [data && data.startDateTime || null, Validators.required],
      endDateTime: [data && data.endDateTime || null],
      measurementValues: this.formBuilder.group({}),
      observers: this.getObserversFormArray(data),
      recorderDepartment: [data && data.recorderDepartment || null, SharedValidators.entity],
      recorderPerson: [data && data.recorderPerson || null, SharedValidators.entity]
    };

  }

  getFormGroupOptions(data?: any): AbstractControlOptions {
    return {
      validators: [SharedFormGroupValidators.dateRange('startDateTime', 'endDateTime')]
    };
  }

  protected fillDefaultOptions(opts?: ObservedLocationValidatorOptions): ObservedLocationValidatorOptions {
    opts = super.fillDefaultOptions(opts);

    opts.withObservers = toBoolean(opts.withObservers,
      toBoolean(opts.program && opts.program.getPropertyAsBoolean(ProgramProperties.TRIP_OBSERVERS_ENABLE),
        ProgramProperties.TRIP_OBSERVERS_ENABLE.defaultValue === "true"));

    opts.withMeasurements = toBoolean(opts.withMeasurements, !!opts.program);

    return opts;
  }
}

