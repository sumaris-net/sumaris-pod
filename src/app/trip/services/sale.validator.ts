import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {Sale} from "./trip.model";
import {SharedValidators} from "../../shared/validator/validators";

@Injectable()
export class SaleValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Sale): FormGroup {

    return this.formBuilder.group({
      'id': [''],
      'updateDate': [''],
      'creationDate': [''],
      'vesselFeatures': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'saleType': ['', Validators.compose([Validators.required, SharedValidators.entity])],
      'startDateTime': [''],
      'endDateTime': [''],
      'saleLocation': ['', SharedValidators.entity],
      'comments': ['', Validators.maxLength(2000)],
      'dirty': ['']
    }, {
      validator: Validators.compose([
        SharedValidators.requiredIf('saleLocation', 'saleType'),
        SharedValidators.requiredIf('startDateTime', 'saleType')
      ])
    });
  }

  setRequired(form: FormGroup, required: boolean) {
    if (required) {
      form.controls['vesselFeatures'].setValidators(Validators.compose([Validators.required, SharedValidators.entity]));
      form.controls['saleType'].setValidators(Validators.compose([Validators.required, SharedValidators.entity]));
    }
    else {
      form.controls['vesselFeatures'].setValidators(SharedValidators.entity);
      form.controls['saleType'].setValidators(SharedValidators.entity);
    }

    form.updateValueAndValidity({emitEvent: false});

  }

}
