import {
  DataRootVesselEntity, EntityUtils,
  fromDateISOString,
  isNotNil, IWithObserversEntity,
  Person,
  ReferentialRef,
  toDateISOString
} from "./base.model";
import {Moment} from "moment";
import {MeasurementUtils} from "./measurement.model";

/**
 * Landing entity
 */
export class Landing extends DataRootVesselEntity<Landing> implements IWithObserversEntity<Landing> {

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
    target.measurementValues = MeasurementUtils.measurementValuesAsObjectMap(this.measurementValues, minify);

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
    this.measurementValues = source.measurementValues;

    return this;
  }

  equals(other: Landing): boolean {
    return super.equals(other)
      || (
        // Same vessel
        (this.vesselFeatures && other.vesselFeatures && this.vesselFeatures.vesselId === other.vesselFeatures.vesselId)
        // Same date
        && (this.landingDateTime === other.landingDateTime)
        // Same location
        && EntityUtils.equals(this.landingLocation, other.landingLocation)
      );
  }
}
