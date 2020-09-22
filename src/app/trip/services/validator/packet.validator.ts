import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormArray, FormBuilder, FormGroup, ValidatorFn, Validators} from "@angular/forms";
import {
  SharedFormArrayValidators,
  SharedFormGroupValidators,
  SharedValidators
} from "../../../shared/validator/validators";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {
  DataEntityValidatorOptions,
  DataEntityValidatorService
} from "../../../data/services/validator/data-entity.validator";
import {Packet, PacketComposition} from "../model/packet.model";
import {PacketCompositionValidatorService} from "./packet-composition.validator";

export interface PacketValidatorOptions extends DataEntityValidatorOptions {
  withComposition?: boolean;
  withSaleProducts?: boolean;
}

@Injectable({providedIn: 'root'})
export class PacketValidatorService<O extends PacketValidatorOptions = PacketValidatorOptions>
  extends DataEntityValidatorService<Packet, O> implements ValidatorService {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService,
    protected packetCompositionValidatorService: PacketCompositionValidatorService
  ) {
    super(formBuilder, settings);
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
        sampledWeight1: [data && data.sampledWeight1, Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
        sampledWeight2: [data && data.sampledWeight2, Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
        sampledWeight3: [data && data.sampledWeight3, Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
        sampledWeight4: [data && data.sampledWeight4, Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
        sampledWeight5: [data && data.sampledWeight5, Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])],
        sampledWeight6: [data && data.sampledWeight6, Validators.compose([Validators.min(0), SharedValidators.double({maxDecimals: 2})])]
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

    if (opts.withSaleProducts) {
      formConfig.saleProducts = this.getSaleProductsFormArray(data);
    } else {
      formConfig.saleProducts = [data && data.saleProducts || null];
    }

    return formConfig;
  }

  updateFormGroup(formGroup: FormGroup, opts?: O) {

    if (opts.withSaleProducts) {
      const saleValidators = [];
      if (formGroup.controls.number.value) {
        saleValidators.push(SharedFormArrayValidators.validSumMaxValue('subgroupCount', formGroup.controls.number.value));
      }
      if (saleValidators.length) {
        formGroup.controls.saleProducts.setValidators(saleValidators);
      }
    }
  }

  /* -- protected methods -- */

  private getCompositionFormArray(data?: Packet) {
    return this.formBuilder.array(
      (data && data.composition || [null]).map(composition => this.getCompositionControl(composition)),
      this.getDefaultCompositionValidators()
    );
  }

  getDefaultCompositionValidators(): ValidatorFn[] {
    return [
      SharedFormArrayValidators.uniqueEntity('taxonGroup')
    ];
  }

  getCompositionControl(composition: PacketComposition): FormGroup {
    return this.packetCompositionValidatorService.getFormGroup(composition);
  }

  private getSaleProductsFormArray(data: Packet): FormArray {
    return this.formBuilder.array(
      (data && data.saleProducts || [null]).map(saleProduct => this.getSaleProductControl(saleProduct))
    );
  }

  getSaleProductControl(sale?: any): FormGroup {
    return this.formBuilder.group({
        saleType: [sale && sale.saleType || null, Validators.compose([Validators.required, SharedValidators.entity])],
        rankOrder: [sale && sale.rankOrder || null],
        subgroupCount: [sale && sale.subgroupCount || null, Validators.compose([SharedValidators.integer, Validators.min(0)])],
        weight: [sale && sale.weight || null],
        weightCalculated: [true],
        averagePackagingPrice: [sale && sale.averagePackagingPrice || null, Validators.compose([SharedValidators.double({maxDecimals: 2}), Validators.min(0)])],
        averagePackagingPriceCalculated: [sale && sale.averagePackagingPriceCalculated || null],
        totalPrice: [sale && sale.totalPrice || null, Validators.compose([SharedValidators.double({maxDecimals: 2}), Validators.min(0)])],
        totalPriceCalculated: [sale && sale.totalPriceCalculated || null],
        productIdByTaxonGroup: [sale && sale.productIdByTaxonGroup || null]
      },
      {
        validators: [
          SharedFormGroupValidators.propagateIfDirty('averagePackagingPrice', 'averagePackagingPriceCalculated', false),
          SharedFormGroupValidators.propagateIfDirty('averagePackagingPrice', 'totalPriceCalculated', true),
          SharedFormGroupValidators.propagateIfDirty('totalPrice', 'totalPriceCalculated', false),
          SharedFormGroupValidators.propagateIfDirty('totalPrice', 'averagePackagingPriceCalculated', true),
        ]
      });
  }
}
