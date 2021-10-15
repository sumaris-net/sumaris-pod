import {Injectable} from '@angular/core';
import {ValidatorService} from '@e-is/ngx-material-table';
import {AbstractControlOptions, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {PositionValidatorService} from './position.validator';
import {LocalSettingsService, SharedFormGroupValidators, SharedValidators, toBoolean} from '@sumaris-net/ngx-components';
import {DataEntityValidatorOptions, DataEntityValidatorService} from '@app/data/services/validator/data-entity.validator';
import {AcquisitionLevelCodes} from '@app/referential/services/model/model.enum';
import {Program} from '@app/referential/services/model/program.model';
import {MeasurementsValidatorService} from './measurement.validator';
import {Operation} from '../model/trip.model';

export interface OperationValidatorOptions extends DataEntityValidatorOptions {
  program?: Program;
  withMeasurements?: boolean;
}

@Injectable({providedIn: 'root'})
export class OperationValidatorService<O extends OperationValidatorOptions = OperationValidatorOptions>
  extends DataEntityValidatorService<Operation, O>
  implements ValidatorService {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService,
    private positionValidator: PositionValidatorService,
    protected measurementsValidatorService: MeasurementsValidatorService
  ) {
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
      const pmfms = (opts.program && opts.program.strategies[0] && opts.program.strategies[0].denormalizedPmfms || [])
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
        startDateTime: [data && data.startDateTime || null, Validators.required],
        fishingStartDateTime: [data && data.fishingStartDateTime || null, opts && opts.isOnFieldMode ? null : Validators.required],
        fishingEndDateTime: [data && data.fishingEndDateTime || null],
        endDateTime: [data && data.endDateTime || null, SharedValidators.copyParentErrors(['dateRange', 'dateMaxDuration'])],
        rankOrderOnPeriod: [data && data.rankOrderOnPeriod || null],
        startPosition: this.positionValidator.getFormGroup(null, {required: true}),
        endPosition: this.positionValidator.getFormGroup(null, {required: !opts.isOnFieldMode}),
        metier: [data && data.metier || null, Validators.compose([Validators.required, SharedValidators.entity])],
        physicalGear: [data && data.physicalGear || null, Validators.compose([Validators.required, SharedValidators.entity])],
        comments: [data && data.comments || null, Validators.maxLength(2000)],
        parentOperation: [data && data.parentOperation || null],
        childOperation: [data && data.childOperation || null],
        qualityFlagId: [data && data.qualityFlagId || null],
        parentOperationLabel: [null]
      });

    return formConfig;
  }

  getFormGroupOptions(data?: Operation, opts?: O): AbstractControlOptions {
    return {
      validators: Validators.compose([
        SharedFormGroupValidators.dateRange('startDateTime', 'endDateTime'),
        SharedFormGroupValidators.dateMaxDuration('startDateTime', 'endDateTime', 100, 'days')
      ])
    };
  }

  /* -- protected methods -- */

  protected fillDefaultOptions(opts?: O): O {
    opts = super.fillDefaultOptions(opts);

    opts.withMeasurements = toBoolean(opts.withMeasurements,  toBoolean(!!opts.program, false));
    //console.debug("[operation-validator] Ope Validator will use options:", opts);

    return opts;
  }

}
