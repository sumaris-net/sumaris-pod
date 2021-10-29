import { IPosition } from '@app/trip/services/model/position.model';
import { isNil } from '@sumaris-net/ngx-components';
import { GeolocationOptions } from '@ionic-native/geolocation';
import { Geolocation } from '@ionic-native/geolocation/ngx';


export abstract class PositionUtils {

  static isNilOrInvalid(position: IPosition, opts?: {debug?: boolean}) {
    // Incomplete value: cannot compute
    if (!position || isNil(position.latitude) || isNil(position.longitude)) return true;

    // Invalid lat/lon
    if (position.latitude < -90 || position.latitude > 90
      || position.longitude < -180 || position.longitude > 180) {

      // /!\ Log in console, because should never occur
      if (opts && opts.debug) console.warn('Invalid lat/long position:  ', position);
      return true;
    }

    return false;
  }

  static computeDistanceInMiles(position1: IPosition, position2: IPosition): number {
    // Invalid position(s): skip
    if (PositionUtils.isNilOrInvalid(position1, {debug: true})
      || PositionUtils.isNilOrInvalid(position2, {debug: true})) return;

    const latitude1Rad = position1.latitude * Math.PI / 180;
    const longitude1Rad = position1.longitude * Math.PI / 180;
    const latitude2Rad = position2.latitude * Math.PI / 180;
    const longitude2Rad = position2.longitude * Math.PI / 180;

    let distance = 2 * 6371 * Math.asin(
      Math.sqrt(
        Math.pow(Math.sin((latitude1Rad - latitude2Rad) / 2), 2)
        + Math.cos(latitude1Rad) * Math.cos(latitude2Rad) * Math.pow(Math.sin((longitude2Rad - longitude1Rad) / 2), 2)
      ));
    distance = Math.round((((distance / 1.852) + Number.EPSILON) * 100)) / 100;
    return distance;
  }

  /**
   * Get the position by geo loc sensor
   */
  static async getCurrentPosition(geolocation?: Geolocation, options?: GeolocationOptions): Promise<{ latitude: number; longitude: number }> {
    options = {
      maximumAge: 30000/*30s*/,
      timeout: 10000/*10s*/,
      enableHighAccuracy: true,
      ...options
    };

    // Use ionic-native plugin
    if (geolocation) {
      console.info('[position-utils] Get current geo position, using cordova plugin...')
      try {
        const res = await geolocation.getCurrentPosition(options);
        return {
          latitude: res.coords.latitude,
          longitude: res.coords.longitude
        };
      } catch (err) {
        console.error('[position-utils] Cannot get current geo position, using cordova plugin:', err);
        throw err;
      }
    }

    // Or fallback to navigator
    console.info('[position-utils] Get current geo position, using browser...')
    return new Promise<{ latitude: number; longitude: number }>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition((res) => {
          resolve({
            latitude: res.coords.latitude,
            longitude: res.coords.longitude
          });
        },
        (err) => {
          console.error('[position-utils] Cannot get current geo position, using browser:', err);
          reject(err);
        },
        options
      );
    });
  }
}