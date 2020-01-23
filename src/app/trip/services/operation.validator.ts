import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Operation} from "./trip.model";
import {PositionValidatorService} from "./position.validator";
import {SharedValidators} from "../../shared/validator/validators";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {DataEntityValidatorOptions, DataEntityValidatorService} from "./validator/base.validator";
import {toBoolean} from "../../shared/functions";
import {AcquisitionLevelCodes, Program} from "../../referential/services/model";
import {MeasurementsValidatorService} from "./measurement.validator";

export interface OperationValidatorOptions extends DataEntityValidatorOptions {
  program?: Program;
  withMeasurements?: boolean;
}

@Injectable()
export class OperationValidatorService<O extends OperationValidatorOptions = OperationValidatorOptions>
  extends DataEntityValidatorService<Operation, O>
  implements ValidatorService {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService,
    private positionValidator: PositionValidatorService,
    protected measurementsValidatorService: MeasurementsValidatorService) {
    super(formBuilder, settings);
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Operation, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    const form = super.getFormGroup(data, opts);

    // Add measurement form
    if (opts.withMeasurements) {
      const pmfms = (opts.program && opts.program.strategies[0] && opts.program.strategies[0].pmfmStrategies || [])
        .filter(p => p.acquisitionLevel === AcquisitionLevelCodes.OPERATION);
      form.addControl('measurements', this.measurementsValidatorService.getFormGroup(data && data.measurements, {
        isOnFieldMode: opts.isOnFieldMode,
        pmfms
      }));
    }

    return form;
  }

  getFormGroupConfig(data?: Operation, opts?: O): { [key: string]: any } {

    const formConfig = Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        __typename: [Operation.TYPENAME],
        startDateTime: [data && data.startDateTime || null, Validators.required],
        endDateTime: [data && data.endDateTime || null, opts.isOnFieldMode ? null : Validators.required],
        rankOrderOnPeriod: [data && data.rankOrderOnPeriod || null],
        startPosition: this.positionValidator.getFormGroup(null, {required: true}),
        endPosition: this.positionValidator.getFormGroup(null, {required: !opts.isOnFieldMode}),
        metier: [data && data.metier || null, Validators.compose([Validators.required, SharedValidators.entity])],
        physicalGear: [data && data.physicalGear || null, Validators.compose([Validators.required, SharedValidators.entity])],
        comments: [data && data.comments || null, Validators.maxLength(2000)]
      });

    return formConfig;
  }

  getFormGroupOptions(data?: Operation, opts?: O): { [p: string]: any } {
    return {
      validator: Validators.compose([
        SharedValidators.dateRange('startDateTime', 'endDateTime'),
        SharedValidators.dateMaxDuration('startDateTime', 'endDateTime', 100, 'days')
      ])
    };
  }

  /* -- protected methods -- */

  protected fillDefaultOptions(opts?: O): O {
    opts = super.fillDefaultOptions(opts);

    opts.withMeasurements = toBoolean(opts.withMeasurements,  toBoolean(!!opts.program, false));

    console.debug("TODO: validator will use options:", opts);

    return opts;
  }

}
