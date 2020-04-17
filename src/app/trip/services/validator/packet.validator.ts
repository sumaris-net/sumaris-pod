import {Injectable} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../../shared/validator/validators";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {DataEntityValidatorOptions, DataEntityValidatorService} from "./base.validator";
import {Packet, PacketComposition} from "../model/packet.model";
import {fragmentOnNonCompositeErrorMessage} from "graphql/validation/rules/FragmentsOnCompositeTypes";

export interface PacketValidatorOptions extends DataEntityValidatorOptions {
  withComposition?: boolean;
}

@Injectable()
export class PacketValidatorService<O extends PacketValidatorOptions = PacketValidatorOptions>
  extends DataEntityValidatorService<Packet, O> implements ValidatorService {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService
  ) {
    super(formBuilder, settings);
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroupConfig(data?: Packet, opts?: O): { [key: string]: any } {

    const formConfig = Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        __typename: [Packet.TYPENAME],
        parent: [data && data.parent || null, Validators.required],
        rankOrder: [data && data.rankOrder || null],
        number: [data && data.number || null, Validators.compose([Validators.required, SharedValidators.integer])],
        weight: [data && data.weight || null, Validators.compose([Validators.required, SharedValidators.double({maxDecimals: 2})])],
        sampledWeight1: [data && data.sampledWeight1, SharedValidators.double({maxDecimals: 2})],
        sampledWeight2: [data && data.sampledWeight2, SharedValidators.double({maxDecimals: 2})],
        sampledWeight3: [data && data.sampledWeight3, SharedValidators.double({maxDecimals: 2})],
        sampledWeight4: [data && data.sampledWeight4, SharedValidators.double({maxDecimals: 2})],
        sampledWeight5: [data && data.sampledWeight5, SharedValidators.double({maxDecimals: 2})],
        sampledWeight6: [data && data.sampledWeight6, SharedValidators.double({maxDecimals: 2})]
      });

    if (opts.withComposition) {
      formConfig.composition = this.getCompositionFormArray(data);
      formConfig.sampledRatio1 = [data && data.sampledRatio1 || null, Validators.max(100)];
      formConfig.sampledRatio2 = [data && data.sampledRatio2 || null, Validators.max(100)];
      formConfig.sampledRatio3 = [data && data.sampledRatio3 || null, Validators.max(100)];
      formConfig.sampledRatio4 = [data && data.sampledRatio4 || null, Validators.max(100)];
      formConfig.sampledRatio5 = [data && data.sampledRatio5 || null, Validators.max(100)];
      formConfig.sampledRatio6 = [data && data.sampledRatio6 || null, Validators.max(100)];
    } else {
      formConfig.composition = [data && data.composition || null, Validators.required];
    }

    return formConfig;
  }
  /* -- protected methods -- */

  getCompositionFormArray(data?: Packet) {
    return this.formBuilder.array(
      (data && data.composition || [null]).map(composition => this.getCompositionControl(composition)),
      SharedValidators.requiredArrayMinLength(1)
    );
  }

  getCompositionControl(composition: PacketComposition) {
    return this.formBuilder.group({
      __typename: [PacketComposition.TYPENAME],
      rankOrder: [composition && composition.rankOrder || null],
      taxonGroup: [composition && composition.taxonGroup || null, Validators.compose([Validators.required, SharedValidators.entity])],
      weight: [composition && composition.weight || null, null],
      ratio1: [composition && composition.ratio1, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
      ratio2: [composition && composition.ratio2, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
      ratio3: [composition && composition.ratio3, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
      ratio4: [composition && composition.ratio4, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
      ratio5: [composition && composition.ratio5, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])],
      ratio6: [composition && composition.ratio6, Validators.compose([SharedValidators.integer, Validators.min(0), Validators.max(100)])]
    });
  }
}
