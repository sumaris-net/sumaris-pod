import { Injectable } from "@angular/core";
import { HammerGestureConfig } from "@angular/platform-browser";

/**
 * @hidden
 * This class overrides the default Angular gesture config.
 *
 * @see https://medium.com/madewithply/ionic-4-long-press-gestures-96cf1e44098b
 */
@Injectable()
export class AppGestureConfig extends HammerGestureConfig {
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
