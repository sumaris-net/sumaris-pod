import {Moment} from "moment/moment";
import {
  fromDateISOString,
  isNil,
  isNilOrBlank,
  isNotNil,
  joinPropertiesPath,
  propertyComparator,
  sort,
  toDateISOString
} from "../../shared/shared.module";
import {isEmptyArray, noTrailingSlash} from "../../shared/functions";
import {FormFieldDefinition, FormFieldDefinitionMap, FormFieldValue} from "../../shared/form/field.model";

export {
  joinPropertiesPath,
  propertyComparator,
  sort
};

export const DATE_ISO_PATTERN = 'YYYY-MM-DDTHH:mm:ss.SSSZ';

// TODO: rename to STATUS_ID_MAP
// then declare a type like this :
// > export declare type StatusIds = keyof typeof STATUS_ID_MAP;
export const StatusIds = {
  DISABLE: 0,
  ENABLE: 1,
  TEMPORARY: 2,
  DELETED: 3,
  ALL: 99
};

export declare interface StatusValue {
  id: number;
  icon: string;
  label: string;
}

export const DefaultStatusList: StatusValue[] = [
  {
    id: StatusIds.ENABLE,
    icon: 'checkmark',
    label: 'REFERENTIAL.STATUS_ENUM.ENABLE'
  },
  {
    id: StatusIds.DISABLE,
    icon: 'close',
    label: 'REFERENTIAL.STATUS_ENUM.DISABLE'
  },
  {
    id: StatusIds.TEMPORARY,
    icon: 'help',
    label: 'REFERENTIAL.STATUS_ENUM.TEMPORARY'
  }
];

export const Locales: LocaleConfig[] = [
  {
    id: 'fr',
    name: 'Français',
    country: 'fr'
  },
  {
    id: 'en',
    name: 'English (UK)',
    country: 'gb'
  },
  {
    id: 'en-US',
    name: 'English (US)',
    country: 'us'
  }
];

// TODO: rename to CONFIG_OPTIONS_MAP
// then declare a type like this :
// > export declare type ConfigOptions = key of CONFIG_OPTIONS_MAP
export const ConfigOptions: FormFieldDefinitionMap = {
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
    values: Locales.map(l => {
      return {key: l.id, value: l.name} as FormFieldValue;
    })
  },
  DEFAULT_LAT_LONG_FORMAT: {
    key: 'sumaris.defaultLatLongFormat',
    label: 'CONFIGURATION.OPTIONS.DEFAULT_LATLONG_FORMAT',
    type: 'enum',
    values: [
      {
        key: 'DDMMSS',
        value: 'COMMON.DDMMSS_PLACEHOLDER'
      },
      {
        key: 'DDMM',
        value: 'COMMON.DDMM_PLACEHOLDER'
      },
      {
        key: 'DD',
        value: 'COMMON.DD_PLACEHOLDER'
      }
    ]
  },
  TESTING: {
    key: 'sumaris.testing.enable',
    label: 'CONFIGURATION.OPTIONS.TESTING',
    type: 'boolean',
  },
  VESSEL_DEFAULT_STATUS: {
    key: 'sumaris.vessel.status.default',
    label: 'CONFIGURATION.OPTIONS.VESSEL.DEFAULT_NEW_VESSEL_STATUS',
    type: 'enum',
    values: [
      {
        key: StatusIds.ENABLE.toString(),
        value: 'REFERENTIAL.STATUS_ENUM.ENABLE'
      },
      {
        key: StatusIds.TEMPORARY.toString(),
        value: 'REFERENTIAL.STATUS_ENUM.TEMPORARY'
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
  },
  COLOR_SUCCESS: {
    key: 'sumaris.color.success',
    label: 'CONFIGURATION.OPTIONS.COLORS.SUCCESS',
    type: 'color'
  },
  COLOR_WARNING: {
    key: 'sumaris.color.warning',
    label: 'CONFIGURATION.OPTIONS.COLORS.WARNING',
    type: 'color'
  },
  COLOR_ACCENT: {
    key: 'sumaris.color.accent',
    label: 'CONFIGURATION.OPTIONS.COLORS.ACCENT',
    type: 'color'
  },
  COLOR_DANGER: {
    key: 'sumaris.color.danger',
    label: 'CONFIGURATION.OPTIONS.COLORS.DANGER',
    type: 'color'
  },
  PROFILE_ADMIN_LABEL: {
    key: 'sumaris.userProfile.ADMIN.label',
    label: 'CONFIGURATION.OPTIONS.PROFILE.ADMIN',
    type: 'string'
  },
  PROFILE_USER_LABEL: {
    key: 'sumaris.userProfile.USER.label',
    label: 'CONFIGURATION.OPTIONS.PROFILE.USER',
    type: 'string'
  },
  PROFILE_SUPERVISOR_LABEL: {
    key: 'sumaris.userProfile.SUPERVISOR.label',
    label: 'CONFIGURATION.OPTIONS.PROFILE.SUPERVISOR',
    type: 'string'
  },
  PROFILE_GUEST_LABEL: {
    key: 'sumaris.userProfile.GUEST.label',
    label: 'CONFIGURATION.OPTIONS.PROFILE.GUEST',
    type: 'string'
  },
  ANDROID_INSTALL_URL:  {
    key: 'sumaris.android.install.url',
    label: 'CONFIGURATION.OPTIONS.ANDROID_INSTALL_URL',
    type: 'string'
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

export function entityToString(obj: Entity<any> | any, properties?: string[]): string | undefined {
  return obj && obj.id && joinPropertiesPath(obj, properties || ['name']) || undefined;
}

export function referentialToString(obj: Referential | any | any, properties?: string[]): string | undefined {
  return obj && obj.id && joinPropertiesPath(obj, properties || ['label', 'name']) || undefined;
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


/* Base */

// todo: replace interfaces from '[key: string]: ?' to ObjectMap<?>, when possible ...
export interface ObjectMap<O = any> { [key: string]: O; }


/* Entities */

export interface EntityAsObjectOptions {
  minify?: boolean;
  keepTypename?: boolean; // true by default
  keepLocalId?: boolean; // true by default
}

export interface IEntity<T, O extends EntityAsObjectOptions = EntityAsObjectOptions> extends Cloneable<T> {
  id: number;
  updateDate: Date | Moment;
  __typename: string;
  equals(other: T): boolean;
  clone(): T;
  asObject(opts?: O): any;
  fromObject(source: any);
}

export abstract class Entity<T extends IEntity<any, any>, O extends EntityAsObjectOptions = EntityAsObjectOptions> implements IEntity<T, O> {

  id: number;
  updateDate: Date | Moment;
  __typename: string;

  abstract clone(): T;

  asObject(opts?: O): any {
    const target: any = Object.assign({}, this); //= {...this};
    if (!opts || opts.keepTypename !== true) delete target.__typename;
    if (target.id < 0 && (!opts || opts.keepLocalId === false)) delete target.id;
    target.updateDate = toDateISOString(this.updateDate);
    return target;
  }

  fromObject(source: any) {
    this.id = (source.id || source.id === 0) ? source.id : undefined;
    this.updateDate = fromDateISOString(source.updateDate);
    this.__typename = source.__typename || this.__typename; // Keep original type (can be set in constructor)
  }

  equals(other: T): boolean {
    return other && this.id === other.id;
  }
}

export class EntityUtils {
  // Check that the object has a NOT nil attribute (ID by default)
  static isNotEmpty(obj: any | IEntity<any>, checkedAttribute: string): boolean {
    return !!obj && obj[checkedAttribute] !== null && obj[checkedAttribute] !== undefined;
  }

  // Check that the object has a NOT nil attribute (ID by default)
  static isNotEmptyEntity<T extends IEntity<any>>(obj: any | IEntity<any>, checkedAttribute: string): obj is T {
    return !!obj && obj[checkedAttribute] !== null && obj[checkedAttribute] !== undefined;
  }

  static isEmpty(obj: any | IEntity<any>, checkedAttribute: string): boolean {
    return !obj || obj[checkedAttribute] === null || obj[checkedAttribute] === undefined;
  }

  static equals(o1: IEntity<any>, o2: IEntity<any>, checkAttribute: string): boolean {
    return (o1 === o2) || (isNil(o1) && isNil(o2))  || (o1 && o2 && o1[checkAttribute] === o2[checkAttribute]);
  }

  static getPropertyByPath(obj: any | IEntity<any>, path: string): any {
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
    if (source instanceof Array) return source;
    return Object.getOwnPropertyNames(source || {})
      .map(key => {
        return {
          key,
          value: source[key]
        };
      });
  }

  static getPropertyArrayAsObject(source?: FormFieldValue[]): { [key: string]: string } {
    return (source || []).reduce((res, item) => {
      res[item.key] = item.value;
      return res;
    }, {});
  }

  static copyIdAndUpdateDate(source: IEntity<any> | undefined, target: IEntity<any>, opts?: { creationDate?: boolean; }) {
    if (!source) return;

    // Update (id and updateDate)
    target.id = isNotNil(source.id) ? source.id : target.id;
    target.updateDate = fromDateISOString(source.updateDate) || target.updateDate;

    // Update creation Date, if exists
    if (source['creationDate']) {
      target['creationDate'] = fromDateISOString(source['creationDate']);
    }
  }

  static async fillLocalIds<T extends IEntity<T>>(items: T[], sequenceFactory: (firstEntity: T, incrementSize: number) => Promise<number>) {
    const newItems = (items || []).filter(item => isNil(item.id) || item.id === 0);
    if (isEmptyArray(newItems)) return;
    // Get the sequence
    let currentId = await sequenceFactory(newItems[0], newItems.length) + 1;
    // Take the min (sequence, id), in case the sequence is corrupted
    currentId = items.filter(item => isNotNil(item.id) && item.id < 0).reduce((res, item) => Math.min(res, item.id), currentId);
    newItems.forEach(item => item.id = --currentId);
  }

  static sort<T extends IEntity<T> | any>(data: T[], sortBy?: string, sortDirection?: string): T[] {
    return data.sort(this.sortComparator(sortBy, sortDirection));
  }

  static sortComparator<T extends IEntity<T> | any>(sortBy?: string, sortDirection?: string): (a: T, b: T) => number {
    const after = (!sortDirection || sortDirection === 'asc') ? 1 : -1;
    const isSimplePath = !sortBy || sortBy.indexOf('.') === -1;
    if (isSimplePath) {
      return (a, b) => {
        const valueA = isNotNil(a) && a[sortBy] || undefined;
        const valueB = isNotNil(b) && b[sortBy] || undefined;
        return EntityUtils.compare(valueA, valueB, after);
      };
    }
    else {
      return (a, b) => {
        const valueA = EntityUtils.getPropertyByPath(a, sortBy);
        const valueB = EntityUtils.getPropertyByPath(b, sortBy);
        return EntityUtils.compare(valueA, valueB, after);
      };
    }
  }

  static compare(value1: any, value2: any, direction: 1 | -1, checkAttribute?: string): number {
    checkAttribute = checkAttribute || 'id';
    if (EntityUtils.isNotEmptyEntity(value1, checkAttribute) && EntityUtils.isNotEmptyEntity(value2, checkAttribute)) {
      return EntityUtils.equals(value1, value2, checkAttribute) ? 0 : (value1[checkAttribute] > value2[checkAttribute] ? direction : (-1 * direction));
    }
    return value1 === value2 ? 0 : (value1 > value2 ? direction : (-1 * direction));
  }

  static filter<T extends IEntity<T> | any>(data: T[],
                                           searchAttribute: string,
                                           searchText: string): T[] {
    const filterFn = this.searchTextFilter(searchAttribute, searchText);
    return data.filter(filterFn);
  }

  static searchTextFilter<T extends IEntity<T> | any>(searchAttribute: string | string[],
                                                     searchText: string): (T) => boolean {
    if (isNilOrBlank(searchAttribute) || isNilOrBlank(searchText)) return undefined; // filter not need

    const searchRegexp = searchText.replace(/[.]/g, '[.]').replace(/[*]+/g, '.*');
    if (searchRegexp === '.*') return undefined; // filter not need

    const flags = 'gi';
    const asPrefixPattern = '^' + searchRegexp;
    const anyMatchPattern = '^.*' + searchRegexp;

    // Only one search attributes
    if (typeof searchAttribute === 'string') {
      const isSimplePath = !searchAttribute || searchAttribute.indexOf('.') === -1;
      const searchAsPrefix =  searchAttribute.toLowerCase().endsWith('label') || searchAttribute.toLowerCase().endsWith('code');
      const regexp = new RegExp(searchAsPrefix && asPrefixPattern || anyMatchPattern, flags);
      if (isSimplePath) {
        return a => regexp.test( a[searchAttribute]);
      }
      else {
        return a => regexp.test(EntityUtils.getPropertyByPath(a, searchAttribute));
      }
    }

    // many search attributes
    else {
      const regexps = searchAttribute.map(path => {
        const searchAsPrefix =  path.toLowerCase().endsWith('label') || path.toLowerCase().endsWith('code');
        return new RegExp( searchAsPrefix && asPrefixPattern || anyMatchPattern, flags);
      });
      return a => !!searchAttribute.find((path, index) => regexps[index].test(EntityUtils.getPropertyByPath(a, path)));
    }
  }

  /**
   * Transform a batch entity tree into a array list. This method keep links parent/children.
   *
   * Method used to find batch without id (e.g. before local storage)
   * @param source
   */
  static treeToArray<T extends ITreeItemEntity<any>>(source: T): T[] {
    if (!source) return null;
    return (source.children || []).reduce((res, batch) => {
      batch.parent = source;
      return res.concat(this.treeToArray(batch)); // recursive call
    }, [source]);
  }

  static listOfTreeToArray<T extends ITreeItemEntity<any>>(sources: T[]): T[] {
    if (!sources || !sources.length) return null;
    return sources.reduce((res, source) => res.concat(this.treeToArray<T>(source)), []);
  }
}


export declare interface ITreeItemEntity<T extends IEntity<T>> {
  parentId: number;
  parent: T;
  children: T[];
}

/* -- Referential -- */


export class Referential<T extends Referential<any> = Referential<any>, O extends ReferentialAsObjectOptions = ReferentialAsObjectOptions>
    extends Entity<T, O> implements IReferentialRef {

  static fromObject(source: any): Referential {
    if (!source || source instanceof Referential) return source;
    const res = new Referential();
    res.fromObject(source);
    return res as Referential;
  }

  label: string;
  name: string;
  description: string;
  comments: string;
  creationDate: Date | Moment;
  statusId: number;
  validityStatusId: number;
  levelId: number;
  parentId: number;
  rankOrder: number;
  entityName: string;

  constructor(data?: {
    id?: number,
    label?: string,
    name?: string,
    parentId?: number,
    levelId?: number,
    rankOrder?: number,
    entityName?: string
  }) {
    super();
    this.id = data && data.id;
    this.label = data && data.label;
    this.name = data && data.name;
    this.parentId = data && data.parentId;
    this.levelId = data && data.levelId;
    this.rankOrder = data && data.rankOrder;
    this.entityName = data && data.entityName;
  }

  clone(): T {
    const target = new Referential<any>() as T;
    target.fromObject(this);
    return target;
  }

  asObject(opts?: O): any {
    if (opts && opts.minify) {
      return {
        id: this.id,
        entityName: opts.keepEntityName && this.entityName || undefined, // Don't keep by default
        __typename: opts.keepTypename && this.__typename || undefined
      };
    }
    const target: any = super.asObject(opts);
    target.creationDate = toDateISOString(this.creationDate);
    if (opts && opts.keepTypename === false) delete target.entityName;
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.description = source.description;
    this.comments = source.comments;
    this.statusId = source.statusId;
    this.validityStatusId  = source.validityStatusId;
    this.levelId = source.levelId && source.levelId !== 0 ? source.levelId : undefined; // Do not set as null (need for account.department, when regsiter)
    this.rankOrder = source.rankOrder;
    this.parentId = source.parentId;
    this.creationDate = fromDateISOString(source.creationDate);
    this.entityName = source.entityName;
  }

  equals(other: T): boolean {
    return super.equals(other) && this.entityName === other.entityName;
  }
}

export class ReferentialUtils {
  static isNotEmpty(obj: any | Referential<any> | ReferentialRef): boolean {
    // A referential entity should always have a 'id' filled (can be negative is local and temporary)
    return EntityUtils.isNotEmpty(obj, 'id');
  }

  static isNotEmptyReferential<T extends Referential<any> | ReferentialRef>(obj: any | Referential<any> | ReferentialRef): obj is T {
    return EntityUtils.isNotEmpty(obj, 'id');
  }

  static isEmpty(obj: any | Referential<any> | ReferentialRef): boolean {
    return EntityUtils.isEmpty(obj, 'id');
  }

  static equals(o1: any | Referential<any> | ReferentialRef, o2: any | Referential<any> | ReferentialRef): boolean {
    return EntityUtils.equals(o1, o2, 'id');
  }

}

export declare interface IReferentialRef {
  label: string;
  name: string;
  statusId: number;
  rankOrder: number;
  entityName: string;
}

export interface ReferentialAsObjectOptions extends EntityAsObjectOptions {
  keepEntityName?: boolean;
}

export const NOT_MINIFY_OPTIONS: ReferentialAsObjectOptions = {minify: false};

export const MINIFY_OPTIONS: ReferentialAsObjectOptions = {minify: true};

export const SAVE_LOCALLY_AS_OBJECT_OPTIONS: ReferentialAsObjectOptions = {
  minify: true,
  keepTypename: true,
  keepEntityName: true,
  keepLocalId: true
};

export const SAVE_AS_OBJECT_OPTIONS: ReferentialAsObjectOptions = {
  minify: true,
  keepTypename: false,
  keepEntityName: false,
  keepLocalId: false
};

export class ReferentialRef<T extends ReferentialRef<any> = ReferentialRef<any>,
    O extends ReferentialAsObjectOptions = ReferentialAsObjectOptions>
    extends Entity<T, O>
    implements IReferentialRef {

  static fromObject<T extends ReferentialRef<any> = ReferentialRef<any>>(source: any): T {
    if (!source) return source;
    if (source instanceof ReferentialRef) return source.clone() as T;
    const res = new ReferentialRef<any>() as T;
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  statusId: number;
  rankOrder: number;
  entityName: string;

  constructor(data?: {
    id?: number,
    label?: string,
    name?: string,
    rankOrder?: number
  }) {
    super();
    this.id = data && data.id;
    this.label = data && data.label;
    this.name = data && data.name;
    this.rankOrder = data && data.rankOrder;
  }

  clone(): T {
    const target = new ReferentialRef() as T;
    target.fromObject(this);
    return target;
  }

  asObject(opts?: O): any {
    if (opts && opts.minify) {
      return {
        id: this.id,
        entityName: opts.keepEntityName && this.entityName || undefined, // Don't keep by default
        __typename: opts.keepTypename && this.__typename || undefined
      };
    }
    const target: any = super.asObject(opts);
    if (opts && opts.keepEntityName === false) delete target.entityName;

    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.statusId = source.statusId;
    this.rankOrder = source.rankOrder;
    this.entityName = source.entityName;
  }
}


/* -- Software & Configuration -- */


export class Software<T extends Software<any> = Software<any>> extends Entity<T> {

 static fromObject(source: any): Software {
    if (!source || source instanceof Software) return source;
    const res = new Software();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  creationDate: Date | Moment;
  statusId: number;
  properties: { [key: string]: string };

  constructor() {
    super();
  }

  clone(): T {
    const target = new Software() as T;
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);
    target.creationDate = toDateISOString(this.creationDate);
    target.properties = this.properties;
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.creationDate = fromDateISOString(source.creationDate);
    this.statusId = source.statusId;

    if (source.properties && source.properties instanceof Array) {
      this.properties = EntityUtils.getPropertyArrayAsObject(source.properties);
    } else {
      this.properties = source.properties;
    }
  }
}

export class Configuration extends Software<Configuration> {

  static fromObject(source: Configuration): Configuration {
    if (!source || source instanceof Configuration) return source;
    const res = new Configuration();
    res.fromObject(source);
    return res;
  }

  smallLogo: string;
  largeLogo: string;
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

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);
    if (this.partners)
      target.partners = (this.partners || []).map(p => p.asObject(options));
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.smallLogo = source.smallLogo;
    this.largeLogo = source.largeLogo;
    this.backgroundImages = source.backgroundImages;
    if (source.partners)
      this.partners = (source.partners || []).map(Department.fromObject);
  }

  getPropertyAsBoolean(definition: FormFieldDefinition): boolean {
    const value = this.getProperty(definition);
    return isNotNil(value) ? (value && value !== "false") : undefined;
  }

  getPropertyAsNumbers(definition: FormFieldDefinition): number[] {
    const value = this.getProperty(definition);
    return value && value.split(',').map(parseInt) || undefined;
  }

  getPropertyAsStrings(definition: FormFieldDefinition): string[] {
    const value = this.getProperty(definition);
    return value && value.split(',') || undefined;
  }

  getProperty<T = string>(definition: FormFieldDefinition): T {
    return isNotNil(this.properties[definition.key]) ? this.properties[definition.key] : (definition.defaultValue || undefined);
  }
}


export class Person<T extends Person<any> = Person<any>> extends Entity<T, ReferentialAsObjectOptions> {

  static TYPENAME = 'PersonVO';

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
    this.department = null;
    this.__typename = Person.TYPENAME;
  }

  clone(): T {
    const target = new Person() as T;
    target.fromObject(this);
    return target;
  }

  asObject(opts?: ReferentialAsObjectOptions): any {
    if (opts && opts.minify)  {
      return {
        id: this.id,
        __typename: opts.keepTypename && this.__typename || undefined,
        firstName: this.firstName,
        lastName: this.lastName
      };
    }
    const target: any = super.asObject(opts);
    target.department = this.department && this.department.asObject(opts) || undefined;
    target.profiles = this.profiles && this.profiles.slice(0) || [];
    // Set profile list from the main profile
    target.profiles = this.mainProfile && [this.mainProfile] || target.profiles || ['GUEST'];
    target.creationDate = toDateISOString(this.creationDate);

    if (!opts || opts.minify !== true) target.mainProfile = getMainProfile(target.profiles);
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.firstName = source.firstName;
    this.lastName = source.lastName;
    this.email = source.email;
    this.creationDate = fromDateISOString(source.creationDate);
    this.pubkey = source.pubkey;
    this.avatar = source.avatar;
    this.statusId = source.statusId;
    this.department = source.department && Department.fromObject(source.department) || undefined;
    this.profiles = source.profiles && source.profiles.slice(0) || [];
    // Add main profile to the list, if need
    if (source.mainProfile && !this.profiles.find(p => p === source.mainProfile)) {
      this.profiles = this.profiles.concat(source.mainProfile);
    }
    this.mainProfile = getMainProfile(this.profiles);
  }
}

export class Department extends Referential<Department> {

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
    this.__typename = 'DepartmentVO';
  }

  clone(): Department {
    const target = new Department();
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.logo = source.logo;
    this.siteUrl = source.siteUrl;
    delete this.entityName; // not need
  }
}

export class UserSettings extends Entity<UserSettings> {
  locale: string;
  latLongFormat: string;
  content: {};
  nonce: string;

  clone(): UserSettings {
    const target = new UserSettings();
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const res: any = super.asObject(options);
    res.content = this.content && JSON.stringify(res.content) || undefined;
    return res;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.locale = source.locale;
    this.latLongFormat = source.latLongFormat;
    if (isNil(source.content) || typeof source.content === 'object') {
      this.content = source.content || {};
    } else {
      this.content = source.content && JSON.parse(source.content) || {};
    }
    this.nonce = source.nonce;
  }
}

/**
 * A user account
 */
export class Account extends Person<Account> {

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
    target.fromObject(this);
    target.settings = this.settings && this.settings.clone() || undefined;
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);
    target.settings = this.settings && this.settings.asObject(options) || undefined;
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    source.settings && this.settings.fromObject(source.settings);
  }

  /**
   * Convert into a Person. This will fill __typename with a right value, for data cache
   */
  asPerson(): Person {
    const person = Person.fromObject(this.asObject({
      keepTypename: true // This is need for the department object
    }));
    person.__typename = Person.TYPENAME; // Do not keep AccountVO as typename
    return person;
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
      useSsl: url.protocol && (url.protocol.startsWith('https') || url.protocol.startsWith('wss')),
      path: noTrailingSlash(url.pathname)
    });
  }

  dns: string;
  ipv4: string;
  ipv6: string;
  port: number;
  useSsl: boolean;
  pubkey: string;
  path?: string;

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
    const target = new Peer();
    target.fromObject(this);
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);
    return target;
  }

  fromObject(source: any) {
    super.fromObject(source);
    this.dns = source.dns || source.host;
    this.ipv4 = source.ipv4;
    this.ipv6 = source.ipv6;
    this.port = isNotNil(source.port) ? +source.port : undefined;
    this.pubkey = source.pubkey;
    this.useSsl = source.useSsl || (this.port === 443);
    this.path = source.path || '';
  }

  equals(other: Peer): boolean {
    return super.equals(other) && this.pubkey === other.pubkey && this.url === other.url;
  }

  /**
   * Return the peer URL (without trailing slash)
   */
  get url(): string {
    return (this.useSsl ? 'https://' : 'http://') + this.hostAndPort + (this.path || '');
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

export declare interface LocaleConfig {
  id: string;
  name: string;
  country?: string;
}

export declare interface PropertiesMap {
  [key: string]: string;
}

export declare interface SynchronizationHistory {
  name: string;
  filter?: any;
  execDate: string;
  execDurationMs: number;
  lastUpdateDate?: string;
  children?: SynchronizationHistory[];
}

export declare interface LocalSettings {
  pages?: any;
  peerUrl?: string;
  latLongFormat: 'DDMMSS' | 'DDMM' | 'DD';
  accountInheritance?: boolean;
  locale: string;
  usageMode?: UsageMode;
  mobile?: boolean;
  touchUi?: boolean;
  properties?: PropertiesMap;
  pageHistory?: HistoryPageReference[];
  offlineFeatures?: string[];
  pageHistoryMaxSize: number;
}


export interface HistoryPageReference {
  title: string;
  subtitle?: string;
  path: string;
  time?: Moment|string;
  icon?: string;
  matIcon?: string;

  children?: HistoryPageReference[];
}
