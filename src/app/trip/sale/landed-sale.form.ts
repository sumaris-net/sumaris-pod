import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild } from '@angular/core';
import { DateAdapter } from '@angular/material/core';
import { Moment } from 'moment';
import { FormBuilder } from '@angular/forms';
import { AppForm, LocalSettingsService } from '@sumaris-net/ngx-components';
import { ProductsTable } from '../product/products.table';
import { MeasurementsForm } from '../measurement/measurements.form.component';
import { SaleValidatorService } from '../services/validator/sale.validator';
import { ExpectedSale } from '@app/trip/services/model/expected-sale.model';

@Component({
  selector: 'app-landed-sale-form',
  templateUrl: './landed-sale.form.html',
  styleUrls: ['./landed-sale.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandedSaleForm extends AppForm<ExpectedSale> implements OnInit {

  @Input() programLabel: string;
  @Input() showError = false;

  @ViewChild('saleMeasurementsForm', {static: true}) saleMeasurementsForm: MeasurementsForm;
  @ViewChild('productsTable', {static: true}) productsTable: ProductsTable;

  totalPriceCalculated: number;

  get value(): any {
    const value = this.form.value;
    value.measurements = this.saleMeasurementsForm.value;
    value.products = this.productsTable.value; //.map(product => product.asObject());
    return value;
  }

  set value(data: any) {
    this.setValue(data);
  }

  constructor(
    protected landedSaleValidatorService: SaleValidatorService,
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, landedSaleValidatorService.getFormGroup(undefined, {required: false}), settings);

  }

  setValue(data: ExpectedSale, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    super.setValue(data, opts);

    this.saleMeasurementsForm.value = data.measurements || [];

    // populate table
    this.productsTable.value = data.products;
  }


  ngOnInit() {
    super.ngOnInit();

  }

  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean }): void {
    super.enable(opts);
    this.saleMeasurementsForm.enable(opts);
  }

  disable(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.disable(opts);
    this.saleMeasurementsForm.disable(opts);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}
