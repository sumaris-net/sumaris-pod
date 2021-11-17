import {Moment} from "moment";
import {ReferentialRef, ReferentialUtils}  from "@sumaris-net/ngx-components";
import {isNil, isNilOrBlank, isNotNil, isNotNilOrNaN, joinPropertiesPath} from "@sumaris-net/ngx-components";
import {IPmfm, Pmfm, PmfmUtils} from './pmfm.model';
import {DenormalizedPmfmStrategy} from "./pmfm-strategy.model";
import {fromDateISOString, toDateISOString} from "@sumaris-net/ngx-components";
import { isNilOrNaN } from '@app/shared/functions';

export declare type PmfmValue = number | string | boolean | Moment | ReferentialRef<any>;
export declare type PmfmDefinition = DenormalizedPmfmStrategy | Pmfm;

export abstract class PmfmValueUtils {

  static toModelValue(value: PmfmValue | any, pmfm: IPmfm): string {
    if (isNil(value) || !pmfm) return undefined;
    switch (pmfm.type) {
      case "qualitative_value":
        return value && isNotNil(value.id) && value.id.toString() || undefined;
      case "integer":
      case "double":
        if (isNil(value) && !isNaN(+value)) return undefined;
        // Apply conversion
        if (isNotNilOrNaN(pmfm.displayConversion?.conversionCoefficient)) {
          value = (+value) / pmfm.displayConversion.conversionCoefficient;
        }
        return value.toString();
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

  static fromModelValue(value: any, pmfm: IPmfm): PmfmValue {
    if (!pmfm) return value;
    // If empty, apply the pmfm default value
    if (isNil(value) && isNotNil(pmfm.defaultValue)) value = pmfm.defaultValue;
    switch (pmfm.type) {
      case "qualitative_value":
        if (isNotNil(value)) {
          const qvId = (typeof value === "object") ? value.id : parseInt(value);
          return (pmfm.qualitativeValues || (PmfmUtils.isFullPmfm(pmfm) && pmfm.parameter && pmfm.parameter.qualitativeValues) || [])
            .find(qv => qv.id === qvId) || null;
        }
        return null;
      case "integer":
        if (isNilOrNaN(value)) return null;
        value = parseInt(value);
        // Apply conversion excepted for displaying the value
        if (pmfm.displayConversion) {
          value = value * pmfm.displayConversion.conversionCoefficient;
        }
        return value;
      case "double":
        if (isNilOrNaN(value)) return null;
        value = parseFloat(value);
        // Apply conversion excepted for displaying the value
        if (pmfm.displayConversion) {
          value = value * pmfm.displayConversion.conversionCoefficient;
        }
        return value;
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

  static valueToString(value: any, opts: { pmfm: IPmfm, propertyNames?: string[]; html?: boolean; } ): string | undefined {
    if (isNil(value) || !opts || !opts.pmfm) return null;
    switch (opts.pmfm.type) {
      case "qualitative_value":
        if (value && typeof value !== "object") {
          const qvId = parseInt(value);
          value = opts.pmfm && (opts.pmfm.qualitativeValues || (PmfmUtils.isFullPmfm(opts.pmfm) && opts.pmfm.parameter && opts.pmfm.parameter.qualitativeValues) || [])
            .find(qv => qv.id === qvId) || null;
        }
        return value && ((opts.propertyNames && joinPropertiesPath(value, opts.propertyNames)) || value.name || value.label) || null;
      case "integer":
      case "double":
        return isNotNil(value) ? value : null;
      case "string":
        return value || null;
      case "date":
        return value || null;
      case "boolean":
        return (value === "true" || value === true || value === 1) ? '&#x2714;' /*checkmark*/ :
          ((value === "false" || value === false || value === 0) ? '&#x2718;' : null); /*empty*/
      default:
        throw new Error("Unknown pmfm's type: " + opts.pmfm.type);
    }
  }

  static isEmpty(value: PmfmValue | any) {
    return isNilOrBlank(value) || ReferentialUtils.isEmpty(value);
  }
}
