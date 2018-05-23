
import {NgModule} from "@angular/core";

import {VesselPage} from "./vessel";
import {VesselForm} from "./form/form-vessel";
import {VesselModal} from "./modal/modal-vessel";
import {VesselsPage} from "./list/vessels";
import {VesselValidatorService} from "./validator/validators";


@NgModule({
  declarations: [
    // Data
    VesselForm,
    VesselPage,
    VesselsPage,
    VesselModal
  ],
  entryComponents: [
    VesselModal
  ],
  imports: [
  ],  
  exports: [
  ],
  providers: [
    VesselValidatorService
  ]
})
export class VesselModule {

}
