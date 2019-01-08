import * as moment from "moment";
import {Moment} from "moment";

export const DATE_ISO_PATTERN = 'YYYY-MM-DDTHH:mm:ss.SSSZ';

export function isNil<T>(obj: T | null | undefined): boolean {
  return obj === undefined || obj === null;
}
export function isNotNil<T>(obj: T | null | undefined): boolean {
  return obj !== undefined && obj !== null;
}
export function nullIfUndefined<T>(obj: T | null | undefined): T | null {
  return obj === undefined ? null : obj;
}

export const toDateISOString = function (value): string | undefined {
  if (!value) return undefined;
  if (typeof value == "string") {
    if (value.indexOf('+')) {
      value = fromDateISOString(value);
    }
    else {
      return value;
    }
  }
  if (typeof value == "object" && value.toISOString) {
    return value.toISOString();
  }
  return moment(value).format(DATE_ISO_PATTERN) || undefined;
}

export const fromDateISOString = function (value): Moment | undefined {
  return value && moment(value, DATE_ISO_PATTERN) || undefined;

}
