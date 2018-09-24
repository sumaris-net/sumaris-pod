import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { VesselService } from './services/vessel-service';
import { VesselValidatorService } from './vessel/validator/validators';
import { ReferentialService } from './services/referential-service';
import { ReferentialValidatorService } from './validator/validators';
import { VesselForm } from "./vessel/form/form-vessel";
import { VesselPage } from "./vessel/page/page-vessel";
import { VesselsPage } from "./vessel/list/vessels";
import { VesselModal } from "./vessel/modal/modal-vessel";
import { ReferentialsPage } from './list/referentials';
import { vesselFeaturesToString, referentialToString } from './services/model';

export { VesselModal, VesselService, ReferentialService, vesselFeaturesToString, referentialToString }

@NgModule({
    imports: [
        CommonModule,
        CoreModule
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
}
