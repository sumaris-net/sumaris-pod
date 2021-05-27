import {Moment} from "moment";
import {DataEntity, DataEntityAsObjectOptions,} from "../../../data/services/model/data-entity.model";
import {IEntityWithMeasurement, Measurement, MeasurementUtils, MeasurementValuesUtils} from "./measurement.model";
import {Sale} from "./sale.model";
import {isEmptyArray, isNotNil} from "../../../shared/functions";
import {FishingArea} from "./fishing-area.model";
import {NOT_MINIFY_OPTIONS, ReferentialRef} from "../../../core/services/model/referential.model";
import {DataRootVesselEntity} from "../../../data/services/model/root-vessel-entity.model";
import {IWithObserversEntity} from "../../../data/services/model/model.utils";
import {RootDataEntity} from "../../../data/services/model/root-data-entity.model";
import {Landing} from "./landing.model";
import {Person} from "../../../core/services/model/person.model";
import {EntityUtils} from "../../../core/services/model/entity.model";
import {fromDateISOString, toDateISOString} from "../../../shared/dates";
import {EntityClass} from "../../../core/services/model/entity.decorators";
import {Operation, OperationGroup} from "./operation.model";

/* -- Helper function -- */


/* -- Data -- */

@EntityClass({typename: "TripVO"})
export class Trip extends DataRootVesselEntity<Trip> implements IWithObserversEntity<Trip> {

  static fromObject: (source: any, opts?: any) => Trip;

  departureDateTime: Moment = null;
  returnDateTime: Moment = null;
  departureLocation: ReferentialRef = null;
  returnLocation: ReferentialRef = null;
  sale: Sale = null;
  gears: PhysicalGear[] = null;
  measurements: Measurement[] = null;
  observers: Person[] = null;
  metiers: ReferentialRef[] = null;
  operations?: Operation[] = null;
  operationGroups?: OperationGroup[] = null;
  fishingArea: FishingArea = null;

  landing?: Landing = null;
  observedLocationId?: number = null;

  constructor() {
    super(Trip.TYPENAME);
  }

  asObject(options?: DataEntityAsObjectOptions & { batchAsTree?: boolean }): any {
    const target = super.asObject(options);
    target.departureDateTime = toDateISOString(this.departureDateTime);
    target.returnDateTime = toDateISOString(this.returnDateTime);
    target.departureLocation = this.departureLocation && this.departureLocation.asObject({...options, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.returnLocation = this.returnLocation && this.returnLocation.asObject({...options, ...NOT_MINIFY_OPTIONS}) || undefined;
    target.sale = this.sale && this.sale.asObject(options) || undefined;
    target.gears = this.gears && this.gears.map(p => p && p.asObject(options)) || undefined;
    target.measurements = this.measurements && this.measurements.filter(MeasurementUtils.isNotEmpty).map(m => m.asObject(options)) || undefined;
    target.observers = this.observers && this.observers.map(p => p && p.asObject({...options, ...NOT_MINIFY_OPTIONS})) || undefined;

    // Metiers
    target.metiers = this.metiers && this.metiers.filter(isNotNil).map(p => p && p.asObject({...options, ...NOT_MINIFY_OPTIONS})) || undefined;
    if (isEmptyArray(target.metiers)) delete target.metiers; // Clean is empty, for compat with previous version

    // Operations
    target.operations = this.operations && this.operations.map(o => o.asObject(options)) || undefined;

    // Operation groups
    target.operationGroups = this.operationGroups && this.operationGroups.filter(isNotNil).map(o => o.asObject(options)) || undefined;
    if (isEmptyArray(target.operationGroups)) delete target.operationGroups; // Clean if empty, for compat with previous version

    // Fishing area
    target.fishingArea = this.fishingArea && this.fishingArea.asObject(options) || undefined;

    // Landing
    target.landing = this.landing && this.landing.asObject(options) || undefined;

    return target;
  }

  fromObject(source: any, opts?: any): Trip {
    super.fromObject(source);
    this.departureDateTime = fromDateISOString(source.departureDateTime);
    this.returnDateTime = fromDateISOString(source.returnDateTime);
    this.departureLocation = source.departureLocation && ReferentialRef.fromObject(source.departureLocation);
    this.returnLocation = source.returnLocation && ReferentialRef.fromObject(source.returnLocation);
    this.sale = source.sale && Sale.fromObject(source.sale) || undefined;

    this.gears = source.gears && source.gears.filter(isNotNil).map(PhysicalGear.fromObject)
      // Sort by rankOrder (useful for gears combo, in the operation form)
      .sort(EntityUtils.sortComparator('rankOrder')) || undefined;

    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || [];
    this.observers = source.observers && source.observers.map(Person.fromObject) || [];
    this.metiers = source.metiers && source.metiers.map(ReferentialRef.fromObject) || [];

    if (source.operations) {
      this.operations = source.operations
        .map(Operation.fromObject)
        .map((o:Operation) => {
          o.tripId = this.id;
          // Ling to trip's gear
          o.physicalGear = o.physicalGear && (this.gears || []).find(g => o.physicalGear.equals(g));
          return o;
        });
    }

    this.operationGroups = source.operationGroups && source.operationGroups.map(OperationGroup.fromObject) || [];

    // Remove fake dates (e.g. if returnDateTime = departureDateTime)
    if (this.returnDateTime && this.returnDateTime.isSameOrBefore(this.departureDateTime)) {
      this.returnDateTime = undefined;
    }

    this.fishingArea = source.fishingArea && FishingArea.fromObject(source.fishingArea) || undefined;

    this.landing = source.landing && Landing.fromObject(source.landing) || undefined;
    this.observedLocationId = source.observedLocationId;

    return this;
  }

  equals(other: Trip): boolean {
    return super.equals(other)
      || (
        // Same vessel
        (this.vesselSnapshot && other.vesselSnapshot && this.vesselSnapshot.id === other.vesselSnapshot.id)
        // Same departure date (or, if not set, same return date)
        && ((this.departureDateTime === other.departureDateTime)
          || (!this.departureDateTime && !other.departureDateTime && this.returnDateTime === other.returnDateTime))
      );
  }
}

@EntityClass({typename: 'PhysicalGearVO'})
export class PhysicalGear extends RootDataEntity<PhysicalGear> implements IEntityWithMeasurement<PhysicalGear> {

  static fromObject: (source: any, opts?: any) => PhysicalGear;

  rankOrder: number = null;
  gear: ReferentialRef = null;
  measurements: Measurement[] = null;
  measurementValues: { [key: string]: string } = {};

  // Parent (used when lookup gears)
  trip: Trip = null;
  tripId: number = null;

  constructor() {
    super(PhysicalGear.TYPENAME);
  }

  clone(): PhysicalGear {
    const target = new PhysicalGear();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: PhysicalGear) {
    target.fromObject(this);
  }

  asObject(opts?: DataEntityAsObjectOptions): any {
    const target = super.asObject(opts);
    target.gear = this.gear && this.gear.asObject({...opts, ...NOT_MINIFY_OPTIONS}) || undefined;
    // Fixme gear entityName here
    if (target.gear)
      target.gear.entityName = 'GearVO';

    target.rankOrder = this.rankOrder;

    // Measurements
    target.measurementValues = MeasurementValuesUtils.asObject(this.measurementValues, opts);
    if (isEmptyArray(target.measurements)) delete target.measurements;

    return target;
  }

  fromObject(source: any): PhysicalGear {
    super.fromObject(source);
    this.rankOrder = source.rankOrder;
    this.gear = source.gear && ReferentialRef.fromObject(source.gear);
    this.measurementValues = source.measurementValues && {...source.measurementValues} || MeasurementUtils.toMeasurementValues(source.measurements);

    // Parent trip
    if (source.trip) {
      this.trip = source.trip && Trip.fromObject(source.trip);
      this.tripId = this.trip && this.trip.id;
    }
    else {
      this.trip = null;
      this.tripId = null;
    }

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

@EntityClass({typename: 'VesselPositionVO'})
export class VesselPosition extends DataEntity<VesselPosition> {

  static fromObject: (source: any, opts?: any) => VesselPosition;

  dateTime: Moment;
  latitude: number;
  longitude: number;
  operationId: number;

  constructor() {
    super();
    this.__typename = VesselPosition.TYPENAME;
  }

  clone(): VesselPosition {
    const target = new VesselPosition();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);
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


