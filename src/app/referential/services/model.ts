import {
  Cloneable, Entity, EntityUtils,
  Referential, ReferentialRef, Department, Person,
  StatusIds, AcquisitionLevelCodes,
  toDateISOString, fromDateISOString, joinProperties, entityToString, referentialToString, isNotNil, isNil
} from "../../core/core.module";
import {Moment} from "moment/moment";

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

export const TaxonomicLevelIds = {
  ORDO: 13,
  FAMILY: 17,
  GENUS: 26,
  SUBGENUS: 27,
  SPECIES: 28,
  SUBSPECIES: 29
}

export const PmfmIds = {
  TRIP_PROGRESS: 34,
  SURVIVAL_SAMPLING_TYPE: 35,
  TAG_ID: 82,
  DISCARD_OR_LANDING: 90,
  IS_DEAD: 94,
  DISCARD_REASON: 95,
  DEATH_TIME: 101,
  VERTEBRAL_COLUMN_ANALYSIS: 102,
  IS_SAMPLING: 121
}

export const QualitativeLabels = {
  DISCARD_OR_LANDING: {
    LANDING: 'LAN',
    DISCARD: 'DIS'
  },
  SURVIVAL_SAMPLING_TYPE: {
    SURVIVAL: 'S',
    CATCH_HAUL: 'C',
    UNSAMPLED: 'N'
  }
}

export const MethodIds = {
  MEASURED_BY_OBSERVER: 1,
  OBSERVED_BY_OBSERVER: 2,
  ESTIMATED_BY_OBSERVER: 3,
  CALCULATED: 4
}

export const PmfmLabelPatterns = {
  BATCH_WEIGHT: /^BATCH_(.+)_WEIGHT$/
}

export const ProgramProperties = {

  // Trip
  TRIP_SALE_ENABLE: 'sumaris.trip.sale.enable',
  BATCH_TAXON_NAME_ENABLE: 'sumaris.trip.operation.batch.taxonName.enable',
  BATCH_TAXON_GROUP_ENABLE: 'sumaris.trip.operation.batch.taxonGroup.enable',
  SAMPLE_TAXON_NAME_ENABLE: 'sumaris.trip.operation.batch.taxonName.enable',
  SAMPLE_TAXON_GROUP_ENABLE: 'sumaris.trip.operation.batch.taxonGroup.enable',
  SURVIVAL_TEST_TAXON_NAME_ENABLE: 'sumaris.trip.operation.survivalTest.taxonName.enable',
  SURVIVAL_TEST_TAXON_GROUP_ENABLE: 'sumaris.trip.operation.survivalTest.taxonGroup.enable',

  // Observed location
  OBSERVED_LOCATION_END_DATE_TIME_ENABLE: 'sumaris.observedLocation.endDateTime.enable',
  OBSERVED_LOCATION_LOCATION_LEVEL_IDS: 'sumaris.observedLocation.location.level.ids'
}

export const QualityFlagIds = {
  NOT_QUALIFIED: 0,
  GOOD: 1,
  OUT_STATS: 2,
  DOUBTFUL: 3,
  BAD: 4,
  FIXED: 5,
  NOT_COMPLETED: 8,
  MISSING: 9
};

const PMFM_NAME_REGEXP = new RegExp(/^\s*([^\/]+)[/]\s*(.*)$/);

export {
  EntityUtils, StatusIds, AcquisitionLevelCodes,
  Cloneable, Entity, Department, Person, Referential, ReferentialRef,
  toDateISOString, fromDateISOString, joinProperties, isNotNil, isNil,
  entityToString, referentialToString
};

export function vesselFeaturesToString(obj: VesselFeatures | any): string | undefined {
  return obj && obj.vesselId && joinProperties(obj, ['exteriorMarking', 'name']) || undefined;
}

export function getPmfmName(pmfm: PmfmStrategy, opts?: {
  withUnit: boolean
}): string {
  var matches = PMFM_NAME_REGEXP.exec(pmfm.name);
  const name = matches && matches[1] || pmfm.name;
  if (opts && opts.withUnit && pmfm.unit && (pmfm.type == 'integer' || pmfm.type == 'double')) {
    return `${name} (${pmfm.unit})`;
  }
  return name;
}

export function qualityFlagToColor(qualityFlagId: number) {
  switch (qualityFlagId) {
    case QualityFlagIds.NOT_QUALIFIED:
      return 'tertiary';
    case QualityFlagIds.GOOD:
    case QualityFlagIds.FIXED:
      return 'success';
    case QualityFlagIds.OUT_STATS:
    case QualityFlagIds.DOUBTFUL:
      return 'warning';
    case QualityFlagIds.BAD:
    case QualityFlagIds.MISSING:
    case QualityFlagIds.NOT_COMPLETED:
      return 'danger';
    default:
      return 'secondary';
  }
}

export interface IWithProgramEntity<T> extends Entity<T> {
  program: Referential|ReferentialRef;
  recorderPerson?: Person;
  recorderDepartment: Referential|ReferentialRef;
}

export class VesselFeatures extends Entity<VesselFeatures> {

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
  administrativePower: number;
  lengthOverAll: number;
  grossTonnageGt: number;
  grossTonnageGrt: number;
  basePortLocation: ReferentialRef;
  creationDate: Date | Moment;
  recorderDepartment: Department;
  recorderPerson: Person;
  comments: string;

  constructor() {
    super();
    this.basePortLocation = new ReferentialRef();
    this.recorderDepartment = new Department();
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

  asObject(minify?: boolean): any {
    const target: any = super.asObject(minify);

    target.basePortLocation = this.basePortLocation && this.basePortLocation.asObject(minify) || undefined;
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(minify) || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(minify) || undefined;

    return target;
  }

  fromObject(source: any): VesselFeatures {
    super.fromObject(source);
    this.exteriorMarking = source.exteriorMarking;
    this.name = source.name;
    this.comments = source.comments || undefined;
    this.vesselId = source.vesselId;
    this.vesselTypeId = source.vesselTypeId;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.administrativePower = source.administrativePower || undefined;
    this.lengthOverAll = source.lengthOverAll || undefined;
    this.grossTonnageGt = source.grossTonnageGt || undefined;
    this.grossTonnageGrt = source.grossTonnageGrt || undefined;
    this.creationDate = fromDateISOString(source.creationDate);
    source.basePortLocation && this.basePortLocation.fromObject(source.basePortLocation);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
    source.recorderPerson && this.recorderPerson.fromObject(source.recorderPerson);
    return this;
  }
}


export class Program extends Entity<Program> {

  static fromObject(source: any): Program {
    const res = new Program();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  description: string;
  comments: string;
  creationDate: Date | Moment;
  statusId: number;
  properties: { [key: string]: string };

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

  clone(): Program {
    return this.copy(new Program());
  }

  copy(target: Program): Program {
    target.fromObject(this);
    return target;
  }

  asObject(minify?: boolean): any {
    if (minify) return {id: this.id}; // minify=keep id only
    const target: any = super.asObject(minify);
    target.creationDate = toDateISOString(this.creationDate);
    target.properties = this.properties;
    return target;
  }

  fromObject(source: any): Entity<Program> {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.description = source.description;
    this.comments = source.comments;
    this.statusId = source.statusId;
    this.creationDate = fromDateISOString(source.creationDate);
    if (source.properties && source.properties instanceof Array) {
      this.properties = EntityUtils.getArrayAsObject(source.properties);
    } else {
      this.properties = source.properties;
    }
    return this;
  }

  equals(other: Program): boolean {
    return super.equals(other) && this.id === other.id;
  }

  getPropertyAsBoolean(key: string, defaultValue?: boolean): boolean {
    return isNotNil(this.properties[key]) ? this.properties[key] !== "false" : (defaultValue || false);
  }

  getPropertyAsNumbers(key: string): number[] {
    return this.properties[key] && this.properties[key].split(',').map(parseInt) || undefined;
  }
}

export declare type PmfmType = 'integer' | 'double' | 'string' | 'qualitative_value' | 'date' | 'boolean' ;

export class PmfmStrategy extends Entity<PmfmStrategy> {

  static fromObject(source: any): PmfmStrategy {
    const res = new PmfmStrategy();
    res.fromObject(source);
    return res;
  }

  pmfmId: number;
  methodId: number;
  label: string;
  name: string;
  unit: string;
  type: string | PmfmType;
  minValue: number;
  maxValue: number;
  maximumNumberDecimals: number;
  defaultValue: number;
  acquisitionNumber: number;
  isMandatory: boolean;
  rankOrder: number;

  acquisitionLevel: string;
  gears: string[];
  qualitativeValues: ReferentialRef[];

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

    this.pmfmId = source.pmfmId;
    this.methodId = source.methodId;
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
    this.qualitativeValues = source.qualitativeValues && source.qualitativeValues.map(ReferentialRef.fromObject) || [];
    return this;
  }

  get isNumeric(): boolean {
    return isNotNil(this.type) && (this.type === 'integer' || this.type === 'double');
  }

  get isDate(): boolean {
    return isNotNil(this.type) && (this.type === 'date');
  }

  get hasUnit(): boolean {
    return isNotNil(this.unit) && this.isNumeric;
  }
}
