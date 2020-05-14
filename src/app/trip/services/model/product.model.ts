import {EntityUtils, isNotNil} from "../../../core/core.module";
import {ReferentialAsObjectOptions, ReferentialRef} from "../../../core/services/model";
import {DataEntity, DataEntityAsObjectOptions, IWithProductsEntity, NOT_MINIFY_OPTIONS} from "./base.model";
import {IEntityWithMeasurement, MeasurementFormValues, MeasurementValuesUtils} from "./measurement.model";
import {equalsOrNil, isNotNilOrBlank} from "../../../shared/functions";
import {DataFilter} from "../../../shared/services/memory-data-service.class";

export class ProductFilter implements DataFilter<Product> {

  static searchFilter<P extends Product>(f: ProductFilter): (T) => boolean {
    return (p: P) => {
      return f.test(p);
    };
  }

  parent?: IWithProductsEntity<any>;

  constructor(parent?: IWithProductsEntity<any>) {
    this.parent = parent || null;
  }

  test(data: Product): boolean {
    if (isNotNil(this.parent)) {
      return this.parent.equals(data.parent);
    }
    return true;
  }
}

export class Product extends DataEntity<Product> implements IEntityWithMeasurement<Product> {

  static TYPENAME = 'ProductVO';

  static fromObject(source: any): Product {
    const target = new Product();
    target.fromObject(source);
    return target;
  }

  public static equals(p1: Product | any, p2: Product | any): boolean {
    return p1 && p2 && ((isNotNil(p1.id) && p1.id === p2.id)
      // Or by functional attributes
      || (p1.rankOrder === p2.rankOrder
        // same operation
        && ((!p1.operationId && !p2.operationId) || p1.operationId === p2.operationId)
        // same taxon group
        && EntityUtils.equals(p1.taxonGroup, p2.taxonGroup)
      ));
  }

  label: string;
  comments: string;
  rankOrder: number;
  individualCount: number;
  subgroupCount: number;
  taxonGroup: ReferentialRef;
  weight: number;
  weightCalculated: boolean;
  saleType: ReferentialRef;

  measurementValues: MeasurementFormValues;
  saleProducts: Product[];

  operationId: number;
  saleId: number;
  landingId: number;
  batchId: number;

  // used only for table column
  parent: IWithProductsEntity<any>;

  constructor() {
    super();
    this.__typename = Product.TYPENAME;
    this.label = null;
    this.comments = null;
    this.rankOrder = null;
    this.individualCount = null;
    this.subgroupCount = null;
    this.taxonGroup = null;
    this.weight = null;
    this.saleType = null;

    this.measurementValues = {};
    this.saleProducts = [];
    this.operationId = null;
    this.saleId = null;
    this.landingId = null;
    this.batchId = null;

  }

  clone(): Product {
    return Product.fromObject(this.asObject());
  }

  asObject(opts?: DataEntityAsObjectOptions): any {
    const target = super.asObject(opts);

    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject({...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true} as ReferentialAsObjectOptions) || undefined;
    target.saleType = this.saleType && this.saleType.asObject({...opts, ...NOT_MINIFY_OPTIONS}) || undefined;

    // target.individualCount = isNotNil(this.individualCount) ? this.individualCount : null;
    target.measurementValues = MeasurementValuesUtils.asObject(this.measurementValues, opts);

    target.saleProducts = this.saleProducts && this.saleProducts.map(s => s.asObject(opts)) || [];

    delete target.parent;
    return target;
  }

  fromObject(source: any, opts?: { withChildren: boolean; }): Product {
    super.fromObject(source);
    this.label = source.label;
    this.comments = source.comments;
    this.rankOrder = +source.rankOrder;
    this.individualCount = isNotNilOrBlank(source.individualCount) ? parseInt(source.individualCount) : null;
    this.subgroupCount = isNotNilOrBlank(source.subgroupCount) ? parseFloat(source.subgroupCount) : null;
    this.taxonGroup = source.taxonGroup && ReferentialRef.fromObject(source.taxonGroup) || undefined;
    this.weight = source.weight || undefined;
    this.weightCalculated = source.weightCalculated || false;
    this.saleType = source.saleType && ReferentialRef.fromObject(source.saleType) || undefined;

    this.parent = source.parent;
    this.operationId = source.operationId;
    this.saleId = source.saleId;
    this.landingId = source.landingId;
    this.batchId = source.batchId;

    // Get all measurements values (by copy)
    this.measurementValues = source.measurementValues && {...source.measurementValues};

    this.saleProducts = source.saleProducts && source.saleProducts.map(saleProduct => Product.fromObject(saleProduct)) || [];

    return this;
  }

  /**
   * This equals function should also works with SaleProduct
   * @param other
   */
  equals(other: Product): boolean {
    return super.equals(other)
      || (
        this.taxonGroup.equals(other.taxonGroup) && this.rankOrder === other.rankOrder
        && equalsOrNil(this.individualCount, other.individualCount) && equalsOrNil(this.weight, other.weight)
        && equalsOrNil(this.subgroupCount, other.subgroupCount) && EntityUtils.equals(this.saleType, other.saleType)
      );
  }
}
