import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {UserEventService} from "./services/user-event.service";
import {UserEventsTable} from "./list/user-events.table";
import {CoreModule} from "../core/core.module";

@NgModule({
  imports: [
    CommonModule,
    CoreModule
  ],
  declarations: [
    UserEventsTable
  ],
  exports: [
    UserEventsTable
  ],
  providers: [
    UserEventService
  ]
})
export class SocialModule {

}
