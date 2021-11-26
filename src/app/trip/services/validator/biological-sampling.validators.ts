import {FormGroup} from "@angular/forms";
import {DenormalizedPmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {Subscription} from "rxjs";
import { isNilOrBlank, isNotNil, isNotNilOrBlank } from '@sumaris-net/ngx-components';
import {ObjectMap} from "@sumaris-net/ngx-components";
import {PmfmIds} from '../../../referential/services/model/model.enum';

export class BiologicalSamplingValidators {


  static addSampleValidators(form: FormGroup, pmfms: DenormalizedPmfmStrategy[],
                             pmfmGroups: ObjectMap<number[]>,
                             opts?: { markForCheck: () => void }): Subscription {
    if (!form) {
      console.warn("Argument 'form' required");
      return null;
    }
    if (!pmfmGroups) {
      console.warn("Argument 'pmfmGroups' required");
      return null;
    }

    form.setValidators( (control) => {
      const measValues = form.controls.measurementValues.value;

      const tagId = measValues[PmfmIds.TAG_ID];
      if (isNotNilOrBlank(tagId) && tagId.length !== 4) {
        return { tagIdLength: 'TRIP.SAMPLE.ERROR.PARAMETERS.EXACT_TAG_ID_LENGTH' };
      }

      const hasWeight = (pmfmGroups.WEIGHT || []).findIndex(pmfmId => isNotNil(measValues[pmfmId.toString()])) !== -1;
      const hasLengthSize = (pmfmGroups.LENGTH || []).findIndex(pmfmId => isNotNil(measValues[pmfmId.toString()])) !== -1;
      if (!hasWeight && !hasLengthSize){
        return { missingWeightOrSize: 'TRIP.SAMPLE.ERROR.PARAMETERS.WEIGHT_OR_LENGTH' };
      }
    });

  }
}
