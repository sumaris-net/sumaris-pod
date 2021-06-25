import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {QualityFlagIds} from "../../../referential/services/model/model.enum";
import {VesselFeatures} from "../model/vessel.model";
import {SharedValidators} from "@sumaris-net/ngx-components";

@Injectable({providedIn: 'root'})
export class VesselFeaturesValidatorService implements ValidatorService {

  constructor(private formBuilder: FormBuilder) {
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: VesselFeatures): FormGroup {
    return this.formBuilder.group({
      __typename: ['VesselFeaturesVO'],
      id: [null],
      updateDate: [null],
      creationDate: [null],
      startDate: [null, Validators.required],
      endDate: [null],
      name: ['', Validators.required],
      exteriorMarking: ['', Validators.required],
      administrativePower: ['', Validators.compose([Validators.min(0), SharedValidators.integer])],
      lengthOverAll: ['', Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
      grossTonnageGrt: ['', Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
      grossTonnageGt: ['', Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
      basePortLocation: ['', Validators.compose([Validators.required, SharedValidators.entity])],
      comments: ['', Validators.maxLength(2000)],
      qualityFlagId: [data && data.qualityFlagId || QualityFlagIds.NOT_QUALIFIED]
    });
  }
}
