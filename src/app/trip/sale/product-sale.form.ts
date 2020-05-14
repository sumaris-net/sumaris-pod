import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {AppForm, AppFormUtils, FormArrayHelper, isNotNil} from "../../core/core.module";
import {Product} from "../services/model/product.model";
import {AbstractControl, FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {UsageMode} from "../../core/services/model";
import {isNotEmptyArray} from "../../shared/functions";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {ProductValidatorService} from "../services/validator/product.validator";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {Subscription} from "rxjs";
import {SaleProduct, SaleProductUtils} from "../services/model/sale-product.model";
import {PmfmStrategy} from "../../referential/services/model";
import {isJsonArray} from "@angular-devkit/core";

@Component({
  selector: 'app-product-sale-form',
  templateUrl: './product-sale.form.html',
  styleUrls: ['./product-sale.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductSaleForm extends AppForm<Product> implements OnInit, OnDestroy {

  computing = false;
  salesHelper: FormArrayHelper<SaleProduct>;
  salesFocusIndex = -1;
  private saleSubscription = new Subscription();

  get saleFormArray(): FormArray {
    return this.form.controls.saleProducts as FormArray;
  }

  mobile: boolean;
  private _product: Product;

  @Input() showError = true;
  @Input() usageMode: UsageMode;
  @Input() productSalePmfms: PmfmStrategy[];

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  get value(): any {
    const json = this.form.value;

    // Convert products sales to products
    json.saleProducts = (json.saleProducts || []).map(saleProduct => SaleProductUtils.saleProductToProduct(this._product, saleProduct, this.productSalePmfms, {keepId: true}));

    return json;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: ProductValidatorService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected formBuilder: FormBuilder,
    protected referentialRefService: ReferentialRefService
  ) {
    super(dateAdapter, validatorService.getFormGroup(undefined, {withSaleProducts: true}), settings);
  }

  ngOnInit() {
    super.ngOnInit();

    this.initSalesHelper();

    this.usageMode = this.usageMode || this.settings.usageMode;

    // Combo: taxonGroup
    this.registerAutocompleteField('taxonGroup', {});

    // Combo: sale types
    this.registerAutocompleteField('saleType', {
      service: this.referentialRefService,
      attributes: ['name'],
      filter: {
        entityName: 'SaleType'
      }
    });

  }

  async setValue(data: Product, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {

    if (!data) return;
    this._product = data;

    // Initialize product sales by converting products to sale products
    data.saleProducts = isNotEmptyArray(data.saleProducts) ? data.saleProducts.map(p => SaleProductUtils.productToSaleProduct(p, this.productSalePmfms)) : [null];
    this.salesHelper.resize(Math.max(1, data.saleProducts.length));

    super.setValue(data, opts);

    // update saleFromArray validators
    this.validatorService.updateFormGroup(this.form, {withSaleProducts: true});

    this.computeAllPrices();

    this.initSubscription();
  }

  private initSubscription() {

    // clear and re-create
    this.saleSubscription.unsubscribe();
    this.saleSubscription = new Subscription();

    // add subscription on each sale form
    for (const saleControl of this.saleFormArray.controls) {
      this.saleSubscription.add(saleControl.valueChanges.subscribe(() => {
        this.computePrices(this.asFormGroup(saleControl).controls);
        saleControl.markAsPristine();
      }));
    }

  }

  private computeAllPrices() {
    for (const sale of this.saleFormArray.controls as FormGroup[] || []) {
      this.computePrices(sale.controls);
    }
  }

  computePrices(controls: { [key: string]: AbstractControl }) {

    if (this.computing)
      return;

    try {
      this.computing = true;

      if (this.isProductWithNumber()) {

        // with product individualCount (should be < whole product individualCount)
        const individualCount = controls.individualCount.value;
        if (individualCount) {
          if (AppFormUtils.isControlHasInput(controls, 'averagePackagingPrice')) {
            // compute total price
            AppFormUtils.setCalculatedValue(controls, 'totalPrice', controls.averagePackagingPrice.value * individualCount);

          } else if (AppFormUtils.isControlHasInput(controls, 'totalPrice')) {
            // compute average packaging price
            AppFormUtils.setCalculatedValue(controls, 'averagePackagingPrice', controls.totalPrice.value / individualCount);

          }
          // compute ratio
          const ratio = individualCount / this.form.controls.individualCount.value * 100;
          AppFormUtils.setCalculatedValue(controls, 'ratio', ratio);

          if (this.isProductWithWeight()) {
            // calculate weight
            AppFormUtils.setCalculatedValue(controls, 'weight', ratio * this.form.controls.weight.value / 100);

            // calculate average weight price
            if (controls.totalPrice.value) {
              AppFormUtils.setCalculatedValue(controls, 'averageWeightPrice', controls.totalPrice.value / this.form.controls.weight.value);
            }
          } else {
            // reset weight part
            AppFormUtils.resetCalculatedValue(controls, 'weight');
            AppFormUtils.resetCalculatedValue(controls, 'averageWeightPrice');
          }

        } else {
          // reset all
          AppFormUtils.resetCalculatedValue(controls, 'averagePackagingPrice');
          AppFormUtils.resetCalculatedValue(controls, 'totalPrice');
          AppFormUtils.resetCalculatedValue(controls, 'ratio');
          AppFormUtils.resetCalculatedValue(controls, 'weight');
          AppFormUtils.resetCalculatedValue(controls, 'averageWeightPrice');
        }

      } else if (this.isProductWithWeight()) {
        // with weight only
        if (AppFormUtils.isControlHasInput(controls, 'weight')) {
          // calculate ratio
          AppFormUtils.setCalculatedValue(controls, 'ratio', controls.weight.value / this.form.controls.weight.value * 100);

        } else if (AppFormUtils.isControlHasInput(controls, 'ratio')) {
          // calculate weight
          AppFormUtils.setCalculatedValue(controls, 'weight', controls.ratio.value * this.form.controls.weight.value / 100);

        } else {
          // reset weight and ratio
          AppFormUtils.resetCalculatedValue(controls, 'ratio');
          AppFormUtils.resetCalculatedValue(controls, 'weight');
        }

        const weight = controls.weight.value;
        if (weight) {

          if (AppFormUtils.isControlHasInput(controls, 'averageWeightPrice')) {
            // compute total price
            AppFormUtils.setCalculatedValue(controls, 'totalPrice', controls.averageWeightPrice.value * weight);

          } else if (AppFormUtils.isControlHasInput(controls, 'totalPrice')) {
            // compute average weight price
            AppFormUtils.setCalculatedValue(controls, 'averageWeightPrice', controls.totalPrice.value / weight);
          }

        } else {
          // reset
          AppFormUtils.resetCalculatedValue(controls, 'averageWeightPrice');
          AppFormUtils.resetCalculatedValue(controls, 'totalPrice');
        }

      }

    } finally {
      this.computing = false;
    }

  }

  isProductWithNumber(): boolean {
    return isNotNil(this.form.controls.individualCount.value);
  }

  isProductWithWeight(): boolean {
    return isNotNil(this.form.controls.weight.value);
  }

  private initSalesHelper() {
    this.salesHelper = new FormArrayHelper<SaleProduct>(
      this.formBuilder,
      this.form,
      'saleProducts',
      (saleProduct) => this.validatorService.getSaleProductControl(saleProduct),
      SaleProductUtils.isSaleProductEquals,
      SaleProductUtils.isSaleProductEmpty,
      {
        allowEmptyArray: true,
        validators: this.validatorService.getDefaultSaleProductValidators()
      }
    );
    if (this.salesHelper.size() === 0) {
      // add at least one sale
      this.salesHelper.resize(1);
    }
    this.markForCheck();

  }

  asFormGroup(control): FormGroup {
    return control;
  }

  addSale() {
    // create new saleProduct directly from form value which is base product
    // const newSaleProduct = SaleProductUtils.productToSaleProduct(this.form.value, this.productSalePmfms);
    // this.salesHelper.add(newSaleProduct);
    this.salesHelper.add();
    this.initSubscription();
    if (!this.mobile) {
      this.salesFocusIndex = this.salesHelper.size() - 1;
    }
  }

  removeSale(index: number) {
    this.salesHelper.removeAt(index);
    this.initSubscription();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  ngOnDestroy() {
    this.saleSubscription.unsubscribe();
    super.ngOnDestroy();
  }
}
