import * as moment from "moment/moment";
import {Moment} from "moment/moment";
import {DATE_ISO_PATTERN} from "../app/constants";

export const StatusIds = {
  DISABLE: 0,
  ENABLE: 1,
  TEMPORARY: 2
}

export declare interface Cloneable<T> {
  clone(): T;
}

export const toDateISOString = function(value) : string | undefined{
  if (!value) return undefined;
  if (typeof value == "string") {
    return value;
  }
  if (typeof value == "object" && value.toISOString) {
    return value.toISOString();
  }
  return moment(value).format(DATE_ISO_PATTERN) || undefined;
}


export const fromDateISOString = function(value): Moment | undefined {
  return value && moment(value, DATE_ISO_PATTERN) || undefined;

}

export abstract class Entity<T> implements Cloneable<T> {
  id: number;
  updateDate: Date | Moment;
  dirty: boolean = false;

  abstract clone(): T;

  asObject(): any {
    const target:any = Object.assign({}, this);
    delete target.dirty;
    delete target.__typename;
    return target;
  }

  fromObject(source:any) {
    this.id = source.id ? source.id : undefined;
    this.updateDate = source.updateDate;
    this.dirty = source.dirty;
  }
}


export class Referential extends Entity<Referential>  {
  label: string;
  name: string;

  constructor(data?: {
    id?: number,
    label?: string,
    name?: string} ) {
    super();
    this.id = data && data.id;
    this.label = data && data.label;
    this.name = data && data.name;
  }

  clone(): Referential {
    return Object.assign(new Referential(), this);
  }

  fromObject(source:any) {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
  }
}

export class VesselFeatures extends Entity<VesselFeatures>  {
  vesselId: number;
  vesselTypeId: number;
  name: string;
  exteriorMarking: string;
  basePortLocation: Referential;  
  creationDate: Date | Moment;
  recorderDepartment: Referential;
  recorderPerson: Person;

  constructor() {
    super();
    this.basePortLocation = new Referential();
    this.recorderDepartment = new Referential();
    this.recorderPerson = new Person();
  }

  clone(): VesselFeatures {
    const target = new VesselFeatures();
    this.copy(target);
    target.basePortLocation = this.basePortLocation && this.basePortLocation.clone() || undefined;
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.clone() || undefined;
    target.recorderPerson  = this.recorderPerson && this.recorderPerson.clone() || undefined;
    return target;
  }

  copy(target: VesselFeatures): VesselFeatures {
    target.fromObject(this);
    return target;
  }

  asObject(): any {
    const target:any = super.asObject();
    target.basePortLocation = this.basePortLocation && this.basePortLocation.asObject() || undefined;
    target.creationDate = toDateISOString(this.creationDate);
    target.updateDate = toDateISOString(this.updateDate);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject() || undefined;
    target.recorderPerson  = this.recorderPerson && this.recorderPerson.asObject() || undefined;

    return target;
  }

  fromObject(source:any): VesselFeatures {
    super.fromObject(source);
    this.exteriorMarking = source.exteriorMarking;
    this.name = source.name;
    this.vesselId = source.vesselId;
    this.vesselTypeId = source.vesselTypeId;
    this.creationDate = fromDateISOString(source.creationDate);
    this.updateDate = fromDateISOString(source.updateDate);
    source.basePortLocation && this.basePortLocation.fromObject(source.basePortLocation);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
    source.recorderPerson  && this.recorderPerson.fromObject(source.recorderPerson);
    return this;
  }
}

export class Trip extends Entity<Trip> {
  departureDateTime: Moment;
  returnDateTime: Moment;
  comments: string;
  creationDate:  Moment;
  departureLocation: Referential;
  returnLocation: Referential;
  recorderDepartment: Referential;
  recorderPerson: Person;
  vesselFeatures: VesselFeatures;

  constructor() {
    super();
    this.departureLocation = new Referential();
    this.returnLocation = new Referential();
    this.recorderDepartment = new Referential();
    this.recorderPerson = new Person();
    this.vesselFeatures = new VesselFeatures();
    this.dirty = false;
  }

  clone(): Trip {
    const res = Object.assign(new Referential(), this);
    res.departureLocation = this.departureLocation && this.departureLocation.clone() || undefined;
    res.returnLocation = this.returnLocation && this.returnLocation.clone() || undefined;
    res.recorderDepartment = this.recorderDepartment && this.recorderDepartment.clone() || undefined;
    res.recorderPerson  = this.recorderPerson && this.recorderPerson.clone() || undefined;
    res.vesselFeatures = this.vesselFeatures && this.vesselFeatures.clone() || undefined;
    return res;
  }

  copy(target: Trip) {
    target.fromObject(this);
  }

  asObject(): any {
    const target:any = Object.assign({}, this);
    delete target.dirty;
    delete target.__typename;
    target.departureDateTime = toDateISOString(this.departureDateTime);
    target.returnDateTime = toDateISOString(this.returnDateTime);
    target.creationDate = toDateISOString(this.creationDate);
    target.updateDate = toDateISOString(this.updateDate);
    target.departureLocation = this.departureLocation && this.departureLocation.asObject() || undefined;
    target.returnLocation = this.returnLocation && this.returnLocation.asObject() || undefined;
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject() || undefined;
    target.recorderPerson  = this.recorderPerson && this.recorderPerson.asObject() || undefined;
    target.vesselFeatures = this.vesselFeatures && this.vesselFeatures.asObject() || undefined;
    return target;
  }

  fromObject(source:any) {
    super.fromObject(source);
    this.departureDateTime = fromDateISOString(source.departureDateTime);
    this.returnDateTime = fromDateISOString(source.returnDateTime);
    this.comments = source.comments;
    this.creationDate = fromDateISOString(source.creationDate);
    this.updateDate  = fromDateISOString(source.updateDate);
    source.departureLocation && this.departureLocation.fromObject(source.departureLocation);
    source.returnLocation && this.returnLocation.fromObject(source.returnLocation);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
    source.recorderPerson  && this.recorderPerson.fromObject(source.recorderPerson);
    source.vesselFeatures && this.vesselFeatures.fromObject(source.vesselFeatures);
  }
}

export class Person extends Entity<Person> implements Cloneable<Person> {
  firstName: string;
  lastName: string;
  email: string;
  pubkey: string;
  avatar: string;
  creationDate: Date | Moment;
  statusId: number;
  department: Referential;
  profiles: Referential[];

  constructor() {
    super();
    this.department = new Referential();
  }
  
  clone(): Person {
    const target = new Person();
    this.copy(target);
    return target;
  }

  copy(target: Person) {
    Object.assign(target, this);
    target.department = this.department.clone();
    target.profiles =  this.profiles && this.profiles.map(p => p.clone()) || undefined;
  }

  asObject(): any {
    const target:any = super.asObject();
    delete target.dirty;
    delete target.__typename;
    target.department = this.department && this.department.asObject() || undefined;
    target.profiles = this.profiles && this.profiles.map(p => p.asObject()) || undefined;
    target.creationDate = toDateISOString(this.creationDate);
    target.updateDate = toDateISOString(this.updateDate);
    return target;
  }

  fromObject(source:any) {
    super.fromObject(source);
    this.firstName = source.firstName;
    this.lastName = source.lastName;
    this.email = source.email;
    this.creationDate = fromDateISOString(source.creationDate);
    this.updateDate = fromDateISOString(source.updateDate);
    this.pubkey = source.pubkey;
    this.avatar = source.avatar;
    this.statusId = source.statusId;
    source.department && this.department.fromObject(source.department);
    this.profiles = source.profiles && source.profiles.map(p => {
      const res = new Referential();
      res.fromObject(p);
      return res;
    }) || undefined;
  }
}


export class UserSettings extends Entity<UserSettings> implements Cloneable<UserSettings> {
  locale: string;

  clone(): UserSettings {
    const res = Object.assign(new UserSettings(), this);
    return res;
  }

  asObject(): any {
    const res:any = super.asObject();
    delete res.dirty;
    delete res.__typename;
    return res;
  }

  fromObject(source:any) {
    super.fromObject(source);
    this.locale = source.locale;
  }
}

/** 
 * An user account
 */
export class Account extends Person {
  settings: UserSettings;

  constructor() {
    super();
    this.settings = new UserSettings();
  }  

  clone(): Account {
    const target = new Account();
    super.copy(target);
    return target;
  }

  copy(target: Account): Account {
    super.copy(target);
    target.settings = this.settings && this.settings.clone() || undefined;
    return target;
  }

  asObject(): any {
    const target:any = super.asObject();
    target.settings = this.settings && this.settings.asObject() || undefined;
    return target;
  }

  fromObject(source:any) {
    super.fromObject(source);
    source.settings && this.settings.fromObject(source.settings);
  }
}
