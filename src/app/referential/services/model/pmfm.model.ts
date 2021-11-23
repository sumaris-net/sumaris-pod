import { BaseReferential, Entity, EntityAsObjectOptions, EntityClass, fromDateISOString, IEntity, isNotNil, ReferentialRef } from '@sumaris-net/ngx-components';
import { MethodIds, PmfmIds, UnitLabel, WeightSymbol } from './model.enum';
import { Parameter, ParameterType } from './parameter.model';
import { PmfmValue } from './pmfm-value.model';
import { Moment } from 'moment';

export declare type PmfmType = ParameterType | 'integer';

export const PMFM_ID_REGEXP = /\d+/;

export const PMFM_NAME_REGEXP = new RegExp(/^\s*([^\/(]+)[/(]\s*(.*)$/);

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
  rankOrder?: number;

  isQualitative: boolean;
  isComputed: boolean;
  isMultiple: boolean;
  required?: boolean;
  hidden?: boolean;

  displayConversion?: UnitConversion;
}

export interface IDenormalizedPmfm<
  T extends Entity<T, ID> = Entity<any, any>,
  ID = number
  > extends IPmfm<T, ID> {

  completeName?: string;
  name?: string;
  acquisitionNumber?: number;

  gearIds: number[];
  taxonGroupIds: number[];
  referenceTaxonIds: number[];

}


export interface IFullPmfm<
  T extends Entity<T, ID> = Entity<any, any>,
  ID = number
  > extends IPmfm<T, ID> {

  parameter: Parameter;
  matrix: ReferentialRef;
  fraction: ReferentialRef;
  method: ReferentialRef;
  unit: ReferentialRef;
}


@EntityClass({typename: 'UnitConversionVO'})
export class UnitConversion {

  static fromObject: (source: Partial<UnitConversion>, opts?: any) => UnitConversion;

  fromUnit: ReferentialRef;
  toUnit: ReferentialRef;
  conversionCoefficient: number;
  updateDate: Moment;

  constructor() {
  }

  fromObject(source: any) {
    this.fromUnit = source.fromUnit && ReferentialRef.fromObject(source.fromUnit);
    this.toUnit = source.toUnit && ReferentialRef.fromObject(source.toUnit);
    this.conversionCoefficient = source.conversionCoefficient;
    this.updateDate = fromDateISOString(source.updateDate);
  }
}

@EntityClass({typename: 'PmfmVO'})
export class Pmfm extends BaseReferential<Pmfm> implements IFullPmfm<Pmfm> {

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
  // alreadyConverted: boolean;

  constructor() {
    super(Pmfm.TYPENAME);
    this.entityName = Pmfm.ENTITY_NAME;
  }

  asObject(opts?: EntityAsObjectOptions): any {
    const target: any = super.asObject({
      ...opts,
      minify: false // Do NOT minify itself
    });

    if (opts && opts.minify) {
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
      target.parameter = this.parameter && this.parameter.asObject(opts);
      target.matrix = this.matrix && this.matrix.asObject(opts);
      target.fraction = this.fraction && this.fraction.asObject(opts);
      target.method = this.method && this.method.asObject(opts);
      target.unit = this.unit && this.unit.asObject(opts);
    }

    target.qualitativeValues = this.qualitativeValues && this.qualitativeValues.map(qv => qv.asObject(opts)) || undefined;
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

  get isMultiple(): boolean {
    return false; // Default value
  }
}

export abstract class PmfmUtils {

  static NAME_WITH_WEIGHT_UNIT_REGEXP = /^(.* )\((t|kg|g|mg)\)( - .*)?$/;

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
    return pmfm.type === 'integer' || pmfm.type === 'double';
  }

  static isAlphanumeric(pmfm: IPmfm): boolean {
    return pmfm.type === 'string';
  }

  static isDate(pmfm: IPmfm): boolean {
    return pmfm.type === 'date';
  }

  static isWeight(pmfm: IPmfm): boolean {
    return pmfm.unitLabel === UnitLabel.KG || pmfm.label?.endsWith("WEIGHT") || (pmfm instanceof Pmfm && (pmfm as Pmfm).parameter?.label?.endsWith("WEIGHT"));
  }

  static hasParameterLabelIncludes(pmfm: Pmfm, labels: string[]): boolean {
    return pmfm && labels.includes(pmfm.parameter.label);
  }

  static isComputed(pmfm: IPmfm) {
    return pmfm.methodId === MethodIds.CALCULATED;
  }

  static isDenormalizedPmfm(pmfm: IPmfm): pmfm is IDenormalizedPmfm {
    return (pmfm['completeName'] || pmfm['name']) && true;
  }

  static isFullPmfm(pmfm: IPmfm): pmfm is IFullPmfm {
    return pmfm['parameter'] && true;
  }

  /**
   * Compute a PMFM.NAME, with the last part of the name
   * @param pmfm
   * @param opts
   */
  static getPmfmName(pmfm: IPmfm, opts?: {
    withUnit?: boolean;
    html?: boolean;
    withDetails?: boolean;
  }): string {
    if (!pmfm) return undefined;

    let name;
    if (PmfmUtils.isDenormalizedPmfm(pmfm)) {
      // Is complete name exists, use it
      if (opts && opts.withDetails && pmfm.completeName) {
        if (opts.html) {
          const parts = pmfm.completeName.split(' - ')
          return parts.length === 1 ? pmfm.completeName : `<b>${parts[0]}</b><br/><span style="font-size: smaller;">` + parts.slice(1).join(' - ') + '</span>';
        }
        return pmfm.completeName;
      }

      // Remove parenthesis content, if any
      const matches = PMFM_NAME_REGEXP.exec(pmfm.name || '');
      name = matches && matches[1] || pmfm.name;
    } else if (PmfmUtils.isFullPmfm(pmfm)) {
      name = pmfm.parameter && pmfm.parameter.name;
      if (opts && opts.withDetails) {
        name += [
          pmfm.matrix && pmfm.matrix.name,
          pmfm.fraction && pmfm.fraction.name,
          pmfm.method && pmfm.method.name
        ].filter(isNotNil).join(' - ');
      }
    }

    // Append unit
    if ((!opts || opts.withUnit !== false) && (pmfm.type === 'integer' || pmfm.type === 'double') && pmfm.unitLabel) {
      if (opts && opts.html) {
        name += `<small><br/>(${pmfm.unitLabel})</small>`;
      } else {
        name += ` (${pmfm.unitLabel})`;
      }
    }
    return name;
  }

  static setUnitConversions<P extends IPmfm>(pmfms: P[], expectedWeightSymbol: WeightSymbol, opts?: {
    clone?: boolean;
  }): P[] {
    (pmfms || []).forEach((pmfm, i) => {
      // Replace weight PMFM
      if (PmfmUtils.isWeight(pmfm)) {
        pmfms[i] = this.setWeightUnitConversion(pmfm, expectedWeightSymbol, opts);
      }
    });
    return pmfms;
  }

  static setWeightUnitConversion<P extends IPmfm>(pmfm: P, expectedWeightSymbol: WeightSymbol, opts?: {
    clone?: boolean;
  }): P {
    if (!this.isWeight(pmfm)) return pmfm;

    const actualWeightUnit = pmfm.unitLabel || UnitLabel.KG;
    if (actualWeightUnit === expectedWeightSymbol) return; // Conversion not need

    // Clone, to keep existing pmfm unchanged
    if (!opts || opts.clone !== false) {
      pmfm = pmfm.clone() as P;
    }

    if (this.isDenormalizedPmfm(pmfm)) {
      pmfm.unitLabel = expectedWeightSymbol;


      // Update the complete name (the unit part), if exists
      const matches = pmfm.completeName && this.NAME_WITH_WEIGHT_UNIT_REGEXP.exec(pmfm.completeName);
      if (matches) {
        pmfm.completeName = `${matches[1]}(${expectedWeightSymbol})${matches[3]||''}`;
      }
    }
    else if ((pmfm instanceof Pmfm) && pmfm.unit) {
      pmfm.unit.label = expectedWeightSymbol;
      pmfm.unit.name = expectedWeightSymbol;
    }
    if (actualWeightUnit === UnitLabel.KG && expectedWeightSymbol === UnitLabel.GRAM) {
      pmfm.displayConversion = UnitConversion.fromObject({conversionCoefficient: 1000});
    }
    else if (actualWeightUnit === UnitLabel.GRAM && expectedWeightSymbol === UnitLabel.KG) {
      pmfm.displayConversion = UnitConversion.fromObject({conversionCoefficient: 1/1000});
    }


  }
}


