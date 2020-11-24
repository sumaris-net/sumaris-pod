import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {
  SharedFormArrayValidators,
  SharedFormGroupValidators,
  SharedValidators
} from "../../../shared/validator/validators";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {SaleValidatorService} from "./sale.validator";
import {MeasurementsValidatorService} from "./measurement.validator";
import {toBoolean} from "../../../shared/functions";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import {Trip} from "../model/trip.model";
import {
  DataRootEntityValidatorOptions,
  DataRootEntityValidatorService
} from "../../../data/services/validator/root-data-entity.validator";
import {ProgramProperties} from "../../../referential/services/config/program.config";

export interface TripValidatorOptions extends DataRootEntityValidatorOptions {
  withSale?: boolean;
  withMeasurements?: boolean;
  withMetiers?: boolean;
}

@Injectable({providedIn: 'root'})
export class TripValidatorService<O extends TripValidatorOptions = TripValidatorOptions>
  extends DataRootEntityValidatorService<Trip, O> {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService,
    protected saleValidator: SaleValidatorService,
    protected measurementsValidatorService: MeasurementsValidatorService
  ) {
    super(formBuilder, settings);
  }

  getFormGroup(data?: Trip, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    const form = super.getFormGroup(data, opts);

    // Add sale form
    if (opts.withSale) {
      form.addControl('sale', this.saleValidator.getFormGroup(data && data.sale, {
        required: false
      }));
    }

    // Add measurement form
    if (opts.withMeasurements) {
      const pmfms = (opts.program && opts.program.strategies[0] && opts.program.strategies[0].pmfmStrategies || [])
        .filter(p => p.acquisitionLevel === AcquisitionLevelCodes.TRIP);
      form.addControl('measurements', this.measurementsValidatorService.getFormGroup(data && data.measurements, {
        isOnFieldMode: opts.isOnFieldMode,
        pmfms
      }));
    }

    return form;
  }

  getFormGroupConfig(data?: Trip, opts?: O): { [key: string]: any } {

    const formConfig = Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        __typename: [Trip.TYPENAME],
        vesselSnapshot: [data && data.vesselSnapshot || null, Validators.compose([Validators.required, SharedValidators.entity])],
        departureDateTime: [data && data.departureDateTime || null, Validators.required],
        departureLocation: [data && data.departureLocation || null, Validators.compose([Validators.required, SharedValidators.entity])],
        returnDateTime: [data && data.returnDateTime || null, opts.isOnFieldMode ? null : Validators.required],
        returnLocation: [data && data.returnLocation || null, opts.isOnFieldMode ? SharedValidators.entity : Validators.compose([Validators.required, SharedValidators.entity])]
      });

    // Add observers
    if (opts.withObservers) {
      formConfig.observers = this.getObserversFormArray(data);
    }

    // Add metiers
    if (opts.withMetiers) {
      formConfig.metiers = this.getMetiersArray(data);
    }

    return formConfig;
  }

  getFormGroupOptions(data?: Trip, opts?: O): { [key: string]: any } {
    return {
      validator: Validators.compose([
        SharedFormGroupValidators.dateRange('departureDateTime', 'returnDateTime'),
        SharedFormGroupValidators.dateMinDuration('departureDateTime', 'returnDateTime', 1, 'hours'),
        SharedFormGroupValidators.dateMaxDuration('departureDateTime', 'returnDateTime', 100, 'days')
      ])
    };
  }

  updateFormGroup(form: FormGroup, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    form.get('returnDateTime').setValidators(opts.isOnFieldMode ? null : Validators.required);
    form.get('returnLocation').setValidators(opts.isOnFieldMode ? SharedValidators.entity : [Validators.required, SharedValidators.entity]);

    return form;
  }

  /* -- protected methods -- */

  protected fillDefaultOptions(opts?: O): O {
    opts = super.fillDefaultOptions(opts);

    opts.withObservers = toBoolean(opts.withObservers,
      toBoolean(opts.program && opts.program.getPropertyAsBoolean(ProgramProperties.TRIP_OBSERVERS_ENABLE),
    ProgramProperties.TRIP_OBSERVERS_ENABLE.defaultValue === "true"));

    opts.withMetiers = toBoolean(opts.withMetiers,
      toBoolean(opts.program && opts.program.getPropertyAsBoolean(ProgramProperties.TRIP_METIERS_ENABLE),
    ProgramProperties.TRIP_METIERS_ENABLE.defaultValue === "true"));

    opts.withSale = toBoolean(opts.withSale,
      toBoolean(opts.program && opts.program.getPropertyAsBoolean(ProgramProperties.TRIP_SALE_ENABLE), false));

    opts.withMeasurements = toBoolean(opts.withMeasurements,  toBoolean(!!opts.program, false));

    return opts;
  }

  getMetiersArray(data?: Trip) {
    return this.formBuilder.array(
      (data && data.metiers || []).map(metier => this.getMetierControl(metier)),
      SharedFormArrayValidators.requiredArrayMinLength(1)
    );
  }

  getMetierControl(value: any) {
    return this.formBuilder.control(value || null, [Validators.required, SharedValidators.entity]);
  }
}


