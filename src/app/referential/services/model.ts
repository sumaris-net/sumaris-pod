import {
  Cloneable,
  Department,
  Entity,
  entityToString,
  EntityUtils,
  fromDateISOString,
  isNil,
  isNotNil,
  joinPropertiesPath,
  Person,
  Referential,
  ReferentialRef,
  referentialToString,
  StatusIds,
  toDateISOString
} from "../../core/core.module";
import {Moment} from "moment/moment";
import {FormFieldDefinition, FormFieldDefinitionMap} from "../../shared/form/field.model";
import {PropertiesMap} from "../../core/services/model";
import {TaxonGroupRef, TaxonNameRef} from "./model/taxon.model";

// TODO BL: gérer pour etre dynamique (=6 pour le SIH)
export const LocationLevelIds = {
  COUNTRY: 1,
  PORT: 6,
  AUCTION: 3
};

export const GearLevelIds = {
  FAO: 1
};

export const TaxonGroupIds = {
  FAO: 2,
  METIER: 3
};

export const TaxonomicLevelIds = {
  ORDO: 13,
  FAMILY: 17,
  GENUS: 26,
  SUBGENUS: 27,
  SPECIES: 28,
  SUBSPECIES: 29
};

export const PmfmIds = {
  TRIP_PROGRESS: 34,
  SURVIVAL_SAMPLING_TYPE: 35,
  TAG_ID: 82,
  DISCARD_OR_LANDING: 90,
  IS_DEAD: 94,
  DISCARD_REASON: 95,
  DEATH_TIME: 101,
  VERTEBRAL_COLUMN_ANALYSIS: 102,
  IS_SAMPLING: 121,

  /* ADAP pmfms */
  CONTROLLED_SPECIES: 134,
  SAMPLE_MEASURED_WEIGHT: 140,
  OUT_OF_SIZE: 142,
  OUT_OF_SIZE_PCT: 143,
  VIVACITY: 144
};

export const QualitativeLabels = {
  DISCARD_OR_LANDING: {
    LANDING: 'LAN',
    DISCARD: 'DIS'
  },
  SURVIVAL_SAMPLING_TYPE: {
    SURVIVAL: 'S',
    CATCH_HAUL: 'C',
    UNSAMPLED: 'N'
  },
  VIVACITY: {
    DEAD: 'MOR'
  }
};

export const MethodIds = {
  MEASURED_BY_OBSERVER: 1,
  OBSERVED_BY_OBSERVER: 2,
  ESTIMATED_BY_OBSERVER: 3,
  CALCULATED: 4
};

export const PmfmLabelPatterns = {
  BATCH_WEIGHT: /^BATCH_(.+)_WEIGHT$/
};

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
  EntityUtils, StatusIds,
  Cloneable, Entity, Department, Person, Referential, ReferentialRef,
  toDateISOString, fromDateISOString, joinPropertiesPath, isNotNil, isNil,
  entityToString, referentialToString
};

export function vesselFeaturesToString(obj: VesselFeatures | any): string | undefined {
  // TODO may be SFA will prefer 'registrationCode' instead of 'exteriorMarking' ?
  return obj && obj.vesselId && joinPropertiesPath(obj, ['exteriorMarking', 'name']) || undefined;
}

export function getPmfmName(pmfm: PmfmStrategy, opts?: {
  withUnit: boolean
}): string {
  const matches = PMFM_NAME_REGEXP.exec(pmfm.name);
  const name = matches && matches[1] || pmfm.name;
  if (opts && opts.withUnit && pmfm.unit && (pmfm.type === 'integer' || pmfm.type === 'double')) {
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
  program: Referential | any;
  recorderPerson?: Person;
  recorderDepartment: Referential | any;
}

export class VesselFeatures extends Entity<VesselFeatures> {

  static fromObject(source: any): VesselFeatures {
    if (!source || source instanceof VesselFeatures) return source;
    const res = new VesselFeatures();
    res.fromObject(source);
    return res;
  }

  vesselId: number;
  vesselType: ReferentialRef;
  vesselStatusId: number;
  name: string;
  startDate: Moment;
  endDate: Moment;
  exteriorMarking: string;
  registrationId: number;
  registrationCode: string;
  registrationStartDate: Moment;
  registrationEndDate: Moment;
  administrativePower: number;
  lengthOverAll: number;
  grossTonnageGt: number;
  grossTonnageGrt: number;
  basePortLocation: ReferentialRef;
  registrationLocation: ReferentialRef;
  creationDate: Moment;
  recorderDepartment: Department;
  recorderPerson: Person;
  comments: string;
  entityName: string;

  constructor() {
    super();
    this.vesselType = new ReferentialRef();
    this.basePortLocation = new ReferentialRef();
    this.registrationLocation = new ReferentialRef();
    this.recorderDepartment = new Department();
    this.recorderPerson = new Person();
  }

  clone(): VesselFeatures {
    const target = new VesselFeatures();
    this.copy(target);
    target.vesselType = this.vesselType && this.vesselType.clone() || undefined;
    target.basePortLocation = this.basePortLocation && this.basePortLocation.clone() || undefined;
    target.registrationLocation = this.registrationLocation && this.registrationLocation.clone() || undefined;
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

    target.vesselType = this.vesselType && this.vesselType.asObject(minify) || undefined;
    target.basePortLocation = this.basePortLocation && this.basePortLocation.asObject(minify) || undefined;
    target.registrationLocation = this.registrationLocation && this.registrationLocation.asObject(minify) || undefined;
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);
    target.registrationStartDate = toDateISOString(this.registrationStartDate);
    target.registrationEndDate = toDateISOString(this.registrationEndDate);
    target.creationDate = toDateISOString(this.creationDate);
    target.recorderDepartment = this.recorderDepartment && this.recorderDepartment.asObject(minify) || undefined;
    target.recorderPerson = this.recorderPerson && this.recorderPerson.asObject(minify) || undefined;

    return target;
  }

  fromObject(source: any): VesselFeatures {
    super.fromObject(source);
    this.exteriorMarking = source.exteriorMarking;
    this.registrationCode = source.registrationCode;
    this.name = source.name;
    this.comments = source.comments || undefined;
    this.entityName = source.entityName;
    this.vesselId = source.vesselId;
    this.vesselStatusId = source.vesselStatusId;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.registrationId = source.registrationId;
    this.registrationStartDate = fromDateISOString(source.registrationStartDate);
    this.registrationEndDate = fromDateISOString(source.registrationEndDate);
    this.administrativePower = source.administrativePower || undefined;
    this.lengthOverAll = source.lengthOverAll || undefined;
    this.grossTonnageGt = source.grossTonnageGt || undefined;
    this.grossTonnageGrt = source.grossTonnageGrt || undefined;
    this.creationDate = fromDateISOString(source.creationDate);
    source.vesselType && this.vesselType.fromObject(source.vesselType);
    source.basePortLocation && this.basePortLocation.fromObject(source.basePortLocation);
    source.registrationLocation && this.registrationLocation.fromObject(source.registrationLocation);
    source.recorderDepartment && this.recorderDepartment.fromObject(source.recorderDepartment);
    source.recorderPerson && this.recorderPerson.fromObject(source.recorderPerson);
    return this;
  }
}
export class VesselRegistration extends Entity<VesselRegistration> {

  static fromObject(source: any): VesselRegistration {
    if (!source || source instanceof VesselRegistration) return source;
    const res = new VesselRegistration();
    res.fromObject(source);
    return res;
  }

  vesselId: number;
  startDate: Moment;
  endDate: Moment;
  registrationCode: string;
  registrationLocation: any;

  constructor() {
    super();
    this.registrationLocation = new ReferentialRef();
  }

  clone(): VesselRegistration {
    const target = new VesselRegistration();
    this.copy(target);
    target.registrationLocation = this.registrationLocation && this.registrationLocation.clone() || undefined;
    return target;
  }

  copy(target: VesselRegistration): VesselRegistration {
    target.fromObject(this);
    return target;
  }

  asObject(minify?: boolean): any {
    const target: any = super.asObject(minify);

    target.registrationLocation = this.registrationLocation && this.registrationLocation.asObject(minify) || undefined;
    target.startDate = toDateISOString(this.startDate);
    target.endDate = toDateISOString(this.endDate);

    return target;
  }

  fromObject(source: any): VesselRegistration {
    super.fromObject(source);
    this.registrationCode = source.registrationCode;
    this.vesselId = source.vesselId;
    this.startDate = fromDateISOString(source.startDate);
    this.endDate = fromDateISOString(source.endDate);
    this.registrationLocation = source.registrationLocation && this.registrationLocation.fromObject(source.registrationLocation);
    return this;
  }
}

export const ProgramProperties: FormFieldDefinitionMap = {
  // Trip
  TRIP_SALE_ENABLE: {
    key: "sumaris.trip.sale.enable",
    label: "PROGRAM.OPTIONS.TRIP_SALE_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_PHYSICAL_GEAR_RANK_ORDER_ENABLE: {
    key: "false",
    label: "PROGRAM.OPTIONS.TRIP_PHYSICAL_GEAR_RANK_ORDER_ENABLE",
    defaultValue: "false",
    type: 'boolean'
  },
  TRIP_BATCH_TAXON_NAME_ENABLE: {
    key: "sumaris.trip.operation.batch.taxonName.enable",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_TAXON_NAME_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_BATCH_TAXON_GROUP_ENABLE: {
    key: "sumaris.trip.operation.batch.taxonGroup.enable",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_TAXON_GROUP_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_BATCH_MEASURE_INDIVIDUAL_COUNT_ENABLE: {
    key: "sumaris.trip.operation.batch.individualCount.enable",
    label: "PROGRAM.OPTIONS.TRIP_BATCH_MEASURE_INDIVIDUAL_COUNT_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_SAMPLE_TAXON_NAME_ENABLE: {
    key: "sumaris.trip.operation.sample.taxonName.enable",
    label: "PROGRAM.OPTIONS.TRIP_SAMPLE_TAXON_NAME_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_SAMPLE_TAXON_GROUP_ENABLE: {
    key: "sumaris.trip.operation.sample.taxonGroup.enable",
    label: "PROGRAM.OPTIONS.TRIP_SAMPLE_TAXON_GROUP_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_SURVIVAL_TEST_TAXON_NAME_ENABLE: {
    key: "sumaris.trip.operation.survivalTest.taxonName.enable",
    label: "PROGRAM.OPTIONS.TRIP_SURVIVAL_TEST_TAXON_NAME_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },
  TRIP_SURVIVAL_TEST_TAXON_GROUP_ENABLE: {
    key: "sumaris.trip.operation.survivalTest.taxonGroup.enable",
    label: "PROGRAM.OPTIONS.TRIP_SURVIVAL_TEST_TAXON_GROUP_ENABLE",
    defaultValue: "true",
    type: 'boolean'
  },

  // Observed location
  OBSERVED_LOCATION_END_DATE_TIME_ENABLE: {
    key: 'sumaris.observedLocation.endDateTime.enable',
    label: "PROGRAM.OPTIONS.OBSERVED_LOCATION_END_DATE_TIME_ENABLE",
    defaultValue: "false",
    type: 'boolean'
  },
  OBSERVED_LOCATION_LOCATION_LEVEL_IDS: {
    key: 'sumaris.observedLocation.location.level.ids',
    label: "PROGRAM.OPTIONS.OBSERVED_LOCATION_LOCATION_LEVEL_IDS",
    type: 'enum',
    values: [
      {
        key: LocationLevelIds.PORT.toString(),
        value: 'PROGRAM.OPTIONS.LOCATION_LEVEL_PORT'
      },
      {
        key: LocationLevelIds.AUCTION.toString(),
        value: 'PROGRAM.OPTIONS.LOCATION_LEVEL_AUCTION'
      }
    ],
    defaultValue: LocationLevelIds.PORT.toString(),
  },

  // Landing
  LANDING_EDITOR: {
    key: 'sumaris.landing.editor',
    label: 'PROGRAM.OPTIONS.LANDING_EDITOR',
    type: 'enum',
    values: [
      {
        key: 'landing',
        value: 'PROGRAM.OPTIONS.LANDING_EDITOR_LANDING'
      },
      {
        key: 'control',
        value: 'PROGRAM.OPTIONS.LANDING_EDITOR_CONTROL'
      }],
    defaultValue: 'landing'
  }

};

export type LandingEditor = 'landing' | 'control';

export class Program extends Entity<Program> {

  static fromObject(source: any): Program {
    if (!source || source instanceof Program) return source;
    const res = new Program();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  description: string;
  comments: string;
  creationDate: Moment;
  statusId: number;
  properties: PropertiesMap;
  strategies: Strategy[];

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
      this.properties = EntityUtils.getPropertyArrayAsObject(source.properties);
    } else {
      this.properties = source.properties;
    }
    this.strategies = (source.strategies || []).map(Strategy.fromObject);
    return this;
  }

  equals(other: Program): boolean {
    return super.equals(other) || this.label === other.label;
  }

  getPropertyAsBoolean(definition: FormFieldDefinition): boolean {
    const value = this.getProperty(definition);
    return value && value !== "false";
  }

  getPropertyAsNumbers(definition: FormFieldDefinition): number[] {
    const value = this.getProperty(definition);
    return value && value.split(',').map(parseInt) || undefined;
  }

  getProperty<T = string>(definition: FormFieldDefinition): T {
    return isNotNil(this.properties[definition.key]) ? this.properties[definition.key] : (definition.defaultValue || undefined);
  }
}

export declare type AcquisitionLevelType = 'TRIP' | 'OPERATION' | 'SALE' | 'LANDING' | 'PHYSICAL_GEAR' | 'CATCH_BATCH'
  | 'SORTING_BATCH' | 'SORTING_BATCH_INDIVIDUAL' | 'SAMPLE' | 'SURVIVAL_TEST' | 'INDIVIDUAL_MONITORING' | 'INDIVIDUAL_RELEASE'
  | 'OBSERVED_LOCATION' | 'OBSERVED_VESSEL' ;

export const AcquisitionLevelCodes: { [key: string]: AcquisitionLevelType} = {
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

export declare type PmfmType = 'integer' | 'double' | 'string' | 'qualitative_value' | 'date' | 'boolean' ;

export class PmfmStrategy extends Entity<PmfmStrategy> {

  static fromObject(source: any): PmfmStrategy {
    //if (!source || source instanceof PmfmStrategy) return source;
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
  taxonGroupIds: number[];
  referenceTaxonIds: number[];
  qualitativeValues: ReferentialRef[];

  hidden?: boolean;

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

  asObject(minify?: boolean): any {
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

  get required(): boolean {
    return this.isMandatory;
  }

  set required(value: boolean) {
    this.isMandatory = value;
  }

  get isNumeric(): boolean {
    return isNotNil(this.type) && (this.type === 'integer' || this.type === 'double');
  }

  get isAlphanumeric(): boolean {
    return isNotNil(this.type) && (this.type === 'string');
  }

  get isDate(): boolean {
    return isNotNil(this.type) && (this.type === 'date');
  }

  get isComputed(): boolean {
    return isNotNil(this.type) && (this.methodId === MethodIds.CALCULATED);
  }

  get isQualitative(): boolean {
    return isNotNil(this.type) && (this.type === 'qualitative_value');
  }

  get hasUnit(): boolean {
    return isNotNil(this.unit) && this.isNumeric;
  }

  get isWeight(): boolean {
    return isNotNil(this.label) && this.label.endsWith("WEIGHT");
  }

}


export class Strategy extends Entity<Strategy> {

  static fromObject(source: any): Strategy {
    if (!source || source instanceof Strategy) return source;
    const res = new Strategy();
    res.fromObject(source);
    return res;
  }

  label: string;
  name: string;
  description: string;
  comments: string;
  creationDate: Moment;
  statusId: number;
  pmfmStrategies: PmfmStrategy[];

  gears: any[];
  taxonGroups: any[];
  taxonNames: any[];

  programId: number;

  constructor(data?: {
    id?: number,
    label?: string,
    name?: string
  }) {
    super();
    this.id = data && data.id;
    this.label = data && data.label;
    this.name = data && data.name;
    this.pmfmStrategies = [];
    this.gears = [];
    this.taxonGroups = [];
    this.taxonNames = [];
  }

  clone(): Strategy {
    return this.copy(new Strategy());
  }

  copy(target: Strategy): Strategy {
    target.fromObject(this);
    return target;
  }

  asObject(minify?: boolean): any {
    if (minify) return {id: this.id}; // minify=keep id only
    const target: any = super.asObject(minify);
    target.programId = this.programId;
    target.creationDate = toDateISOString(this.creationDate);
    target.pmfmStrategies = this.pmfmStrategies && this.pmfmStrategies.map(s => s.asObject(false/*minify*/));
    target.gears = this.gears && this.gears.map(s => s.asObject(minify));
    target.taxonGroups = this.taxonGroups && this.taxonGroups.map(s => s.asObject(minify));
    target.taxonNames = this.taxonNames && this.taxonNames.map(s => s.asObject(minify));
    return target;
  }

  fromObject(source: any): Strategy {
    super.fromObject(source);
    this.label = source.label;
    this.name = source.name;
    this.description = source.description;
    this.comments = source.comments;
    this.statusId = source.statusId;
    this.creationDate = fromDateISOString(source.creationDate);
    this.pmfmStrategies = source.pmfmStrategies && source.pmfmStrategies.map(PmfmStrategy.fromObject) || [];
    this.gears = source.gears && source.gears.map(ReferentialRef.fromObject) || [];
    // Taxon groups, sorted by priority level
    this.taxonGroups = source.taxonGroups && (source.taxonGroups as { priorityLevel: number; taxonGroup: any; }[])
      // FIXME: priority Level is not always set, in the DB
      //.sort(propertyComparator('priorityLevel'))
      //.sort(propertyPathComparator('taxonGroup.label'))
        .map(item => TaxonGroupRef.fromObject(item.taxonGroup))  || [];
    // Taxon names, sorted by priority level
    this.taxonNames = source.taxonNames && (source.taxonNames as { priorityLevel: number; taxonName: any; }[])
      // FIXME: priority Level is not always set, in the DB
      //.sort(propertyComparator('priorityLevel'))
      //.sort(propertyPathComparator('taxonName.name'))
      .map(item => TaxonNameRef.fromObject(item.taxonName)) || [];
    //console.log('TODO check not empty strat taxon: ', this.taxonGroups, this.taxonNames)
    return this;
  }

  equals(other: Strategy): boolean {
    return super.equals(other)
      // Or by functional attributes
      || (
        // Same label
        this.label === other.label
        // Same program
        && ((!this.programId && !other.programId) || this.programId === other.programId)
      );
  }
}



export class PmfmUtils {

  static getFirstQualitativePmfm(pmfms: PmfmStrategy[]): PmfmStrategy {
    let qvPmfm = pmfms.find(p => p.isQualitative
      // exclude hidden pmfm (see batch modal)
      && !p.hidden
    );
    // If landing/discard: 'Landing' is always before 'Discard (see issue #122)
    if (qvPmfm && qvPmfm.pmfmId === PmfmIds.DISCARD_OR_LANDING) {
      qvPmfm = qvPmfm.clone(); // copy, to keep original array
      qvPmfm.qualitativeValues.sort((qv1, qv2) => qv1.label === 'LAN' ? -1 : 1);
    }
    return qvPmfm;
  }
}
