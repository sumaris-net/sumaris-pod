import {Entity, EntityAsObjectOptions, IEntity}  from "@sumaris-net/ngx-components";
import {BaseReferential, ReferentialRef}  from "@sumaris-net/ngx-components";
import {isNotNil} from "@sumaris-net/ngx-components";
import {MethodIds, PmfmIds} from "./model.enum";
import {Parameter, ParameterType} from "./parameter.model";
import {PmfmValue} from "./pmfm-value.model";
import {EntityClass}  from "@sumaris-net/ngx-components";

export declare type PmfmType = ParameterType | 'integer';

export const PMFM_ID_REGEXP = /\d+/;

export const PMFM_NAME_REGEXP = new RegExp(/^\s*([^\/]+)[/]\s*(.*)$/);

export interface IPmfm<
  T extends Entity<T, ID> = Entity<any, any>,
  ID = number
  > extends IEntity<IPmfm<T, ID>, ID> {
  id: ID;
  label: string;

  type: string | PmfmType;
  minValue: number;
  maxValue: number;
  defaultValue: number|PmfmValue;
  maximumNumberDecimals: number;
  signifFiguresNumber: number;

  matrixId: number;
  fractionId: number;
  methodId: number;

  qualitativeValues: ReferentialRef[];

  unitLabel: string;
  isQualitative: boolean;
  required?: boolean;
  isComputed: boolean;
  hidden?: boolean;
  rankOrder?: number;
}

@EntityClass()
export class Pmfm extends BaseReferential<Pmfm> implements IPmfm<Pmfm> {

  static ENTITY_NAME = 'Pmfm';
  static fromObject: (source: any, opts?: any) => Pmfm;

  type: string | PmfmType;
  minValue: number;
  maxValue: number;
  defaultValue: number;
  maximumNumberDecimals: number;
  signifFiguresNumber: number;

  parameter: Parameter;
  matrix: ReferentialRef;
  fraction: ReferentialRef;
  method: ReferentialRef;
  unit: ReferentialRef;

  qualitativeValues: ReferentialRef[];

  completeName: string; // Computed attributes

  constructor() {
    super();
    this.entityName = Pmfm.ENTITY_NAME;
  }

  asObject(options?: EntityAsObjectOptions): any {
    const target: any = super.asObject({
      ...options,
      minify: false // Do NOT minify itself
    });

    if (options && options.minify) {
      target.parameterId = this.parameter && this.parameter.id;
      target.matrixId = this.matrix && this.matrix.id;
      target.fractionId = this.fraction && this.fraction.id;
      target.methodId = this.method && this.method.id;
      target.unitId = this.unit && this.unit.id;
      delete target.parameter;
      delete target.matrix;
      delete target.fraction;
      delete target.method;
      delete target.unit;
    }
    else {
      target.parameter = this.parameter && this.parameter.asObject(options);
      target.matrix = this.matrix && this.matrix.asObject(options);
      target.fraction = this.fraction && this.fraction.asObject(options);
      target.method = this.method && this.method.asObject(options);
      target.unit = this.unit && this.unit.asObject(options);
    }

    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.asObject(options)) || undefined;
    return target;
  }

  fromObject(source: any): Pmfm {
    super.fromObject(source);

    this.entityName = source.entityName || Pmfm.ENTITY_NAME;
    this.type = source.type;
    this.minValue = source.minValue;
    this.maxValue = source.maxValue;
    this.defaultValue = source.defaultValue;
    this.maximumNumberDecimals = source.maximumNumberDecimals;
    this.signifFiguresNumber = source.signifFiguresNumber;

    this.parameter = source.parameter && Parameter.fromObject(source.parameter);
    this.matrix = source.matrix && ReferentialRef.fromObject(source.matrix);
    this.fraction = source.fraction && ReferentialRef.fromObject(source.fraction);
    this.method = source.method && ReferentialRef.fromObject(source.method);
    this.unit = source.unit && ReferentialRef.fromObject(source.unit);

    this.qualitativeValues = source.qualitativeValues && source.qualitativeValues.map(ReferentialRef.fromObject) || undefined;

    this.completeName = source.completeName;
    return this;
  }

  get isQualitative(): boolean {
    return this.type === 'qualitative_value';
  }

  get matrixId(): number {
    return this.matrix && this.matrix.id;
  }

  get fractionId(): number {
    return this.fraction && this.fraction.id;
  }

  get methodId(): number {
    return this.method && this.method.id;
  }

  get unitLabel(): string {
    return this.unit && this.unit.label;
  }

  get isComputed(): boolean {
    return PmfmUtils.isComputed(this);
  }
}



export abstract class PmfmUtils {

  static getFirstQualitativePmfm<P extends IPmfm>(pmfms: P[]): P {
    let qvPmfm = pmfms.find(p => p.type === 'qualitative_value'
      // exclude hidden pmfm (see batch modal)
      && !p.hidden
    );
    // If landing/discard: 'Landing' is always before 'Discard (see issue #122)
    if (qvPmfm && qvPmfm.id === PmfmIds.DISCARD_OR_LANDING) {
      qvPmfm = qvPmfm.clone() as P; // copy, to keep original array
      qvPmfm.qualitativeValues.sort((qv1, qv2) => qv1.label === 'LAN' ? -1 : 1);
    }
    return qvPmfm;
  }

  static isNumeric(pmfm: IPmfm): boolean {
    return isNotNil(pmfm.type) && (pmfm.type === 'integer' || pmfm.type === 'double');
  }

  static isAlphanumeric(pmfm: IPmfm): boolean {
    return isNotNil(pmfm.type) && (pmfm.type === 'string');
  }

  static isDate(pmfm: IPmfm): boolean {
    return isNotNil(pmfm.type) && (pmfm.type === 'date');
  }

  static isWeight(pmfm: IPmfm): boolean {
    return isNotNil(pmfm.label) && pmfm.label.endsWith("WEIGHT");
  }

  static hasParameterLabelIncludes(pmfm: Pmfm, labels: string[]): boolean {
    return pmfm && isNotNil(pmfm.parameter) && labels.includes(pmfm.parameter.label);
  }

  static isComputed(pmfm: IPmfm) {
    return pmfm.methodId === MethodIds.CALCULATED;
  }
}

