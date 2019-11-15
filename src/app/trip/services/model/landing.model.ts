import {
  DataEntityAsObjectOptions,
  DataRootVesselEntity,
  EntityUtils,
  fromDateISOString,
  IWithObserversEntity,
  Person,
  ReferentialRef,
  toDateISOString,
  NOT_MINIFY_OPTIONS
} from "./base.model";
import {Moment} from "moment";
import {MeasurementUtils} from "./measurement.model";
import {Sample} from "./sample.model";
import {ReferentialAsObjectOptions} from "../../../core/services/model";

/**
 * Landing entity
 */
export class Landing extends DataRootVesselEntity<Landing> implements IWithObserversEntity<Landing> {

  static fromObject(source: any): Landing {
    const res = new Landing();
    res.fromObject(source);
    return res;
  }

  dateTime: Moment;
  location: ReferentialRef;
  rankOrder?: number;
  measurementValues: { [key: string]: any };

  tripId: number;
  observedLocationId: number;

  observers: Person[];

  samples: Sample[];

  constructor() {
    super();
    this.program = new ReferentialRef();
    this.location = new ReferentialRef();
    this.observers = [];
    this.measurementValues = {};
    this.samples = [];
  }

  clone(): Landing {
    const target = new Landing();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: Landing) {
    target.fromObject(this);
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);
    target.dateTime = toDateISOString(this.dateTime);
    target.location = this.location && this.location.asObject({ ...options, ...NOT_MINIFY_OPTIONS /*keep for list*/ } as ReferentialAsObjectOptions) || undefined;
    target.observers = this.observers && this.observers.map(p => p && p.asObject(options)) || undefined;
    target.measurementValues = MeasurementUtils.measurementValuesAsObjectMap(this.measurementValues, options);

    // Samples
    target.samples = this.samples && this.samples.map(s => s.asObject(options)) || undefined;

    return target;
  }

  fromObject(source: any): Landing {
    super.fromObject(source);
    this.dateTime = fromDateISOString(source.dateTime);
    source.location && this.location.fromObject(source.location);
    this.rankOrder = source.rankOrder;
    this.observers = source.observers && source.observers.map(Person.fromObject) || [];
    this.measurementValues = source.measurementValues;
    if (this.measurementValues === undefined) {
      console.warn("Source as no measurementValues. Should never occur ! ", source);
    }

    // Parent link
    this.observedLocationId = source.observedLocationId;
    this.tripId = source.tripId;

    // Samples
    this.samples = source.samples && source.samples.map(Sample.fromObject) || undefined;

    return this;
  }

  equals(other: Landing): boolean {
    return super.equals(other)
      || (
        // Same vessel
        (this.vesselFeatures && other.vesselFeatures && this.vesselFeatures.vesselId === other.vesselFeatures.vesselId)
        // Same date
        && (this.dateTime === other.dateTime)
        // Same location
        && EntityUtils.equals(this.location, other.location)
      );
  }
}
