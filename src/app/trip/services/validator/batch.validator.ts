import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, ValidationErrors, Validators} from "@angular/forms";
import {SharedValidators} from "@sumaris-net/ngx-components";
import {Batch, BatchUtils, BatchWeight} from "../model/batch.model";
import {debounceTime, filter, map, tap} from "rxjs/operators";
import {isNil, isNotNilOrNaN, toBoolean, toNumber} from "@sumaris-net/ngx-components";
import {MethodIds} from "../../../referential/services/model/model.enum";
import {Subject, Subscription} from "rxjs";

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
  }): FormGroup {
    const form = this.formBuilder.group(this.getFormGroupConfig(data, {...opts}));

    // Add weight sub form
    if (opts && opts.withWeight) {
      form.addControl('weight', this.getWeightFormGroup(data && data.weight));
    }

    return form;
  }

  protected getFormGroupConfig(data?: T,  opts?: {
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

  addSamplingFormValidators(form: FormGroup, opts?: {
    requiredSampleWeight?: boolean;
  }): Subscription {

    // Sampling ratio: should be a percentage
    form.get("samplingRatio").setValidators(
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
        map(() => BatchValidatorService.computeSamplingWeight(form, { ...opts, emitEvent: false, onlySelf: false}))
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

  /**
   * Computing weight, samplingWeight or sampling ratio
   * @param form
   * @param opts
   */
  static computeSamplingWeight(form: FormGroup, opts?: {
    requiredSampleWeight?: boolean;
    emitEvent?: boolean;
    onlySelf?: boolean;
  }): ValidationErrors | null {
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
          samplingWeightValueControl.setErrors({...samplingWeightValueControl.errors, max: {max: totalWeight} }, opts);
        }
        return {max: {max: totalWeight}} as ValidationErrors;
      }
      else {
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
}
