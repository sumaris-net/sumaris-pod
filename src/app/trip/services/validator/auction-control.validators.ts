import {FormGroup, ValidationErrors, Validators} from "@angular/forms";
import {Subject, Subscription} from "rxjs";
import {debounceTime, filter, map, startWith, tap} from "rxjs/operators";
import {PmfmIds} from "../../../referential/services/model/model.enum";
import {AppFormUtils} from "../../../core/form/form.utils";
import {isNotNilOrBlank} from "../../../shared/functions";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";

export class AuctionControlValidators {

  static addSampleValidators(form: FormGroup, pmfms: PmfmStrategy[],
                             opts?: { markForCheck: () => void }): Subscription {

    // Label: remove 'required', and add integer
    form.get('label').setValidators(Validators.pattern(/^[0-9]*$/));

    // Disable computed pmfms
    AppFormUtils.disableControls(form,
      pmfms
      .filter(p => p.isComputed)
      .map(p => 'measurementValues.' + p.pmfmId));

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
        map(() => AuctionControlValidators.computeAndValidate(form, pmfms, {emitEvent: false, onlySelf: false})),
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
                            opts?: { emitEvent?: boolean; onlySelf?: boolean; }): ValidationErrors | null {

    console.debug("[auction-control-validator] Starting computation and validation...");

    // Read pmfms
    const vivacityPmfm = pmfms.find(p => p.pmfmId === PmfmIds.VIVACITY);
    const weightPmfm = pmfms.find(p => p.label.endsWith('_WEIGHT'));

    // Get controls
    const outOfSizeControl = form.get('measurementValues.' + PmfmIds.OUT_OF_SIZE);
    const outOfSizePctControl = form.get('measurementValues.' + PmfmIds.OUT_OF_SIZE_PCT);

    // Read values
    const weight = weightPmfm ? +AppFormUtils.getControlFromPath(form, 'measurementValues.' + weightPmfm.pmfmId).value : undefined;
    const outOfSize = outOfSizeControl ? +outOfSizeControl.value : undefined;

    // Compute out of size percentage
    if (outOfSizePctControl) {
      if (isNotNilOrBlank(weight) && isNotNilOrBlank(outOfSize)
        && outOfSize <= weight) {
        const pct = Math.trunc(10000 * outOfSize / weight) / 100;
        outOfSizePctControl.setValue(pct, opts);
      } else {
        outOfSizePctControl.setValue(null, opts); // Reset
      }
    }

    // Check out of size max
    if (outOfSizeControl) {
      if (isNotNilOrBlank(outOfSize) && isNotNilOrBlank(weight) && outOfSize > weight) {
        const error = {max: {actual: outOfSize, max: weight}};
        outOfSizeControl.setErrors(error, opts);
        return error;
      }
    }

    return null;
  }
}
