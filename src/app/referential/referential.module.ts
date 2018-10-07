import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { VesselService } from './services/vessel-service';
import { VesselValidatorService } from './services/vessel.validator';
import { ReferentialRefService } from './services/referential-ref.service';
import { ReferentialService } from './services/referential.service';
import { ReferentialValidatorService } from './services/referential.validator';
import { ProgramService } from './services/program.service';
import { VesselForm } from "./vessel/form/form-vessel";
import { VesselPage } from "./vessel/page/page-vessel";
import { VesselsPage } from "./vessel/list/vessels";
import { VesselModal } from "./vessel/modal/modal-vessel";
import { ReferentialsPage } from './list/referentials';
import { vesselFeaturesToString, referentialToString } from './services/model';

export { VesselModal, VesselService, ReferentialService, ProgramService, ReferentialRefService, vesselFeaturesToString, referentialToString }

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
        ReferentialRefService,
        ReferentialService,
        ReferentialValidatorService,
        ProgramService,
        VesselService,
        VesselValidatorService
    ]
})
export class ReferentialModule {
}
