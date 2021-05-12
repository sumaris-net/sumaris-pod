import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import {toBoolean} from "../../../shared/functions";
import {
  DataEntityValidatorOptions,
  DataEntityValidatorService
} from "../../../data/services/validator/data-entity.validator";
import {MeasurementsValidatorService} from "./measurement.validator";
import {OperationGroup} from "../model/trip.model";
import {Program} from "../../../referential/services/model/program.model";

export interface OperationGroupValidatorOptions extends DataEntityValidatorOptions {
  program?: Program;
  withMeasurements?: boolean;
}

@Injectable({providedIn: 'root'})
export class OperationGroupValidatorService<O extends OperationGroupValidatorOptions = OperationGroupValidatorOptions>
  extends DataEntityValidatorService<OperationGroup, O> implements ValidatorService {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService,
    protected measurementsValidatorService: MeasurementsValidatorService
  ) {
    super(formBuilder, settings);
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: OperationGroup, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    const form = super.getFormGroup(data, opts);

    // Add measurement form
    if (opts.withMeasurements) {
      const pmfms = (opts.program && opts.program.strategies[0] && opts.program.strategies[0].denormalizedPmfms || [])
        .filter(p => p.acquisitionLevel === AcquisitionLevelCodes.OPERATION);
      form.addControl('measurements', this.measurementsValidatorService.getFormGroup(data && data.measurements, {
        isOnFieldMode: opts.isOnFieldMode,
        pmfms
      }));
    }

    return form;
  }

  getFormGroupConfig(data?: OperationGroup, opts?: O): { [key: string]: any } {

    const formConfig = Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        __typename: [OperationGroup.TYPENAME],
        rankOrderOnPeriod: [data && data.rankOrderOnPeriod || null],
        metier: [data && data.metier || null, Validators.compose([Validators.required, SharedValidators.entity])],
        // physicalGear: [data && data.physicalGear || null, Validators.compose([Validators.required, SharedValidators.entity])],
        physicalGear: [data && data.physicalGear || null, Validators.required], // Just required because new physicalGear fails the entity validator
        comments: [data && data.comments || null, Validators.maxLength(2000)]
      });

    return formConfig;
  }

  /* -- protected methods -- */

  protected fillDefaultOptions(opts?: O): O {
    opts = super.fillDefaultOptions(opts);

    opts.withMeasurements = toBoolean(opts.withMeasurements, toBoolean(!!opts.program, false));
    //console.debug("[operation-validator] Ope Validator will use options:", opts);

    return opts;
  }

}
