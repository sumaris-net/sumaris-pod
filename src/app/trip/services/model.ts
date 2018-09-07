import {
  Referential, Department, Person,
  toDateISOString, fromDateISOString,
  vesselFeaturesToString, entityToString, referentialToString,
  StatusIds, Cloneable, Entity, LocationLevelIds, VesselFeatures, GearLevelIds, TaxonGroupIds,
  PmfmStrategy
} from "../../referential/services/model";
import { Moment } from "moment/moment";

export {
  Referential, Person, Department,
  toDateISOString, fromDateISOString,
  vesselFeaturesToString, entityToString, referentialToString,
  StatusIds, Cloneable, Entity, VesselFeatures, LocationLevelIds, GearLevelIds, TaxonGroupIds,
  PmfmStrategy
};

/* -- Helper function -- */

export function fillRankOrder(values: { rankOrder: number }[]) {
  // Compute rankOrder
  let maxRankOrder = 0;
  (values || []).forEach(m => {
    if (m.rankOrder && m.rankOrder > maxRankOrder) maxRankOrder = m.rankOrder;
  });
  (values || []).forEach(m => {
    m.rankOrder = m.rankOrder || maxRankOrder++;
  });
}

/* -- DATA -- */
export abstract class DataEntity<T> extends Entity<T> {
  recorderDepartment: Department;

  constructor() {
    super();
    this.recorderDepartment = new Department();
  }

  asObject(): any {
    const target = super.asObject();
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject() || undefined;
    return target;
  }

  fromObject(source: any): DataEntity<T> {
    super.fromObject(source);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
    return this;
  }

}

export abstract class DataRootEntity<T> extends DataEntity<T> {
  comments: string;
  creationDate: Moment;
  recorderPerson: Person;

  constructor() {
    super();
    this.recorderPerson = new Person();
  }

  asObject(): any {
    const target = super.asObject();
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject() || undefined;
    return target;
  }

  fromObject(source: any): DataRootEntity<T> {
    super.fromObject(source);
    this.comments = source.comments;
    this.creationDate = fromDateISOString(source.creationDate);
    source.recorderPerson && this.recorderPerson.fromObject(source.recorderPerson);
    return this;
  }
}

export abstract class DataRootVesselEntity<T> extends DataRootEntity<T> {
  vesselFeatures: VesselFeatures;

  constructor() {
    super();
    this.vesselFeatures = new VesselFeatures();
  }

  asObject(): any {
    const target = super.asObject();
    target.vesselFeatures = this.vesselFeatures && this.vesselFeatures.asObject() || undefined;
    return target;
  }

  fromObject(source: any): DataRootVesselEntity<T> {
    super.fromObject(source);
    source.vesselFeatures && this.vesselFeatures.fromObject(source.vesselFeatures);
    return this;
  }
}

export class Trip extends DataRootVesselEntity<Trip> {

  static fromObject(source: any): Trip {
    const res = new Trip();
    res.fromObject(source);
    return res;
  }

  departureDateTime: Moment;
  returnDateTime: Moment;
  departureLocation: Referential;
  returnLocation: Referential;
  sale: Sale;
  gears: PhysicalGear[];
  measurements: Measurement[];

  constructor() {
    super();
    this.departureLocation = new Referential();
    this.returnLocation = new Referential();
    this.measurements = [];
  }

  clone(): Trip {
    const target = new Trip();
    target.fromObject(this.asObject);
    return target;
  }

  copy(target: Trip) {
    target.fromObject(this);
  }

  asObject(): any {
    const target = super.asObject();
    target.departureDateTime = toDateISOString(this.departureDateTime);
    target.returnDateTime = toDateISOString(this.returnDateTime);
    target.departureLocation = this.departureLocation && this.departureLocation.asObject() || undefined;
    target.returnLocation = this.returnLocation && this.returnLocation.asObject() || undefined;
    target.sale = this.sale && this.sale.asObject() || undefined;
    target.gears = this.gears && this.gears.map(p => p && p.asObject()) || undefined;
    target.measurements = this.measurements && this.measurements.map(m => m.asObject()) || undefined;
    return target;
  }

  fromObject(source: any): Trip {
    super.fromObject(source);
    this.departureDateTime = fromDateISOString(source.departureDateTime);
    this.returnDateTime = fromDateISOString(source.returnDateTime);
    source.departureLocation && this.departureLocation.fromObject(source.departureLocation);
    source.returnLocation && this.returnLocation.fromObject(source.returnLocation);
    if (source.sale) {
      this.sale = new Sale();
      this.sale.fromObject(source.sale);
    };
    this.gears = source.gears && source.gears.filter(g => !!g).map(PhysicalGear.fromObject) || undefined;
    this.measurements = source.measurements && source.measurements.filter(m => !!m).map(Measurement.fromObject) || undefined;
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


export class PhysicalGear extends DataRootEntity<PhysicalGear> {

  static fromObject(source: any): PhysicalGear {
    const res = new PhysicalGear();
    res.fromObject(source);
    return res;
  }

  gear: Referential;
  comments: string;
  measurements: Measurement[];
  rankOrder: number;

  constructor() {
    super();
    this.gear = new Referential();
    this.measurements = [];
  }

  clone(): PhysicalGear {
    const target = new PhysicalGear();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: PhysicalGear) {
    target.fromObject(this);
  }

  asObject(): any {
    const target = super.asObject();
    target.gear = this.gear && this.gear.asObject() || undefined;

    // Measurements
    target.measurements = this.measurements && this.measurements.map(m => m.asObject()) || undefined;

    return target;
  }

  fromObject(source: any): PhysicalGear {
    super.fromObject(source);
    this.rankOrder = source.rankOrder;
    this.comments = source.comments;
    source.gear && this.gear.fromObject(source.gear);

    // Measurements
    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || undefined;

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

export class Measurement extends DataEntity<Measurement> {
  pmfmId: number;
  alphanumericalValue: string;
  numericalValue: number;
  qualitativeValue: Referential;
  digitCount: number;
  rankOrder: number;

  static fromObject(source: any): Measurement {
    const res = new Measurement();
    res.fromObject(source);
    return res;
  }

  constructor() {
    super();
  }

  clone(): Measurement {
    const target = new Measurement();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: Measurement) {
    target.fromObject(this);
  }

  asObject(): any {
    const target = super.asObject();
    target.qualitativeValue = this.qualitativeValue && this.qualitativeValue.id && { id: this.qualitativeValue.id };
    return target;
  }

  fromObject(source: any): Measurement {
    super.fromObject(source);
    this.pmfmId = source.pmfmId;
    this.alphanumericalValue = source.alphanumericalValue;
    this.numericalValue = source.numericalValue;
    this.digitCount = source.digitCount;
    this.qualitativeValue = source.qualitativeValue && Referential.fromObject(source.qualitativeValue);

    return this;
  }

  equals(other: Measurement): boolean {
    return super.equals(other)
      || (
        // Same [pmfmId, rankOrder]
        (this.pmfmId && other.pmfmId && this.rankOrder === other.rankOrder)
      );
  }

  isEmpty(): boolean {
    return !this.alphanumericalValue
      && this.numericalValue === undefined
      && !(this.qualitativeValue && this.qualitativeValue.id)
  }
}

export class Sale extends DataRootVesselEntity<Sale> {
  startDateTime: Moment;
  endDateTime: Moment;
  saleLocation: Referential;
  saleType: Referential;

  constructor() {
    super();
    this.saleLocation = new Referential();
    this.saleType = new Referential();
  }

  clone(): Sale {
    const target = new Sale();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: Sale) {
    target.fromObject(this);
  }

  asObject(): any {
    const target = super.asObject();
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.saleLocation = this.saleLocation && this.saleLocation.asObject() || undefined;
    target.saleType = this.saleType && this.saleType.asObject() || undefined;
    return target;
  }

  fromObject(source: any): Sale {
    super.fromObject(source);
    this.startDateTime = fromDateISOString(source.startDateTime);
    this.endDateTime = fromDateISOString(source.endDateTime);
    source.saleLocation && this.saleLocation.fromObject(source.saleLocation);
    source.saleType && this.saleType.fromObject(source.saleType);
    return this;
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

  metier: Referential;
  physicalGear: PhysicalGear;
  tripId: number;

  measurements: Measurement[];

  constructor() {
    super();
    this.metier = new Referential();
    this.startPosition = new VesselPosition();
    this.endPosition = new VesselPosition();
    this.physicalGear = new PhysicalGear();
    this.measurements = [];
  }

  clone(): Operation {
    const target = new Operation();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(): any {
    const target = super.asObject();
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.fishingStartDateTime = toDateISOString(this.fishingStartDateTime);
    target.fishingEndDateTime = toDateISOString(this.fishingEndDateTime);
    target.metier = this.metier && this.metier.asObject() || undefined;

    // Create an array of position, instead of start/end
    target.positions = [this.startPosition, this.endPosition].map(p => p && p.asObject()) || undefined;
    delete target.startPosition;
    delete target.endPosition;

    // Physical gear: keep id
    target.physicalGearId = this.physicalGear && this.physicalGear.id;
    delete target.physicalGear;

    // Measurements
    target.measurements = this.measurements && this.measurements.map(m => m.asObject()) || undefined;

    return target;
  }

  fromObject(source: any): Operation {
    super.fromObject(source);
    this.hasCatch = source.hasCatch;
    this.comments = source.comments;
    this.tripId = source.tripId;
    this.physicalGear = source.physicalGear && PhysicalGear.fromObject(source.physicalGear) || new PhysicalGear();
    this.physicalGear.id = this.physicalGear.id || source.physicalGearId;
    this.startDateTime = fromDateISOString(source.startDateTime);
    this.endDateTime = fromDateISOString(source.endDateTime);
    this.fishingStartDateTime = fromDateISOString(source.fishingStartDateTime);
    this.fishingEndDateTime = fromDateISOString(source.fishingEndDateTime);
    this.rankOrderOnPeriod = source.rankOrderOnPeriod;
    source.metier && this.metier.fromObject(source.metier);
    this.positions = source.positions && source.positions.map(VesselPosition.fromObject) || undefined;
    if (this.positions && this.positions.length == 2) {
      this.startPosition = this.positions[0];
      this.endPosition = this.positions[1];
    }
    delete this.positions;
    this.measurements = source.measurements && source.measurements.map(Measurement.fromObject) || undefined;
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

  asObject(): any {
    const target = super.asObject();
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