import {fromDateISOString, isNotNil, Person, ReferentialRef, toDateISOString} from "../../core/core.module";

import {DataRootEntity, Measurement, MeasurementUtils, Sale} from "./trip.model";


import {Moment} from "moment/moment";


export class ObservedLocation extends DataRootEntity<ObservedLocation> {

  static fromObject(source: any): ObservedLocation {
    const res = new ObservedLocation();
    res.fromObject(source);
    return res;
  }

  program: ReferentialRef;
  startDateTime: Moment;
  endDateTime: Moment;
  location: ReferentialRef;

  // TODO: remove this
  measurements: Measurement[];
  measurementValues: { [key: string]: any };

  sales: Sale[];
  observers: Person[];

  // TODO: add observers

  constructor() {
    super();
    this.program = new ReferentialRef();
    this.location = new ReferentialRef();
    // TODO: remove this
    this.measurements = [];
    this.measurementValues = {};

    this.sales = null;
    this.observers = [];
  }

  clone(): ObservedLocation {
    const target = new ObservedLocation();
    target.fromObject(this.asObject);
    return target;
  }

  copy(target: ObservedLocation) {
    target.fromObject(this);
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.program = this.program && this.program.asObject(false/*keep for trips list*/) || undefined;
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.location = this.location && this.location.asObject(false/*keep for trips list*/) || undefined;

    // TODO: remove this
    target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(minify)) || undefined;

    // Measurement: keep only the map
    if (minify) {
      target.measurementValues = this.measurementValues && Object.getOwnPropertyNames(this.measurementValues)
        .reduce((map, pmfmId) => {
          const value = this.measurementValues[pmfmId] && this.measurementValues[pmfmId].id || this.measurementValues[pmfmId];
          if (isNotNil(value)) map[pmfmId] = '' + value;
          return map;
        }, {}) || undefined;
    }

    target.sales = this.sales && this.sales.map(s => s.asObject(minify)) || undefined;
    target.observers = this.observers && this.observers.map(o => o.asObject(minify)) || undefined;

    return target;
  }

  fromObject(source: any): ObservedLocation {
    super.fromObject(source);
    source.program && this.program.fromObject(source.program);
    this.startDateTime = fromDateISOString(source.startDateTime);
    this.endDateTime = fromDateISOString(source.endDateTime);
    source.location && this.location.fromObject(source.location);
    // TODO: remove this
    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || [];

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

    this.sales = source.sales && source.sales.map(Sale.fromObject) || [];
    this.observers = source.observers && source.observers.map(Person.fromObject) || [];

    return this;
  }

  equals(other: ObservedLocation): boolean {
    return super.equals(other)
      || (
        // Same location
        (this.location && other.location && this.location.id === other.location.id)
        // Same start date/time
        && (this.startDateTime === other.startDateTime)
        // Same recorder person
        && (this.recorderPerson && other.recorderPerson && this.recorderPerson.id === other.recorderPerson.id)
      );
  }
}
