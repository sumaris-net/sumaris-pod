import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {AppForm, FormArrayHelper, isNotNil} from "../../core/core.module";
import {ProductSale, ProductSaleUtils} from "../services/model/product-sale.model";
import {Product} from "../services/model/product.model";
import {AbstractControl, FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {UsageMode} from "../../core/services/model";
import {isNotEmptyArray, round, toBoolean} from "../../shared/functions";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {ProductValidatorService} from "../services/validator/product.validator";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {Subscription} from "rxjs";

@Component({
  selector: 'app-product-sale-form',
  templateUrl: './product-sale.form.html',
  styleUrls: ['./product-sale.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductSaleForm extends AppForm<Product> implements OnInit, OnDestroy {

  computing = false;
  salesHelper: FormArrayHelper<ProductSale>;
  salesFocusIndex = -1;
  private saleSubscription = new Subscription();

  get saleFormArray(): FormArray {
    return this.form.controls.productSales as FormArray;
  }

  mobile: boolean;

  @Input() showError = true;
  @Input() usageMode: UsageMode;

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  get value(): any {
    const json = this.form.value;

    // Update products sales if needed

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
    super(dateAdapter, validatorService.getFormGroup(undefined, {withProductSales: true}), settings);
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

  setValue(data: Product, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {

    if (!data) return;

    // Initialize product sales
    data.productSales = isNotEmptyArray(data.productSales) ? data.productSales : [null];
    this.salesHelper.resize(Math.max(1, data.productSales.length));

    super.setValue(data, opts);

    // update saleFromArray validators
    this.validatorService.updateFormGroup(this.form, {withProductSales: true});

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

        // with product number (should be < individualCount)
        const number = controls.number.value;
        if (number) {
          if (this.isControlHasInput(controls, 'averagePackagingPrice')) {
            // compute total price
            this.setCalculatedValue(controls, 'totalPrice', controls.averagePackagingPrice.value * number);

          } else if (this.isControlHasInput(controls, 'totalPrice')) {
            // compute average packaging price
            this.setCalculatedValue(controls, 'averagePackagingPrice', controls.totalPrice.value / number);

          }
          // compute ratio
          const ratio = number / this.form.controls.individualCount.value * 100;
          this.setCalculatedValue(controls, 'ratio', ratio);

          if (this.isProductWithWeight()) {
            // calculate weight
            this.setCalculatedValue(controls, 'weight', ratio * this.form.controls.weight.value / 100);

            // calculate average weight price
            if (controls.totalPrice.value) {
              this.setCalculatedValue(controls, 'averageWeightPrice', controls.totalPrice.value / this.form.controls.weight.value);
            }
          } else {
            // reset weight part
            this.resetCalculatedValue(controls, 'weight');
            this.resetCalculatedValue(controls, 'averageWeightPrice');
          }

        } else {
          // reset all
          this.resetCalculatedValue(controls, 'averagePackagingPrice');
          this.resetCalculatedValue(controls, 'totalPrice');
          this.resetCalculatedValue(controls, 'ratio');
          this.resetCalculatedValue(controls, 'weight');
          this.resetCalculatedValue(controls, 'averageWeightPrice');
        }

      } else if (this.isProductWithWeight()) {
        // with weight only
        if (this.isControlHasInput(controls, 'weight')) {
          // calculate ratio
          this.setCalculatedValue(controls, 'ratio', controls.weight.value / this.form.controls.weight.value * 100);

        } else if (this.isControlHasInput(controls, 'ratio')) {
          // calculate weight
          this.setCalculatedValue(controls, 'weight', controls.ratio.value * this.form.controls.weight.value / 100);

        } else {
          // reset weight and ratio
          this.resetCalculatedValue(controls, 'ratio');
          this.resetCalculatedValue(controls, 'weight');
        }

        const weight = controls.weight.value;
        if (weight) {

          if (this.isControlHasInput(controls, 'averageWeightPrice')) {
            // compute total price
            this.setCalculatedValue(controls, 'totalPrice', controls.averageWeightPrice.value * weight);

          } else if (this.isControlHasInput(controls, 'totalPrice')) {
            // compute average weight price
            this.setCalculatedValue(controls, 'averageWeightPrice', controls.totalPrice.value / weight);
          }

        } else {
          // reset
          this.resetCalculatedValue(controls, 'averageWeightPrice');
          this.resetCalculatedValue(controls, 'totalPrice');
        }

      }

    } finally {
      this.computing = false;
    }

  }

  isControlHasInput(controls: { [key: string]: AbstractControl }, controlName: string): boolean {
    // true if the control has a value and its 'calculated' control has the value 'false'
    return controls[controlName].value && !toBoolean(controls[controlName + 'Calculated'].value, false);
  }

  setCalculatedValue(controls: { [key: string]: AbstractControl }, controlName: string, value: number | undefined) {
    // set value to control
    controls[controlName].setValue(round(value));
    // set 'calculated' control to 'true'
    controls[controlName + 'Calculated'].setValue(true);
  }

  resetCalculatedValue(controls: { [key: string]: AbstractControl }, controlName: string) {
    if (!this.isControlHasInput(controls, controlName)) {
      // set undefined only if control already calculated
      this.setCalculatedValue(controls, controlName, undefined);
    }
  }

  isProductWithNumber(): boolean {
    return isNotNil(this.form.controls.individualCount.value);
  }

  isProductWithWeight(): boolean {
    return isNotNil(this.form.controls.weight.value);
  }

  private initSalesHelper() {
    this.salesHelper = new FormArrayHelper<ProductSale>(
      this.formBuilder,
      this.form,
      'productSales',
      (productSale) => this.validatorService.getProductSaleControl(productSale),
      ProductSaleUtils.isProductSaleEquals,
      ProductSaleUtils.isProductSaleEmpty,
      {
        allowEmptyArray: true,
        validators: this.validatorService.getDefaultProductSaleValidators()
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
