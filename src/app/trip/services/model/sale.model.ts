import {Person} from "../../../core/services/model/person.model";
import {NOT_MINIFY_OPTIONS, ReferentialRef} from "../../../core/services/model/referential.model";
import {Moment} from "moment/moment";
import {DataEntityAsObjectOptions} from "../../../data/services/model/data-entity.model";
import {Sample} from "./sample.model";
import {Measurement, MeasurementUtils} from "./measurement.model";
import {IWithProductsEntity, Product} from "./product.model";
import {fromDateISOString, isNotEmptyArray, toDateISOString} from "../../../shared/functions";
import {DataRootVesselEntity} from "../../../data/services/model/root-vessel-entity.model";


export class Sale extends DataRootVesselEntity<Sale>
  implements IWithProductsEntity<Sale> {

  static TYPENAME = 'SaleVO';

  static fromObject(source: any): Sale {
    if (!source) return null;
    const res = new Sale();
    res.fromObject(source);
    return res;
  }

  startDateTime: Moment;
  endDateTime: Moment;
  saleLocation: ReferentialRef;
  saleType: ReferentialRef;
  observedLocationId: number;
  tripId: number;
  measurements: Measurement[];
  samples: Sample[];
  rankOrder: number;
  observers: Person[];
  products: Product[];

  constructor() {
    super();
    this.__typename = Sale.TYPENAME;
    this.saleLocation = new ReferentialRef();
    this.saleType = new ReferentialRef();
    this.measurements = [];
    this.samples = [];
    this.observers = [];
    this.products = [];
  }

  clone(): Sale {
    const target = new Sale();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: Sale) {
    target.fromObject(this);
  }


  fromObject(source: any): Sale {
    super.fromObject(source);
    this.startDateTime = fromDateISOString(source.startDateTime);
    this.endDateTime = fromDateISOString(source.endDateTime);
    source.saleLocation && this.saleLocation.fromObject(source.saleLocation);
    source.saleType && this.saleType.fromObject(source.saleType);
    this.rankOrder = source.rankOrder;
    this.tripId = source.tripId;
    this.observedLocationId = source.observedLocationId;
    this.samples = source.samples && source.samples.map(Sample.fromObject) || [];
    this.observers = source.observers && source.observers.map(Person.fromObject) || [];
    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || [];

    // Products (sale)
    this.products = source.products && source.products.map(Product.fromObject) || [];
    // Affect parent
    this.products.forEach(product => {
      product.parent = this;
    });

    return this;
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.saleLocation = this.saleLocation && this.saleLocation.asObject({...options, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.saleType = this.saleType && this.saleType.asObject({...options, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.samples = this.samples && this.samples.map(s => s.asObject(options)) || undefined;
    target.observers = this.observers && this.observers.map(o => o.asObject(options)) || undefined;
    target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(options)) || undefined;

    // Products
    target.products = this.products && this.products.map(o => o.asObject(options)) || undefined;
    // Affect parent link
    if (isNotEmptyArray(target.products)) {
      target.products.forEach(product => {
        product.saleId = target.id;
        // todo product.landingId must also be set, but not here, see pod
        delete product.parent;
      });
    }

    return target;
  }

}
