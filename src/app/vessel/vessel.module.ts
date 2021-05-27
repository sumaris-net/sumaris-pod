import {NgModule} from '@angular/core';
import {CoreModule} from '../core/core.module';
import {VesselForm} from "./form/form-vessel";
import {VesselPage} from "./page/vessel.page";
import {VesselsTable} from "./list/vessels.table";
import {VesselModal} from "./modal/vessel-modal";
import {VesselsPage} from "./list/vessels.page";
import {TranslateModule} from "@ngx-translate/core";

import {TextMaskModule} from "angular2-text-mask";
import {CommonModule} from "@angular/common";
import {DataModule} from "../data/data.module";
import {VesselFeaturesHistoryComponent} from "./page/vessel-features-history.component";
import {VesselRegistrationHistoryComponent} from "./page/vessel-registration-history.component";
import {ReferentialModule} from "../referential/referential.module";

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    ReferentialModule,
    DataModule,
    TextMaskModule,
    TranslateModule.forChild()
  ],
  declarations: [

    // Components
    VesselsTable,
    VesselPage,
    VesselsPage,
    VesselForm,
    VesselModal,
    VesselFeaturesHistoryComponent,
    VesselRegistrationHistoryComponent
  ],
  exports: [
    TranslateModule,

    // Components
    VesselsTable,
    VesselPage,
    VesselsPage,
    VesselForm,
    VesselsPage
  ]
})
export class VesselModule {
}
