import {Injectable} from "@angular/core";
import {AbstractControl, AsyncValidatorFn, FormBuilder, FormGroup} from "@angular/forms";
import {BatchValidatorService} from "../batch.validator";
import {EntityUtils} from "../../../core/services/model";

@Injectable()
export class SpeciesBatchValidatorService extends BatchValidatorService {

  constructor(
    protected formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: any): FormGroup {
    return this.formBuilder.group(
      Object.assign(
        this.getFormGroupConfig(),
        // Add children (with one sample batch)
        {
          children: this.formBuilder.array(
            [this.formBuilder.group(this.getFormGroupConfig())]
          )
        }),
      {
        asyncValidators: this.requiredTaxon()
      }
    );
  }

  protected requiredTaxon(): AsyncValidatorFn {
    const error = {requiredTaxon: true};
    return async (control: AbstractControl) => {
      if (control instanceof FormGroup) {
        const taxonGroup = control.get('taxonGroup');
        const taxonName = control.get('taxonName');
        if (EntityUtils.isEmpty(taxonGroup.value) && EntityUtils.isEmpty(taxonName.value)) {
          console.warn("Missing taxon in form !");
          return error;
        }
      }
    };
  }
}
