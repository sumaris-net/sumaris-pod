import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {InMemoryEntitiesService} from "@sumaris-net/ngx-components";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {ProductValidatorService} from "../services/validator/product.validator";
import {IWithProductsEntity, Product, ProductFilter} from "../services/model/product.model";
import {Platform} from "@ionic/angular";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {BehaviorSubject} from "rxjs";
import {IReferentialRef, referentialToString}  from "@sumaris-net/ngx-components";
import {TableElement} from "@e-is/ngx-material-table";
import {ProductSaleModal} from "../sale/product-sale.modal";
import {isNotEmptyArray} from "@sumaris-net/ngx-components";
import {SaleProductUtils} from "../services/model/sale-product.model";
import {filterNotNil} from "@sumaris-net/ngx-components";
import {DenormalizedPmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {environment} from "../../../environments/environment";
import {SamplesModal} from "../sample/samples.modal";
import {LoadResult} from "@sumaris-net/ngx-components";
import {IPmfm} from "../../referential/services/model/pmfm.model";

export const PRODUCT_RESERVED_START_COLUMNS: string[] = ['parent', 'taxonGroup', 'weight', 'individualCount'];
export const PRODUCT_RESERVED_END_COLUMNS: string[] = []; // ['comments']; // todo

@Component({
  selector: 'app-products-table',
  templateUrl: 'products.table.html',
  styleUrls: ['products.table.scss'],
  providers: [
    {
      provide: InMemoryEntitiesService,
      useFactory: () => new InMemoryEntitiesService(Product, ProductFilter, {
        equals: Product.equals
      })
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductsTable extends AppMeasurementsTable<Product, ProductFilter> implements OnInit, OnDestroy {

  @Input() showParent = true;
  @Input() $parents: BehaviorSubject<IWithProductsEntity<any>[]>;
  @Input() parentAttributes: string[];

  @Input() set parentFilter(productFilter: ProductFilter) {
    this.setFilter(productFilter);
  }

  @Input()
  set value(data: Product[]) {
    this.memoryDataService.value = data;
  }

  get value(): Product[] {
    return this.memoryDataService.value;
  }

  get dirty(): boolean {
    return this._dirty || this.memoryDataService.dirty;
  }

  private productSalePmfms: DenormalizedPmfmStrategy[];

  constructor(
    injector: Injector,
    protected platform: Platform,
    protected validatorService: ProductValidatorService,
    protected memoryDataService: InMemoryEntitiesService<Product, ProductFilter>,
    protected cd: ChangeDetectorRef,
  ) {
    super(injector,
      Product,
      memoryDataService,
      validatorService,
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: PRODUCT_RESERVED_START_COLUMNS,
        reservedEndColumns: platform.is('mobile') ? [] : PRODUCT_RESERVED_END_COLUMNS,
        mapPmfms: (pmfms) => this.mapPmfms(pmfms),
      });
    this.i18nColumnPrefix = 'TRIP.PRODUCT.LIST.';
    this.autoLoad = false; // waiting parent to be loaded
    this.inlineEdition = true;
    this.confirmBeforeDelete = true;
    this.defaultPageSize = -1; // Do not use paginator

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.PRODUCT;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerAutocompleteField('parent', {
      items: this.$parents,
      attributes: this.parentAttributes,
      columnNames: ['REFERENTIAL.LABEL', 'REFERENTIAL.NAME'],
      columnSizes: this.parentAttributes.map(attr => attr === 'metier.label' ? 3 : undefined)
    });

    const taxonGroupAttributes = this.settings.getFieldDisplayAttributes('taxonGroup');
    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options),
      columnSizes: taxonGroupAttributes.map(attr => attr === 'label' ? 3 : undefined)
    });

    this.registerSubscription(
      filterNotNil(this.$pmfms)
        .subscribe(() => {
          // if main pmfms are loaded, then other pmfm can be loaded
          this.programRefService.loadProgramPmfms(this.programLabel, {acquisitionLevel: AcquisitionLevelCodes.PRODUCT_SALE})
            .then(productSalePmfms => this.productSalePmfms = productSalePmfms);
        }));

    this.registerSubscription(this.onStartEditingRow.subscribe(row => this.onStartEditProduct(row)));
  }

  /* -- protected methods -- */

  protected async suggestTaxonGroups(value: any, options?: any): Promise<LoadResult<IReferentialRef>> {
    return this.programRefService.suggestTaxonGroups(value,
      {
        program: this.programLabel,
        searchAttribute: options && options.searchAttribute
      });
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  private mapPmfms(pmfms: IPmfm[]): IPmfm[] {

    if (this.platform.is('mobile')) {
      // hide pmfms on mobile
      return [];
    }

    return pmfms;
  }

  confirmEditCreate(event?: any, row?: TableElement<Product>): boolean {
    row = row || this.editedRow;

    const confirmed = super.confirmEditCreate(event, row);

    if (confirmed && row) {
      // update sales if any
      if (isNotEmptyArray(row.currentData.saleProducts)) {
        const updatedSaleProducts = SaleProductUtils.updateSaleProducts(row.currentData, this.productSalePmfms);
        row.validator.patchValue({saleProducts: updatedSaleProducts}, {emitEvent: true});
      }
    }
    return confirmed;
  }

  async openProductSale(event: MouseEvent, row: TableElement<Product>) {
    if (event) event.stopPropagation();

    const modal = await this.modalCtrl.create({
      component: ProductSaleModal,
      componentProps: {
        product: row.currentData,
        productSalePmfms: this.productSalePmfms
      },
      backdropDismiss: false,
      cssClass: 'modal-large'
    });

    modal.present();
    const res = await modal.onDidDismiss();

    if (res && res.data) {
      // patch saleProducts only
      row.validator.patchValue({saleProducts: res.data.saleProducts}, {emitEvent: true});
      this.markAsDirty();
    }

  }

  async openSampling(event: MouseEvent, row: TableElement<Product>) {
    if (event) event.stopPropagation();

    // test sample modal
    this.markAsLoading();

    const samples = row.currentData.samples || [];
    const taxonGroup = row.currentData.taxonGroup;
    const title = await this.translate.get('TRIP.SAMPLE.EDIT.TITLE', {label: referentialToString(taxonGroup)}).toPromise();

    const modal = await this.modalCtrl.create({
      component: SamplesModal,
      componentProps: {
        programLabel: this.programLabel,
        disabled: this.disabled,
        value: samples,
        defaultSampleDate: new Date(), // trick to valid sample row, should be set with correct date
        defaultTaxonGroup: taxonGroup,
        showLabel: false,
        showTaxonGroup: false,
        showTaxonName: false,
        title
        // onReady: (obj) => this.onInitForm && this.onInitForm.emit({form: obj.form.form, pmfms: obj.$pmfms.getValue()})
      },
      keyboardClose: true
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    // if (data && this.debug)
      console.debug("[products-table] Modal result: ", data);
    this.markAsLoaded();

    if (data) {
      // patch samples only
      row.validator.patchValue({samples: data}, {emitEvent: true});
      this.markAsDirty();
    }

  }

  private onStartEditProduct(row: TableElement<Product>) {
    if (this.filter && this.filter.parent && row.currentData && !row.currentData.parent) {
      row.validator.patchValue({parent: this.filter.parent});
    }
  }
}
