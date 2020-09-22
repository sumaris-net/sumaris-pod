import {Injectable} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {FormArray, FormBuilder, FormGroup, ValidatorFn, Validators} from "@angular/forms";
import {
  SharedFormArrayValidators,
  SharedFormGroupValidators,
  SharedValidators
} from "../../../shared/validator/validators";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {Program} from "../../../referential/services/model/program.model";
import {toBoolean} from "../../../shared/functions";
import {
  DataEntityValidatorOptions,
  DataEntityValidatorService
} from "../../../data/services/validator/data-entity.validator";
import {MeasurementsValidatorService} from "./measurement.validator";
import {Product} from "../model/product.model";
import {OperationGroup} from "../model/trip.model";

export interface ProductValidatorOptions extends DataEntityValidatorOptions {
  program?: Program;
  withMeasurements?: boolean;
  withSaleProducts?: boolean;
}

@Injectable({providedIn: 'root'})
export class ProductValidatorService<O extends ProductValidatorOptions = ProductValidatorOptions>
  extends DataEntityValidatorService<Product, O> implements ValidatorService {

  constructor(
    formBuilder: FormBuilder,
    settings: LocalSettingsService,
    protected measurementsValidatorService: MeasurementsValidatorService
  ) {
    super(formBuilder, settings);
  }

  getRowValidator(): FormGroup {
    return this.getFormGroup();
  }

  getFormGroup(data?: Product, opts?: O): FormGroup {
    opts = this.fillDefaultOptions(opts);

    const form = super.getFormGroup(data, opts);

    // Add measurement form
    // if (opts.withMeasurements) {
    //   const pmfms = (opts.program && opts.program.strategies[0] && opts.program.strategies[0].pmfmStrategies || [])
    //     .filter(p => p.acquisitionLevel === AcquisitionLevelCodes.OPERATION);
    //   form.addControl('measurements', this.measurementsValidatorService.getFormGroup(data && data.measurements, {
    //     isOnFieldMode: opts.isOnFieldMode,
    //     pmfms
    //   }));
    // }

    return form;
  }

  getFormGroupConfig(data?: Product, opts?: O): { [key: string]: any } {

    const formConfig = Object.assign(
      super.getFormGroupConfig(data, opts),
      {
        __typename: [OperationGroup.TYPENAME],
        parent: [data && data.parent || null, Validators.required],
        rankOrder: [data && data.rankOrder || null],
        taxonGroup: [data && data.taxonGroup || null, Validators.compose([Validators.required, SharedValidators.entity])],
        weight: [data && data.weight || '', SharedValidators.double({maxDecimals: 2})],
        individualCount: [data && data.individualCount || '', SharedValidators.integer],
        measurementValues: this.formBuilder.group({})
        // comments: [data && data.comments || null, Validators.maxLength(2000)]
      });

    if (opts.withSaleProducts) {
      formConfig.saleProducts = this.getSaleProductsFormArray(data);
    } else {
      formConfig.saleProducts = [data && data.saleProducts || null];
    }

    return formConfig;
  }

  getFormGroupOptions(data?: Product, opts?: O): { [p: string]: any } {
    return {
      validator: [
        SharedFormGroupValidators.requiredIfEmpty('weight', 'individualCount'),
        SharedFormGroupValidators.requiredIfEmpty('individualCount', 'weight')
      ]
    };
  }


  protected fillDefaultOptions(opts?: O): O {
    opts = super.fillDefaultOptions(opts);

    opts.withMeasurements = toBoolean(opts.withMeasurements, toBoolean(!!opts.program, false));

    return opts;
  }

  updateFormGroup(formGroup: FormGroup, opts?: O) {

    if (opts.withSaleProducts) {
      const saleValidators = this.getDefaultSaleProductValidators();
      if (formGroup.controls.individualCount.value) {
        saleValidators.push(SharedFormArrayValidators.validSumMaxValue('individualCount', formGroup.controls.individualCount.value));
      }
      if (formGroup.controls.weight.value) {
        saleValidators.push(SharedFormArrayValidators.validSumMaxValue('weight', formGroup.controls.weight.value));
      }
      if (saleValidators.length) {
        formGroup.controls.saleProducts.setValidators(saleValidators);
      }
    }
  }

  /* -- protected methods -- */

  private getSaleProductsFormArray(data: Product): FormArray {
    return this.formBuilder.array(
      (data && data.saleProducts || [null]).map(saleProduct => this.getSaleProductControl(saleProduct)),
      this.getDefaultSaleProductValidators()
    );
  }

  getDefaultSaleProductValidators(): ValidatorFn[] {
    return [
      SharedFormArrayValidators.validSumMaxValue('ratio', 100)
    ];
  }

  getSaleProductControl(sale?: any): FormGroup {
    return this.formBuilder.group({
        id: [sale && sale.id || null],
        saleType: [sale && sale.saleType || null, Validators.compose([Validators.required, SharedValidators.entity])],
        ratio: [sale && sale.ratio || null, Validators.compose([SharedValidators.double({maxDecimals: 2}), Validators.min(0), Validators.max(100)])],
        ratioCalculated: [sale && sale.ratioCalculated || null],
        weight: [sale && sale.weight || null, Validators.compose([SharedValidators.double({maxDecimals: 2}), Validators.min(0)])],
        weightCalculated: [sale && sale.weightCalculated || null],
        individualCount: [sale && sale.individualCount || null, Validators.compose([SharedValidators.integer, Validators.min(0)])],
        averageWeightPrice: [sale && sale.averageWeightPrice || null, Validators.compose([SharedValidators.double({maxDecimals: 2}), Validators.min(0)])],
        averageWeightPriceCalculated: [sale && sale.averageWeightPriceCalculated || null],
        averagePackagingPrice: [sale && sale.averagePackagingPrice || null, Validators.compose([SharedValidators.double({maxDecimals: 2}), Validators.min(0)])],
        averagePackagingPriceCalculated: [sale && sale.averagePackagingPriceCalculated || null],
        totalPrice: [sale && sale.totalPrice || null, Validators.compose([SharedValidators.double({maxDecimals: 2}), Validators.min(0)])],
        totalPriceCalculated: [sale && sale.totalPriceCalculated || null]
      },
      {
        validators: [
          SharedFormGroupValidators.propagateIfDirty('ratio', 'ratioCalculated', false),
          SharedFormGroupValidators.propagateIfDirty('ratio', 'weightCalculated', true),
          SharedFormGroupValidators.propagateIfDirty('weight', 'weightCalculated', false),
          SharedFormGroupValidators.propagateIfDirty('weight', 'ratioCalculated', true),
          SharedFormGroupValidators.propagateIfDirty('averageWeightPrice', 'averageWeightPriceCalculated', false),
          SharedFormGroupValidators.propagateIfDirty('averageWeightPrice', 'totalPriceCalculated', true),
          SharedFormGroupValidators.propagateIfDirty('averagePackagingPrice', 'averagePackagingPriceCalculated', false),
          SharedFormGroupValidators.propagateIfDirty('averagePackagingPrice', 'totalPriceCalculated', true),
          SharedFormGroupValidators.propagateIfDirty('totalPrice', 'totalPriceCalculated', false),
          SharedFormGroupValidators.propagateIfDirty('totalPrice', 'averageWeightPriceCalculated', true),
          SharedFormGroupValidators.propagateIfDirty('totalPrice', 'averagePackagingPriceCalculated', true),
        ]
      });
  }
}
