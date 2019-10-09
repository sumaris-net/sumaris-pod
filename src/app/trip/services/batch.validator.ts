import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AbstractControl, FormArray, FormBuilder, FormGroup, ValidationErrors, Validators} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";
import {Batch, BatchUtils, BatchWeight} from "./model/batch.model";
import {map, takeUntil, tap} from "rxjs/operators";
import {isNil, isNotNilOrNaN} from "../../shared/functions";
import {MethodIds} from "../../referential/services/model";
import {Observable, Subject} from "rxjs";

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

  setAsyncValidators(form: FormGroup, opts?: {
    withSampleBatch: boolean
  }) {

    if (opts && opts.withSampleBatch) {
      // Sampling ratio: should be a percentage
      form.get("samplingRatio").setValidators(
        Validators.compose([Validators.min(0), Validators.max(100), SharedValidators.double({maxDecimals: 2})])
      );

      form.setAsyncValidators([
        this.computeSamplingWeight
      ]);

    }
  }

  /**
   * Computing weight, samplingWeight or sampling ratio
   * @param form
   */
  computeSamplingWeight(form: AbstractControl): Observable<ValidationErrors | null> {
    if (!form.dirty) return Observable.empty();

    const $stop = new Subject();

    return Observable.timer(500)
      .pipe(
        takeUntil($stop),
        map(() => {
          const batch = form.value;
          if (!batch.weight) return null;

          const sampleBatch = BatchUtils.getSamplingChild(batch);
          if (!sampleBatch) return null;

          $stop.next(true);
          console.debug('[batch-validator] Computing calculated fields...');

          //const errorControls = [];
          const sampleForm = (form.get('children') as FormArray).at(0) as FormGroup;
          const totalWeight = batch.weight.value;
          const samplingWeight = sampleBatch.weight.value;
          const samplingRatioPct = sampleBatch.samplingRatio;

          // Compute samplingRatio, using weights
          if (!batch.weight.calculated && isNotNilOrNaN(totalWeight) && totalWeight > 0
            && !sampleBatch.weight.calculated && isNotNilOrNaN(samplingWeight) && samplingWeight > 0) {

            // Sampling weight must be under total weight
            if (samplingWeight >= totalWeight) {
              const samplingWeightControl = sampleForm.get('weight.value');
              if (!samplingWeightControl.hasError('max') || samplingWeightControl.errors['max'] !== totalWeight) {
                samplingWeightControl.markAsPending({onlySelf: true, emitEvent: true}); //{onlySelf: true, emitEvent: false});
                samplingWeightControl.markAsTouched({onlySelf: true});
                samplingWeightControl.setErrors(
                  Object.assign(samplingWeightControl.errors || {}, { max: totalWeight }));
              }
              return { max: totalWeight } as ValidationErrors;
            }

            // Update sampling ratio
            const calculatedSamplingRatioPct = Math.round(100 * samplingWeight / totalWeight);
            if (samplingRatioPct !== calculatedSamplingRatioPct) {
              sampleForm.patchValue({
                samplingRatio: calculatedSamplingRatioPct,
                samplingRatioText: `${totalWeight}/${samplingWeight}`
              }, {emitEvent: true});
            }

            // Disable ratio control
            const samplingRatioControl = sampleForm.get('samplingRatio');
            if (samplingRatioControl.enabled) samplingRatioControl.disable({onlySelf: true, emitEvent: true});
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
                });
              }

              // Disable sampling weight control
              const samplingWeightControl = sampleForm.get('weight.value');
              if (samplingWeightControl.enabled) samplingWeightControl.disable({onlySelf: true, emitEvent: true});
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
              }, {emitEvent: true});

              // Disable total weight control
              const totalWeightControl = form.get('weight.value');
              totalWeightControl.disable({onlySelf: true, emitEvent: true});
              return;
            }
          }

          else if (isNotNilOrNaN(samplingWeight)) {
            const samplingRatioControl = sampleForm.get('samplingRatio');
            if (samplingRatioControl.disabled && sampleForm.enabled) {
              samplingRatioControl.enable({onlySelf: true, emitEvent: true});
            }
          }

          else {
            const samplingRatioControl = sampleForm.get('samplingRatio');
            const samplingWeightControl = sampleForm.get('weight.value');
            if (sampleForm.enabled) {
              if (samplingRatioControl.disabled) {
                samplingRatioControl.enable({onlySelf: true, emitEvent: true});
              }
              if (samplingWeightControl.disabled) {
                samplingWeightControl.patchValue({
                  calculated: false,
                  estimated: false
                });
                samplingWeightControl.setErrors(null, {emitEvent: true});
                samplingWeightControl.enable({onlySelf: true, emitEvent: true});
              }
            }
            else {
              if (samplingRatioControl.enabled) {
                samplingRatioControl.disable({onlySelf: false});
              }
              if (samplingWeightControl.enabled) {
                samplingWeightControl.disable({onlySelf: false});
              }
            }
          }
        }),
        tap((errors: any) => {

          if (errors) {
            //setTimeout(() => {
              //form.updateValueAndValidity();
             // AppFormUtils.markAsTouched(form);
            //});
          }
          return errors;
        })
      );

  }
}
