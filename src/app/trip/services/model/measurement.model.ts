import {AppFormUtils, Entity, fromDateISOString, isNil, isNotNil, toDateISOString} from "../../../core/core.module";
import {PmfmStrategy, ReferentialRef} from "../../../referential/referential.module";
import {DataEntity} from "./base.model";
import {FormGroup} from "@angular/forms";
import {isNotNilOrNaN} from "../../../shared/functions";

export const PMFM_ID_REGEXP = /\d+/;




export declare interface IEntityWithMeasurement<T> extends Entity<T> {
  measurementValues: { [key: string]: string };
  rankOrder?: number;
  comments?: string;
}

export class Measurement extends DataEntity<Measurement> {
  pmfmId: number;
  alphanumericalValue: string;
  numericalValue: number;
  qualitativeValue: ReferentialRef;
  digitCount: number;
  rankOrder: number;

  static fromObject(source: any): Measurement {
    const res = new Measurement();
    res.fromObject(source);
    return res;
  }

  constructor() {
    super();
    this.rankOrder = null;
  }

  clone(): Measurement {
    const target = new Measurement();
    target.fromObject(this.asObject());
    return target;
  }

  copy(target: Measurement) {
    target.fromObject(this);
  }

  asObject(minify?: boolean): any {
    const target = super.asObject(minify);
    target.qualitativeValue = this.qualitativeValue && this.qualitativeValue.asObject(minify) || undefined;
    return target;
  }

  fromObject(source: any): Measurement {
    super.fromObject(source);
    this.pmfmId = source.pmfmId;
    this.alphanumericalValue = source.alphanumericalValue;
    this.numericalValue = source.numericalValue;
    this.digitCount = source.digitCount;
    this.rankOrder = source.rankOrder;
    this.qualitativeValue = source.qualitativeValue && ReferentialRef.fromObject(source.qualitativeValue);

    return this;
  }

  equals(other: Measurement): boolean {
    return super.equals(other)
      || (
        // Same [pmfmId, rankOrder]
        (this.pmfmId && other.pmfmId && this.rankOrder === other.rankOrder)
      );
  }

}

export class MeasurementUtils {



  static initAllMeasurements(source: Measurement[], pmfms: PmfmStrategy[]): Measurement[] {
    // Work on a copy, to be able to reduce the array
    let rankOrder = 1;
    return (pmfms || []).map(pmfm => {
      const m = (source || []).find(m => m.pmfmId === pmfm.pmfmId) || new Measurement();
      m.pmfmId = pmfm.pmfmId; // apply the pmfm (need for new object)
      m.rankOrder = rankOrder++;
      return m;
    });
  }



  static getMeasurementEntityValue(source: Measurement, pmfm: PmfmStrategy): any {
    switch (pmfm.type) {
      case "qualitative_value":
        if (source.qualitativeValue && source.qualitativeValue.id) {
          return pmfm.qualitativeValues.find(qv => qv.id == source.qualitativeValue.id);
        }
        return null;
      case "integer":
      case "double":
        return source.numericalValue;
      case "string":
        return source.alphanumericalValue;
      case "boolean":
        return source.numericalValue === 1 ? true : (source.numericalValue === 0 ? false : undefined);
      case "date":
        return fromDateISOString(source.alphanumericalValue);
      default:
        throw new Error("Unknown pmfm.type for getting value of measurement: " + pmfm.type);
    }
  }


  static setMeasurementValue(value: any, target: Measurement, pmfm: PmfmStrategy) {
    value = (value === null || value === undefined) ? undefined : value;
    switch (pmfm.type) {
      case "qualitative_value":
        target.qualitativeValue = value;
        break;
      case "integer":
      case "double":
        target.numericalValue = value;
        break;
      case "string":
        target.alphanumericalValue = value;
        break;
      case "boolean":
        target.numericalValue = (value === true || value === "true") ? 1 : ((value === false || value === "false") ? 0 : undefined);
        break;
      case "date":
        target.alphanumericalValue = toDateISOString(value);
        break;
      default:
        throw new Error("Unknown pmfm.type: " + pmfm.type);
    }
  }

  static toEntityValue(value: any, pmfm: PmfmStrategy): string {
    if (isNil(value) || !pmfm) return;
    switch (pmfm.type) {
      case "qualitative_value":
        return isNotNil(value) && value.id && value.id.toString() || undefined;
      case "integer":
      case "double":
        return isNotNil(value) && !isNaN(value) && value.toString() || undefined;
      case "string":
        return value;
      case "boolean":
        return (value === true || value === "true") ? "true" : ((value === false || value === "false") ? "false" : undefined);
      case "date":
        return toDateISOString(value);
      default:
        throw new Error("Unknown pmfm.type: " + pmfm.type);
    }
  }

  static toEntityValues(source: { [key: number]: any }, pmfms: PmfmStrategy[]): { [key: string]: any } {
    const target = {};
    pmfms.forEach(pmfm => {
      target[pmfm.pmfmId] = MeasurementUtils.toEntityValue(source[pmfm.pmfmId], pmfm);
    });
    return target;
  }

  static measurementValuesAsObjectMap(source: { [key: number]: any }, minify: boolean): { [key: string]: any } {
    if (!minify) return source;
    return source && Object.getOwnPropertyNames(source)
      .reduce((map, pmfmId) => {
        const value = source[pmfmId] && source[pmfmId].id || source[pmfmId];
        if (isNotNil(value)) map[pmfmId] = '' + value;
        return map;
      }, {}) || undefined;
  }

  static measurementsValuesFromObjectArray(measurements: Measurement[]) {
    return measurements && measurements.reduce((map, m) => {
      const value = m && m.pmfmId && (m.alphanumericalValue || m.numericalValue || (m.qualitativeValue && m.qualitativeValue.id));
      if (value) map[m.pmfmId] = value;
      return map;
    }, {}) || undefined;
  }

  static isEmpty(source: Measurement | any): boolean {
    if (!source) return true;
    return isNil(source.alphanumericalValue)
      && isNil(source.numericalValue)
      && (!source.qualitativeValue || isNil(source.qualitativeValue.id));
  }

  static isNotEmpty(source: Measurement | any): boolean {
    return !MeasurementUtils.isEmpty(source);
  }

}

export class MeasurementValuesUtils {


  static equalsValue(v1: any,
                     v2: any): boolean {
    return (v1 === v2) || (v1 && v2 && isNotNil(v1.id) && v1.id === v2.id);
  }

  static equals(m1: { [pmfmId: number]: any }, m2: { [pmfmId: number]: any }): boolean {
    return (isNil(m1) && isNil(m2))
      || !(Object.getOwnPropertyNames(m1).find(pmfmId => !MeasurementValuesUtils.equalsValue(m1[pmfmId], m2[pmfmId])));
  }

  static equalsPmfms(m1: { [pmfmId: number]: any },
                     m2: { [pmfmId: number]: any },
                     pmfms: PmfmStrategy[]): boolean {
    return (isNil(m1) && isNil(m2))
      || !pmfms.find(pmfm => !MeasurementValuesUtils.equalsValue(m1[pmfm.pmfmId], m2[pmfm.pmfmId]));
  }

  static valueToString = measurementValueToString;

  static toFormValues(source: Measurement[], pmfms: PmfmStrategy[]): any {
    const res: any = {};
    pmfms.forEach(p => {
      const m = source && source.find(m => m.pmfmId === p.pmfmId);
      if (m) {
        res[p.pmfmId] = MeasurementValuesUtils.normalizeFormValue(MeasurementUtils.getMeasurementEntityValue(m, p), p);
      } else {
        res[p.pmfmId] = null;
      }
    });
    return res;
  }

  static toEntityValues(source: { [key: number]: any }, pmfms: PmfmStrategy[], opts?: {
    keepSourceObject?: boolean
  }): { [key: string]: any } {
    const target = opts && opts.keepSourceObject ? source : {};
    pmfms.forEach(pmfm => {
      target[pmfm.pmfmId] = MeasurementUtils.toEntityValue(source[pmfm.pmfmId], pmfm);
    });
    return target;
  }

  // Update measurement values
  static updateMeasurementValues(valuesMap: { [key: number]: any }, measurements: Measurement[], pmfms: PmfmStrategy[]) {
    (measurements || []).forEach(m => {
      const pmfm = pmfms && pmfms.find(pmfm => pmfm.pmfmId === m.pmfmId);
      if (pmfm) MeasurementUtils.setMeasurementValue(valuesMap[pmfm.pmfmId], m, pmfm);
    });
  }

  static normalizeFormValue(value: any, pmfm: PmfmStrategy): any {
    if (!pmfm) return value;
    // If empty, apply the pmfm default value
    if (isNil(value) && pmfm.defaultValue) value = pmfm.defaultValue;
    switch (pmfm.type) {
      case "qualitative_value":
        if (isNotNil(value)) {
          const qvId = (typeof value === "object") ? value.id : parseInt(value);
          return pmfm.qualitativeValues && pmfm.qualitativeValues.find(qv => qv.id === qvId) || null;
        }
        return null;
      case "integer":
        return isNotNilOrNaN(value) ? parseInt(value) : null;
      case "double":
        return isNotNilOrNaN(value) ? parseFloat(value) : null;
      case "string":
        return value || null;
      case "boolean":
        return (value === "true" || value === true || value === 1) ? true : ((value === "false" || value === false || value === 0) ? false : null);
      case "date":
        return fromDateISOString(value) || null;
      default:
        throw new Error("Unknown pmfm.type: " + pmfm.type);
    }
  }

  static normalizeFormValues(source: { [key: number]: any }, pmfms: PmfmStrategy[], opts?: {
    keepSourceObject?: boolean
  }): any {

    const target = opts && opts.keepSourceObject ? source : {};
    (pmfms || []).forEach(pmfm => {
      target[pmfm.pmfmId] = MeasurementValuesUtils.normalizeFormValue(source[pmfm.pmfmId], pmfm);
    });
    return target;
  }

  static normalizeFormEntity(data: IEntityWithMeasurement<any>, pmfms: PmfmStrategy[], form?: FormGroup) {
    if (!data) return; // skip

    // If a form exists, remove extra PMFMS values (before adapt to form)
    if (form) {
      const measFormGroup = form.get('measurementValues');

      if (measFormGroup instanceof FormGroup) {
        // This will remove extra PMFM, according to the form group
        const measurementValues = AppFormUtils.getFormValueFromEntity(data.measurementValues || {}, measFormGroup);
        // This will adapt to form (e.g. transform a QV_ID into a an object)
        data.measurementValues = MeasurementValuesUtils.normalizeFormValues(measurementValues, pmfms);
      } else {
        throw Error("No measurementValues found in form ! Make sure you use the right validator");
      }
    }
    // No validator: just normalize values
    else {
      data.measurementValues = data.measurementValues || {};
      MeasurementValuesUtils.normalizeFormValues(data.measurementValues, pmfms, {
        // Keep extra pmfm values (not need to remove, when no validator used)
        keepSourceObject: true
      });
    }

  }
}


export function measurementValueToString(value: any, pmfm: PmfmStrategy, propertyName?: string): string | undefined {
  if (isNil(value) || !pmfm) return null;
  switch (pmfm.type) {
    case "qualitative_value":
      if (value && typeof value !== "object") {
        const qvId = parseInt(value);
        value = pmfm.qualitativeValues && pmfm.qualitativeValues.find(qv => qv.id === qvId) || null;
      }
      return value && (value[propertyName] || value.name || value.label) || null;
    case "integer":
    case "double":
      return isNotNil(value) ? value : null;
    case "string":
    case "date":
      return value || null;
    case "boolean":
      return (value === "true" || value === true || value === 1) ? 'COMMON.YES' :
        ((value === "false" || value === false || value === 0) ? 'COMMON.NO' : null);
    default:
      throw new Error("Unknown pmfm.type: " + pmfm.type);
  }
}
