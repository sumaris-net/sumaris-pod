import {NgModule} from "@angular/core";
import {SharedModule} from "../shared.module";
import {SharedTestRoutingModule} from "./shared-test-routing.module";
import {FormTestPage} from "./form/form.test";


@NgModule({
  imports: [
    SharedModule,
    SharedTestRoutingModule
  ],
  declarations: [
    FormTestPage
  ],
  exports: [
    FormTestPage
  ]
})
export class SharedTestModule {
}
