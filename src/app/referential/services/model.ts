import { Referential, EntityUtils, Department, Person, toDateISOString, fromDateISOString, StatusIds, Cloneable, Entity, joinProperties, entityToString, referentialToString } from "../../core/services/model";
import { Moment } from "moment/moment";

export const LocationLevelIds = {
  COUNTRY: 1,
  PORT: 2
}

export const GearLevelIds = {
  FAO: 1
}

export const TaxonGroupIds = {
  FAO: 2,
  METIER: 3
}

export { Referential, EntityUtils, Person, toDateISOString, fromDateISOString, joinProperties, StatusIds, Cloneable, Entity, Department, entityToString, referentialToString };

export function vesselFeaturesToString(obj: VesselFeatures | any): string | undefined {
  return obj && obj.vesselId && joinProperties(obj, ['exteriorMarking', 'name']) || undefined;
}

export class VesselFeatures extends Entity<VesselFeatures>  {

  static fromObject(source: any): VesselFeatures {
    const res = new VesselFeatures();
    res.fromObject(source);
    return res;
  }

  vesselId: number;
  vesselTypeId: number;
  name: string;
  startDate: Date | Moment;
  endDate: Date | Moment;
  exteriorMarking: string;
  basePortLocation: Referential;
  creationDate: Date | Moment;
  recorderDepartment: Referential;
  recorderPerson: Person;
  comments: string;

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
    target.recorderPerson = this.recorderPerson && this.recorderPerson.clone() || undefined;
    return target;
  }

  copy(target: VesselFeatures): VesselFeatures {
    target.fromObject(this);
    return target;
  }

  asObject(): any {
    const target: any = super.asObject();
    target.basePortLocation = this.basePortLocation && this.basePortLocation.asObject() || undefined;
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject() || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject() || undefined;

    return target;
  }

  fromObject(source: any): VesselFeatures {
    super.fromObject(source);
    this.exteriorMarking = source.exteriorMarking;
    this.name = source.name;
    this.comments = source.comments;
    this.vesselId = source.vesselId;
    this.vesselTypeId = source.vesselTypeId;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.creationDate = fromDateISOString(source.creationDate);
    source.basePortLocation && this.basePortLocation.fromObject(source.basePortLocation);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
    source.recorderPerson && this.recorderPerson.fromObject(source.recorderPerson);
    return this;
  }
}


export class PmfmStrategy extends Entity<PmfmStrategy>  {
  label: string;
  name: string;
  unit: string;
  type: string;
  minValue: number;
  maxValue: number;
  maximumNumberDecimals: number;
  defaultValue: number;
  acquisitionNumber: number;
  isMandatory: boolean;
  rankOrder: number;

  acquisitionLevel: string;
  gears: string[];
  qualitativeValues: Referential[];

  constructor() {
    super();
  }

  clone(): PmfmStrategy {
    const target = new PmfmStrategy();
    this.copy(target);
    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.clone()) || undefined;
    return target;
  }

  copy(target: PmfmStrategy): PmfmStrategy {
    target.fromObject(this);
    return target;
  }

  asObject(): any {
    const target: any = super.asObject();
    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.asObject()) || undefined;
    return target;
  }

  fromObject(source: any): PmfmStrategy {
    super.fromObject(source);

    this.label = source.label;
    this.name = source.name;
    this.unit = source.unit;
    this.type = source.type;
    this.minValue = source.minValue;
    this.maxValue = source.maxValue;
    this.maximumNumberDecimals = source.maximumNumberDecimals;
    this.defaultValue = source.defaultValue;
    this.acquisitionNumber = source.acquisitionNumber;
    this.isMandatory = source.isMandatory;
    this.rankOrder = source.rankOrder;
    this.acquisitionLevel = source.acquisitionLevel;
    this.gears = source.gears || [];
    this.qualitativeValues = source.qualitativeValues && source.qualitativeValues.map(json => Referential.fromObject(json)) || [];
    return this;
  }
}