import { Injectable } from "@angular/core";
import { ValidatorService } from "angular4-material-table";
import { FormGroup, Validators, FormBuilder, AbstractControl } from "@angular/forms";
import { Operation, PmfmStrategy } from "../../services/model";
import { PositionValidatorService } from "../../position/validator/validators";

@Injectable()
export class MeasurementsValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  getRowValidator(options?: any): FormGroup {
    return this.getFormGroup(options && options.pmfms || []);
  }

  getFormGroup(pmfms: PmfmStrategy[]): FormGroup {
    const controlConfig: any = {};
    pmfms.forEach(pmfm => {
      controlConfig[pmfm.id] = pmfm.isMandatory ? ['', Validators.required] : [''];
    });

    return this.formBuilder.group(controlConfig);
  }

  updateFormGroup(form: FormGroup, pmfms: PmfmStrategy[]) {
    let controlNamesToRemove: any = {};
    for (let controlName in form.controls) {
      controlNamesToRemove[controlName] = true;
    }
    pmfms.forEach(pmfm => {
      const controlName = pmfm.id.toString();
      let formControl: AbstractControl = form.get(controlName);
      if (!formControl) {
        formControl = this.formBuilder.control('', pmfm.isMandatory ? Validators.required : undefined);
        form.addControl(controlName, formControl);
      }
      controlNamesToRemove[controlName] = false;
    });
    for (let controlName in controlNamesToRemove) {
      if (controlNamesToRemove[controlName]) {
        form.removeControl(controlName);
      }
    }
  }
}
