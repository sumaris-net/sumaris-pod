import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, ValidationErrors, Validators} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";
import {Batch, BatchUtils, BatchWeight} from "./model/batch.model";
import {debounceTime, filter, map, tap} from "rxjs/operators";
import {isNil, isNotNilOrNaN} from "../../shared/functions";
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
    if (opts && opts.withWeight) {
      form.addControl('weight', this.getWeightFormGroup(data && data.weight));
    }

    return form;
  }

  protected getFormGroupConfig(data?: Batch): { [key: string]: any } {
    return {
      __typename: ['BatchVO'],
      id: [''],
      updateDate: [''],
      rankOrder: ['1', Validators.required],
      label: [data && data.label || ''],
      individualCount: ['', Validators.compose([Validators.min(0), SharedValidators.integer])],
      samplingRatio: ['',  SharedValidators.double()],
      samplingRatioText: [''],
      taxonGroup: ['', SharedValidators.entity],
      taxonName: ['', SharedValidators.entity],
      comments: [''],
      parent: ['', SharedValidators.entity],
      measurementValues: this.formBuilder.group({}),
      children: this.formBuilder.array([])
    };
  }

  getWeightFormGroup(data?: BatchWeight): FormGroup {
    return this.formBuilder.group({
      methodId: ['', SharedValidators.integer],
      estimated: [''],
      calculated: [''],
      value: ['', SharedValidators.double({maxDecimals: 2})]
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

    // When unscribe, remove async validator
    subscription.add(() => {
      $errors.next(null);
      $errors.complete();
      form.clearAsyncValidators();
    });

    return subscription;
  }

  /**
   * Computing weight, samplingWeight or sampling ratio
   * @param control
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

    const samplingWeightValueControl = sampleForm.get('weight.value');
    const samplingRatioControl = sampleForm.get('samplingRatio');

    // Compute samplingRatio, using weights
    if (!totalWeight.calculated && isNotNilOrNaN(totalWeight) && totalWeight > 0
      && !sampleBatch.weight.calculated && isNotNilOrNaN(samplingWeight) && samplingWeight > 0) {

      // Sampling weight must be under total weight
      if (samplingWeight >= totalWeight) {
        if (!samplingWeightValueControl.hasError('max') || samplingWeightValueControl.errors['max'] !== totalWeight) {
          samplingWeightValueControl.markAsPending({onlySelf: true, emitEvent: true}); //{onlySelf: true, emitEvent: false});
          samplingWeightValueControl.markAsTouched({onlySelf: true});
          samplingWeightValueControl.setErrors(
            Object.assign(samplingWeightValueControl.errors || {}, {max: totalWeight}), opts);
        }
        return {max: totalWeight} as ValidationErrors;
      }

      // Update sampling ratio
      const calculatedSamplingRatioPct = Math.round(100 * samplingWeight / totalWeight);
      if (samplingRatioPct !== calculatedSamplingRatioPct) {
        sampleForm.patchValue({
          samplingRatio: calculatedSamplingRatioPct,
          samplingRatioText: `${totalWeight}/${samplingWeight}`
        }, opts);
      }

      // Disable ratio control
      samplingRatioControl.disable(opts);
      return;
    }

    // Compute sample weight using ratio and total weight
    else if (isNotNilOrNaN(samplingRatioPct) && samplingRatioPct <= 100 && samplingRatioPct > 0
      && !batch.weight.calculated && isNotNilOrNaN(totalWeight) && totalWeight >= 0) {

      if (sampleBatch.weight.calculated || isNil(samplingWeight)) {
        const calculatedSamplingWeight = Math.round(totalWeight * samplingRatioPct) / 100;
        if (samplingWeight !== calculatedSamplingWeight) {
          sampleForm.patchValue({
            samplingRatioText: `${samplingRatioPct}%`,
            weight: {
              calculated: true,
              estimated: false,
              value: calculatedSamplingWeight,
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
      && !sampleBatch.weight.calculated && isNotNilOrNaN(samplingWeight) && samplingWeight >= 0) {
      if (batch.weight.calculated || isNil(totalWeight)) {
        form.patchValue({
          weight: {
            calculated: true,
            estimated: false,
            value: Math.round(samplingWeight / samplingRatioPct) * 100,
            methodId: MethodIds.CALCULATED
          }
        }, opts);

        // Disable total weight control
        form.get('weight.value').disable(opts);
        return;
      }
    } else if (isNotNilOrNaN(samplingWeight)) {
      if (samplingRatioControl.disabled && sampleForm.enabled) {
        samplingRatioControl.enable(opts);
      }
    } else {
      if (sampleForm.enabled) {
        samplingRatioControl.enable(opts);
        sampleForm.get('weight').patchValue({
          value: null,
          calculated: false,
          estimated: false
        }, opts);
        samplingWeightValueControl.setErrors(null, opts);
        samplingWeightValueControl.enable(opts);
      } else {
        samplingRatioControl.disable(opts);
        samplingWeightValueControl.disable(opts);
      }
    }
  }
}
