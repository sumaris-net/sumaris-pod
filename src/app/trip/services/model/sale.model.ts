import {fromDateISOString, isNotNil, toDateISOString} from "../../../core/core.module";
import {Person, ReferentialRef} from "../../../referential/referential.module";
import {Moment} from "moment/moment";
import {DataEntityAsObjectOptions, DataRootVesselEntity} from "./base.model";
import {Sample} from "./sample.model";
import {MeasurementUtils, MeasurementValuesUtils} from "./measurement.model";


export class Sale extends DataRootVesselEntity<Sale> {

  static fromObject(source: any): Sale {
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
  measurementValues: { [key: string]: any };
  samples: Sample[];
  rankOrder: number;
  observers: Person[];

  constructor() {
    super();
    this.saleLocation = new ReferentialRef();
    this.saleType = new ReferentialRef();
    this.measurementValues = {};
    this.samples = [];
    this.observers = [];
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

    if (source.measurementValues) {
      this.measurementValues = source.measurementValues;
    }
    // Convert measurement to map
    else if (source.measurements) {
      this.measurementValues = source.measurements && source.measurements.reduce((map, m) => {
        const value = m && m.pmfmId && (m.alphanumericalValue || m.numericalValue || (m.qualitativeValue && m.qualitativeValue.id));
        if (value) map[m.pmfmId] = value;
        return map;
      }, {}) || undefined;
    }


    return this;
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.saleLocation = this.saleLocation && this.saleLocation.asObject(options) || undefined;
    target.saleType = this.saleType && this.saleType.asObject(options) || undefined;
    target.samples = this.samples && this.samples.map(s => s.asObject(options)) || undefined;
    target.observers = this.observers && this.observers.map(o => o.asObject(options)) || undefined;
    target.measurementValues = MeasurementUtils.measurementValuesAsObjectMap(this.measurementValues, options);

    return target;
  }

}
