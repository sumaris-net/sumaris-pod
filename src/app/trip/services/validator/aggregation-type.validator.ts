import {Injectable} from "@angular/core";
import {FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedFormArrayValidators, SharedValidators} from "../../../shared/validator/validators";
import {AggregationStrata, AggregationType} from "../model/extraction.model";
import {AppValidatorService} from "../../../core/services/validator/base.validator.class";

@Injectable()
export class AggregationTypeValidatorService extends AppValidatorService<AggregationType> {

  constructor(
    protected formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormGroup(data?: AggregationType): FormGroup {
    return this.formBuilder.group({
      __typename: ['AggregationTypeVO'],
      id: [data && data.id || null],
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
      recorderPerson: ['', SharedValidators.entity],
      stratum: this.getStratumArray(data),
    });
  }

  getStratumArray(data?: AggregationType): FormArray {
    return this.formBuilder.array(
      (data && data.stratum || []).map(this.getStrataFormGroup),
      SharedFormArrayValidators.requiredArrayMinLength(1)
    );
  }

  getStrataFormGroup(data?: AggregationStrata) {
    return this.formBuilder.group({
      __typename: ['AggregationStrataVO'],
      id: [null],
      label: ['default', Validators.required],
      timeColumnName: ['year', Validators.required],
      spaceColumnName: ['square', Validators.required],
      aggColumnName: [null, Validators.required],
      aggFunction: ['SUM', Validators.required],
      techColumnName: [null]
    });
  }
}
