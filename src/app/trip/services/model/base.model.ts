import {
  Cloneable,
  Entity,
  entityToString,
  fromDateISOString,
  isNil,
  isNotNil,
  personsToString,
  personToString,
  referentialToString,
  StatusIds,
  toDateISOString
} from "../../../core/core.module";
import {
  AcquisitionLevelCodes,
  Department,
  EntityUtils,
  GearLevelIds,
  getPmfmName,
  LocationLevelIds,
  Person,
  PmfmStrategy,
  QualityFlagIds,
  Referential,
  ReferentialRef,
  TaxonGroupIds,
  VesselSnapshot,
  vesselSnapshotToString
} from "../../../referential/referential.module";
import {Moment} from "moment/moment";
import {IWithProgramEntity} from "../../../referential/services/model";
import {IEntity, MINIFY_OPTIONS, NOT_MINIFY_OPTIONS, ReferentialAsObjectOptions} from "../../../core/services/model";
import {Product} from "./product.model";
import {Packet} from "./packet.model";


export {
  Referential, ReferentialRef, EntityUtils, Person, Department,
  toDateISOString, fromDateISOString, isNotNil, isNil,
  vesselSnapshotToString, entityToString, referentialToString, personToString, personsToString, getPmfmName,
  StatusIds, Cloneable, Entity, VesselSnapshot, LocationLevelIds, GearLevelIds, TaxonGroupIds, QualityFlagIds,
  PmfmStrategy, AcquisitionLevelCodes, NOT_MINIFY_OPTIONS, MINIFY_OPTIONS
};


/* -- Helper function -- */

export function getMaxRankOrder(values: { rankOrder: number }[]): number {
  let maxRankOrder = 0;
  (values || []).forEach(m => {
    if (m.rankOrder && m.rankOrder > maxRankOrder) maxRankOrder = m.rankOrder;
  });
  return maxRankOrder;
}

export function fillRankOrder(values: { rankOrder: number }[]) {
  // Compute rankOrder
  let maxRankOrder = getMaxRankOrder(values);
  (values || []).forEach(m => {
    m.rankOrder = m.rankOrder || ++maxRankOrder;
  });
}

/**
 * Compare unique rankOrder from values with values count
 * @param values
 * @return true if all rankOrder are unique
 */
export function isRankOrderValid(values: { rankOrder: number }[]): boolean {
  return (values || []).length ===
    (values || []).filter((v1, i, array) => array.findIndex(v2 => v2.rankOrder === v1.rankOrder) === i).length;
}

/* -- Data entity -- */

export interface IWithRecorderDepartmentEntity<T> extends IEntity<T> {
  recorderDepartment: Department|ReferentialRef|Referential;
}

export interface IWithRecorderPersonEntity<T> extends IEntity<T> {
  recorderPerson: Person;
}

export interface IWithVesselSnapshotEntity<T> extends IEntity<T> {
  vesselSnapshot: VesselSnapshot;
}
export interface IWithObserversEntity<T> extends IEntity<T> {
  observers: Person[];
}
export interface IWithProductsEntity<T> extends IEntity<T> {
  products: Product[];
}
export interface IWithPacketsEntity<T> extends IEntity<T> {
  packets: Packet[];
}

export interface DataEntityAsObjectOptions extends ReferentialAsObjectOptions {
  keepSynchronizationStatus?: boolean;
}

export const SAVE_OPTIMISTIC_AS_OBJECT_OPTIONS: DataEntityAsObjectOptions = {
  minify: false,
  keepTypename: true,
  keepEntityName: true,
  keepLocalId: true,
  keepSynchronizationStatus: true
};
export const SAVE_LOCALLY_AS_OBJECT_OPTIONS: DataEntityAsObjectOptions = {
  minify: true,
  keepTypename: true,
  keepEntityName: true,
  keepLocalId: true,
  keepSynchronizationStatus: true
};

export const SAVE_AS_OBJECT_OPTIONS: DataEntityAsObjectOptions = {
  minify: true,
  keepTypename: false,
  keepEntityName: false,
  keepLocalId: false,
  keepSynchronizationStatus: false
};


export abstract class DataEntity<T extends DataEntity<any>, O extends DataEntityAsObjectOptions = DataEntityAsObjectOptions, F = any>
  extends Entity<T, O>
  implements IWithRecorderDepartmentEntity<T> {

  recorderDepartment: Department;
  controlDate: Moment;
  qualificationDate: Moment;
  qualificationComments: string;
  qualityFlagId: number;

  protected constructor() {
    super();
    this.recorderDepartment = null;
  }

  asObject(opts?: O): any {
    const target = super.asObject(opts);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(opts) || undefined;
    target.controlDate = toDateISOString(this.controlDate);
    target.qualificationDate = toDateISOString(this.qualificationDate);
    target.qualificationComments = this.qualificationComments || undefined;
    target.qualityFlagId = isNotNil(this.qualityFlagId) ? this.qualityFlagId : undefined;
    return target;
  }

  fromObject(source: any, opts?: F) {
    super.fromObject(source);
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
    this.controlDate = fromDateISOString(source.controlDate);
    this.qualificationDate = fromDateISOString(source.qualificationDate);
    this.qualificationComments = source.qualificationComments;
    this.qualityFlagId = source.qualityFlagId;
  }
}

export type SynchronizationStatus = 'DIRTY' | 'READY_TO_SYNC' | 'SYNC' | 'DELETED';
export const SynchronizationStatusEnum = {
  DIRTY: 'DIRTY',
  READY_TO_SYNC: 'READY_TO_SYNC',
  SYNC: 'SYNC',
  DELETED: 'DELETED'
};

export abstract class RootDataEntity<T extends RootDataEntity<any>, O extends DataEntityAsObjectOptions = DataEntityAsObjectOptions, F = any>
  extends DataEntity<T, O, F>
  implements IWithRecorderPersonEntity<T>, IWithProgramEntity<T> {

  creationDate: Moment;
  validationDate: Moment;
  comments: string = null;
  recorderPerson: Person;
  program: ReferentialRef;
  synchronizationStatus?: SynchronizationStatus;

  protected constructor() {
    super();
    this.creationDate = null;
    this.validationDate = null;
    this.comments = null;
    this.recorderPerson = null;
    this.program = null;
  }

  asObject(options?: O): any {
    const target = super.asObject(options);
    target.creationDate = toDateISOString(this.creationDate);
    target.validationDate = toDateISOString(this.validationDate);
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(options) || undefined;
    target.program = this.program && this.program.asObject({ ...options, ...NOT_MINIFY_OPTIONS /*always keep for table*/ } as ReferentialAsObjectOptions) || undefined;
    if (options && options.minify) {
      if (target.program) delete target.program.entityName;
      if (options.keepSynchronizationStatus !== true) {
        delete target.synchronizationStatus; // Remove by default, when minify, because not exists on pod's model
      }
    }
    return target;
  }


  fromObject(source: any, opts?: F) {
    super.fromObject(source, opts);
    this.comments = source.comments;
    this.creationDate = fromDateISOString(source.creationDate);
    this.validationDate = fromDateISOString(source.validationDate);
    this.recorderPerson = source.recorderPerson && Person.fromObject(source.recorderPerson);
    this.program = source.program && ReferentialRef.fromObject(source.program);
    this.synchronizationStatus = source.synchronizationStatus;
  }
}

export abstract class DataRootVesselEntity<T extends DataRootVesselEntity<any>, O extends DataEntityAsObjectOptions = DataEntityAsObjectOptions, F = any>
  extends RootDataEntity<T, O, F> implements IWithVesselSnapshotEntity<T> {

  vesselSnapshot: VesselSnapshot;

  protected constructor() {
    super();
    this.vesselSnapshot = null;
  }

  asObject(options?: O): any {
    const target = super.asObject(options);
    target.vesselSnapshot = this.vesselSnapshot && this.vesselSnapshot.asObject({ ...options, ...NOT_MINIFY_OPTIONS }) || undefined;
    return target;
  }

  fromObject(source: any, opts?: F) {
    super.fromObject(source);
    this.vesselSnapshot = source.vesselSnapshot && VesselSnapshot.fromObject(source.vesselSnapshot);
  }
}

export class DataRootEntityUtils {

  static copyControlAndValidationDate(source: RootDataEntity<any> | undefined, target: RootDataEntity<any>) {
    if (!source) return;

    // Update (id and updateDate)
    target.controlDate = source.controlDate;
    target.validationDate = fromDateISOString(source.validationDate);

    // Update creation Date, if exists
    if (source['creationDate']) {
      target['creationDate'] = fromDateISOString(source['creationDate']);
    }
  }
}
