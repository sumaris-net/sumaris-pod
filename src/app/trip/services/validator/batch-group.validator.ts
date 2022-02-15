import {Injectable} from '@angular/core';
import {FormBuilder, FormGroup, ValidationErrors} from '@angular/forms';
import {BatchValidators, BatchValidatorService} from './batch.validator';
import {BatchGroup} from '../model/batch-group.model';
import {SharedValidators} from '@sumaris-net/ngx-components';
import {IPmfm} from '@app/referential/services/model/pmfm.model';
import {Subject, Subscription} from 'rxjs';
import {debounceTime, filter, map, tap} from 'rxjs/operators';
import {PmfmForm} from '@app/trip/services/validator/operation.validator';
import {MeasurementsValidatorService} from '@app/trip/services/validator/measurement.validator';

@Injectable({providedIn: 'root'})
export class BatchGroupValidatorService extends BatchValidatorService<BatchGroup> {

  qvPmfm: IPmfm;

  constructor(
    protected measurementsValidatorService: MeasurementsValidatorService,
    formBuilder: FormBuilder) {
    super(measurementsValidatorService, formBuilder);
  }

  getRowValidator(): FormGroup {
    return super.getFormGroup(null, {withWeight: true, withChildren: true, qvPmfm: this.qvPmfm, pmfms:this.pmfms});
  }

  getFormGroup(data?: BatchGroup, opts?: {
    withWeight?: boolean;
    rankOrderRequired?: boolean;
    labelRequired?: boolean;
  }): FormGroup {
    return super.getFormGroup(data, {withWeight: true, withChildren: true, qvPmfm: this.qvPmfm, ...opts});
  }

  protected getFormGroupConfig(data?: BatchGroup, opts?: {
    withWeight?: boolean;
    rankOrderRequired?: boolean;
    labelRequired?: boolean;
  }): { [key: string]: any } {
    const config = super.getFormGroupConfig(data, opts);

    config.observedIndividualCount = [data && data.observedIndividualCount, SharedValidators.integer];

    return config;
  }
}

export class BatchGroupValidators {

 static addSamplingRowValidators(pmfmForm: PmfmForm, opts?: {
    requiredSampleWeight?: boolean;
    qvPmfm?: IPmfm;
  }): Subscription {

   const {form, pmfms} = pmfmForm;
   if (!form) {
     console.warn('Argument \'form\' required');
     return null;
   }

    const $errors = new Subject<ValidationErrors | null>();
    form.setAsyncValidators((control) => $errors);

    let computing = false;
    const subscription = form.valueChanges
      .pipe(
        filter(() => !computing),
        // Protected against loop
        tap(() => computing = true),
        debounceTime(250),
        map(() => BatchValidators.computeSamplingWeightRow(form, {
            ...opts,
            emitEvent: false,
            onlySelf: false
          })
        )
      ).subscribe((errors) => {
        computing = false;
        $errors.next(errors);
      });

    // When unsubscribe, remove async validator
    subscription.add(() => {
      $errors.next(null);
      $errors.complete();
      form.clearAsyncValidators();
    });

    return subscription;
  }
}
