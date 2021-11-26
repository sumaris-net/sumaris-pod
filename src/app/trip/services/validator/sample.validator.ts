import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {AppValidatorService, SharedFormGroupValidators, SharedValidators} from '@sumaris-net/ngx-components';
import {Sample} from "../model/sample.model";
import {isNotNil, toNumber} from "@sumaris-net/ngx-components";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {Subscription} from "rxjs";
import {PmfmUtils} from "../../../referential/services/model/pmfm.model";
import {ParameterLabelGroups} from "../../../referential/services/model/model.enum";
import {TranslateService} from '@ngx-translate/core';

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
    if (errorKey === 'missingWeightOrSize') return this.translate.instant(errorContent);
    if (errorKey === 'missingDressing') return this.translate.instant(errorContent);
    return super.getI18nError(errorKey, errorContent);
  }

}


export const SAMPLE_VALIDATOR_I18N_ERROR_KEYS = {
  missingWeightOrSize: 'TRIP.SAMPLE.ERROR.PARAMETERS.WEIGHT_OR_LENGTH',
  missingDressing: 'TRIP.SAMPLE.ERROR.PARAMETERS.DRESSING',
  tagIdLength: 'TRIP.SAMPLE.ERROR.PARAMETERS.EXACT_TAG_ID_LENGTH'
}
