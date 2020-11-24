import { Injectable } from "@angular/core";
import { HammerGestureConfig } from "@angular/platform-browser";
import * as Hammer from 'hammerjs';

/**
 * @hidden
 * This class overrides the default Angular gesture config.
 *
 */
@Injectable()
export class AppGestureConfig extends HammerGestureConfig {

  // Override HammerJS default settings
  overrides = <any>{
    press: {
      time: 500 // Increase need for double tap (default: 251 ms)
    },
    pinch: {
      enable: false
    },
    rotate: {
      enable: false
    },
    // see https://hammerjs.github.io/recognizer-swipe/
    swipe: {
      //direction: Hammer.DIRECTION_ALL,
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
