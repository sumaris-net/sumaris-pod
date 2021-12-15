import {Injectable} from '@angular/core';
import {DataEntityValidatorOptions, DataEntityValidatorService} from '../../../data/services/validator/data-entity.validator';
import {FishingArea} from '../model/fishing-area.model';
import {AbstractControlOptions, FormBuilder, FormGroup, ValidatorFn, Validators} from '@angular/forms';
import {LocalSettingsService, SharedFormGroupValidators, SharedValidators, toBoolean} from '@sumaris-net/ngx-components';

export interface FishingAreaValidatorOptions extends DataEntityValidatorOptions {
  required?: boolean;
}

@Injectable({providedIn: 'root'})
export class FishingAreaValidatorService<O extends FishingAreaValidatorOptions = FishingAreaValidatorOptions>
  extends DataEntityValidatorService<FishingArea, FishingAreaValidatorOptions> {

  constructor(
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService) {
    super(formBuilder, settings);
  }

  getFormGroupConfig(data?: FishingArea, opts?: FishingAreaValidatorOptions): { [p: string]: any } {
    return Object.assign(super.getFormGroupConfig(data, opts), {
      __typename: [FishingArea.TYPENAME],
      location: [data && data.location || null, this.getLocationValidators(opts)],
      distanceToCoastGradient: [data && data.distanceToCoastGradient || null, SharedValidators.entity],
      depthGradient: [data && data.depthGradient || null, SharedValidators.entity],
      nearbySpecificArea: [data && data.nearbySpecificArea || null, SharedValidators.entity]
    });
  }

  getFormGroupOptions(data?: FishingArea, opts?: FishingAreaValidatorOptions): AbstractControlOptions | null{
    return <AbstractControlOptions>{
      validator: [
        SharedFormGroupValidators.requiredIf('location', 'distanceToCoastGradient'),
        SharedFormGroupValidators.requiredIf('location', 'depthGradient'),
        SharedFormGroupValidators.requiredIf('location', 'nearbySpecificArea')
      ]
    };
  }

  updateFormGroup(formGroup: FormGroup, opts?: FishingAreaValidatorOptions) {
    opts = this.fillDefaultOptions(opts);

    const locationValidators = this.getLocationValidators(opts);
    formGroup.get('location').setValidators(locationValidators);

    formGroup.updateValueAndValidity({emitEvent: false});
  }

  getLocationValidators(opts?: FishingAreaValidatorOptions): ValidatorFn {
    return (opts && opts.required) ? Validators.compose([Validators.required, SharedValidators.entity]) : SharedValidators.entity;
  }

  protected fillDefaultOptions(opts?: FishingAreaValidatorOptions): FishingAreaValidatorOptions {
    opts = super.fillDefaultOptions(opts);

    opts.required = toBoolean(opts.required, true);

    return opts;
  }
}
