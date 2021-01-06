import {FormGroup, ValidationErrors, Validators} from "@angular/forms";
import {Subject, Subscription} from "rxjs";
import {debounceTime, filter, map, startWith, tap} from "rxjs/operators";
import {PmfmIds} from "../../../referential/services/model/model.enum";
import {AppFormUtils} from "../../../core/form/form.utils";
import {isNotNilOrBlank} from "../../../shared/functions";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {SharedValidators} from "../../../shared/validator/validators";

export class AuctionControlValidators {

  static addSampleValidators(form: FormGroup, pmfms: PmfmStrategy[],
                             opts?: { markForCheck: () => void }): Subscription {

    // Label: remove 'required', and add integer
    form.get('label').setValidators(Validators.pattern(/^[0-9]*$/));

    // Disable computed pmfms
    AppFormUtils.disableControls(form,
      pmfms
      .filter(p => p.isComputed)
      .map(p => 'measurementValues.' + p.pmfmId), {onlySelf: true, emitEvent: false});

    const $errors = new Subject<ValidationErrors | null>();
    form.setAsyncValidators((control) => $errors);

    let computing = false;
    const subscription = form.valueChanges
      .pipe(
        startWith<any, any>(form.value),
        filter(() => !computing),
        // Protected against loop
        tap(() => computing = true),
        debounceTime(250),
        map(() => AuctionControlValidators.computeAndValidate(form, pmfms, {...opts, emitEvent: false, onlySelf: false})),
        tap(errors => {
          computing = false;
          $errors.next(errors);
        })
      )
      .subscribe();

    // When unsubscribe, remove async validator
    subscription.add(() => {
      $errors.next(null);
      $errors.complete();
      form.clearAsyncValidators();
    });

    return subscription;
  }

  /**
   * Validate and compute
   * @param control
   */
  static computeAndValidate(form: FormGroup,
                            pmfms: PmfmStrategy[],
                            opts?: {
    emitEvent?: boolean;
    onlySelf?: boolean;
    markForCheck: () => void;
  }): ValidationErrors | null {

    console.debug("[auction-control-validator] Starting computation and validation...");
    let errors: any;

    // Read pmfms
    const weightPmfm = pmfms.find(p => p.label.endsWith('_WEIGHT') || p.label === 'SAMPLE_INDIV_COUNT');
    const indivCountPmfm = pmfms.find(p => p.pmfmId === PmfmIds.SAMPLE_INDIV_COUNT);

    // Get controls
    const outOfSizeWeightControl = form.get('measurementValues.' + PmfmIds.OUT_OF_SIZE_WEIGHT);
    const outOfSizeCountControl = form.get('measurementValues.' + PmfmIds.OUT_OF_SIZE_INDIV_COUNT);
    const outOfSizePctControl = form.get('measurementValues.' + PmfmIds.OUT_OF_SIZE_PCT);
    const parasitizedCountControl = form.get('measurementValues.' + PmfmIds.PARASITIZED_INDIV_COUNT);
    const dirtyCountControl = form.get('measurementValues.' + PmfmIds.DIRTY_INDIV_COUNT);

    // Get PMFM values
    const weight = weightPmfm ? +form.get('measurementValues.' + weightPmfm.pmfmId).value : undefined;
    const indivCount = indivCountPmfm ? +form.get('measurementValues.' + indivCountPmfm.pmfmId).value : undefined;
    const outOfSizeWeight = outOfSizeWeightControl ? +outOfSizeWeightControl.value : undefined;
    const outOfSizeCount = outOfSizeCountControl ? +outOfSizeCountControl.value : undefined;

    // Out of size: compute percentage
    if (outOfSizePctControl) {
      if (isNotNilOrBlank(weight) && isNotNilOrBlank(outOfSizeWeight)
        && outOfSizeWeight <= weight) {
        const pct = Math.trunc(10000 * outOfSizeWeight / weight) / 100;
        outOfSizePctControl.setValue(pct, opts);
      } else if (isNotNilOrBlank(indivCount) && isNotNilOrBlank(outOfSizeCount)
        && outOfSizeCount <= indivCount) {
        const pct = Math.trunc(10000 * outOfSizeCount / indivCount) / 100;
        outOfSizePctControl.setValue(pct, opts);
      } else {
        outOfSizePctControl.setValue(null, opts); // Reset
      }
    }

    // Out of size: check max
    if (outOfSizeWeightControl) {
      if (isNotNilOrBlank(outOfSizeWeight) && isNotNilOrBlank(weight) && outOfSizeWeight > weight) {
        const error = {max: {actual: outOfSizeWeight, max: weight}};
        outOfSizeWeightControl.setErrors(error, opts);
        errors = {...errors, ...error};
      }
      else {
        SharedValidators.clearError(outOfSizeWeightControl, 'max');
      }
    }
    if (outOfSizeCountControl) {
      if (isNotNilOrBlank(outOfSizeCount) && isNotNilOrBlank(indivCount) && outOfSizeCount > indivCount) {
        const error = {max: {actual: outOfSizeCount, max: indivCount}};
        outOfSizeCountControl.setErrors(error, opts);
        errors = {...errors, ...error};
      }
      else {
        SharedValidators.clearError(outOfSizeCountControl, 'max');
      }
    }

    // Parasitized: compute percentile
    const parasitizedCount = parasitizedCountControl ? +parasitizedCountControl.value : undefined;
    const parasitizedPctControl = form.get('measurementValues.' + PmfmIds.PARASITIZED_INDIV_PCT);
    // Compute out of size percentage
    if (parasitizedPctControl) {
      if (isNotNilOrBlank(indivCount) && isNotNilOrBlank(parasitizedCount)
        && parasitizedCount <= indivCount) {
        const pct = Math.trunc(10000 * parasitizedCount / indivCount) / 100;
        parasitizedPctControl.setValue(pct, opts);
      } else {
        parasitizedPctControl.setValue(null, opts); // Reset
      }
    }
    // Parasitized: check max
    if (parasitizedCountControl) {
      if (isNotNilOrBlank(parasitizedCount) && isNotNilOrBlank(indivCount) && parasitizedCount > indivCount) {
        const error = {max: {actual: parasitizedCount, max: indivCount}};
        parasitizedCountControl.setErrors(error, opts);
        errors = {...errors, ...error};
      }
      else {
        SharedValidators.clearError(parasitizedCountControl, 'max');
      }
    }

    // Dirty: compute percentile
    const dirtyCount = dirtyCountControl ? +dirtyCountControl.value : undefined;
    const dirtyPctControl = form.get('measurementValues.' + PmfmIds.DIRTY_INDIV_PCT);
    if (dirtyPctControl) {
      if (isNotNilOrBlank(indivCount) && isNotNilOrBlank(parasitizedCount)
        && dirtyCount <= indivCount) {
        const pct = Math.trunc(10000 * dirtyCount / indivCount) / 100;
        dirtyPctControl.setValue(pct, opts);
      } else {
        dirtyPctControl.setValue(null, opts); // Reset
      }
    }
    // Dirty: check max
    if (dirtyCountControl) {
      if (isNotNilOrBlank(dirtyCount) && isNotNilOrBlank(indivCount) && dirtyCount > indivCount) {
        const error = {max: {actual: dirtyCount, max: indivCount}};
        dirtyCountControl.setErrors(error, opts);
        errors = {...errors, ...error};
      }
      else {
        SharedValidators.clearError(dirtyCountControl, 'max');
      }
    }

    if (opts && opts.markForCheck) opts.markForCheck();
    return errors;
  }
}
