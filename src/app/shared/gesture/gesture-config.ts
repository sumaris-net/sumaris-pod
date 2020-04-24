import { Injectable } from "@angular/core";
import { HammerGestureConfig } from "@angular/platform-browser";

/**
 * @hidden
 * This class overrides the default Angular gesture config.
 *
 */
@Injectable()
export class AppGestureConfig extends HammerGestureConfig {

  // override default settings
  overrides = <any>{

      // see https://hammerjs.github.io/recognizer-swipe/
     'swipe': {
       threshold: 30,  // Minimal distance required before recognizing.
       velocity: 0.4  //Minimal velocity required before recognizing, unit is in px per ms.
     }
   };

  buildHammer(element: HTMLElement) {
    const mc = new (<any>window).Hammer(element);

    for (const eventName in this.overrides) {
      if (eventName) {
        mc.get(eventName).set(this.overrides[eventName]);
      }
    }

    return mc;
  }
}
