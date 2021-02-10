import {FormGroup} from "@angular/forms";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {Subscription} from "rxjs";
import {isNotNil} from "../../../shared/functions";
import {ObjectMap} from "../../../shared/types";

export class BiologicalSamplingValidators {


  static addSampleValidators(form: FormGroup, pmfms: PmfmStrategy[],
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
      const hasWeight = (pmfmGroups.WEIGHT || []).findIndex(pmfmId => isNotNil(measValues[pmfmId.toString()])) !== -1;
      const hasLengthSize = (pmfmGroups.LENGTH || []).findIndex(pmfmId => isNotNil(measValues[pmfmId.toString()])) !== -1;

      if (!hasWeight && !hasLengthSize){
        return { missingWeightOrSize: true };
      }
    });

  }
}
