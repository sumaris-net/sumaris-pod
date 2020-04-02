import {isNotNil} from "../../../core/core.module";
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

export declare interface ProductWeight extends IMeasurementValue {
  unit?: 'kg';
}

export class ProductFilter {

  static searchFilter<P extends Product>(f: ProductFilter): (T) => boolean {
    return (p: P) => {
      // Operation
      if (isNotNil(f.operationId) && isNotNil(p.operationId) && f.operationId !== p.operationId) {
        return false;
      }

      // Sale
      if (isNotNil(f.saleId) && isNotNil(p.saleId) && f.saleId !== p.saleId) {
        return false;
      }

      return true;
    };
  }

  operationId?: number;
  saleId?: number;
}

export class Product extends DataEntity<Product> implements IEntityWithMeasurement<Product> {

  static TYPENAME = 'ProductVO';

  static fromObject(source: any): Product {
    const target = new Product();
    target.fromObject(source);
    return target;
  }

  public static equals(p1: Product | any, p2: Product | any): boolean {
    return p1 && p1.equals(p2);
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

    target.taxonGroup = this.taxonGroup && this.taxonGroup.asObject({ ...opts, ...NOT_MINIFY_OPTIONS, keepEntityName: true} as ReferentialAsObjectOptions) || undefined;
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

    this.operationId = source.operationId;
    this.saleId = source.saleId;
    this.landingId = source.landingId;
    this.batchId = source.batchId;

    // Get all measurements values
    if (source.measurementValues) {
      this.measurementValues = source.measurementValues;
    }

    // Get also measurement lists (can be filled from pod)
    if (source.sortingMeasurements || source.quantificationMeasurements) {
      const measurements = (source.sortingMeasurements || []).concat(source.quantificationMeasurements || []);
      Object.assign(this.measurementValues, MeasurementUtils.toMeasurementValues(measurements));
    }

    return this;
  }

}
