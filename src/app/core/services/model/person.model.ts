import {Moment} from "moment";
import {ReferentialAsObjectOptions} from "./referential.model";
import {Entity} from "./entity.model";
import {Department} from "./department.model";
import {fromDateISOString, toDateISOString} from "../../../shared/dates";


export type UserProfileLabel = 'ADMIN' | 'USER' | 'SUPERVISOR' | 'GUEST';



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

export const PRIORITIZED_USER_PROFILES: UserProfileLabel[] = ['ADMIN', 'SUPERVISOR', 'USER', 'GUEST'];

export class PersonUtils {
  static getMainProfile = getMainProfile;
  static getMainProfileIndex = getMainProfileIndex;
  static hasUpperOrEqualsProfile = hasUpperOrEqualsProfile;
  static personToString = personToString;
  static personsToString = personsToString;
}

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
