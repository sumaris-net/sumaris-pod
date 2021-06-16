import {Injectable} from "@angular/core";
import {AbstractControlOptions, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedFormGroupValidators, SharedValidators} from "@sumaris-net/ngx-components";
import {toBoolean} from "@sumaris-net/ngx-components";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {Sale} from "../model/sale.model";
import {
  DataRootEntityValidatorOptions,
  DataRootEntityValidatorService
} from "../../../data/services/validator/root-data-entity.validator";

export interface SaleValidatorOptions extends DataRootEntityValidatorOptions {
  required?: boolean;
  withProgram?: boolean;
}

@Injectable({providedIn: 'root'})
export class SaleValidatorService<O extends SaleValidatorOptions = SaleValidatorOptions>
  extends DataRootEntityValidatorService<Sale, SaleValidatorOptions> {

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
      id: [data && data.id || null],
      updateDate: [data && data.updateDate || null],
      creationDate: [data && data.creationDate || null],
      vesselSnapshot: [data && data.vesselSnapshot || null, !opts.required ? SharedValidators.entity : Validators.compose([Validators.required, SharedValidators.entity])],
      saleType: [data && data.saleType || null, !opts.required ? SharedValidators.entity : Validators.compose([Validators.required, SharedValidators.entity])],
      startDateTime: [data && data.startDateTime || null],
      endDateTime: [data && data.endDateTime || null],
      saleLocation: [data && data.saleLocation || null, SharedValidators.entity],
      comments: [data && data.comments || null, Validators.maxLength(2000)]
    };

    return formConfig;
  }

  getFormGroupOptions(data?: Sale, opts?: SaleValidatorOptions): AbstractControlOptions {
    return <AbstractControlOptions>{
      validator: Validators.compose([
        SharedFormGroupValidators.requiredIf('saleLocation', 'saleType'),
        SharedFormGroupValidators.requiredIf('startDateTime', 'saleType')
      ])
    };
  }

  updateFormGroup(form: FormGroup, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    if (opts && opts.required === true) {
      form.controls['vesselSnapshot'].setValidators([Validators.required, SharedValidators.entity]);
      form.controls['saleType'].setValidators([Validators.required, SharedValidators.entity]);
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
