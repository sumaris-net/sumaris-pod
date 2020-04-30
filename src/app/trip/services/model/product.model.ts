import {EntityUtils, isNotNil} from "../../../core/core.module";
import {ReferentialRef} from "../../../core/services/model";
import {DataEntity, DataEntityAsObjectOptions, IWithProductsEntity, NOT_MINIFY_OPTIONS} from "./base.model";
import {
  IEntityWithMeasurement,
  IMeasurementValue, MeasurementFormValues,
  MeasurementModelValues,
  MeasurementUtils,
  MeasurementValuesUtils
} from "./measurement.model";
import {isNotNilOrBlank} from "../../../shared/functions";
import {ReferentialAsObjectOptions} from "../../../core/services/model";
import {DataFilter} from "../../../shared/services/memory-data-service.class";

export declare interface ProductWeight extends IMeasurementValue {
  unit?: 'kg';
}

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
  weight: ProductWeight;
  weightMethod: ReferentialRef;
  saleType: ReferentialRef;

  measurementValues: MeasurementFormValues;

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
    this.weightMethod = null;
    this.saleType = null;

    this.measurementValues = {};
    this.operationId = null;
    this.saleId = null;
    this.landingId = null;
    this.batchId = null;

  }

  clone(): Product {
    const target = new Product();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(opts?: DataEntityAsObjectOptions): any {
    const target = super.asObject(opts);

    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject({...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true} as ReferentialAsObjectOptions) || undefined;
    target.weightMethod = this.weightMethod && this.weightMethod.asObject({...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true}) || undefined;
    target.saleType = this.saleType && this.saleType.asObject({...opts, ...NOT_MINIFY_OPTIONS}) || undefined;

    // target.individualCount = isNotNil(this.individualCount) ? this.individualCount : null;
    target.measurementValues = MeasurementValuesUtils.asObject(this.measurementValues, opts);

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
    this.weightMethod = source.weightMethod && ReferentialRef.fromObject(source.weightMethod) || undefined;
    this.saleType = source.saleType && ReferentialRef.fromObject(source.saleType) || undefined;

    this.parent = source.parent;
    this.operationId = source.operationId;
    this.saleId = source.saleId;
    this.landingId = source.landingId;
    this.batchId = source.batchId;

    // Get all measurements values
    this.measurementValues = source.measurementValues;

    return this;
  }

}
