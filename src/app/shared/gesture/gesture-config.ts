import {Injectable} from "@angular/core";
import {HammerGestureConfig} from "@angular/platform-browser";

export const HAMMER_TAP_TIME = 500;
/**
 * @hidden
 * This class overrides the default Angular gesture config.
 *
 */
@Injectable({providedIn: 'root'})
export class AppGestureConfig extends HammerGestureConfig {

  // Override HammerJS default settings
  overrides = <any>{
    press: {
      time: HAMMER_TAP_TIME // Increase need for double tap (default: 251 ms)
    },
    pinch: {
      enable: false
    },
    rotate: {
      enable: false
    },
    // see https://hammerjs.github.io/recognizer-swipe/
    swipe: {
      threshold: 30,  // Minimal distance required before recognizing.
      velocity: 0.4  //Minimal velocity required before recognizing, unit is in px per ms.
    }
  };

  buildHammer(element: HTMLElement) {
    console.debug("[gesture] Override HammerJS default config: ", this.overrides);
    const mc = new (<any>window).Hammer(element);

    for (const eventName in this.overrides) {
      if (eventName) {
        mc.get(eventName).set(this.overrides[eventName]);
      }
    }

    return mc;
  }
}
