import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {ProductValidatorService} from "../services/validator/product.validator";
import {Product, ProductFilter} from "../services/model/product.model";
import {Platform} from "@ionic/angular";
import {environment} from "../../../environments/environment";
import {AcquisitionLevelCodes, PmfmStrategy} from "../../referential/services/model";
import {BehaviorSubject, Observable} from "rxjs";
import {IWithProductsEntity} from "../services/model/base.model";
import {IReferentialRef} from "../../core/services/model";
import {TableElement} from "angular4-material-table";

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
        equals: Product.equals
      })
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductsTable extends AppMeasurementsTable<Product, ProductFilter> implements OnInit, OnDestroy {

  @Input() $parentFilter: Observable<any>;
  @Input() $parents: BehaviorSubject<IWithProductsEntity<any>[]>;
  @Input() parentAttributes: string[];

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

    // this.displayAttributes = {
    //   parent: this.parentAttributes
    // };

    this.registerAutocompleteField('parent', {
      items: this.$parents,
      attributes: this.parentAttributes
    });

    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options)
    });

    this.registerSubscription(this.$parentFilter.subscribe(parentFilter => {
      // console.debug('parent test change', parentFilter);
      this.setFilter(new ProductFilter(parentFilter));
    }));
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

  // TODO prévoir le filtrage des avgPrice
  // changer les label des pmfm d'avgPrice : AVERAG_PRICE_<packaging_label> et construire une map d'id <qv,pmfm>
  private mapPmfms(pmfms: PmfmStrategy[]): PmfmStrategy[] {

    if (this.platform.is('mobile')) {
      // hide pmfms on mobile
      return [];
    }

    return pmfms;
  }


  openSampling($event: MouseEvent, row: TableElement<Product>) {
    // todo
  }
}
