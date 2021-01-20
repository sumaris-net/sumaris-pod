import {Entity, EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {Pmfm, PMFM_NAME_REGEXP, PmfmType} from "./pmfm.model";
import {ReferentialRef} from "../../../core/services/model/referential.model";
import {isNotNil, toNumber} from "../../../shared/functions";
import { PmfmValue, PmfmValueUtils} from "./pmfm-value.model";
import {MethodIds} from "./model.enum";
import {DataEntity, DataEntityAsObjectOptions,} from "../../../data/services/model/data-entity.model";


export function getPmfmName(pmfm: PmfmStrategy, opts?: {
  withUnit: boolean
}): string {
  const matches = PMFM_NAME_REGEXP.exec(pmfm.name);
  const name = matches && matches[1] || pmfm.name;
  if (opts && opts.withUnit && pmfm.unitLabel && (pmfm.type === 'integer' || pmfm.type === 'double')) {
    return `${name} (${pmfm.unitLabel})`;
  }
  return name;
}


export interface PmfmStrategyAsObjectOptions extends DataEntityAsObjectOptions {
  batchAsTree?: boolean;
}
export interface PmfmStrategyFromObjectOptions {
}

export class PmfmStrategy extends DataEntity<PmfmStrategy, PmfmStrategyAsObjectOptions, PmfmStrategyFromObjectOptions> {

  static TYPENAME = 'PmfmStrategyVO';

  static fromObject(source: any, opts?: PmfmStrategyFromObjectOptions): PmfmStrategy {
    if (!source || source instanceof PmfmStrategy) return source;
    const res = new PmfmStrategy();
    res.fromObject(source, opts);
    return res;
  }

  pmfmId: number;
  pmfm: Pmfm;

  parameterId: number;
  matrixId: number;
  fractionId: number;
  methodId: number;

  parameter: ReferentialRef;

  label: string;
  name: string;
  unitLabel: string;
  type: string | PmfmType;
  minValue: number;
  maxValue: number;
  maximumNumberDecimals: number;
  defaultValue: PmfmValue;
  acquisitionNumber: number;
  isMandatory: boolean;
  rankOrder: number;

  acquisitionLevel: string|ReferentialRef;

  gearIds: number[];
  taxonGroupIds: number[];
  referenceTaxonIds: number[];

  qualitativeValues: ReferentialRef[];

  strategyId: number;
  hidden?: boolean;

  constructor() {
    super();
    this.__typename = PmfmStrategy.TYPENAME;
  }

  clone(): PmfmStrategy {
    const target = new PmfmStrategy();
    target.fromObject(this.asObject());
    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.clone()) || undefined;
    return target;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject(options);
    target.acquisitionLevel = (target.acquisitionLevel && typeof target.acquisitionLevel === "object" && target.acquisitionLevel.label)
      || target.acquisitionLevel;
    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.asObject(options)) || undefined;

    target.pmfmId = this.pmfm && this.pmfm.id ?  toNumber(this.pmfm.id, this.pmfmId) : null;
    delete target.pmfm;

    if (this.defaultValue) console.log("TODO check serialize PmfmStrategy.defaultValue :", target);
    target.defaultValue = +(PmfmValueUtils.toModelValue(this.defaultValue, this.pmfm));

    return target;
  }

  fromObject(source: any, opts?: PmfmStrategyFromObjectOptions): PmfmStrategy {
    super.fromObject(source, opts);

    this.pmfmId = source.pmfmId;
    this.pmfm = source.pmfm && Pmfm.fromObject(source.pmfm);
    this.parameterId =  source.parameterId ? (source.parameterId.id ? source.parameterId.id : source.parameterId) : undefined;
    this.parameter = source.parameter;
    this.matrixId = source.matrixId ? (source.matrixId.id ? source.matrixId.id : source.matrixId) : undefined; 
    this.fractionId = source.fractionId ? (source.fractionId.id ? source.fractionId.id : source.fractionId) : undefined; 
    this.methodId = source.methodId ? (source.methodId.id ? source.methodId.id : source.methodId) : undefined; 
    this.label = source.label;
    this.name = source.name;
    this.unitLabel = source.unitLabel;
    this.type = source.type;
    this.minValue = source.minValue;
    this.maxValue = source.maxValue;
    this.maximumNumberDecimals = source.maximumNumberDecimals;
    this.defaultValue = source.defaultValue;
    this.acquisitionNumber = source.acquisitionNumber;
    this.isMandatory = source.isMandatory;
    this.rankOrder = source.rankOrder;
    this.acquisitionLevel = source.acquisitionLevel;
    this.gearIds = source.gearIds;
    this.taxonGroupIds = source.taxonGroupIds;
    this.referenceTaxonIds = source.referenceTaxonIds;
    this.qualitativeValues = source.qualitativeValues && source.qualitativeValues.map(ReferentialRef.fromObject)
      || this.pmfm && (this.pmfm.qualitativeValues || this.pmfm.parameter && this.pmfm.parameter.qualitativeValues)
      || undefined;
    this.strategyId = source.strategyId;

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
    return isNotNil(this.unitLabel) && this.isNumeric;
  }

  get isWeight(): boolean {
    return isNotNil(this.label) && this.label.endsWith("WEIGHT");
  }

  equals(other: PmfmStrategy): boolean {
    return other && (this.id === other.id
      // Same acquisitionLevel and pmfmId
      || (this.strategyId === other.strategyId && this.acquisitionLevel === other.acquisitionLevel && (this.pmfmId === other.pmfmId || -this.pmfm && this.pmfm.id === other.pmfmId) )
    );
  }
}
