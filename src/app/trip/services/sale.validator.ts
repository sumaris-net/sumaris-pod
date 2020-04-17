import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../shared/validator/validators";
import {DataRootEntityValidatorOptions, DataRootEntityValidatorService} from "./validator/base.validator";
import {toBoolean} from "../../shared/functions";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {Sale} from "./model/sale.model";

export interface SaleValidatorOptions extends DataRootEntityValidatorOptions {
  required?: boolean;
  withProgram?: boolean;
}

@Injectable()
export class SaleValidatorService<O extends SaleValidatorOptions = SaleValidatorOptions> extends DataRootEntityValidatorService<Sale, SaleValidatorOptions> {

  constructor(
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService) {
    super(formBuilder, settings);
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Sale, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);
    return this.formBuilder.group(
      this.getFormGroupConfig(data, opts),
      this.getFormGroupOptions(data, opts)
    );
  }


  getFormGroupConfig(data?: Sale, opts?: O): { [key: string]: any } {

    const formConfig = {
      __typename: [Sale.TYPENAME],
      id: [data && data.id || null],
      updateDate: [data && data.updateDate || null],
      creationDate: [data && data.creationDate || null],
      vesselSnapshot: [data && data.vesselSnapshot || null, !opts.required ? SharedValidators.entity : Validators.compose([Validators.required, SharedValidators.entity])],
      saleType: [data && data.saleType || null, !opts.required ? SharedValidators.entity : Validators.compose([Validators.required, SharedValidators.entity])],
      startDateTime: [data && data.startDateTime || null],
      endDateTime: [data && data.endDateTime || null],
      saleLocation: [data && data.saleLocation || null, SharedValidators.entity],
      comments: [data && data.comments || null, Validators.maxLength(2000)]
    };

    return formConfig;
  }

  getFormGroupOptions(data?: Sale, opts?: SaleValidatorOptions): { [key: string]: any } {
    return {
      validator: Validators.compose([
        SharedValidators.requiredIf('saleLocation', 'saleType'),
        SharedValidators.requiredIf('startDateTime', 'saleType')
      ])
    };
  }

  updateFormGroup(form: FormGroup, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    if (opts && opts.required === true) {
      form.controls['vesselSnapshot'].setValidators(Validators.compose([Validators.required, SharedValidators.entity]));
      form.controls['saleType'].setValidators(Validators.compose([Validators.required, SharedValidators.entity]));
    }
    else {
      form.controls['vesselSnapshot'].setValidators(SharedValidators.entity);
      form.controls['saleType'].setValidators(SharedValidators.entity);
    }

    form.updateValueAndValidity({emitEvent: false});
    return form;
  }

  /* -- fill options defaults -- */

  protected fillDefaultOptions(opts?: O): O {
    opts = opts || {} as O;

    opts.isOnFieldMode = toBoolean(opts.isOnFieldMode, this.settings.isUsageMode('FIELD'));

    opts.required = toBoolean(opts.required, true);

    opts.withProgram = toBoolean(opts.withProgram, false);

    return opts;
  }
}
