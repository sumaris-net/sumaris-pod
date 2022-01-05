import {Injectable} from '@angular/core';
import {FormBuilder, FormGroup, ValidationErrors, Validators} from '@angular/forms';
import {BatchValidators, BatchValidatorService} from './batch.validator';
import {BatchGroup} from '../model/batch-group.model';
import {AppFormUtils, isNotNil, SharedValidators} from '@sumaris-net/ngx-components';
import {IPmfm, Pmfm} from '@app/referential/services/model/pmfm.model';
import {merge, Observable, Subject, Subscription} from 'rxjs';
import {PmfmIds} from '@app/referential/services/model/model.enum';
import {debounceTime, filter, map, startWith, tap} from 'rxjs/operators';
import {PmfmForm} from '@app/trip/services/validator/operation.validator';

@Injectable({providedIn: 'root'})
export class BatchGroupValidatorService extends BatchValidatorService<BatchGroup> {

  qvPmfm: IPmfm;

  constructor(
    formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getRowValidator(): FormGroup {
    return super.getFormGroup(null, {withWeight: true, withChildren: true, qvPmfm: this.qvPmfm});
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
