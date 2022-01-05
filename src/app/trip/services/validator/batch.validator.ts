import {Injectable} from '@angular/core';
import {ValidatorService} from '@e-is/ngx-material-table';
import {FormArray, FormBuilder, FormGroup, ValidationErrors, Validators} from '@angular/forms';
import {EntityUtils, FormArrayHelper, isNotNilString, SharedValidators} from '@sumaris-net/ngx-components';
import {Batch, BatchUtils, BatchWeight} from '../model/batch.model';
import {debounceTime, filter, map, tap} from 'rxjs/operators';
import {isNil, isNotNilOrNaN, toBoolean, toNumber} from '@sumaris-net/ngx-components';
import {MethodIds} from '../../../referential/services/model/model.enum';
import {merge, Subject, Subscription} from 'rxjs';
import {IPmfm} from '@app/referential/services/model/pmfm.model';
import {SampleForm} from '@app/trip/sample/sample.form';

@Injectable({providedIn: 'root'})
export class BatchValidatorService<T extends Batch = Batch> implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: T, opts?: {
    withWeight?: boolean;
    rankOrderRequired?: boolean;
    labelRequired?: boolean;
    withChildren?: boolean;
    qvPmfm?: IPmfm;
  }): FormGroup {
    const form = this.formBuilder.group(this.getFormGroupConfig(data, {...opts}));

    if (opts && ((opts.qvPmfm && opts.qvPmfm.qualitativeValues.length) || opts.withChildren)) {
      const formChildrenHelper = this.getChildrenFormHelper(form, {withChildren: !!opts.qvPmfm});

      formChildrenHelper.resize(opts.qvPmfm?.qualitativeValues?.length || 1);
    }

    // Add weight sub form
    if (opts && opts.withWeight) {
      form.addControl('weight', this.getWeightFormGroup(data && data.weight));
    }

    return form;
  }

  protected getFormGroupConfig(data?: T, opts?: {
    rankOrderRequired?: boolean;
    labelRequired?: boolean;
  }): { [key: string]: any } {
    const rankOrder = toNumber(data && data.rankOrder, null);
    const label = data && data.label || null;
    return {
      __typename: [Batch.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      rankOrder: !opts || opts.rankOrderRequired !== false ? [rankOrder, Validators.required] : [rankOrder],
      label: !opts || opts.labelRequired !== false ? [label, Validators.required] : [label],
      individualCount: [toNumber(data && data.individualCount, null), Validators.compose([Validators.min(0), SharedValidators.integer])],
      samplingRatio: [toNumber(data && data.samplingRatio, null), SharedValidators.double()],
      samplingRatioText: [data && data.samplingRatioText || null],
      taxonGroup: [data && data.taxonGroup || null, SharedValidators.entity],
      taxonName: [data && data.taxonName || null, SharedValidators.entity],
      comments: [data && data.comments || null],
      parent: [data && data.parent || null, SharedValidators.entity],
      measurementValues: this.formBuilder.group({}),
      children: this.formBuilder.array([]),
      qualityFlagId: [toNumber(data && data.qualityFlagId, 0)]
    };
  }

  getWeightFormGroup(data?: BatchWeight): FormGroup {
    return this.formBuilder.group({
      methodId: [toNumber(data && data.methodId, null), SharedValidators.integer],
      estimated: [toBoolean(data && data.estimated, null)],
      computed: [toBoolean(data && data.computed, null)],
      value: [toNumber(data && data.value, null), SharedValidators.double({maxDecimals: 2})]
    });
  }

  protected getChildrenFormHelper(form: FormGroup, opts?: { withChildren: boolean }): FormArrayHelper<T> {
    let arrayControl = form.get('children') as FormArray;
    if (!arrayControl) {
      arrayControl = this.formBuilder.array([]);
      form.addControl('children', arrayControl);
    }
    return new FormArrayHelper<T>(
      arrayControl,
      (value) => this.getFormGroup(value, {withWeight: true, qvPmfm: undefined, ...opts}),
      (v1, v2) => EntityUtils.equals(v1, v2, 'label'),
      (value) => isNil(value),
      {allowEmptyArray: true}
    );
  }

  addSamplingFormValidators(form: FormGroup, opts?: {
    requiredSampleWeight?: boolean;
  }): Subscription {

    // Sampling ratio: should be a percentage
    form.get('samplingRatio').setValidators(
      Validators.compose([Validators.min(0), Validators.max(100), SharedValidators.double({maxDecimals: 2})])
    );

    const $errors = new Subject<ValidationErrors | null>();
    form.setAsyncValidators((control) => $errors);

    let computing = false;
    const subscription = form.valueChanges
      .pipe(
        filter(() => !computing),
        // Protected against loop
        tap(() => computing = true),
        debounceTime(250),
        map(() => BatchValidators.computeSamplingWeight(form, {...opts, emitEvent: false, onlySelf: false}))
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

export class BatchValidators {
  static computeSamplingWeightRow(form: FormGroup, opts?: {
    requiredSampleWeight?: boolean;
    emitEvent?: boolean;
    onlySelf?: boolean;
    qvPmfm?: IPmfm;
  }): ValidationErrors | null {

    let errors: ValidationErrors | null;
    if (opts && opts.qvPmfm) {
      opts.qvPmfm.qualitativeValues.forEach((qv, index) => {
        const qvSuffix = 'children.' + index + '.';

        if (form.get(qvSuffix + 'individualCount').disabled) {
          form.get(qvSuffix + 'individualCount').enable();
        }
        if (form.get(qvSuffix + 'children.0.individualCount').disabled) {
          form.get(qvSuffix + 'children.0.individualCount').enable();
        }
        const qvErrors = BatchValidators.newComputeSamplingWeight(form, {
          ...opts,
          weightPath: qvSuffix + 'weight',
          samplingWeightPath: qvSuffix + 'children.0.weight',
          samplingRatioPath: qvSuffix + 'children.0.samplingRatio',
          qvIndex: index
        });

        if (qvErrors) {
          if (errors) {
            errors = {...errors, ...qvErrors};
          } else {
            errors = qvErrors;
          }
        }
      });
    }
    return errors;
  }

  /**
   * Computing weight, samplingWeight or sampling ratio
   * @param form
   * @param opts
   */
  static computeSamplingWeight(form: FormGroup, opts?: {
    requiredSampleWeight?: boolean;
    emitEvent?: boolean;
    onlySelf?: boolean;
  }): ValidationErrors | null
  {
    const sampleForm = form.get('children.0');

    const batch = form.value;
    if (!batch.weight) return null;

    const sampleBatch = BatchUtils.getSamplingChild(batch);
    if (!sampleBatch || !sampleBatch.weight) return null;

    const totalWeight = batch.weight.value;
    const samplingRatioPct = sampleBatch.samplingRatio;
    const samplingWeight = sampleBatch.weight.value;

    // DEBUG
    //console.debug("[batch-validator] Start computing: ", [totalWeight, samplingRatioPct, samplingWeight]);

    const totalWeightValueControl = form.get('weight.value');
    const samplingWeightValueControl = sampleForm.get('weight.value');
    const samplingRatioControl = sampleForm.get('samplingRatio');

    // Compute samplingRatio, using weights
    if (!batch.weight.computed && isNotNilOrNaN(totalWeight) && totalWeight > 0
      && !sampleBatch.weight.computed && isNotNilOrNaN(samplingWeight) && samplingWeight > 0) {

      // Sampling weight must be under total weight
      if (samplingWeight > totalWeight) {
        if (!samplingWeightValueControl.hasError('max') || samplingWeightValueControl.errors['max'] !== totalWeight) {
          samplingWeightValueControl.markAsPending({onlySelf: true, emitEvent: true}); //{onlySelf: true, emitEvent: false});
          samplingWeightValueControl.markAsTouched({onlySelf: true});
          samplingWeightValueControl.setErrors({...samplingWeightValueControl.errors, max: {max: totalWeight}}, opts);
        }
        return {max: {max: totalWeight}} as ValidationErrors;
      } else {
        SharedValidators.clearError(samplingWeightValueControl, 'max');
      }

      // Update sampling ratio
      const computedSamplingRatioPct = Math.round(100 * samplingWeight / totalWeight);
      if (samplingRatioPct !== computedSamplingRatioPct) {
        sampleForm.patchValue({
          samplingRatio: computedSamplingRatioPct,
          samplingRatioText: `${totalWeight}/${samplingWeight}`
        }, opts);
      }

      // Disable ratio control
      samplingRatioControl.disable({...opts, emitEvent: true /*force repaint*/});
      return;
    }

    // Compute sample weight using ratio and total weight
    else if (isNotNilOrNaN(samplingRatioPct) && samplingRatioPct <= 100 && samplingRatioPct > 0
      && !batch.weight.computed && isNotNilOrNaN(totalWeight) && totalWeight >= 0) {

      if (sampleBatch.weight.computed || isNil(samplingWeight)) {
        const computedSamplingWeight = Math.round(totalWeight * samplingRatioPct) / 100;
        if (samplingWeight !== computedSamplingWeight) {
          sampleForm.patchValue({
            samplingRatioText: `${samplingRatioPct}%`,
            weight: {
              computed: true,
              estimated: false,
              value: computedSamplingWeight,
              methodId: MethodIds.CALCULATED
            }
          }, opts);
        }

        // Disable sampling weight control
        samplingWeightValueControl.disable(opts);
        return;
      }
    }

    // Compute total weight using ratio and sample weight
    else if (isNotNilOrNaN(samplingRatioPct) && samplingRatioPct <= 100 && samplingRatioPct > 0
      && !sampleBatch.weight.computed && isNotNilOrNaN(samplingWeight) && samplingWeight >= 0) {
      if (batch.weight.computed || isNil(totalWeight)) {
        form.patchValue({
          weight: {
            computed: true,
            estimated: false,
            value: Math.round(samplingWeight * (100 / samplingRatioPct) * 100) / 100,
            methodId: MethodIds.CALCULATED
          }
        }, opts);

        // Disable total weight control
        form.get('weight.value').disable(opts);
        return;
      }
    } else if (isNotNilOrNaN(samplingWeight)) {
      if (samplingRatioControl.disabled && sampleForm.enabled) {
        samplingRatioControl.enable({...opts, emitEvent: true/*force repaint*/});
      }
    }

    // Nothing can be computed: enable all controls
    else {

      // Enable total weight (and remove computed value, if any)
      if (batch.weight.computed) {
        form.patchValue({
          weight: {
            value: null,
            computed: false,
            estimated: false
          }
        }, opts);
      }
      totalWeightValueControl.enable(opts);

      if (sampleForm.enabled) {
        // Enable sampling ratio
        samplingRatioControl.enable({...opts, emitEvent: true/*force repaint*/});

        // Enable sampling weight (and remove computed value, if any)
        if (sampleBatch.weight.computed) {
          sampleForm.patchValue({
            weight: {
              value: null,
              computed: false,
              estimated: false
            }
          }, opts);
        }

        // If sampling weight is required
        if (opts && opts.requiredSampleWeight === true) {
          if (!samplingWeightValueControl.hasError('required')) {
            samplingWeightValueControl.setErrors({...samplingWeightValueControl.errors, required: true}, opts);
          }
        }

        // If sampling weight is NOT required
        else {
          samplingWeightValueControl.setErrors(null, opts);
        }
        samplingWeightValueControl.enable(opts);

      } else {
        samplingRatioControl.disable({...opts, emitEvent: true/*force repaint*/});
        samplingWeightValueControl.disable(opts);
      }
    }
  }

  /**
   * Computing weight, samplingWeight or sampling ratio
   * @param form
   * @param opts
   */
  static newComputeSamplingWeight(form: FormGroup, opts?: {
    requiredSampleWeight?: boolean;
    emitEvent?: boolean;
    onlySelf?: boolean;
    weightPath?: string;
    samplingWeightPath?: string;
    samplingRatioPath?: string;
    qvIndex?: number;
  }): ValidationErrors | null {

    const qvSuffix = opts && isNotNilOrNaN(opts.qvIndex) ? 'children.' + opts.qvIndex.toString() : '';
    const sampleFormSuffix = qvSuffix + (qvSuffix ? '.' : '') + 'children.0';

    const sampleForm = form.get(sampleFormSuffix);
    if (!sampleForm) return;

    const weightPath = opts && opts.weightPath || 'weight.value';
    const samplingWeightPath = opts && opts.samplingWeightPath || sampleFormSuffix + '.' + weightPath;
    const samplingRatioPath = opts && opts.samplingRatioPath || sampleFormSuffix + '.samplingRatio';

    const totalWeightControl = form.get(weightPath);
    const samplingWeightControl = form.get(samplingWeightPath);
    const samplingRatioControl = form.get(samplingRatioPath);

    const totalWeight = totalWeightControl.value?.value;
    const samplingRatioPct = samplingRatioControl.value;
    const samplingWeight = samplingWeightControl.value?.value;

    if (totalWeightControl.disabled) totalWeightControl.enable(opts);
    if (samplingRatioControl.disabled) samplingRatioControl.enable(opts);
    if (samplingWeightControl.disabled) samplingWeightControl.enable(opts);

    const batch = isNotNilString(qvSuffix) ? form.get(qvSuffix).value : form.value;
    if (!batch.weight) {
      batch.weight = {
        value: totalWeight || 0,
        computed: false,
        estimated: false
      };
    }

    let sampleBatch = BatchUtils.getSamplingChild(batch);
    if (!sampleBatch) {
      sampleBatch = sampleForm.value;
      batch.children.push(sampleBatch);
    }
    if (!sampleBatch.weight) {
      sampleBatch.weight = {
        value: samplingWeight || 0,
        computed: false,
        estimated: false,
        methodId: batch.weight.methodId
      };
    }

    // DEBUG
    console.debug('[batch-validator] Start computing: ', [totalWeight, samplingRatioPct, samplingWeight]);

    // Compute samplingRatio, using weights
    if (!batch.weight.computed && isNotNilOrNaN(totalWeight) && totalWeight > 0
      && !sampleBatch.weight.computed && isNotNilOrNaN(samplingWeight) && samplingWeight > 0) {

      // Sampling weight must be under total weight
      if (samplingWeight > totalWeight) {
        if (!samplingWeightControl.hasError('max') || samplingWeightControl.errors['max'] !== totalWeight) {
          samplingWeightControl.markAsPending({onlySelf: true, emitEvent: true}); //{onlySelf: true, emitEvent: false});
          samplingWeightControl.markAsTouched({onlySelf: true});
          samplingWeightControl.setErrors({...samplingWeightControl.errors, max: {max: totalWeight}}, opts);
        }
        return {max: {max: totalWeight}} as ValidationErrors;
      } else {
        SharedValidators.clearError(samplingWeightControl, 'max');
      }

      // Update sampling ratio
      const computedSamplingRatioPct = Math.round(100 * samplingWeight / totalWeight);
      if (samplingRatioPct !== computedSamplingRatioPct) {
        sampleForm.patchValue({
          samplingRatio: computedSamplingRatioPct,
          samplingRatioText: `${totalWeight}/${samplingWeight}`
        }, opts);
      }

      // Disable ratio control
      samplingRatioControl.disable({...opts, emitEvent: true /*force repaint*/});
      return;
    }

    // Compute sample weight using ratio and total weight
    else if (isNotNilOrNaN(samplingRatioPct) && samplingRatioPct <= 100 && samplingRatioPct > 0
      && !batch.weight.computed && isNotNilOrNaN(totalWeight) && totalWeight >= 0) {

      if (sampleBatch.weight.computed || isNil(samplingWeight)) {
        const computedSamplingWeight = Math.round(totalWeight * samplingRatioPct) / 100;
        if (samplingWeight !== computedSamplingWeight) {
          sampleForm.patchValue({
            samplingRatioText: `${samplingRatioPct}%`,
            weight: {
              computed: true,
              estimated: false,
              value: computedSamplingWeight,
              methodId: MethodIds.CALCULATED
            }
          }, opts);
        }

        // Disable sampling weight control
        samplingWeightControl.disable({...opts, emitEvent: true /*force repaint*/});
        return;
      }
    }

    // Compute total weight using ratio and sample weight
    else if (isNotNilOrNaN(samplingRatioPct) && samplingRatioPct <= 100 && samplingRatioPct > 0
      && !sampleBatch.weight.computed && isNotNilOrNaN(samplingWeight) && samplingWeight >= 0) {
      if (batch.weight.computed || isNil(totalWeight)) {
        totalWeightControl.patchValue({
          computed: true,
          estimated: false,
          value: Math.round(samplingWeight * (100 / samplingRatioPct) * 100) / 100,
          methodId: MethodIds.CALCULATED
        }, opts);

        // Disable total weight control
        totalWeightControl.disable({...opts, emitEvent: true /*force repaint*/});
        return;
      }
    }

    // Nothing can be computed: enable all controls
    else {

      // Enable total weight (and remove computed value, if any)
      if (batch.weight.computed) {
        totalWeightControl.patchValue({
          value: null,
          computed: false,
          estimated: false
        }, opts);
      }
      totalWeightControl.enable(opts);

      if (sampleForm.enabled) {
        // Enable sampling ratio
        samplingRatioControl.enable({...opts, emitEvent: true/*force repaint*/});

        // Enable sampling weight (and remove computed value, if any)
        if (sampleBatch.weight.computed) {
          samplingWeightControl.patchValue({
            value: null,
            computed: false,
            estimated: false
          }, opts);
        }

        // If sampling weight is required
        if (opts && opts.requiredSampleWeight === true) {
          if (!samplingWeightControl.hasError('required')) {
            samplingWeightControl.setErrors({...samplingWeightControl.errors, required: true}, opts);
          }
        }

        // If sampling weight is NOT required
        else {
          samplingWeightControl.setErrors(null, opts);
        }
        samplingWeightControl.enable(opts);

      } else {
        samplingRatioControl.disable({...opts, emitEvent: true/*force repaint*/});
        samplingWeightControl.disable(opts);
      }
    }
  }

}
