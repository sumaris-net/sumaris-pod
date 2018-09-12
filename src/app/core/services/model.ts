import * as moment from "moment/moment";
import { Moment } from "moment/moment";

export const DATE_ISO_PATTERN = 'YYYY-MM-DDTHH:mm:ss.SSSZ';

export const StatusIds = {
  DISABLE: 0,
  ENABLE: 1,
  TEMPORARY: 2,
  DELETED: 3
}

export const LocationLevelIds = {
  COUNTRY: 1,
  PORT: 2
}


export const AcquisitionLevelCodes = {
  TRIP: 'TRIP',
  PHYSICAL_GEAR: 'PHYSICAL_GEAR',
  OPERATION: 'OPERATION',
  CATCH_BATCH: 'CATCH_BATCH',
  SORTING_BATCH: 'SORTING_BATCH',
  SAMPLE: 'SAMPLE'
}

export declare interface Cloneable<T> {
  clone(): T;
}

export const toDateISOString = function (value): string | undefined {
  if (!value) return undefined;
  if (typeof value == "string") {
    return value;
  }
  if (typeof value == "object" && value.toISOString) {
    return value.toISOString();
  }
  return moment(value).format(DATE_ISO_PATTERN) || undefined;
}

export const fromDateISOString = function (value): Moment | undefined {
  return value && moment(value, DATE_ISO_PATTERN) || undefined;

}

export function joinProperties(obj: any, properties: String[], separator?: string): string | undefined {
  if (!obj) throw "Could not display an undefined entity.";
  separator = separator || " - ";
  return properties.reduce((result: string, key: string, index: number) => {
    return index ? (result + separator + obj[key]) : obj[key];
  }, "");
}

export abstract class Entity<T> implements Cloneable<T> {
  id: number;
  updateDate: Date | Moment;
  dirty: boolean = false;

  abstract clone(): T;

  asObject(): any {
    const target: any = Object.assign({}, this);
    delete target.dirty;
    delete target.__typename;
    target.updateDate = toDateISOString(this.updateDate);
    return target;
  }

  fromObject(source: any): Entity<T> {
    this.id = source.id ? source.id : undefined;
    this.updateDate = fromDateISOString(source.updateDate);
    this.dirty = source.dirty;
    return this;
  }

  equals(other: Entity<T>): boolean {
    return other && this.id === other.id;
  }
}

export class Referential extends Entity<Referential>  {

  static fromObject(source: any): Referential {
    const res = new Referential();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  creationDate: Date | Moment;
  statusId: number;
  levelId: number;
  parentId: number;
  entityName: string;

  constructor(data?: {
    id?: number,
    label?: string,
    name?: string,
    parentId?: number,
    levelId?: number,
    entityName?: string
  }) {
    super();
    this.id = data && data.id;
    this.label = data && data.label;
    this.name = data && data.name;
    this.parentId = data && data.parentId;
    this.levelId = data && data.levelId;
    this.entityName = data && data.entityName;
  }

  clone(): Referential {
    return this.copy(new Referential());
  }

  copy(target: Referential): Referential {
    target.fromObject(this);
    return target;
  }

  asObject(): any {
    const target: any = super.asObject();
    target.creationDate = toDateISOString(this.creationDate);
    return target;
  }

  fromObject(source: any): Entity<Referential> {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.statusId = source.statusId;
    this.levelId = source.levelId;
    this.parentId = source.parentId;
    this.creationDate = fromDateISOString(source.creationDate);
    this.entityName = source.entityName;
    return this;
  }

  equals(other: Referential): boolean {
    return super.equals(other) && this.entityName === other.entityName;
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
    target.profiles = this.profiles && this.profiles.map(p => p.clone()) || undefined;
  }

  asObject(): any {
    const target: any = super.asObject();
    delete target.dirty;
    delete target.__typename;
    target.department = this.department && this.department.asObject() || undefined;
    target.profiles = this.profiles && this.profiles.map(p => p.asObject()) || undefined;
    target.creationDate = toDateISOString(this.creationDate);
    return target;
  }

  fromObject(source: any): Person {
    super.fromObject(source);
    this.firstName = source.firstName;
    this.lastName = source.lastName;
    this.email = source.email;
    this.creationDate = fromDateISOString(source.creationDate);
    this.pubkey = source.pubkey;
    this.avatar = source.avatar;
    this.statusId = source.statusId;
    source.department && this.department.fromObject(source.department);
    this.profiles = source.profiles && source.profiles.map(p => {
      const res = new Referential();
      res.fromObject(p);
      return res;
    }) || undefined;
    return this;
  }
}

export class Department extends Referential implements Cloneable<Department>{
  logo: string;

  constructor() {
    super();
  }

  clone(): Department {
    return this.copy(new Department());
  }

  copy(target: Department): Department {
    target.fromObject(this);
    return target;
  }

  fromObject(source: any): Department {
    super.fromObject(source);
    this.logo = source.logo;
    return this;
  }
}

export class UserSettings extends Entity<UserSettings> implements Cloneable<UserSettings> {
  locale: string;
  latLongFormat: string;

  content: string;
  nonce: string;

  clone(): UserSettings {
    const res = Object.assign(new UserSettings(), this);
    return res;
  }

  asObject(): any {
    const res: any = super.asObject();
    delete res.dirty;
    delete res.__typename;
    return res;
  }

  fromObject(source: any): UserSettings {
    super.fromObject(source);
    this.locale = source.locale;
    this.latLongFormat = source.latLongFormat;
    this.content = source.content;
    this.nonce = source.nonce;
    return this;
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
    const target: any = super.asObject();
    target.settings = this.settings && this.settings.asObject() || undefined;
    return target;
  }

  fromObject(source: any): Account {
    super.fromObject(source);
    source.settings && this.settings.fromObject(source.settings);
    return this;
  }
}
