import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {ProductValidatorService} from "../services/validator/product.validator";
import {IWithProductsEntity, Product, ProductFilter} from "../services/model/product.model";
import {Platform} from "@ionic/angular";
import {environment} from "../../../environments/environment";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {BehaviorSubject, Observable} from "rxjs";
import {IReferentialRef} from "../../core/services/model/referential.model";
import {TableElement} from "angular4-material-table";
import {ProductSaleModal} from "../sale/product-sale.modal";
import {isNotEmptyArray} from "../../shared/functions";
import {SaleProductUtils} from "../services/model/sale-product.model";
import {filterNotNil} from "../../shared/observables";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";

export const PRODUCT_RESERVED_START_COLUMNS: string[] = ['parent', 'taxonGroup', 'weight', 'individualCount'];
export const PRODUCT_RESERVED_END_COLUMNS: string[] = []; // ['comments']; // todo

@Component({
  selector: 'app-products-table',
  templateUrl: 'products.table.html',
  styleUrls: ['products.table.scss'],
  providers: [
    {
      provide: InMemoryTableDataService,
      useFactory: () => new InMemoryTableDataService<Product, ProductFilter>(Product, {
        equals: Product.equals,
        filterFnFactory: ProductFilter.searchFilter
      })
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductsTable extends AppMeasurementsTable<Product, ProductFilter> implements OnInit, OnDestroy {

  @Input() $parentFilter: Observable<any>;
  @Input() $parents: BehaviorSubject<IWithProductsEntity<any>[]>;
  @Input() parentAttributes: string[];
  @Input() showParent = true;

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

  private productSalePmfms: PmfmStrategy[];

  constructor(
    injector: Injector,
    protected platform: Platform,
    protected validatorService: ProductValidatorService,
    protected memoryDataService: InMemoryTableDataService<Product, ProductFilter>,
    protected cd: ChangeDetectorRef
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
    this.pageSize = 1000; // Do not use paginator

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

    if (this.$parentFilter) {
      this.registerSubscription(this.$parentFilter.subscribe(parentFilter => this.setFilter(parentFilter)));
    }

    this.registerSubscription(
      filterNotNil(this.$pmfms)
        .subscribe(() => {
          // if main pmfms are loaded, then other pmfm can be loaded
          this.programService.loadProgramPmfms(this.program, {acquisitionLevel: AcquisitionLevelCodes.PRODUCT_SALE})
            .then(productSalePmfms => this.productSalePmfms = productSalePmfms);
        }));

    this.registerSubscription(this.onStartEditingRow.subscribe(row => this.onStartEditProduct(row)));
  }

  /* -- protected methods -- */

  protected async suggestTaxonGroups(value: any, options?: any): Promise<IReferentialRef[]> {
    return this.programService.suggestTaxonGroups(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute
      });
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  private mapPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {

    if (this.platform.is('mobile')) {
      // hide pmfms on mobile
      return [];
    }

    return pmfms;
  }

  confirmEditCreate(event?: any, row?: TableElement<Product>): boolean {
    const confirmed = super.confirmEditCreate(event, row);
    if (confirmed && row && row.currentData) {
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

  openSampling(event: MouseEvent, row: TableElement<Product>) {
    if (event) event.stopPropagation();
    // todo
  }

  private onStartEditProduct(row: TableElement<Product>) {
    if (this.filter && this.filter.parent && row.currentData && !row.currentData.parent) {
      row.validator.patchValue({parent: this.filter.parent});
    }
  }
}
