import {Moment} from "moment";
import {ReferentialRef, ReferentialUtils} from "../../../core/services/model/referential.model";
import {
  fromDateISOString,
  isNil, isNilOrBlank,
  isNotNil,
  isNotNilOrNaN,
  joinPropertiesPath,
  toDateISOString
} from "../../../shared/functions";
import {Pmfm} from "./pmfm.model";
import {PmfmStrategy} from "./pmfm-strategy.model";

export declare type PmfmValue = number | string | boolean | Moment | ReferentialRef<any>;

export abstract class PmfmValueUtils {

  static toModelValue(value: PmfmValue | any, pmfm: PmfmStrategy | Pmfm): string {
    if (isNil(value) || !pmfm) return undefined;
    switch (pmfm.type) {
      case "qualitative_value":
        return value && isNotNil(value.id) && value.id.toString() || undefined;
      case "integer":
      case "double":
        return isNotNil(value) && !isNaN(+value) && value.toString() || undefined;
      case "string":
        return value;
      case "boolean":
        return (value === true || value === "true") ? "true" : ((value === false || value === "false") ? "false" : undefined);
      case "date":
        return toDateISOString(value);
      default:
        throw new Error("Unknown pmfm's type: " + pmfm.type);
    }
  }

  static fromModelValue(value: any, pmfm: PmfmStrategy | Pmfm): PmfmValue {
    if (!pmfm) return value;
    // If empty, apply the pmfm default value
    if (isNil(value) && isNotNil(pmfm.defaultValue)) value = pmfm.defaultValue;
    switch (pmfm.type) {
      case "qualitative_value":
        if (isNotNil(value)) {
          const qvId = (typeof value === "object") ? value.id : parseInt(value);
          return (pmfm.qualitativeValues || (pmfm instanceof Pmfm && pmfm.parameter && pmfm.parameter.qualitativeValues) || [])
            .find(qv => qv.id === qvId) || null;
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
        throw new Error("Unknown pmfm's type: " + pmfm.type);
    }
  }

  static valueToString(value: any, pmfm: PmfmStrategy | Pmfm, propertyNames?: string[]): string | undefined {
    if (isNil(value) || !pmfm) return null;
    switch (pmfm.type) {
      case "qualitative_value":
        if (value && typeof value !== "object") {
          const qvId = parseInt(value);
          value = (pmfm.qualitativeValues || (pmfm instanceof Pmfm && pmfm.parameter && pmfm.parameter.qualitativeValues) || [])
            .find(qv => qv.id === qvId) || null;
        }
        return value && ((propertyNames && joinPropertiesPath(value, propertyNames)) || value.name || value.label) || null;
      case "integer":
      case "double":
        return isNotNil(value) ? value : null;
      case "string":
        return value || null;
      case "date":
        return value || null;
      case "boolean":
        return (value === "true" || value === true || value === 1) ? '&#x2714;' /*checkmark*/ :
          ((value === "false" || value === false || value === 0) ? '' : null); /*empty*/
      default:
        throw new Error("Unknown pmfm's type: " + pmfm.type);
    }
  }

  static isEmpty(value: PmfmValue | any) {
    return isNilOrBlank(value) || ReferentialUtils.isEmpty(value);
  }
}
