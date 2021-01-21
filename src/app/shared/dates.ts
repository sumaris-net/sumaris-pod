import * as moment from "moment";
import {Duration, isMoment, Moment} from "moment";


export const DATE_ISO_PATTERN = 'YYYY-MM-DDTHH:mm:ss.SSSZ';
export const DATE_UNIX_TIMESTAMP = 'X';
export const DATE_UNIX_MS_TIMESTAMP = 'x';

export function toDateISOString(value: any): string | undefined {
  if (!value) return undefined;

  // Already a valid ISO date time string (without timezone): use it
  if (typeof value === "string"
    && value.indexOf('+') === -1
    && value.lastIndexOf('Z') === value.length - 1) {

    return value;
  }
  // Make sure to have a Moment object
  value = fromDateISOString(value);
  return value && value.toISOString() || undefined;
}

export function fromDateISOString(value: any): Moment | undefined {
  // Already a moment object: use it
  if (!value || isMoment(value)) return value;

  // Parse the input value, as a ISO date time
  const date: Moment = moment(value, DATE_ISO_PATTERN);
  if (date.isValid()) return date;


  // Not valid: trying to convert from unix timestamp
  if (typeof value === 'string') {
    console.warn('Wrong date format - Trying to convert from local time: ' + value);
    if (value.length === 10) {
      return moment(value, DATE_UNIX_TIMESTAMP);
    }
    else if (value.length === 13) {
      return moment(value, DATE_UNIX_MS_TIMESTAMP);
    }
  }

  console.warn('Unable to parse date: ' + value);
  return undefined;
}

export function toDuration(value: number, unit?: moment.unitOfTime.DurationConstructor): Duration {
  if (!value) return undefined;

  const duration = moment.duration(value, unit);

  // fix 990+ ms
  if (duration.milliseconds() >= 990) {
    duration.add(1000 - duration.milliseconds(), "ms");
  }
  // fix 59 s
  if (duration.seconds() >= 59) {
    duration.add(60 - duration.seconds(), "s");
  }

  return duration;
}
