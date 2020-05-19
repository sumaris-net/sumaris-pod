import { Pipe, Injectable, PipeTransform } from '@angular/core';
import {formatLatitude, formatLongitude} from "../material/latlong/latlong.utils";


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
    return formatLongitude(value, { ...args });
  }
}
