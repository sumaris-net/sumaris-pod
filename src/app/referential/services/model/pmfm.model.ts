import {Entity, EntityAsObjectOptions, IEntity} from "../../../core/services/model/entity.model";
import {Referential, ReferentialRef} from "../../../core/services/model/referential.model";
import {isNotNil} from "../../../shared/functions";
import {MethodIds, PmfmIds} from "./model.enum";
import {Parameter, ParameterType} from "./parameter.model";
import {PmfmValue} from "./pmfm-value.model";

export declare type PmfmType = ParameterType | 'integer';

export const PMFM_ID_REGEXP = /\d+/;

export const PMFM_NAME_REGEXP = new RegExp(/^\s*([^\/]+)[/]\s*(.*)$/);

export interface IPmfm<T extends Entity<T> = Entity<any>> extends IEntity<IPmfm<T>> {
  id: number;
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
}

export class Pmfm extends Referential<Pmfm> implements IPmfm<Pmfm> {

  static TYPENAME = 'Pmfm';

  static fromObject(source: any): Pmfm {
    if (!source || source instanceof Pmfm) return source;
    const res = new Pmfm();
    res.fromObject(source);
    return res;
  }

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
    this.entityName = Pmfm.TYPENAME;
  }

  clone(): Pmfm {
    const target = new Pmfm();
    this.copy(target);
    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.clone()) || undefined;
    return target;
  }

  copy(target: Pmfm): Pmfm {
    target.fromObject(this);
    return target;
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

    this.entityName = source.entityName || Pmfm.TYPENAME;
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

