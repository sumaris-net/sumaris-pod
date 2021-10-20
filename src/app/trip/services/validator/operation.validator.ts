import { Injectable } from '@angular/core';
import { ValidatorService } from '@e-is/ngx-material-table';
import { AbstractControlOptions, FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { PositionValidatorService } from './position.validator';
import { fromDateISOString, isNotNil, LocalSettingsService, SharedFormGroupValidators, SharedValidators, toBoolean } from '@sumaris-net/ngx-components';
import { DataEntityValidatorOptions, DataEntityValidatorService } from '@app/data/services/validator/data-entity.validator';
import { AcquisitionLevelCodes } from '@app/referential/services/model/model.enum';
import { Program } from '@app/referential/services/model/program.model';
import { MeasurementsValidatorService } from './measurement.validator';
import { Operation, Trip } from '../model/trip.model';

export interface OperationValidatorOptions extends DataEntityValidatorOptions {
  program?: Program;
  withMeasurements?: boolean;
  withParent?: boolean;
  withChild?: boolean;
  trip?: Trip;
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
        qualityFlagId: [data && data.qualityFlagId || null]
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

  updateFormGroup(formGroup: FormGroup, opts?: O) {

    // DEBUG
    //console.debug(`[operation-validator] Updating form group validators`);

    let endDateTimeControlName;
    let disabledEndDateTimeControlName;
    const endDateTimeValidators: ValidatorFn[] = [];
    const parentControl = formGroup.get('parentOperation');
    const childControl = formGroup.get('childOperation');

    // iS child
    if (opts?.withParent) {
      parentControl.setValidators(Validators.compose([Validators.required, SharedValidators.entity]));
      parentControl.enable();
      childControl.disable();

      formGroup.get('fishingEndDateTime').setValidators(Validators.required);
      formGroup.get('fishingEndDateTime').setAsyncValidators(async (control) => {
        const fishingEndDateTime = fromDateISOString(control.value);
        const fishingStartDateTime = fromDateISOString((control.parent as FormGroup).get('fishingStartDateTime').value);
        // Error if fishingEndDateTime <= fishingStartDateTime
        if (fishingStartDateTime && fishingEndDateTime?.isSameOrBefore(fishingStartDateTime)) {
          console.warn(`[operation] Invalid operation fishingEndDateTime: before fishingStartDateTime! `, fishingEndDateTime, fishingStartDateTime);
          return <ValidationErrors>{msg: 'TRIP.OPERATION.ERROR.FIELD_DATE_BEFORE_PARENT_OPERATION'};
        }
        // OK: clear existing errors
        SharedValidators.clearError(control, 'msg');
        return null;
      });

      endDateTimeControlName = 'endDateTime';
      disabledEndDateTimeControlName = 'fishingStartDateTime';
    }

    // Is parent
    if (opts?.withChild) {
      parentControl.clearValidators();
      parentControl.disable();

      formGroup.get('fishingEndDateTime').clearValidators();
      formGroup.get('fishingEndDateTime').clearAsyncValidators();

      endDateTimeControlName = 'fishingStartDateTime';
      disabledEndDateTimeControlName = 'endDateTime';

      endDateTimeValidators.push((control) => {
        const endDateTime = fromDateISOString(control.value);
        const fishingEndDateTime = fromDateISOString(childControl.value?.fishingEndDateTime);
        if (fishingEndDateTime && endDateTime && endDateTime.isBefore(fishingEndDateTime) === false) {
          console.warn(`[operation] Invalid operation ${endDateTimeControlName}: after the child operation's start! `, endDateTime, fishingEndDateTime);
          return <ValidationErrors>{msg: 'TRIP.OPERATION.ERROR.FIELD_DATE_AFTER_CHILD_OPERATION'};
        }
      });
    }

    // Default case
    if (!opts || opts.withParent !== true && opts.withChild !== true) {
      parentControl.clearValidators();
      parentControl.disable();

      childControl.clearValidators();
      childControl.disable();

      formGroup.get('fishingEndDateTime').clearValidators();
      formGroup.get('fishingEndDateTime').clearAsyncValidators();
      endDateTimeControlName = 'endDateTime';
      disabledEndDateTimeControlName = 'fishingStartDateTime';
    }

    // Add required
    if (opts?.isOnFieldMode) endDateTimeValidators.push(Validators.required);


    const trip = opts.trip;
    if (trip) {
      endDateTimeValidators.push((control) => {
        // TODO
        //if (!control.touched && !control.dirty) return null;

        const endDateTime = fromDateISOString(control.value);
        // Make sure trip.departureDateTime < operation.endDateTime
        if (endDateTime && trip.departureDateTime && trip.departureDateTime.isBefore(endDateTime) === false) {
          console.warn(`[operation] Invalid operation ${endDateTimeControlName}: before the trip!`, endDateTime, trip.departureDateTime);
          return <ValidationErrors>{msg: 'TRIP.OPERATION.ERROR.FIELD_DATE_BEFORE_TRIP'};
        }
        // Make sure operation.endDateTime < trip.returnDateTime
        else if (endDateTime && trip.returnDateTime && endDateTime.isBefore(trip.returnDateTime) === false) {
          console.warn(`[operation] Invalid operation ${endDateTimeControlName}: after the trip! `, endDateTime, trip.returnDateTime);
          return <ValidationErrors>{msg: 'TRIP.OPERATION.ERROR.FIELD_DATE_AFTER_TRIP'};
        }
      });
    }


    formGroup.get(disabledEndDateTimeControlName).clearAsyncValidators();
    SharedValidators.clearError(formGroup.get(disabledEndDateTimeControlName), 'required');
    formGroup.get(endDateTimeControlName).setAsyncValidators(async (control) => {

      const errors: ValidationErrors = endDateTimeValidators
        .map(validator => validator(control))
        .find(isNotNil) || null;
      //if (!control.touched && !control.dirty) return null;

      // Clear unused errors
      if (!errors || !errors.msg) SharedValidators.clearError(control, 'msg');
      if (!errors || !errors.required) SharedValidators.clearError(control, 'required');
      return errors;
    });
  }

  /* -- protected methods -- */

  protected fillDefaultOptions(opts?: O): O {
    opts = super.fillDefaultOptions(opts);

    opts.withMeasurements = toBoolean(opts.withMeasurements,  toBoolean(!!opts.program, false));
    //console.debug("[operation-validator] Ope Validator will use options:", opts);

    return opts;
  }

}
