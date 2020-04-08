import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Referential} from "../model";
import {Parameter} from "../model/pmfm.model";
import {ReferentialValidatorService} from "../referential.validator";

@Injectable()
export class ParameterValidatorService extends ReferentialValidatorService<Parameter> {

  constructor(
    protected formBuilder: FormBuilder
  ) {
    super(formBuilder);
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroupConfig(data?: Parameter, opts?: { withDescription?: boolean; withComments?: boolean }): { [p: string]: any } {
    const config = super.getFormGroupConfig(data, opts);
    return {
      ...config,
      type : [data && data.type || null, Validators.required],
      qualitativeValues: this.formBuilder.array(
        (data && data.qualitativeValues || []).map(item => this.getQualitativeValuesFormGroup(item))
      )
    } ;
  }

  getQualitativeValuesFormGroup(data?: Referential): FormGroup {
    return this.formBuilder.group(super.getFormGroupConfig(data as Parameter));
  }
}
