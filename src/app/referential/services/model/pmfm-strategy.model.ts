import {EntityAsObjectOptions} from "../../../core/services/model/entity.model";
import {Pmfm, PMFM_NAME_REGEXP, PmfmType} from "./pmfm.model";
import {ReferentialRef} from "../../../core/services/model/referential.model";
import {toNumber} from "../../../shared/functions";
import {PmfmValue, PmfmValueUtils} from "./pmfm-value.model";
import {MethodIds} from "./model.enum";
import {DataEntity, DataEntityAsObjectOptions} from "../../../data/services/model/data-entity.model";


/**
 * Compute a PMFM.NAME, with the last part of the name
 * @param pmfm
 * @param opts
 */
export function getPmfmName(pmfm: PmfmStrategy, opts?: {
  withUnit?: boolean;
  html?: boolean;
  withDetails?: boolean;
}): string {
  if (!pmfm) return undefined;

  // Is complete name exists, use it
  if (opts && opts.withDetails && pmfm.completeName) return pmfm.completeName;

  // Remove parenthesis content, if any
  const matches = PMFM_NAME_REGEXP.exec(pmfm.name || '');
  let name = matches && matches[1] || pmfm.name;

  // Wen name is defined in pmfmStrategy.pmfm but not in pmfmStrategy
  if (!name && pmfm.pmfm) {
    const matchesInPmfm = PMFM_NAME_REGEXP.exec(pmfm.pmfm.name || '');
    name = matchesInPmfm && matchesInPmfm[1] || pmfm.pmfm.name;
  }
  const unitLabel = (pmfm.type === 'integer' || pmfm.type === 'double') && (pmfm.unitLabel || (pmfm.pmfm && pmfm.pmfm.unit && pmfm.pmfm.unit.label));

  if (opts && opts.withDetails) {
    // Try using the pmfm
    if (opts.withUnit && unitLabel) {
      name += ` (${unitLabel})`;
    }
    if (pmfm.matrix && pmfm.matrix.name){
      name += ` - ${pmfm.matrix.name}`;
    }
    if (pmfm.fraction && pmfm.fraction.name) {
      name += ` - ${pmfm.fraction.name}`;
    }
    if (pmfm.method && pmfm.method.name){
      name += ` - ${pmfm.method.name}`;
    }
  }
  else if ((!opts || opts.withUnit !== false) && unitLabel) {
    if (opts && opts.html) {
      name += `<small><br/>(${unitLabel})</small>`;
    }
    else {
      name += ` (${unitLabel})`;
    }
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
    const target = new PmfmStrategy();
    target.fromObject(source, opts);
    return target;
  }

  static fromPmfm(source: Pmfm): PmfmStrategy {
    if (!source || !(source instanceof Pmfm)) return undefined;
    const target = new PmfmStrategy();
    target.fromObject({
      id: -1,
      pmfmId: source.id,
      pmfm: source.asObject()
    });
    return target;
  }

  pmfmId: number;
  pmfm: Pmfm;

  parameterId: number;
  matrixId: number;
  fractionId: number;
  methodId: number;

  parameter: ReferentialRef;
  matrix: ReferentialRef;
  fraction: ReferentialRef;
  method: ReferentialRef;

  label: string;
  name: string;
  completeName: string;
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

    target.pmfmId = toNumber(this.pmfmId, this.pmfm && this.pmfm.id);
    delete target.pmfm;

    // Serialize default value
    // only if NOT an alphanumerical value (DB column is a double) or a computed PMFM
    if (this.defaultValue && (!this.isAlphanumeric && !this.isComputed)) {
      target.defaultValue = +(PmfmValueUtils.toModelValue(this.defaultValue, this.pmfm));
    }
    else {
      delete target.defaultValue;
    }

    return target;
  }

  fromObject(source: any, opts?: PmfmStrategyFromObjectOptions): PmfmStrategy {
    super.fromObject(source, opts);

    this.pmfm = source.pmfm && Pmfm.fromObject(source.pmfm);
    this.pmfmId = toNumber(source.pmfmId, source.pmfm && source.pmfm.id);
    this.parameterId =  source.parameterId ? (source.parameterId.id ? source.parameterId.id : source.parameterId) : undefined;
    this.parameter = source.parameter;
    this.matrixId = source.matrixId;
    this.fractionId = source.fractionId;
    this.methodId = source.methodId;
    this.label = source.label;
    this.name = source.name || (source.pmfm && source.pmfm.name);
    this.unitLabel = source.unitLabel || (source.pmfm && source.pmfm.unit && source.pmfm.unit.label);
    this.completeName = source.completeName;
    this.type = source.type || source.pmfm && source.pmfm.type;
    this.minValue = source.minValue;
    this.maxValue = source.maxValue;
    this.maximumNumberDecimals = source.maximumNumberDecimals;
    this.defaultValue = source.defaultValue;
    this.acquisitionNumber = source.acquisitionNumber;
    this.isMandatory = source.isMandatory;
    this.rankOrder = source.rankOrder;
    this.acquisitionLevel = source.acquisitionLevel;
    this.gearIds = source.gearIds && [...source.gearIds] || undefined;
    this.taxonGroupIds = source.taxonGroupIds && [...source.taxonGroupIds] || undefined;
    this.referenceTaxonIds = source.referenceTaxonIds && [...source.referenceTaxonIds] || undefined;
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
    return this.type === 'integer' || this.type === 'double';
  }

  get isAlphanumeric(): boolean {
    return this.type === 'string';
  }

  get isDate(): boolean {
    return this.type === 'date';
  }

  get isComputed(): boolean {
    return this.type && (this.methodId === MethodIds.CALCULATED);
  }

  get isQualitative(): boolean {
    return this.type === 'qualitative_value';
  }

  get hasUnit(): boolean {
    return this.unitLabel && this.isNumeric;
  }

  get isWeight(): boolean {
    return this.label && this.label.endsWith("WEIGHT");
  }

  equals(other: PmfmStrategy): boolean {
    return other && (this.id === other.id
      // Same acquisitionLevel and pmfmId
      || (this.strategyId === other.strategyId && this.acquisitionLevel === other.acquisitionLevel && (this.pmfmId === other.pmfmId || -this.pmfm && this.pmfm.id === other.pmfmId) )
    );
  }
}


export class PmfmStrategyRef extends DataEntity<PmfmStrategy, PmfmStrategyAsObjectOptions> {

  static TYPENAME = 'PmfmStrategyVO';

  static fromObject(source: any, opts?: any): PmfmStrategyRef {
    if (!source || source instanceof PmfmStrategyRef) return source;
    const res = new PmfmStrategyRef();
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
  matrix: ReferentialRef;
  fraction: ReferentialRef;
  method: ReferentialRef;

  label: string;
  name: string;
  headerName: string;
  unitLabel: string;
  type: string | PmfmType;
  minValue: number;
  maxValue: number;
  maximumNumberDecimals: number;
  defaultValue: PmfmValue;
  acquisitionNumber: number;
  isMandatory: boolean;
  rankOrder: number;

  acquisitionLevel: string;

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
    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.asObject(options)) || undefined;
    target.defaultValue = +(PmfmValueUtils.toModelValue(this.defaultValue, this.pmfm));
    return target;
  }

  fromObject(source: any, opts?: any): PmfmStrategyRef {
    super.fromObject(source, opts);

    this.pmfmId = source.pmfmId;
    this.parameterId =  source.parameterId;
    this.matrixId = source.matrixId;
    this.fractionId = source.fractionId;
    this.methodId = source.methodId;
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
    this.gearIds = source.gearIds && [...source.gearIds] || undefined;
    this.taxonGroupIds = source.taxonGroupIds && [...source.taxonGroupIds] || undefined;
    this.referenceTaxonIds = source.referenceTaxonIds && [...source.referenceTaxonIds] || undefined;
    this.qualitativeValues = source.qualitativeValues && source.qualitativeValues.map(ReferentialRef.fromObject);
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
    return this.type === 'integer' || this.type === 'double';
  }

  get isAlphanumeric(): boolean {
    return this.type === 'string';
  }

  get isDate(): boolean {
    return this.type === 'date';
  }

  get isComputed(): boolean {
    return this.type && (this.methodId === MethodIds.CALCULATED);
  }

  get isQualitative(): boolean {
    return this.type === 'qualitative_value';
  }

  get hasUnit(): boolean {
    return this.unitLabel && this.isNumeric;
  }

  get isWeight(): boolean {
    return this.label && this.label.endsWith("WEIGHT");
  }

  equals(other: PmfmStrategy): boolean {
    return other && (this.id === other.id
      // Same strategy, acquisitionLevel, pmfmId
      || (this.strategyId === other.strategyId && this.acquisitionLevel === other.acquisitionLevel && (this.pmfmId === other.pmfmId) )
    );
  }
}
