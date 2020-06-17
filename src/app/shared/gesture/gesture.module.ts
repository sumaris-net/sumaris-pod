import {NgModule} from "@angular/core";
import {BrowserModule, HAMMER_GESTURE_CONFIG, HammerModule} from "@angular/platform-browser";
import {CommonModule} from "@angular/common";
import {AppGestureConfig} from "./gesture-config";
import {HammerSwipeAction, HammerSwipeEvent} from "./hammer.utils";

export {HammerSwipeAction, HammerSwipeEvent};


@NgModule({
  imports: [
    CommonModule,
    HammerModule
  ],
  exports: [
    HammerModule
  ],
  providers: [
    // Configure hammer gesture
    {provide: HAMMER_GESTURE_CONFIG, useClass: AppGestureConfig}
  ]
})
export class SharedGestureModule {
}
