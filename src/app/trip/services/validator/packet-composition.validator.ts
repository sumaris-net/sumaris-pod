import {Injectable} from "@angular/core";
import {
  DataEntityValidatorOptions,
  DataEntityValidatorService
} from "../../../data/services/validator/data-entity.validator";
import {PacketComposition} from "../model/packet.model";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormBuilder, Validators} from "@angular/forms";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {SharedValidators} from "@sumaris-net/ngx-components";

@Injectable({providedIn: 'root'})
export class PacketCompositionValidatorService
  extends DataEntityValidatorService<PacketComposition> implements ValidatorService {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService
  ) {
    super(formBuilder, settings);
  }

  getFormGroupConfig(data?: PacketComposition, opts?: DataEntityValidatorOptions): { [p: string]: any } {
    return Object.assign(
        super.getFormGroupConfig(data, opts),
        {
          __typename: [PacketComposition.TYPENAME],
          rankOrder: [data && data.rankOrder || null],
          taxonGroup: [data && data.taxonGroup || null, Validators.compose([Validators.required, SharedValidators.entity])],
          weight: [data && data.weight || null, null],
          ratio1: [data && data.ratio1, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
          ratio2: [data && data.ratio2, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
          ratio3: [data && data.ratio3, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
          ratio4: [data && data.ratio4, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
          ratio5: [data && data.ratio5, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
          ratio6: [data && data.ratio6, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])]
        });
  }
}
