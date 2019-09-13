import {fromDateISOString, toDateISOString} from "../../../core/core.module";
import {Moment} from "moment/moment";
import {
  DataEntity,
  DataRootEntity,
  DataRootVesselEntity,
  IWithObserversEntity,
  Person,
  ReferentialRef
} from "./base.model";
import {IEntityWithMeasurement, Measurement, MeasurementUtils} from "./measurement.model";
import {Sale} from "./sale.model";
import {Sample} from "./sample.model";
import {Batch} from "./batch.model";
import {MetierRef} from "../../../referential/services/model/taxon.model";


/* -- Helper function -- */

const sortByDateTimeFn = (n1: VesselPosition, n2: VesselPosition) => { return n1.dateTime.isSame(n2.dateTime) ? 0 : (n1.dateTime.isAfter(n2.dateTime) ? 1 : -1); };

/* -- Data -- */

export class Trip extends DataRootVesselEntity<Trip> implements IWithObserversEntity<Trip> {

  static fromObject(source: any): Trip {
    const res = new Trip();
    res.fromObject(source);
    return res;
  }

  departureDateTime: Moment;
  returnDateTime: Moment;
  departureLocation: ReferentialRef;
  returnLocation: ReferentialRef;
  sale: Sale;
  gears: PhysicalGear[];
  measurements: Measurement[];
  observers: Person[];

  constructor() {
    super();
    this.departureLocation = null;
    this.returnLocation = null;
    this.measurements = [];
    this.observers = [];
  }

  clone(): Trip {
    const target = new Trip();
    target.fromObject(this.asObject);
    return target;
  }

  copy(target: Trip) {
    target.fromObject(this);
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.departureDateTime = toDateISOString(this.departureDateTime);
    target.returnDateTime = toDateISOString(this.returnDateTime);
    target.departureLocation = this.departureLocation && this.departureLocation.asObject(false/*keep for trips list*/) || undefined;
    target.returnLocation = this.returnLocation && this.returnLocation.asObject(false/*keep for trips list*/) || undefined;
    target.sale = this.sale && this.sale.asObject(minify) || undefined;
    target.gears = this.gears && this.gears.map(p => p && p.asObject(minify)) || undefined;
    target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(minify)) || undefined;
    target.observers = this.observers && this.observers.map(p => p && p.asObject(minify)) || undefined;
    return target;
  }

  fromObject(source: any): Trip {
    super.fromObject(source);
    this.departureDateTime = fromDateISOString(source.departureDateTime);
    this.returnDateTime = fromDateISOString(source.returnDateTime);
    this.departureLocation = source.departureLocation && ReferentialRef.fromObject(source.departureLocation);
    this.returnLocation = source.returnLocation && ReferentialRef.fromObject(source.returnLocation);
    this.sale = source.sale && Sale.fromObject(source.sale) || undefined;
    this.gears = source.gears && source.gears.filter(g => !!g).map(PhysicalGear.fromObject) || undefined;
    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || [];
    this.observers = source.observers && source.observers.map(Person.fromObject) || [];

    // Remove fake dates (e.g. if returnDateTime = departureDateTime)
    if (this.returnDateTime && this.returnDateTime.isSameOrBefore(this.departureDateTime)) {
      this.returnDateTime = undefined;
    }
    return this;
  }

  equals(other: Trip): boolean {
    return super.equals(other)
      || (
        // Same vessel
        (this.vesselFeatures && other.vesselFeatures && this.vesselFeatures.vesselId === other.vesselFeatures.vesselId)
        // Same departure date (or, if not set, same return date)
        && ((this.departureDateTime === other.departureDateTime)
          || (!this.departureDateTime && !other.departureDateTime && this.returnDateTime === other.returnDateTime))
      );
  }
}

export class PhysicalGear extends DataRootEntity<PhysicalGear> implements IEntityWithMeasurement<PhysicalGear> {

  static fromObject(source: any): PhysicalGear {
    const res = new PhysicalGear();
    res.fromObject(source);
    return res;
  }

  rankOrder: number;
  gear: ReferentialRef;
  measurements: Measurement[];
  measurementValues: { [key: string]: string };

  constructor() {
    super();
    this.gear = new ReferentialRef();
    this.rankOrder = null;
    this.measurements = [];
    this.measurementValues = {};
  }

  clone(): PhysicalGear {
    const target = new PhysicalGear();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: PhysicalGear) {
    target.fromObject(this);
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.gear = this.gear && this.gear.asObject(minify) || undefined;

    // Measurements
    target.measurementValues = MeasurementUtils.measurementValuesAsObjectMap( this.measurementValues, minify);

    return target;
  }

  fromObject(source: any): PhysicalGear {
    super.fromObject(source);
    this.rankOrder = source.rankOrder;
    source.gear && this.gear.fromObject(source.gear);
    this.measurementValues = source.measurementValues || MeasurementUtils.toMeasurementValues(source.measurements);
    return this;
  }

  equals(other: PhysicalGear): boolean {
    return super.equals(other)
      || (
        // Same gear
        (this.gear && other.gear && this.gear.id === other.gear.id)
        // Same rankOrder
        && (this.rankOrder === other.rankOrder)
      );
  }
}

export class Operation extends DataEntity<Operation> {

  static fromObject(source: any): Operation {
    const res = new Operation();
    res.fromObject(source);
    return res;
  }

  startDateTime: Moment;
  endDateTime: Moment;
  fishingStartDateTime: Moment;
  fishingEndDateTime: Moment;
  comments: string;
  rankOrderOnPeriod: number;
  hasCatch: boolean;
  positions: VesselPosition[];
  startPosition: VesselPosition;
  endPosition: VesselPosition;

  metier: MetierRef;
  physicalGear: PhysicalGear;
  tripId: number;

  measurements: Measurement[];
  samples: Sample[];
  catchBatch: Batch;

  constructor() {
    super();
    this.metier = null;
    this.startPosition = new VesselPosition();
    this.endPosition = new VesselPosition();
    this.physicalGear = null;
    this.measurements = [];
    this.samples = [];
    this.catchBatch = null;
  }

  clone(): Operation {
    const target = new Operation();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.fishingStartDateTime = toDateISOString(this.fishingStartDateTime);
    target.fishingEndDateTime = toDateISOString(this.fishingEndDateTime);

    // Fill end date, using start date (can be null if ON FIELD mode)
    target.endDateTime = target.endDateTime || target.startDateTime;
    if (target.endPosition && (target.endPosition.latitude || target.endPosition.longitude)) {
      target.endPosition.dateTime = target.endPosition.dateTime || target.fishingEndDateTime || target.endDateTime;
    }

    target.metier = this.metier && this.metier.asObject(false/*Always minify=false, because of operations tables cache*/) || undefined;

    // Create an array of position, instead of start/end
    target.positions = [this.startPosition, this.endPosition]
      .filter(p => p && p.dateTime)
      .map(p => p && p.asObject(minify)) || undefined;
    delete target.startPosition;
    delete target.endPosition;

    // Physical gear: keep id
    target.physicalGearId = this.physicalGear && this.physicalGear.id;
    delete target.physicalGear;

    // Measurements
    target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(minify)) || undefined;

    // Samples
    target.samples = this.samples && this.samples.map(s => s.asObject(minify)) || undefined;

    // Batch
    target.catchBatch = this.catchBatch && this.catchBatch.asObject(minify) || undefined;

    return target;
  }

  fromObject(source: any): Operation {
    super.fromObject(source);
    this.hasCatch = source.hasCatch;
    this.comments = source.comments;
    this.tripId = source.tripId;
    this.physicalGear = (source.physicalGear || source.physicalGearId) ? PhysicalGear.fromObject(source.physicalGear || {id: source.physicalGearId}) : undefined;
    this.startDateTime = fromDateISOString(source.startDateTime);
    this.endDateTime = fromDateISOString(source.endDateTime);

    this.fishingStartDateTime = fromDateISOString(source.fishingStartDateTime);
    this.fishingEndDateTime = fromDateISOString(source.fishingEndDateTime);
    this.rankOrderOnPeriod = source.rankOrderOnPeriod;
    this.metier = source.metier && MetierRef.fromObject(source.metier, true/*Copy taxonGroup label/name*/) || undefined;
    if (source.startPosition || source.endPosition) {
      this.startPosition = source.startPosition && VesselPosition.fromObject(source.startPosition);
      this.endPosition = source.endPosition && VesselPosition.fromObject(source.endPosition);
      this.positions = undefined;
    }
    else if (source.positions) {
      const positions = source.positions.map(VesselPosition.fromObject).sort(sortByDateTimeFn) || undefined;
      if (positions.length >= 1 && positions.length <= 2) {
        this.startPosition = positions[0];
        if (positions.length > 1) {
          this.endPosition = positions[positions.length-1];
        }
        this.positions = undefined;
      }
      else {
        this.startPosition = undefined;
        this.endPosition = undefined;
        this.positions = positions;
      }
    }
    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || [];

    // Samples
    this.samples = source.samples && source.samples.map(Sample.fromObject) || undefined;

    // Batches list to tree
    this.catchBatch = Batch.fromObjectArrayAsTree(source.batches);

    // Remove fake dates (e.g. if endDateTime = startDateTime)
    if (this.endDateTime && this.endDateTime.isSameOrBefore(this.startDateTime)) {
      this.endDateTime = undefined;
    }
    if (this.fishingEndDateTime && this.fishingEndDateTime.isSameOrBefore(this.fishingStartDateTime)) {
      this.fishingEndDateTime = undefined;
    }
    if (this.endPosition && this.endPosition.dateTime && this.startPosition && this.endPosition.dateTime.isSameOrBefore(this.startPosition.dateTime)) {
      this.endPosition.dateTime = undefined;
    }

    return this;
  }

  equals(other: Operation): boolean {
    return super.equals(other)
      || ((this.startDateTime === other.startDateTime
        || (!this.startDateTime && !other.startDateTime && this.fishingStartDateTime === other.fishingStartDateTime))
        && ((!this.rankOrderOnPeriod && !other.rankOrderOnPeriod) || (this.rankOrderOnPeriod === other.rankOrderOnPeriod))
      );
  }
}

export class VesselPosition extends DataEntity<Operation> {

  static fromObject(source: any): VesselPosition {
    const res = new VesselPosition();
    res.fromObject(source);
    return res;
  }

  dateTime: Moment;
  latitude: number;
  longitude: number;
  operationId: number;

  constructor() {
    super();
  }

  clone(): Operation {
    const target = new Operation();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.dateTime = toDateISOString(this.dateTime);
    return target;
  }

  fromObject(source: any): VesselPosition {
    super.fromObject(source);
    this.latitude = source.latitude;
    this.longitude = source.longitude;
    this.operationId = source.operationId;
    this.dateTime = fromDateISOString(source.dateTime);
    return this;
  }

  equals(other: VesselPosition): boolean {
    return super.equals(other)
      || (this.dateTime === other.dateTime
        && (!this.operationId && !other.operationId || this.operationId === other.operationId));
  }
}
