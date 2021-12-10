import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild } from '@angular/core';
import { Injector } from '@angular/core';
import { Moment } from 'moment';
import { FormBuilder, FormGroup } from '@angular/forms';
import { AppTabEditor, firstNotNilPromise, isNotNil, LocalSettingsService, round } from '@sumaris-net/ngx-components';
import { ProductsTable } from '../product/products.table';
import { MeasurementsForm } from '../measurement/measurements.form.component';
import { ExpectedSale } from '@app/trip/services/model/expected-sale.model';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AlertController } from '@ionic/angular';
import { Product } from '@app/trip/services/model/product.model';
import { SaleProductUtils } from '@app/trip/services/model/sale-product.model';
import { DenormalizedPmfmStrategy } from '@app/referential/services/model/pmfm-strategy.model';
import { MeasurementValuesUtils } from '@app/trip/services/model/measurement.model';
import { PmfmIds } from '@app/referential/services/model/model.enum';

@Component({
  selector: 'app-expected-sale-form',
  templateUrl: './expected-sale.form.html',
  styleUrls: ['./expected-sale.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpectedSaleForm extends AppTabEditor<ExpectedSale> implements OnInit {

  @Input() programLabel: string;
  @Input() showError = false;
  @Input() mobile: boolean;

  @ViewChild('saleMeasurementsForm', {static: true}) saleMeasurementsForm: MeasurementsForm;
  @ViewChild('productsTable', {static: true}) productsTable: ProductsTable;

  data: ExpectedSale;
  form: FormGroup;
  totalPriceCalculated: number;

  get value(): ExpectedSale {
    this.data.measurements = this.saleMeasurementsForm.value;
    this.data.products = null; // don't return readonly table value
    return this.data;
  }

  set value(data: ExpectedSale) {
    this.setValue(isNotNil(data) ? data : new ExpectedSale());
  }

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, alertCtrl, translate);

    this.form = formBuilder.group({});
  }

  setValue(data: ExpectedSale, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    this.data = data;

    this.saleMeasurementsForm.value = data.measurements || [];

    this.updateProducts(data.products);
  }

  async updateProducts(value: Product[]) {

    const pmfms = (await firstNotNilPromise(this.productsTable.$pmfms))
      .map(pmfm => DenormalizedPmfmStrategy.fromObject(pmfm));
    let products = (value || []).slice();
    this.totalPriceCalculated = 0;

    // compute prices
    products = products.map(product => {
      const saleProduct = SaleProductUtils.productToSaleProduct(product, pmfms);
      SaleProductUtils.computeSaleProduct(
        product,
        saleProduct,
        (object, valueName) => !!object[valueName],
        (object, valueName) => object[valueName],
        (object, valueName, value) => object[valueName] = round(value),
        (object, valueName) => object[valueName] = undefined,
        true,
        'individualCount'
      );
      const target = {...product, ...saleProduct};
      target.measurementValues = MeasurementValuesUtils.normalizeValuesToForm(target.measurementValues, pmfms);

      // add measurements for each calculated or non calculated values
      MeasurementValuesUtils.setValue(target.measurementValues, pmfms, PmfmIds.AVERAGE_WEIGHT_PRICE, saleProduct.averageWeightPrice);
      MeasurementValuesUtils.setValue(target.measurementValues, pmfms, PmfmIds.AVERAGE_PACKAGING_PRICE, saleProduct.averagePackagingPrice);
      MeasurementValuesUtils.setValue(target.measurementValues, pmfms, PmfmIds.TOTAL_PRICE, saleProduct.totalPrice);
      this.totalPriceCalculated += saleProduct.totalPrice;

      return Product.fromObject(target);
    })

    if (this.totalPriceCalculated == 0) this.totalPriceCalculated = undefined;

    // populate table
    this.productsTable.value = products;

  }


  ngOnInit() {
    super.ngOnInit();


    this.addChildForms([this.saleMeasurementsForm]);
  }


  protected markForCheck() {
    this.cd.markForCheck();
  }

  get isNewData(): boolean {
    return false;
  }

  load(id: number | undefined, options: any): Promise<void> {
    return Promise.resolve(undefined);
  }

  reload(): Promise<void> {
    return Promise.resolve(undefined);
  }

  save(event: Event | undefined, options: any): Promise<boolean> {
    return Promise.resolve(false);
  }

}
