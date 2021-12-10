import { Injectable } from '@angular/core';
import { ValidatorService } from '@e-is/ngx-material-table';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AppValidatorService, SharedFormGroupValidators, SharedValidators, toNumber } from '@sumaris-net/ngx-components';
import { Sample } from '../model/sample.model';
import { TranslateService } from '@ngx-translate/core';

@Injectable({providedIn: 'root'})
export class SampleValidatorService extends AppValidatorService implements ValidatorService {

  constructor(
    protected formBuilder: FormBuilder,
    protected translate: TranslateService) {
    super(formBuilder, translate);
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Sample): FormGroup {
    return this.formBuilder.group({
      __typename: [Sample.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      creationDate: [data && data.creationDate || null],
      rankOrder: [toNumber(data && data.rankOrder, null), Validators.required],
      label: [data && data.label || null, Validators.required],
      individualCount: [toNumber(data && data.individualCount, null), Validators.compose([Validators.min(0), SharedValidators.integer])],
      sampleDate: [data && data.sampleDate || null, Validators.required],
      taxonGroup: [data && data.taxonGroup || null, SharedValidators.entity],
      taxonName: [data && data.taxonName || null, SharedValidators.entity],
      matrixId: [toNumber(data && data.matrixId, null)],
      batchId: [toNumber(data && data.batchId, null)],
      size: [toNumber(data && data.size, null)],
      sizeUnit: [data && data.sizeUnit || null],
      comments: [data && data.comments || null],
      parent: [data && data.parent || null, SharedValidators.entity],
      measurementValues: this.formBuilder.group({}),
      children: this.formBuilder.array([])
    }, {
      validators: [
        SharedFormGroupValidators.requiredIfEmpty('taxonGroup', 'taxonName'),
        SharedFormGroupValidators.requiredIfEmpty('taxonName', 'taxonGroup')
      ]
    });
  }

  getI18nError(errorKey: string, errorContent?: any): any {
    if (SAMPLE_VALIDATOR_I18N_ERROR_KEYS[errorKey]) return this.translate.instant(SAMPLE_VALIDATOR_I18N_ERROR_KEYS[errorKey], errorContent);
    return super.getI18nError(errorKey, errorContent);
  }

}


export const SAMPLE_VALIDATOR_I18N_ERROR_KEYS = {
  missingWeightOrSize: 'TRIP.SAMPLE.ERROR.WEIGHT_OR_LENGTH_REQUIRED',
  tagIdLength: 'TRIP.SAMPLE.ERROR.INVALID_TAG_ID_LENGTH'
}
