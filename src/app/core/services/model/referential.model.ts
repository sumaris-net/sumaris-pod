import {Moment} from "moment";
import {joinPropertiesPath} from "../../../shared/functions";
import {Entity, EntityAsObjectOptions, EntityUtils} from "./entity.model";
import {StatusIds} from "./model.enum";
import {fromDateISOString, toDateISOString} from "../../../shared/dates";
import {Person, personToString} from "./person.model";

export function referentialToString(obj: Referential | any, properties?: string[]): string | undefined {
  return obj && obj.id && joinPropertiesPath(obj, properties || ['label', 'name']) || undefined;
}
export function referentialsToString(values: Referential[], properties?: string[], separator?: string): string {
  return (values || []).map(v => referentialToString(v, properties)).join(separator || ", ");
}

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


export class Referential<T extends Referential<any> = Referential<any>, O extends ReferentialAsObjectOptions = ReferentialAsObjectOptions>
    extends Entity<T, O> implements IReferentialRef {

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
  static fromObject(source: any): Referential {
    if (!source || source instanceof Referential) return source;
    const res = new Referential();
    res.fromObject(source);
    return res as Referential;
  }

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
  id: number;
  label: string;
  name: string;
  levelId?: number;
  statusId: number;
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
    this.entityName = source.entityName;
  }
}
