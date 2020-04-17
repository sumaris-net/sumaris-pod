import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";
import {DataRootEntityValidatorOptions, DataRootVesselEntityValidatorService} from "./validator/base.validator";
import {toBoolean, toNumber} from "../../shared/functions";
import {ProgramProperties} from "../../referential/services/model";
import {MeasurementsValidatorService} from "./measurement.validator";
import {Landing} from "./model/landing.model";

export interface LandingValidatorOptions extends DataRootEntityValidatorOptions {
  withMeasurements?: boolean;
}

@Injectable()
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

    // TODO to activate following code, Landing must have 'measurements' instead of 'measurementValues'
    // Add measurement form
    // if (opts.withMeasurements) {
    //   const pmfms = (opts.program && opts.program.strategies[0] && opts.program.strategies[0].pmfmStrategies || [])
    //     .filter(p => p.acquisitionLevel === AcquisitionLevelCodes.TRIP);
    //   form.addControl('measurements', this.measurementsValidatorService.getFormGroup(data && data.measurements, {
    //     isOnFieldMode: opts.isOnFieldMode,
    //     pmfms
    //   }));
    // }

    return form;
  }

  getFormGroupConfig(data?: Landing, opts?: O): { [p: string]: any } {

    const formConfig = Object.assign(super.getFormGroupConfig(data), {
      __typename: [Landing.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      location: [data && data.location || null, SharedValidators.entity],
      dateTime: [data && data.dateTime || null],
      rankOrder: [toNumber(data && data.rankOrder, null), Validators.compose([SharedValidators.integer, Validators.min(1)])],
      rankOrderOnVessel: [toNumber(data && data.rankOrderOnVessel, null), Validators.compose([SharedValidators.integer, Validators.min(1)])],
      measurementValues: this.formBuilder.group({}),
      observedLocationId: [toNumber(data && data.observedLocationId, null)],
      tripId: [toNumber(data && data.tripId, null)],
      comments: [data && data.comments || null]
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
      toBoolean(opts.program && opts.program.getPropertyAsBoolean(ProgramProperties.LANDING_OBSERVERS_ENABLE),
        ProgramProperties.LANDING_OBSERVERS_ENABLE.defaultValue === "true"));

    opts.withMeasurements = toBoolean(opts.withMeasurements,  toBoolean(!!opts.program, false));

    return opts;
  }

}
