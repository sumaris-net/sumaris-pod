import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {UserEventService} from "./services/user-event.service";
import {UserEventsComponent} from "./list/user-events.component";
import {CoreModule} from "../core/core.module";

@NgModule({
  imports: [
    CommonModule,
    CoreModule
  ],
  declarations: [
    UserEventsComponent
  ],
  exports: [
    UserEventsComponent
  ],
  providers: [
    UserEventService
  ]
})
export class SocialModule {

}
