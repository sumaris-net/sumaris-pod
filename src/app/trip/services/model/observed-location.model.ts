import {fromDateISOString, isNotNil, Person, ReferentialRef, toDateISOString} from "../../../core/core.module";

import {DataRootEntity, DataRootVesselEntity, IWithObserversEntity} from "./base.model";


import {Moment} from "moment/moment";
import {IEntityWithMeasurement, MeasurementUtils} from "./measurement.model";
import {Landing} from "./landing.model";


export class ObservedLocation extends DataRootEntity<ObservedLocation>
  implements IEntityWithMeasurement<ObservedLocation>, IWithObserversEntity<ObservedLocation> {

  static fromObject(source: any): ObservedLocation {
    const res = new ObservedLocation();
    res.fromObject(source);
    return res;
  }

  program: ReferentialRef;
  startDateTime: Moment;
  endDateTime: Moment;
  location: ReferentialRef;
  measurementValues: { [key: string]: any };
  observers: Person[];

  landings: Landing[];

  constructor() {
    super();
    this.program = null;
    this.location = null;
    this.measurementValues = {};
    this.observers = [];
    this.landings = [];
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
    target.program = this.program && this.program.asObject(false/*keep it for table*/) || undefined;
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.location = this.location && this.location.asObject(false/*keep it for table*/) || undefined;
    target.measurementValues = MeasurementUtils.measurementValuesAsObjectMap(this.measurementValues, minify);
    target.landings = this.landings && this.landings.map(s => s.asObject(minify)) || undefined;
    target.observers = this.observers && this.observers.map(o => o.asObject(minify)) || undefined;

    return target;
  }

  fromObject(source: any): ObservedLocation {
    super.fromObject(source);
    this.program = source.program && ReferentialRef.fromObject(source.program);
    this.startDateTime = fromDateISOString(source.startDateTime);
    this.endDateTime = fromDateISOString(source.endDateTime);
    this.location = source.location && ReferentialRef.fromObject(source.location);

    this.measurementValues = source.measurementValues || MeasurementUtils.measurementsValuesFromObjectArray(source.measurements);
    this.observers = source.observers && source.observers.map(Person.fromObject) || [];
    this.landings = source.landings && source.landings.map(Landing.fromObject) || [];

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


export class ObservedVessel extends DataRootVesselEntity<ObservedVessel> {

  static fromObject(source: any): ObservedVessel {
    const res = new ObservedVessel();
    res.fromObject(source);
    return res;
  }

  dateTime: Moment;
  observedLocationId: number;
  landingCount: number;
  rankOrder: number;
  observers: Person[];
  measurementValues: { [key: string]: any };

  constructor() {
    super();
    this.observers = [];
    this.measurementValues = {};
  }

  clone(): ObservedVessel {
    const target = new ObservedVessel();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: ObservedVessel) {
    target.fromObject(this);
  }


  fromObject(source: any): ObservedVessel {
    super.fromObject(source);
    this.dateTime = fromDateISOString(source.dateTime);
    this.observedLocationId = source.observedLocationId;
    this.rankOrder = source.rankOrder;
    this.landingCount = source.landingCount;
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

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.dateTime = toDateISOString(this.dateTime);
    target.observers = this.observers && this.observers.map(o => o.asObject(minify)) || undefined;

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

}

