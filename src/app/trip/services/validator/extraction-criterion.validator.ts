import {Injectable} from "@angular/core";
import {FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ValidatorService} from "angular4-material-table";
import {SharedValidators} from "../../../shared/validator/validators";
import {AggregationStrata, AggregationType, ExtractionFilterCriterion, ExtractionType} from "../extraction.model";
import {toInt} from "../../../shared/functions";

@Injectable()
export class ExtractionCriteriaValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: ExtractionType): FormGroup {
    return this.formBuilder.group({ });
  }

  getCriterionFormGroup(criterion?: ExtractionFilterCriterion, sheetName?: string) {
    return this.formBuilder.group({
      name: [criterion && criterion.name || null],
      operator: [criterion && criterion.operator || '=', Validators.required],
      value: [criterion && criterion.value || null],
      endValue: [criterion && criterion.endValue || null],
      sheetName: [criterion && criterion.sheetName || sheetName]
    });
  }
}
