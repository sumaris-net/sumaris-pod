import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, ValidationErrors, Validators} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";
import {Batch, BatchUtils, BatchWeight} from "./model/batch.model";
import {debounceTime, filter, map, tap} from "rxjs/operators";
import {isNil, isNotNilOrNaN, toBoolean, toNumber} from "../../shared/functions";
import {MethodIds} from "../../referential/services/model";
import {Subject, Subscription} from "rxjs";

@Injectable()
export class BatchValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Batch, opts?: {
    withWeight?: boolean
  }): FormGroup {
    const form = this.formBuilder.group(this.getFormGroupConfig(data));

    // Add weight sub form
    if (opts && opts.withWeight) {
      form.addControl('weight', this.getWeightFormGroup(data && data.weight));
    }

    return form;
  }

  protected getFormGroupConfig(data?: Batch): { [key: string]: any } {
    return {
      __typename: [Batch.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      rankOrder: [toNumber(data && data.rankOrder, null), Validators.required],
      label: [data && data.label || null, Validators.required],
      individualCount: [toNumber(data && data.individualCount, null), Validators.compose([Validators.min(0), SharedValidators.integer])],
      samplingRatio: [toNumber(data && data.samplingRatio, null), SharedValidators.double()],
      samplingRatioText: [data && data.samplingRatioText || null],
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

  addSamplingFormValidators(form: FormGroup): Subscription {

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
        map(() => BatchValidatorService.computeSamplingWeight(form, {emitEvent: false, onlySelf: false}))
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
  static computeSamplingWeight(form: FormGroup, opts?: {emitEvent?: boolean; onlySelf?: boolean; }): ValidationErrors | null {
    const sampleForm = form.get('children.0');

    const batch = form.value;
    if (!batch.weight) return null;

    const sampleBatch = BatchUtils.getSamplingChild(batch);
    if (!sampleBatch) return null;

    const totalWeight = batch.weight.value;
    const samplingWeight = sampleBatch.weight.value;
    const samplingRatioPct = sampleBatch.samplingRatio;
    console.table("[batch-validator]  Start computing: ", totalWeight, samplingRatioPct, samplingWeight);

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
        return {max: totalWeight} as ValidationErrors;
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
          weight: {
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
        samplingWeightValueControl.setErrors(null, opts);
        samplingWeightValueControl.enable(opts);

      } else {
        samplingRatioControl.disable({...opts, emitEvent: true/*force repaint*/});
        samplingWeightValueControl.disable(opts);
      }
    }
  }
}
