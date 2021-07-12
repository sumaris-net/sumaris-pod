import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "@sumaris-net/ngx-components";
import {toNumber} from "@sumaris-net/ngx-components";
import {Batch} from "../model/batch.model";
import {SubBatch} from "../model/subbatch.model";

@Injectable({providedIn: 'root'})
export class SubBatchValidatorService implements ValidatorService {

  constructor(
    private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: SubBatch, opts?: {
    rankOrderRequired?: boolean;
  }): FormGroup {
    const rankOrder = toNumber(data && data.rankOrder, null);
    return this.formBuilder.group({
      __typename: [Batch.TYPENAME],
      id: [toNumber(data && data.id, null)],
      updateDate: [data && data.updateDate || null],
      rankOrder: !opts || opts.rankOrderRequired !== false ? [rankOrder, Validators.required] : [rankOrder],
      label: [data && data.label || null],
      individualCount: [toNumber(data && data.individualCount, null), Validators.compose([Validators.min(1), SharedValidators.integer])],
      samplingRatio: [toNumber(data && data.samplingRatio, null), SharedValidators.empty], // Make no sense to have sampling ratio
      samplingRatioText: [data && data.samplingRatioText || null, SharedValidators.empty], // Make no sense to have sampling ratio
      taxonGroup: [data && data.taxonGroup || null, SharedValidators.entity],
      taxonName: [data && data.taxonName || null, SharedValidators.entity],
      comments: [data && data.comments || null],
      parent: [data && data.parent || null, SharedValidators.object],
      measurementValues: this.formBuilder.group({}),

      // Specific for SubBatch
      parentGroup: [data && data.parentGroup || null, Validators.compose([Validators.required, SharedValidators.object])]
    });
  }
}
