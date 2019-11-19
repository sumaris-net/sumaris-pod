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
  Department,
  EntityUtils,
  GearLevelIds,
  getPmfmName,
  Person,
  PmfmStrategy,
  QualityFlagIds,
  Referential,
  ReferentialRef,
  TaxonGroupIds,
  LocationLevelIds,
  AcquisitionLevelCodes,
  VesselFeatures,
  vesselFeaturesToString
} from "../../../referential/referential.module";
import {Moment} from "moment/moment";
import {IWithProgramEntity} from "../../../referential/services/model";
import {EntityAsObjectOptions, MINIFY_OPTIONS, NOT_MINIFY_OPTIONS, ReferentialAsObjectOptions} from "../../../core/services/model";


export {
  Referential, ReferentialRef, EntityUtils, Person, Department,
  toDateISOString, fromDateISOString, isNotNil, isNil,
  vesselFeaturesToString, entityToString, referentialToString, personToString, personsToString, getPmfmName,
  StatusIds, Cloneable, Entity, VesselFeatures, LocationLevelIds, GearLevelIds, TaxonGroupIds, QualityFlagIds,
  PmfmStrategy, AcquisitionLevelCodes, NOT_MINIFY_OPTIONS, MINIFY_OPTIONS
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

/* -- Data entity -- */

export interface IWithRecorderDepartmentEntity<T> extends Entity<T> {
  recorderDepartment: Department|ReferentialRef|Referential;
}

export interface IWithRecorderPersonEntity<T> extends Entity<T> {
  recorderPerson: Person;
}

export interface IWithVesselFeaturesEntity<T> extends Entity<T> {
  vesselFeatures: VesselFeatures;
}
export interface IWithObserversEntity<T> extends Entity<T> {
  observers: Person[];
}

export interface DataEntityAsObjectOptions extends ReferentialAsObjectOptions {
}

export const OPTIMISTIC_AS_OBJECT_OPTIONS: DataEntityAsObjectOptions = {
 minify: false,
 keepTypename: true,
 keepEntityName: true
};

export const SAVE_AS_OBJECT_OPTIONS: DataEntityAsObjectOptions = {
  minify: true,
  keepTypename: false,
  keepEntityName: false
};

export abstract class DataEntity<T> extends Entity<T> implements IWithRecorderDepartmentEntity<T> {
  recorderDepartment: Department;
  controlDate: Moment;
  qualificationDate: Moment;
  qualificationComments: string;
  qualityFlagId: number;

  protected constructor() {
    super();
    this.recorderDepartment = null;
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(options) || undefined;
    target.controlDate = toDateISOString(this.controlDate);
    target.qualificationDate = toDateISOString(this.qualificationDate);
    target.qualificationComments = this.qualificationComments || undefined;
    target.qualityFlagId = isNotNil(this.qualityFlagId) ? this.qualityFlagId : undefined;
    return target;
  }

  fromObject(source: any): DataEntity<T> {
    super.fromObject(source);
    this.recorderDepartment = source.recorderDepartment && Department.fromObject(source.recorderDepartment);
    this.controlDate = fromDateISOString(source.controlDate);
    this.qualificationDate = fromDateISOString(source.qualificationDate);
    this.qualificationComments = source.qualificationComments;
    this.qualityFlagId = source.qualityFlagId;
    return this;
  }

}

export abstract class DataRootEntity<T> extends DataEntity<T> implements IWithRecorderPersonEntity<T>, IWithProgramEntity<T> {
  creationDate: Moment;
  validationDate: Moment;
  comments: string = null;
  recorderPerson: Person;
  program: ReferentialRef;

  protected constructor() {
    super();
    this.creationDate = null;
    this.validationDate = null;
    this.comments = null;
    this.recorderPerson = null;
    this.program = null;
  }

  asObject(options?: DataEntityAsObjectOptions): any {
    const target = super.asObject(options);
    target.creationDate = toDateISOString(this.creationDate);
    target.validationDate = toDateISOString(this.validationDate);
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(options) || undefined;
    target.program = this.program && this.program.asObject({ ...options, ...NOT_MINIFY_OPTIONS /*keep for trips list*/ } as ReferentialAsObjectOptions) || undefined;
    return target;
  }

  fromObject(source: any): DataRootEntity<T> {
    super.fromObject(source);
    this.comments = source.comments;
    this.creationDate = fromDateISOString(source.creationDate);
    this.validationDate = fromDateISOString(source.validationDate);
    this.recorderPerson = source.recorderPerson && Person.fromObject(source.recorderPerson);
    this.program = source.program && ReferentialRef.fromObject(source.program);
    return this;
  }
}


export abstract class DataRootVesselEntity<T> extends DataRootEntity<T> implements IWithVesselFeaturesEntity<T> {
  vesselFeatures: VesselFeatures;

  protected constructor() {
    super();
    this.vesselFeatures = null;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target = super.asObject(options);
    target.vesselFeatures = this.vesselFeatures && this.vesselFeatures.asObject({ ...options, ...NOT_MINIFY_OPTIONS }) || undefined;
    return target;
  }

  fromObject(source: any): DataRootVesselEntity<T> {
    super.fromObject(source);
    this.vesselFeatures = source.vesselFeatures && VesselFeatures.fromObject(source.vesselFeatures);
    return this;
  }
}

export class DataRootEntityUtils {

  static copyControlAndValidationDate(source: DataRootEntity<any> | undefined, target: DataRootEntity<any>) {
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
