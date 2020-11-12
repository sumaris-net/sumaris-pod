import {Injectable} from "@angular/core";
import {FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedFormArrayValidators, SharedValidators} from "../../../shared/validator/validators";
import {AggregationStrata, AggregationType} from "../model/extraction.model";
import {AppValidatorService} from "../../../core/services/validator/base.validator.class";
import {toBoolean, toNumber} from "../../../shared/functions";

@Injectable({providedIn: 'root'})
export class AggregationTypeValidatorService extends AppValidatorService<AggregationType> {

  constructor(
    protected formBuilder: FormBuilder) {
    super(formBuilder);
  }

  getFormGroup(data?: AggregationType): FormGroup {
    return this.formBuilder.group({
      __typename: ['AggregationTypeVO'],
      id: [data && data.id || null],
      updateDate: [data && data.updateDate || null],
      creationDate: [data && data.creationDate || null],
      category: [data && data.category || null],
      label: [data && data.label || null, Validators.required],
      name: [data && data.name || null, Validators.required],
      description: [data && data.description || null, Validators.maxLength(255)],
      comments: [data && data.comments || null, Validators.maxLength(2000)],
      version: [data && data.version || null, Validators.maxLength(10)],
      documentation: [data && data.documentation || null, Validators.maxLength(10000)],
      statusId: [toNumber(data && data.statusId, null), Validators.required],
      isSpatial: [toBoolean(data && data.isSpatial, false)],
      recorderDepartment: [data && data.recorderDepartment || null, SharedValidators.entity],
      recorderPerson: [data && data.recorderPerson || null, SharedValidators.entity],
      stratum: this.getStratumArray(data),
    });
  }

  getStratumArray(data?: AggregationType): FormArray {
    return this.formBuilder.array(
      (data && data.stratum || []).map(this.getStrataFormGroup)
    );
  }

  getStrataFormGroup(data?: AggregationStrata) {
    return this.formBuilder.group({
      __typename: ['AggregationStrataVO'],
      id: [null],
      sheetName: [data && data.sheetName || null, Validators.required],
      timeColumnName: [data && data.timeColumnName || 'year', Validators.required],
      spatialColumnName: [data && data.spatialColumnName || 'square', Validators.required],
      aggColumnName: [data && data.aggColumnName || null, Validators.required],
      aggFunction: [data && data.aggFunction || 'SUM', Validators.required],
      techColumnName: [data && data.techColumnName || null]
    });
  }
}
