import {FormGroup} from "@angular/forms";
import {DenormalizedPmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {Subscription} from "rxjs";
import {isNotNil} from "@sumaris-net/ngx-components";
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
      const formGroup = control as FormGroup;
      const measValues = formGroup.get('measurementValues').value;
      // ensure dressing pmfm exist
      const tagIdIndex = (pmfmGroups.TAG_ID || []).findIndex(pmfmId => pmfmId === PmfmIds.DRESSING);
      let hasTagId
      if (tagIdIndex !== -1) {
        hasTagId = measValues[pmfmGroups.TAG_ID[tagIdIndex].toString()] && (measValues[pmfmGroups.TAG_ID[tagIdIndex].toString()] !== "");
      } else {
        hasTagId = false;
      }
      const hasWeight = (pmfmGroups.WEIGHT || []).findIndex(pmfmId => isNotNil(measValues[pmfmId.toString()])) !== -1;
      const hasLengthSize = (pmfmGroups.LENGTH || []).findIndex(pmfmId => isNotNil(measValues[pmfmId.toString()])) !== -1;
      let exactTagIdLength;
      if (measValues[PmfmIds.TAG_ID]) {
        exactTagIdLength = measValues[PmfmIds.TAG_ID]?.length === 0 || measValues[PmfmIds.TAG_ID]?.length === 4;
      } else {
        exactTagIdLength = true;
      }

      if (!exactTagIdLength) {
        return { tagIdLength: 'TRIP.SAMPLE.ERROR.PARAMETERS.EXACT_TAG_ID_LENGTH' };
      }
      if (!hasTagId) {
        return { missingDressing: 'TRIP.SAMPLE.ERROR.PARAMETERS.DRESSING' };
      }
      if (!hasWeight && !hasLengthSize){
        return { missingWeightOrSize: 'TRIP.SAMPLE.ERROR.PARAMETERS.WEIGHT_OR_LENGTH' };
      }
    });

  }
}
