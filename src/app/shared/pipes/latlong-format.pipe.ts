import { Pipe, Injectable, PipeTransform } from '@angular/core';
import { DEFAULT_PLACEHOLDER_CHAR } from '../constants';

const DEFAULT_MAX_DECIMALS = 3;

declare class LatLongFormatOptions {
    pattern: 'DDMMSS' | 'DDMM' | 'DD';
    maxDecimals?: number;
    placeholderChar?: string;
}

function formatLatitude(value: number | null, opts?: LatLongFormatOptions): string {
  opts = opts || { pattern: 'DDMM' };
  if (value === undefined || value === null) return "";
  if (opts.pattern === 'DDMMSS') return formatToDDMMSS(value, false, opts.maxDecimals || DEFAULT_MAX_DECIMALS, opts.placeholderChar);
  if (opts.pattern === 'DDMM') return formatToDDMM(value, false, opts.maxDecimals || DEFAULT_MAX_DECIMALS, opts.placeholderChar);

  return formatToDD(value, false, opts.maxDecimals || DEFAULT_MAX_DECIMALS, opts.placeholderChar);
}

function formatLongitude(value: number | null, opts?: LatLongFormatOptions): string {
    opts = opts || { pattern: 'DDMM' };
    if (value === undefined || value === null) return "";

    if (opts.pattern === 'DDMMSS') return formatToDDMMSS(value, true, opts.maxDecimals || DEFAULT_MAX_DECIMALS, opts.placeholderChar);
    if (opts.pattern === 'DDMM') return formatToDDMM(value, true, opts.maxDecimals || DEFAULT_MAX_DECIMALS, opts.placeholderChar);

    return formatToDD(value, true, opts.maxDecimals || DEFAULT_MAX_DECIMALS, opts.placeholderChar);
}

function formatToDD(value: number, isLongitude: boolean, maxDecimals: number, placeholderChar?: string): string {
  // opts.pattern === DD
  const negative = value < 0;
  if (negative) value *= -1;

  // Force spacer
  let prefix = negative ? '-' : '+';
  if (placeholderChar) {
    if (value < 10) {
      prefix += placeholderChar;
    }
    if (value < 100 && isLongitude) {
      prefix += placeholderChar;
    }
  }
  return prefix + roundFloat(value, maxDecimals).toString() + '°';
}

function formatToDDMMSS(value: number, isLongitude: boolean, maxDecimals: number, placeholderChar?: string): string {
  const negative = value < 0;
  if (negative) value *= -1;

  let degrees: number | string = Math.trunc(value);
  let minutes: number | string = Math.trunc((value - degrees) * 60);
  let seconds = roundFloat(((value - degrees) * 60 - minutes) * 60, maxDecimals);
  while (seconds >= 60) {
      seconds -= 60;
      minutes += 1;
  }
  while (minutes >= 60) {
    minutes -= 60;
    degrees += 1;
  }
  const direction = isLongitude ? (negative ? 'W' : 'E') : (negative ? 'S' : 'N');

  // Force spacer
  if (placeholderChar) {
    if (value < 10) {
      degrees = placeholderChar + degrees;
    }
    if (value < 100 && isLongitude) {
      degrees = placeholderChar + degrees;
    }
    if (minutes < 10) {
      minutes = placeholderChar + minutes;
    }
    else {
      minutes = minutes.toString(); // convert to string (need by  the while)
    }
    if (maxDecimals > 0) {
      // Add decimal separator
      if (minutes.length === 2) {
          minutes += '.';
      }
      // Add trailing placeholder chars
      while ((minutes.length < maxDecimals + 3)) {
          minutes += placeholderChar;
      }
    }
  }

  const output = degrees + '° ' + minutes + '\' ' + seconds + '" ' + direction;
  //console.debug("formatToDDMMSS: " + value + " -> " + output);
  return output;
}

function formatToDDMM(value: number, isLongitude: boolean, maxDecimals: number, placeholderChar?: string): string {
  const negative = value < 0;
  if (negative) value *= -1;

  let degrees: number | string = Math.trunc(value);
  let minutes: number | string = roundFloat((value - degrees) * 60, maxDecimals);
  while (minutes >= 60) {
    minutes -= 60;
    degrees += 1;
  }
  const direction = isLongitude ? (negative ? 'W' : 'E') : (negative ? 'S' : 'N');

  // Add placeholderChar
  if (placeholderChar) {
    if (value < 10) {
      degrees = placeholderChar + degrees;
    }
    if (value < 100 && isLongitude) {
      degrees = placeholderChar + degrees;
    }

    if (minutes < 10) {
      minutes = placeholderChar + minutes;
    }
    else {
      // convert to string (need by  the while)
      minutes = minutes.toString();
    }
    if (maxDecimals > 0) {
      // Add decimal separator
      if (minutes.length === 2) {
          minutes += '.';
      }
      // Add trailing placeholder chars
      while ((minutes.length < maxDecimals + 3)) {
          minutes += placeholderChar;
      }
    }
  }

  return degrees + '° ' + minutes + '\' ' + direction;
}

// 36°57'9" N  = 36.9525000
// 10°4'21" W = -10.0725000
function parseLatitudeOrLongitude(input: string, pattern: string, maxDecimals?: number, placeholderChar?: string): number | null {
  // Remove all placeholder (= trim on each parts)
  const inputFix = input.trim().replace(new RegExp("[+" + (placeholderChar || DEFAULT_PLACEHOLDER_CHAR) + ']+', "g"), '');

  const parts = inputFix.split(/[^\d\w-.,]+/);
  let degrees = parseFloat(parts[0].replace(/,/g, '.'));
  if (isNaN(degrees)) {
      console.debug("parseLatitudeOrLongitude " + input + " -> Invalid degrees (NaN). Parts found:", parts);
      return NaN;
  }

  if (pattern === 'DD') return roundFloat(degrees, maxDecimals);

  const minutes = (pattern === 'DDMMSS' || pattern === 'DDMM') && parts[1] && parseFloat(parts[1].replace(/,/g, '.')) || 0;
  const seconds = (pattern === 'DDMMSS') && parts[2] && parseFloat(parts[2].replace(/,/g, '.')) || 0;
  const direction = ((pattern === 'DDMMSS') && parts[3]) || ((pattern === 'DDMM') && parts[2]) || undefined;
  const sign = (direction && (direction === "s" || direction === "S" || direction === "w" || direction === "W")) ? -1 : 1;

  degrees = sign * (degrees + minutes / 60 + seconds / (60 * 60));

  return roundFloat(degrees, maxDecimals);
}

function roundFloat(input: number, maxDecimals: number): number {
    if (maxDecimals > 0) {
        const powDecimal = Math.pow(10, maxDecimals);
        return Math.trunc(input * powDecimal + 0.5) / powDecimal;
    }
    return input;
}

export { DEFAULT_PLACEHOLDER_CHAR, DEFAULT_MAX_DECIMALS, parseLatitudeOrLongitude, formatLatitude, formatLongitude };

@Pipe({
  name: 'latLongFormat'
})
@Injectable({providedIn: 'root'})
/**
 * @deprecated use 'latitudeFormat' or 'longitudeFormat' instead
 */
export class LatLongFormatPipe implements PipeTransform {

    transform(value: number, args?: any): string | Promise<string> {
        args = args || {};
        return ((!args.type || args.type !== 'latitude') ? formatLongitude : formatLatitude)
            (value, { pattern: args.pattern, maxDecimals: args.maxDecimals, placeholderChar: args.placeholderChar });
    }


}
@Pipe({
  name: 'latitudeFormat'
})
@Injectable({providedIn: 'root'})
export class LatitudeFormatPipe implements PipeTransform {
  transform(value: number, args?: any): string | Promise<string> {
    args = args || {};
    return formatLatitude(value, { pattern: args.pattern, maxDecimals: args.maxDecimals, placeholderChar: args.placeholderChar });
  }
}

@Pipe({
  name: 'longitudeFormat'
})
@Injectable({providedIn: 'root'})
export class LongitudeFormatPipe implements PipeTransform {
  transform(value: number, args?: any): string | Promise<string> {
    args = args || {};
    return formatLongitude(value, { pattern: args.pattern, maxDecimals: args.maxDecimals, placeholderChar: args.placeholderChar });
  }
}
