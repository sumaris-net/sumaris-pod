import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedFormArrayValidators, SharedValidators} from "../../../shared/validator/validators";
import {toBoolean, toNumber} from "../../../shared/functions";
import {ProgramProperties} from "../../../referential/services/config/program.config";
import {MeasurementsValidatorService} from "./measurement.validator";
import {Landing} from "../model/landing.model";
import {DataRootEntityValidatorOptions} from "../../../data/services/validator/root-data-entity.validator";
import {DataRootVesselEntityValidatorService} from "../../../data/services/validator/root-vessel-entity.validator";
import {SimpleStrategy} from "../../../referential/services/model/simpleStrategy.model";

export interface Landing2ValidatorOptions extends DataRootEntityValidatorOptions {
  withMeasurements?: boolean;
}

@Injectable({providedIn: 'root'})
export class Landing2ValidatorService<O extends Landing2ValidatorOptions = Landing2ValidatorOptions>
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
    //   const pmfms = (opts.program && opts.program.strategies[0] && opts.program.strategies[0].pmfmStrategies || [])
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
      sampleRowCode: [null, SharedValidators.entity],
      fishingArea:  [null, SharedValidators.entity],//this.getFishingAreasFormArray(data),
      taxonName: [data && data.samples[0] && data.samples[0].taxonName || null],
    });

    // Add observers
    if (opts.withObservers) {
      formConfig.observers = this.getObserversFormArray(data);
    }

    return formConfig;
  }

  // FishingArea Control -----------------------------------------------------------------------------------
  getFishingAreasFormArray(data?: Landing) {
    return this.formBuilder.array(
      (/*data && data.fishingAreas ||*/ []).map(fishingArea => this.getControl(fishingArea)),
      SharedFormArrayValidators.requiredArrayMinLength(1)
    );
  }

  getControl(value: any) {
    return this.formBuilder.control(value || null, [Validators.required, SharedValidators.entity]);
  }

  protected fillDefaultOptions(opts?: O): O {
    opts = super.fillDefaultOptions(opts);

    opts.withObservers = toBoolean(opts.withObservers,
      opts.program && opts.program.getPropertyAsBoolean(ProgramProperties.LANDING_OBSERVERS_ENABLE) || false);

    opts.withMeasurements = toBoolean(opts.withMeasurements,  toBoolean(!!opts.program, false));

    return opts;
  }

}
