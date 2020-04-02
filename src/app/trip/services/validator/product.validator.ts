import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormGroup, Validators, FormBuilder} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {OperationGroup} from "../model/trip.model";
import {AcquisitionLevelCodes, Program} from "../../../referential/services/model";
import {toBoolean} from "../../../shared/functions";
import {DataEntityValidatorOptions, DataEntityValidatorService} from "./base.validator";
import {MeasurementsValidatorService} from "../measurement.validator";
import {Product} from "../model/product.model";

export interface ProductValidatorOptions extends DataEntityValidatorOptions {
  program?: Program;
  withMeasurements?: boolean;
}

@Injectable()
export class ProductValidatorService<O extends ProductValidatorOptions = ProductValidatorOptions>
  extends DataEntityValidatorService<Product, O> implements ValidatorService {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService,
    protected measurementsValidatorService: MeasurementsValidatorService
  ) {
    super(formBuilder, settings);
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Product, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    const form = super.getFormGroup(data, opts);

    // Add measurement form
    // if (opts.withMeasurements) {
    //   const pmfms = (opts.program && opts.program.strategies[0] && opts.program.strategies[0].pmfmStrategies || [])
    //     .filter(p => p.acquisitionLevel === AcquisitionLevelCodes.OPERATION);
    //   form.addControl('measurements', this.measurementsValidatorService.getFormGroup(data && data.measurements, {
    //     isOnFieldMode: opts.isOnFieldMode,
    //     pmfms
    //   }));
    // }

    return form;
  }

  getFormGroupConfig(data?: Product, opts?: O): { [key: string]: any } {

    const formConfig = Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        __typename: [OperationGroup.TYPENAME],
        parent: [data && data.parent || null, Validators.compose([Validators.required, SharedValidators.entity])],
        rankOrder: [data && data.rankOrder || null],
        taxonGroup: [data && data.taxonGroup || null, Validators.compose([Validators.required, SharedValidators.entity])],
        weight: [data && data.weight || null, SharedValidators.double],
        weightMethod: [data && data.weightMethod || null],
        individualCount: [data && data.individualCount || null, SharedValidators.integer],

        // comments: [data && data.comments || null, Validators.maxLength(2000)]
      });

    return formConfig;
  }

  getFormGroupOptions(data?: Product, opts?: O): { [p: string]: any } {
    return {
      validator: [
        SharedValidators.requiredIfEmpty('weight', 'individualCount'),
        SharedValidators.requiredIfEmpty('individualCount', 'weight')
      ]
    };
  }

  /* -- protected methods -- */

  protected fillDefaultOptions(opts?: O): O {
    opts = super.fillDefaultOptions(opts);

    opts.withMeasurements = toBoolean(opts.withMeasurements, toBoolean(!!opts.program, false));
    //console.debug("[operation-validator] Ope Validator will use options:", opts);

    return opts;
  }

}
