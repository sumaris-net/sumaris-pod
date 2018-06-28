import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { ReferentialRoutingModule } from './referential-routing.module';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { VesselService } from './services/vessel-service';
import { VesselValidatorService } from './vessel/validator/validators';
import { ReferentialService } from './services/referential-service';
import { ReferentialValidatorService } from './validator/validators';
import { VesselForm } from "./vessel/form/form-vessel";
import { VesselPage } from "./vessel/page/page-vessel";
import { VesselsPage } from "./vessel/list/vessels";
import { VesselModal } from "./vessel/modal/modal-vessel";
import { ReferentialsPage } from './list/referentials';
import { DateFormatPipe } from '../shared/pipes/date-format.pipe';

export { VesselModal, VesselService, ReferentialService }

@NgModule({
    imports: [
        CommonModule,
        CoreModule
        //ReferentialRoutingModule
    ],

    declarations: [
        VesselPage,
        VesselsPage,
        ReferentialsPage,
        VesselForm,
        VesselModal
    ],
    exports: [
        VesselPage,
        VesselsPage,
        ReferentialsPage,
        VesselForm
    ],
    entryComponents: [
        VesselModal
    ],
    providers: [
        ReferentialService,
        ReferentialValidatorService,
        VesselService,
        VesselValidatorService
    ]
})
export class ReferentialModule {
    constructor() {
        console.info("[referential] Starting module...");
    }
}