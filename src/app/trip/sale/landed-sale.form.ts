import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import {AppForm} from "../../core/core.module";
import {Sale} from "../services/model/sale.model";
import {DateAdapter, MatTabChangeEvent, MatTabGroup} from "@angular/material";
import {Moment} from "moment";
import {FormBuilder} from "@angular/forms";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {ProductSaleValidatorService} from "../services/validator/product-sale.validator";
import {ProductsTable} from "../product/products.table";
import {Product} from "../services/model/product.model";

@Component({
  selector: 'app-landed-sale-form',
  templateUrl: './landed-sale.form.html',
  styleUrls: ['./landed-sale.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandedSaleForm extends AppForm<Sale> implements OnInit {

  @Input() program: string;


  selectedTabIndex = 0;
  @ViewChild('tabGroup', {static: true}) tabGroup: MatTabGroup;
  @Output() tabChange: EventEmitter<any> = new EventEmitter<any>();

  @Input() showError = false;

  @ViewChild('productsTable', {static: true}) productsTable: ProductsTable;

  get value(): any {
    const value = this.form.value;
    value.products = this.productsTable.value; //.map(product => product.asObject());
    return value;
  }

  set value(data: any) {
    this.setValue(data);
  }

  constructor(
    protected productSaleValidatorService: ProductSaleValidatorService, // fixme c'est pas le bon validateur
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, productSaleValidatorService.getFormGroup(), settings);

  }

  setValue(data: Sale, opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    super.setValue(data, opts);

    this.productsTable.value = data.products;
  }


  ngOnInit() {
    super.ngOnInit();


  }

  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean }): void {
    super.enable(opts);
  }


  protected markForCheck() {
    this.cd.markForCheck();
  }

}
