import {AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {Subject, Subscription} from "rxjs";
import {AppFormUtils} from "../../core/form/form.utils";
import {ProductSaleForm} from "./product-sale.form";
import {Product} from "../services/model/product.model";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {TranslateService} from "@ngx-translate/core";
import {referentialToString} from "../../core/services/model/referential.model";

@Component({
  selector: 'app-product-sale-modal',
  templateUrl: './product-sale.modal.html'
})
export class ProductSaleModal implements OnInit, OnDestroy, AfterViewInit {

  loading = false;
  subscription = new Subscription();
  $title = new Subject<string>();

  @ViewChild('productSaleForm', {static: true}) productSaleForm: ProductSaleForm;

  @Input() product: Product;
  @Input() productSalePmfms: PmfmStrategy[];

  get disabled() {
    return this.productSaleForm.disabled;
  }

  get enabled() {
    return this.productSaleForm.enabled;
  }

  get valid() {
    return this.productSaleForm && this.productSaleForm.valid || false;
  }


  constructor(
    protected viewCtrl: ModalController,
    protected translate: TranslateService
  ) {

  }

  ngOnInit(): void {
  }

  ngAfterViewInit(): void {

    setTimeout(() => {
      this.enable();
      this.productSaleForm.setValue(this.product);
      this.updateTitle();
    });

  }

  protected async updateTitle() {
    const title = await this.translate.get('TRIP.PRODUCT.SALE.TITLE', {taxonGroupLabel: referentialToString(this.product.taxonGroup)}).toPromise();
    this.$title.next(title);
  }


  async onSave(event: any): Promise<any> {

    // Avoid multiple call
    if (this.disabled) return;

    await AppFormUtils.waitWhilePending(this.productSaleForm);

    if (this.productSaleForm.invalid) {
      AppFormUtils.logFormErrors(this.productSaleForm.form);
      return;
    }

    this.loading = true;

    try {
      const value = this.productSaleForm.value;
      this.disable();
      await this.viewCtrl.dismiss(value);
      this.productSaleForm.error = null;
    } catch (err) {
      console.error(err);
      this.productSaleForm.error = err && err.message || err;
      this.enable();
      this.loading = false;
    }
  }

  disable() {
    this.productSaleForm.disable();
  }

  enable() {
    this.productSaleForm.enable();
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  referentialToString = referentialToString;
}
