import {Injectable} from "@angular/core";
import {DataEntityValidatorOptions} from "./base.validator";
import {FormArray, FormBuilder, FormControl, FormGroup, Validators} from "@angular/forms";
import {toBoolean} from "../../../shared/functions";
import {SharedFormArrayValidators, SharedValidators} from "../../../shared/validator/validators";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {VesselActivity} from "../model/aggregated-landing.model";
import {ValidatorService} from "angular4-material-table";
import {Metier} from "../../../referential/services/model/taxon.model";

export interface VesselActivityValidatorOptions extends DataEntityValidatorOptions {
  required?: boolean;
}

@Injectable()
export class VesselActivityValidatorService<T extends VesselActivity = VesselActivity, O extends VesselActivityValidatorOptions = VesselActivityValidatorOptions>
  implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService) {
  }

  getRowValidator(opts?: O) {
    return this.getFormGroup();
  }

  getFormGroup(data?: T, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    return this.formBuilder.group(
      this.getFormGroupConfig(data, opts),
      this.getFormGroupOptions(data, opts)
    );
  }

  getFormGroupConfig(data?: T, opts?: O): { [p: string]: any } {
    opts = this.fillDefaultOptions(opts);

    return {
      __typename: [VesselActivity.TYPENAME],
      date: [data && data.date, Validators.compose([Validators.required, SharedValidators.validDate])],
      rankOrder: [data && data.rankOrder, Validators.compose([Validators.required, SharedValidators.integer])],
      comments: [data && data.comments, Validators.maxLength(2000)],
      metiers: this.getMetiersFormArray(data),
      measurementValues: [data && data.measurementValues || {}],
      tripId: [data && data.tripId]
    };
  }

  getFormGroupOptions(data?: T, opts?: O): {
    [key: string]: any;
  } {
    return {};
  }

  protected fillDefaultOptions(opts?: O): O {
    opts = opts || {} as O;

    opts.required = toBoolean(opts.required, true);

    return opts;
  }


  private getMetiersFormArray(data?: VesselActivity) : FormArray {
    return this.formBuilder.array(
      (data && data.metiers || []).map(metier => this.getMetierFormControl(metier)),
      SharedFormArrayValidators.requiredArrayMinLength(1)
    )
  }

  private getMetierFormControl(data?: Metier): FormControl {
    return this.formBuilder.control(data || null, [Validators.required, SharedValidators.entity]);
  }
}
