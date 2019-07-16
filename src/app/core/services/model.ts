import {Moment} from "moment/moment";
import {fromDateISOString, isNil, isNotNil, toDateISOString} from "../../shared/shared.module";
import {isNilOrBlank} from "../../shared/functions";

export const DATE_ISO_PATTERN = 'YYYY-MM-DDTHH:mm:ss.SSSZ';

export const StatusIds = {
  DISABLE: 0,
  ENABLE: 1,
  TEMPORARY: 2,
  DELETED: 3,
  ALL: 99
};

export const LocationLevelIds = {
  COUNTRY: 1,
  PORT: 2,
  AUCTION: 3
};

export const AcquisitionLevelCodes = {
  TRIP: 'TRIP',
  PHYSICAL_GEAR: 'PHYSICAL_GEAR',
  OPERATION: 'OPERATION',
  CATCH_BATCH: 'CATCH_BATCH',
  SORTING_BATCH: 'SORTING_BATCH',
  SORTING_BATCH_INDIVIDUAL: 'SORTING_BATCH_INDIVIDUAL',
  SAMPLE: 'SAMPLE',
  SURVIVAL_TEST: 'SURVIVAL_TEST',
  INDIVIDUAL_MONITORING: 'INDIVIDUAL_MONITORING',
  INDIVIDUAL_RELEASE: 'INDIVIDUAL_RELEASE',
  LANDING: 'LANDING',
  SALE: 'SALE',
  OBSERVED_LOCATION: 'OBSERVED_LOCATION',
  OBSERVED_VESSEL: 'OBSERVED_VESSEL'
};

export declare type ConfigOptionType = 'integer' | 'double' | 'boolean' | 'string' | 'enum' | 'color';

export declare interface ConfigOptionEnum {
  key: string;
  label: string;
}

export declare interface ConfigOption {
  key: string;
  label: string;
  defaultValue?: any;
  isTransient?: boolean; // Useful only for remote configuration
  values?: ConfigOptionEnum[];
  type: ConfigOptionType;
}

export const ConfigOptions = {
  LOGO: {
    key: 'sumaris.logo',
    label: 'CONFIGURATION.OPTIONS.LOGO',
    type: 'string'
  },
  FAVICON: {
    key: 'sumaris.favicon',
    label: 'CONFIGURATION.OPTIONS.FAVICON',
    type: 'string'
  },
  DEFAULT_LOCALE: {
    key: 'sumaris.defaultLocale',
    label: 'CONFIGURATION.OPTIONS.DEFAULT_LOCALE',
    type: 'enum',
    values: [
      {
        key: 'en',
        label: 'English'
      },
      {
        key: 'fr',
        label: 'Français'
      }
    ]
  },
  DEFAULT_LAT_LONG_FORMAT: {
    key: 'sumaris.defaultLatLongFormat',
    label: 'CONFIGURATION.OPTIONS.DEFAULT_LATLONG_FORMAT',
    type: 'enum',
    values: [
      {
        key: 'DDMMSS',
        label: 'COMMON.DDMMSS_PLACEHOLDER'
      },
      {
        key: 'DDMM',
        label: 'COMMON.DDMM_PLACEHOLDER'
      },
      {
        key: 'DD',
        label: 'COMMON.DD_PLACEHOLDER'
      }
    ]
  },
  LOGO_LARGE: {
    key: 'sumaris.logo.large',
    label: 'CONFIGURATION.OPTIONS.HOME.LOGO_LARGE',
    type: 'string'
  },
  HOME_PARTNERS_DEPARTMENTS: {
    key: 'sumaris.partner.departments',
    label: 'CONFIGURATION.OPTIONS.HOME.PARTNER_DEPARTMENTS',
    type: 'string'
  },
  HOME_BACKGROUND_IMAGE: {
    key: 'sumaris.background.images',
    label: 'CONFIGURATION.OPTIONS.HOME.BACKGROUND_IMAGES',
    type: 'string'
  },
  COLOR_PRIMARY: {
    key: 'sumaris.color.primary',
    label: 'CONFIGURATION.OPTIONS.COLORS.PRIMARY',
    type: 'color'
  },
  COLOR_SECONDARY: {
    key: 'sumaris.color.secondary',
    label: 'CONFIGURATION.OPTIONS.COLORS.SECONDARY',
    type: 'color'
  },
  COLOR_TERTIARY: {
    key: 'sumaris.color.tertiary',
    label: 'CONFIGURATION.OPTIONS.COLORS.TERTIARY',
    type: 'color'
  }
};

export type UsageMode = 'DESK' | 'FIELD';

export type UserProfileLabel = 'ADMIN' | 'USER' | 'SUPERVISOR' | 'GUEST';

export const PRIORITIZED_USER_PROFILES: UserProfileLabel[] = ['ADMIN', 'SUPERVISOR', 'USER', 'GUEST'];

export function getMainProfile(profiles?: string[]): UserProfileLabel {
  return profiles && profiles.length && PRIORITIZED_USER_PROFILES.find(pp => profiles.indexOf(pp) > -1) || 'GUEST';
}

export function getMainProfileIndex(profiles?: string[]): number {
  if (!profiles && !profiles.length) return PRIORITIZED_USER_PROFILES.length - 1; // return last profile
  const index = PRIORITIZED_USER_PROFILES.findIndex(pp => profiles.indexOf(pp) > -1);
  return (index !== -1) ? index : (PRIORITIZED_USER_PROFILES.length - 1);
}

export function hasUpperOrEqualsProfile(actualProfiles: string[], expectedProfile: UserProfileLabel): boolean {
  const expectedProfileIndex = PRIORITIZED_USER_PROFILES.indexOf(expectedProfile);
  return expectedProfileIndex !== -1 && getMainProfileIndex(actualProfiles) <= expectedProfileIndex;
}

export declare interface Cloneable<T> {
  clone(): T;
}

export function joinProperties(obj: any, properties: String[], separator?: string): string | undefined {
  if (!obj) throw "Could not display an undefined entity.";
  separator = separator || " - ";
  return properties.reduce((result: string, key: string, index: number) => {
    return index ? (result + separator + obj[key]) : obj[key];
  }, "");
}

export function attributeComparator<T>(attribute: string): (a: T, b: T) => number {
  return (a: T, b: T) => {
    const valueA = a[attribute];
    const valueB = b[attribute];
    return valueA === valueB ? 0 : (valueA > valueB ? 1 : -1);
  };
}

export function sort<T>(array: T[], attribute: string): T[] {
  return array.sort((a, b) => {
    const valueA = a[attribute];
    const valueB = b[attribute];
    return valueA === valueB ? 0 : (valueA > valueB ? 1 : -1);
  });
}

export function entityToString(obj: Entity<any> | any, properties?: String[]): string | undefined {
  return obj && obj.id && joinProperties(obj, properties || ['name']) || undefined;
}

export function referentialToString(obj: Referential | ReferentialRef | any, properties?: String[]): string | undefined {
  return obj && obj.id && joinProperties(obj, properties || ['label', 'name']) || undefined;
}

export function personToString(obj: Person): string {
  return obj && obj.id && (obj.lastName + ' ' + obj.firstName) || undefined;
}

export function personsToString(data: Person[], separator?: string): string {
  if (!data || !data.length) return '';
  separator = separator || ", ";
  return data.reduce((result: string, person: Person, index: number) => {
    return index ? (result + separator + personToString(person)) : personToString(person);
  }, '');
}

export abstract class Entity<T> implements Cloneable<T> {
  id: number;
  updateDate: Date | Moment;

  abstract clone(): T;

  asObject(minify?: boolean): any {
    const target: any = Object.assign({}, this);
    delete target.__typename;
    target.updateDate = toDateISOString(this.updateDate);
    return target;
  }

  fromObject(source: any): Entity<T> {
    this.id = (source.id || source.id === 0) ? source.id : undefined;
    this.updateDate = fromDateISOString(source.updateDate);
    return this;
  }

  equals(other: Entity<T>): boolean {
    return other && this.id === other.id;
  }
}

export class EntityUtils {
  static isNotEmpty(obj: any | Entity<any>): boolean {
    return !!obj && obj['id'];
  }

  static isEmpty(obj: any | Entity<any>): boolean {
    return !obj || !obj['id'];
  }

  static getPropertyByPath(obj: any | Entity<any>, path: string): any {
    if (isNil(obj)) return undefined;
    const i = path.indexOf('.');
    if (i === -1) {
      return obj[path];
    }
    const key = path.substring(0, i);
    if (isNil(obj[key])) return undefined;
    if (obj[key] && typeof obj[key] === "object") {
      return EntityUtils.getPropertyByPath(obj[key], path.substring(i + 1));
    }
    throw new Error(`Invalid form path: '${key}' is not an valid object.`);
  }

  static getMapAsArray(source?: Map<string, string>): { key: string; value?: string; }[] {
    return Object.getOwnPropertyNames(source || {})
      .map(key => {
        return {
          key,
          value: source[key]
        };
      });
  }

  static getArrayAsMap(source?: { key: string; value?: string; }[]): Map<string, string> {
    const target = new Map<string, string>();
    (source || []).forEach(item => target.set(item.key, item.value));
    return target;
  }

  static getObjectAsArray(source?: { [key: string]: string }): { key: string; value?: string; }[] {
    return Object.getOwnPropertyNames(source || {})
      .map(key => {
        return {
          key,
          value: source[key]
        };
      });
  }

  static getArrayAsObject(source?: { key: string; value?: string; }[]): { [key: string]: string } {
    return (source || []).reduce((res, item) => {
      res[item.key] = item.value;
      return res;
    }, {});
  }

  static equals(o1: Entity<any>, o2: Entity<any>): boolean {
    return (!!o1 === !!o2) || (o1 && o2 && o1.id === o2.id);
  }

  static copyIdAndUpdateDate(source: Entity<any> | undefined, target: Entity<any>, opts?: { creationDate?: boolean; }) {
    if (!source) return;

    // Update (id and updateDate)
    target.id = isNotNil(source.id) ? source.id : target.id;
    target.updateDate = fromDateISOString(source.updateDate) || target.updateDate;

    // Update creation Date, if exists
    if (source['creationDate']) {
      target['creationDate'] = fromDateISOString(source['creationDate']);
    }
  }

  static sort<T extends Entity<T> | any>(data: T[], sortBy?: string, sortDirection?: string): T[] {
    const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
    return data.sort((a, b) => {
      const valueA = EntityUtils.getPropertyByPath(a, sortBy);
      const valueB = EntityUtils.getPropertyByPath(b, sortBy);
      return valueA === valueB ? 0 : (valueA > valueB ? after : (-1 * after));
    });
  }
}

/* -- Referential -- */

export class Referential extends Entity<Referential> {

  static fromObject(source: any): Referential {
    const res = new Referential();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  description: string;
  comments: string;
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

  asObject(minify?: boolean): any {
    const target: any = super.asObject(minify);
    target.creationDate = toDateISOString(this.creationDate);
    return target;
  }

  fromObject(source: any): Entity<Referential> {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.description = source.description;
    this.comments = source.comments;
    this.statusId = source.statusId;
    this.levelId = source.levelId && source.levelId !== 0 ? source.levelId : undefined; // Do not set as null (need for account.department, when regsiter)
    this.parentId = source.parentId;
    this.creationDate = fromDateISOString(source.creationDate);
    this.entityName = source.entityName;
    return this;
  }

  equals(other: Referential): boolean {
    return super.equals(other) && this.entityName === other.entityName;
  }
}

export declare interface IReferentialRef {
  label: string;
  name: string;
  statusId: number;
  entityName: string;
}

export class ReferentialRef extends Entity<ReferentialRef> implements IReferentialRef {

  static fromObject(source: any): ReferentialRef {
    if (!source || source instanceof ReferentialRef) return source;
    const res = new ReferentialRef();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  statusId: number;
  entityName: string;

  constructor(data?: {
    id?: number,
    label?: string,
    name?: string
  }) {
    super();
    this.id = data && data.id;
    this.label = data && data.label;
    this.name = data && data.name;
  }

  clone(): ReferentialRef {
    return this.copy(new ReferentialRef());
  }

  copy(target: ReferentialRef): ReferentialRef {
    target.fromObject(this);
    return target;
  }

  asObject(minify?: boolean): any {
    if (minify) return {id: this.id}; // minify=keep id only
    const target: any = super.asObject();
    delete target.entityName;
    return target;
  }

  fromObject(source: any): Entity<ReferentialRef> {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.statusId = source.statusId;
    this.entityName = source.entityName;
    return this;
  }
}


/* -- Configuration -- */

export class Configuration extends Entity<Configuration> {

  static fromObject(source: Configuration): Configuration {
    if (!source || source instanceof Configuration) return source;
    const res = new Configuration();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  creationDate: Date | Moment;
  statusId: number;

  smallLogo: string;
  largeLogo: string;
  properties: { [key: string]: string };
  backgroundImages: string[];
  partners: Department[];

  constructor() {
    super();
  }

  clone(): Configuration {
    return this.copy(new Configuration());
  }

  copy(target: Configuration): Configuration {
    target.fromObject(this);
    return target;
  }

  asObject(minify?: boolean): any {
    if (minify) return {id: this.id}; // minify=keep id only
    const target: any = super.asObject();
    target.creationDate = toDateISOString(this.creationDate);
    target.partners = (this.partners || []).map(p => p.asObject());
    target.properties = this.properties;
    return target;
  }

  fromObject(source: any): Configuration {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.creationDate = fromDateISOString(source.creationDate);
    this.smallLogo = source.smallLogo;
    this.largeLogo = source.largeLogo;
    this.backgroundImages = source.backgroundImages;
    this.partners = (source.partners || []).map(Department.fromObject);

    if (source.properties && source.properties instanceof Array) {
      this.properties = EntityUtils.getArrayAsObject(source.properties);
    } else {
      this.properties = source.properties;
    }

    return this;
  }
}


export class Person extends Entity<Person> implements Cloneable<Person> {

  static fromObject(source: any): Person {
    if (!source || source instanceof Person) return source;
    const result = new Person();
    result.fromObject(source);
    return result;
  }

  firstName: string;
  lastName: string;
  email: string;
  pubkey: string;
  avatar: string;
  creationDate: Date | Moment;
  statusId: number;
  department: Department;
  profiles: UserProfileLabel[];
  mainProfile: UserProfileLabel;

  constructor() {
    super();
    this.department = new Department();
  }

  clone(): Person {
    const target = new Person();
    this.copy(target);
    return target;
  }

  copy(target: Person) {
    Object.assign(target, this);
    target.department = this.department.clone();
    target.profiles = this.profiles && this.profiles.slice(0) || undefined;
  }

  asObject(minify?: boolean): any {
    if (minify) return {id: this.id}; // minify=keep id only
    const target: any = super.asObject();
    target.department = this.department && this.department.asObject() || undefined;
    target.profiles = this.profiles && this.profiles.slice(0) || [];
    // Set profile list from the main profile
    target.profiles = this.mainProfile && [this.mainProfile] || target.profiles || ['GUEST'];
    target.creationDate = toDateISOString(this.creationDate);

    if (!minify) target.mainProfile = getMainProfile(target.profiles);
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
    this.profiles = source.profiles && source.profiles.slice(0) || [];
    // Add main profile to the list, if need
    if (source.mainProfile && !this.profiles.find(p => p === source.mainProfile)) {
      this.profiles = this.profiles.concat(source.mainProfile);
    }
    this.mainProfile = getMainProfile(this.profiles);
    return this;
  }
}

export class Department extends Referential implements Cloneable<Department> {

  logo: string;
  siteUrl: string;

  static fromObject(source: any): Department {
    if (!source || source instanceof Department) return source;
    const res = new Department();
    res.fromObject(source);
    return res;
  }

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

  asObject(minify?: boolean): any {
    if (minify) return {id: this.id}; // minify=keep id only
    const target: any = super.asObject();
    delete target.entityName;
    return target;
  }

  fromObject(source: any): Department {
    super.fromObject(source);
    this.logo = source.logo;
    this.siteUrl = source.siteUrl;
    delete this.entityName; // not need
    return this;
  }
}

export class UserSettings extends Entity<UserSettings> implements Cloneable<UserSettings> {
  locale: string;
  latLongFormat: string;
  content: {};
  nonce: string;

  clone(): UserSettings {
    const res = Object.assign(new UserSettings(), this);
    return res;
  }

  asObject(minify?: boolean): any {
    const res: any = super.asObject(minify);
    res.content = this.content && JSON.stringify(res.content) || undefined;
    return res;
  }

  fromObject(source: any): UserSettings {
    super.fromObject(source);
    this.locale = source.locale;
    this.latLongFormat = source.latLongFormat;
    if (isNil(source.content) || typeof source.content === 'object') {
      this.content = source.content || {};
    } else {
      this.content = source.content && JSON.parse(source.content) || {};
    }
    this.nonce = source.nonce;
    return this;
  }
}

/**
 * A user account
 */
export class Account extends Person {

  static fromObject(source: any): Account {
    if (!source || source instanceof Account) return source;
    const result = new Account();
    result.fromObject(source);
    return result;
  }

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

  asObject(minify?: boolean): any {
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

/* -- Network -- */

export class Peer extends Entity<Peer> implements Cloneable<Peer> {

  static fromObject(source: any): Peer {
    if (!source || source instanceof Peer) return source;
    const res = new Peer();
    res.fromObject(source);
    return res;
  }

  static parseUrl(peerUrl: string) {
    const url = new URL(peerUrl);
    return Peer.fromObject({
      dns: url.hostname,
      port: isNilOrBlank(url.port) ? undefined : url.port,
      useSsl: url.protocol && (url.protocol.startsWith('https') || url.protocol.startsWith('wss'))
    });
  }

  dns: string;
  ipv4: string;
  ipv6: string;
  port: number;
  useSsl: boolean;
  pubkey: string;

  favicon: string;
  status: 'UP' | 'DOWN';
  softwareName: string;
  softwareVersion: string;
  label: string;
  name: string;

  constructor() {
    super();
  }

  clone(): Peer {
    return this.copy(new Peer());
  }

  copy(target: Peer): Peer {
    target.fromObject(this);
    return target;
  }

  asObject(minify?: boolean): any {
    const target: any = super.asObject(minify);
    return target;
  }

  fromObject(source: any): Entity<Peer> {
    super.fromObject(source);
    this.dns = source.dns;
    this.ipv4 = source.ipv4;
    this.ipv6 = source.ipv6;
    this.port = isNotNil(source.port) ? +source.port : undefined;
    this.pubkey = source.pubkey;
    this.useSsl = source.useSsl || (this.port === 443);
    return this;
  }

  equals(other: Peer): boolean {
    return super.equals(other) && this.pubkey === other.pubkey && this.url === other.url;
  }

  get url(): string {
    return (this.useSsl ? 'https://' : 'http://') + this.hostAndPort;
  }

  get hostAndPort(): string {
    return (this.dns || this.ipv4 || this.ipv6) +
      ((this.port && this.port !== 80 && this.port !== 443) ? ':' + this.port : '');
  }

  get reachable(): boolean {
    return this.status && this.status === 'UP';
  }
}

/* -- Local settings -- */
export declare class FieldOptions {
  key: string;
  attributes: string;
  attributesArray?: string[];
  searchAttribute?: string;
  //defaultValues: any[];
}

export declare interface LocalSettings {
  pages?: any;
  peerUrl?: string;
  latLongFormat?: 'DDMMSS' | 'DDMM' | 'DD';
  accountInheritance?: boolean;
  locale?: string;
  usageMode?: UsageMode;
  defaultPrograms?: string[];
  mobile?: boolean;
  touchUi?: boolean;
  fields?: FieldOptions[];
}
