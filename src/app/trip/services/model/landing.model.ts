import {
  DataRootVesselEntity,
  fromDateISOString,
  isNotNil,
  Person,
  ReferentialRef,
  toDateISOString
} from "./base.model";
import {Moment} from "moment";

/**
 * Landing entity
 */
export class Landing extends DataRootVesselEntity<Landing> {

  static fromObject(source: any): Landing {
    const res = new Landing();
    res.fromObject(source);
    return res;
  }

  program: ReferentialRef;
  landingDateTime: Moment;
  landingLocation: ReferentialRef;
  rankOrder: number;
  measurementValues: { [key: string]: any };

  tripId: number;
  observedLocationId: number;

  observers: Person[];

  constructor() {
    super();
    this.program = new ReferentialRef();
    this.landingLocation = new ReferentialRef();
    this.observers = [];
    this.measurementValues = {};
  }

  clone(): Landing {
    const target = new Landing();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: Landing) {
    target.fromObject(this);
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.program = this.program && this.program.asObject(false/*keep it for table*/) || undefined;
    target.landingDateTime = toDateISOString(this.landingDateTime);
    target.landingLocation = this.landingLocation && this.landingLocation.asObject(false/*keep for landing list*/) || undefined;
    target.observers = this.observers && this.observers.map(p => p && p.asObject(minify)) || undefined;

    // Measurement: keep only the map
    if (minify) {
      target.measurementValues = this.measurementValues && Object.getOwnPropertyNames(this.measurementValues)
        .reduce((map, pmfmId) => {
          const value = this.measurementValues[pmfmId] && this.measurementValues[pmfmId].id || this.measurementValues[pmfmId];
          if (isNotNil(value)) map[pmfmId] = '' + value;
          return map;
        }, {}) || undefined;
    }

    return target;
  }

  fromObject(source: any): Landing {
    super.fromObject(source);
    source.program && this.program.fromObject(source.program);
    this.landingDateTime = fromDateISOString(source.landingDateTime);
    source.landingLocation && this.landingLocation.fromObject(source.landingLocation);
    this.rankOrder = source.rankOrder;
    this.observers = source.observers && source.observers.map(Person.fromObject) || [];
    this.observedLocationId = source.observedLocationId;
    this.tripId = source.tripId;

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


}
