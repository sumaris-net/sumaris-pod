import {
  Cloneable,
  Entity,
  entityToString,
  fromDateISOString,
  isNil,
  isNotNil,
  LocationLevelIds,
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
  AcquisitionLevelCodes,
  VesselFeatures,
  vesselFeaturesToString
} from "../../../referential/referential.module";
import {Moment} from "moment/moment";


export {
  Referential, ReferentialRef, EntityUtils, Person, Department,
  toDateISOString, fromDateISOString, isNotNil, isNil,
  vesselFeaturesToString, entityToString, referentialToString, personToString, personsToString, getPmfmName,
  StatusIds, Cloneable, Entity, VesselFeatures, LocationLevelIds, GearLevelIds, TaxonGroupIds, QualityFlagIds,
  PmfmStrategy, AcquisitionLevelCodes
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

/* -- Trip entity -- */

export abstract class DataEntity<T> extends Entity<T> {
  recorderDepartment: Department;
  controlDate: Moment;
  qualificationDate: Moment;
  qualificationComments: string;
  // TODO use a ReferentialRef when qualification is developed
  qualityFlagId: number;

  protected constructor() {
    super();
    this.recorderDepartment = new Department();
    // this.qualityFlagId = new ReferentialRef();
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(minify) || undefined;
    target.controlDate = toDateISOString(this.controlDate);
    target.qualificationDate = toDateISOString(this.qualificationDate);
    target.qualificationComments = this.qualificationComments || undefined;
    target.qualityFlag = this.qualityFlagId || undefined;
    return target;
  }

  fromObject(source: any): DataEntity<T> {
    super.fromObject(source);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
    this.controlDate = fromDateISOString(source.controlDate);
    this.qualificationDate = fromDateISOString(source.qualificationDate);
    this.qualificationComments = source.qualificationComments;
    // source.qualityFlagId && this.qualityFlagId.fromObject(source.qualityFlagId);
    this.qualityFlagId = source.qualityFlagId;
    return this;
  }

}

export abstract class DataRootEntity<T> extends DataEntity<T> {
  comments: string = null;
  creationDate: Moment;
  recorderPerson: Person;
  validationDate: Moment;

  constructor() {
    super();
    this.comments = null;
    this.creationDate = null;
    this.validationDate = null;
    this.recorderPerson = new Person();
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(minify) || undefined;
    target.validationDate = toDateISOString(this.validationDate);
    return target;
  }

  fromObject(source: any): DataRootEntity<T> {
    super.fromObject(source);
    this.comments = source.comments;
    this.creationDate = fromDateISOString(source.creationDate);
    source.recorderPerson && this.recorderPerson.fromObject(source.recorderPerson);
    this.validationDate = fromDateISOString(source.validationDate);
    return this;
  }
}


export abstract class DataRootVesselEntity<T> extends DataRootEntity<T> {
  vesselFeatures: VesselFeatures;
  // TODO: program: string;

  constructor() {
    super();
    this.vesselFeatures = new VesselFeatures();
  }

  asObject(minify?: boolean): any {
    const target = super.asObject();
    target.vesselFeatures = this.vesselFeatures && this.vesselFeatures.asObject(minify) || undefined;
    return target;
  }

  fromObject(source: any): DataRootVesselEntity<T> {
    super.fromObject(source);
    source.vesselFeatures && this.vesselFeatures.fromObject(source.vesselFeatures);
    // TODO: source.program && this.program;
    return this;
  }
}
