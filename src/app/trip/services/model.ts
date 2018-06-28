import {
  Referential, Department, Person,
  toDateISOString, fromDateISOString,
  vesselFeaturesToString, entityToString, referentialToString,
  StatusIds, Cloneable, Entity, LocationLevelIds, VesselFeatures
} from "../../referential/services/model";
import { Moment } from "moment/moment";

export {
  Referential, Person, Department,
  toDateISOString, fromDateISOString,
  vesselFeaturesToString, entityToString, referentialToString,
  StatusIds, Cloneable, Entity, VesselFeatures, LocationLevelIds
};

/* -- DATA -- */
export abstract class DataRootEntity<T> extends Entity<T> {
  comments: string;
  creationDate: Moment;
  recorderDepartment: Department;
  recorderPerson: Person;

  constructor() {
    super();
    this.recorderDepartment = new Department();
    this.recorderPerson = new Person();
  }

  asObject(): any {
    const target = super.asObject();
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject() || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject() || undefined;
    return target;
  }

  fromObject(source: any): DataRootEntity<T> {
    super.fromObject(source);
    this.comments = source.comments;
    this.creationDate = fromDateISOString(source.creationDate);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
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

  constructor() {
    super();
    this.departureLocation = new Referential();
    this.returnLocation = new Referential();
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
    return this;
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


export class Operation extends Entity<Operation> {

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
    target.startDateTime = toDateISOString(this.startDateTime);
    target.endDateTime = toDateISOString(this.endDateTime);
    target.fishingStartDateTime = toDateISOString(this.fishingStartDateTime);
    target.fishingEndDateTime = toDateISOString(this.fishingEndDateTime);
    target.positions = this.positions && this.positions.map(p => p.asObject()) || undefined;
    return target;
  }

  fromObject(source: any): Operation {
    super.fromObject(source);
    this.hasCatch = source.hasCatch;
    this.comments = source.comments;
    this.startDateTime = fromDateISOString(source.startDateTime);
    this.endDateTime = fromDateISOString(source.endDateTime);
    this.fishingStartDateTime = fromDateISOString(source.fishingStartDateTime);
    this.fishingEndDateTime = fromDateISOString(source.fishingEndDateTime);
    this.positions = source.positions && source.positions.map(VesselPosition.fromObject) || undefined;
    return this;
  }
}

export class VesselPosition extends Entity<Operation> {

  static fromObject(source: any): VesselPosition {
    const res = new VesselPosition();
    res.fromObject(source);
    return res;
  }

  dateTime: Moment;
  recorderDepartment: Referential;

  constructor() {
    super();
    this.recorderDepartment = new Referential();
  }

  clone(): Operation {
    const target = new Operation();
    target.fromObject(this.asObject());
    return target;
  }

  asObject(): any {
    const target = super.asObject();
    target.dateTime = toDateISOString(this.dateTime);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject() || undefined;
    return target;
  }

  fromObject(source: any): VesselPosition {
    super.fromObject(source);
    this.dateTime = fromDateISOString(source.dateTime);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
    return this;
  }
}