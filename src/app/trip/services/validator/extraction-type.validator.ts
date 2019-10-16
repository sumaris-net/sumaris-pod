import {Injectable} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ValidatorService} from "angular4-material-table";
import {SharedValidators} from "../../../shared/validator/validators";

@Injectable()
export class ExtractionTypeValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: any): FormGroup {
    return this.formBuilder.group({
      __typename: ['ExtractionTypeVO'],
      id: [''],
      updateDate: [''],
      creationDate: [''],
      category: [''],
      label: ['', Validators.required],
      name: ['', Validators.required],
      description: ['', Validators.maxLength(255)],
      comments: ['', Validators.maxLength(2000)],
      statusId: ['', Validators.required],
      isSpatial: [''],
      recorderDepartment: ['', SharedValidators.entity],
      recorderPerson: ['', SharedValidators.entity]
    });
  }

}
