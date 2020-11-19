import {NgModule} from '@angular/core';
import {CoreModule} from '../core/core.module';
import {VesselForm} from "./vessel/form/form-vessel";
import {VesselPage} from "./vessel/page/vessel.page";
import {VesselsTable} from "./vessel/list/vessels.table";
import {VesselModal} from "./vessel/modal/modal-vessel";
import {ReferentialsPage} from './list/referentials';

import {ReferentialForm} from "./form/referential.form";
import {ProgramPage} from "./program/program.page";
import {StrategiesTable} from "./strategy/strategies.table";
import {SoftwarePage} from "./software/software.page";
import {VesselFeaturesHistoryComponent} from "./vessel/page/vessel-features-history.component";
import {VesselRegistrationHistoryComponent} from "./vessel/page/vessel-registration-history.component";
import {VesselsPage} from "./vessel/list/vessels.page";
import {PmfmPage} from "./pmfm/pmfm.page";
import {ParameterPage} from "./pmfm/parameter.page";
import {ReferentialTable} from "./list/referential.table";
import {PmfmStrategiesTable} from "./strategy/pmfm-strategies.table";
import {SelectReferentialModal} from "./list/select-referential.modal";
import {ReferentialRefTable} from "./list/referential-ref.table";
import {StrategyForm} from "./strategy/strategy.form";
import {PmfmQvFormField} from "./pmfm/pmfm-qv.form-field.component";
import {PmfmFormField} from "./pmfm/pmfm.form-field.component";
import {ReferentialToStringPipe} from "./services/pipes/referential-to-string.pipe";
import {TranslateModule} from "@ngx-translate/core";
import {IsComputedPmfmPipe, IsDatePmfmPipe, PmfmNamePipe, PmfmValueToStringPipe} from "./pipes/pmfms.pipe";


@NgModule({
  imports: [
    CoreModule,
    TranslateModule.forChild()
  ],
  declarations: [
    // Pipes
    ReferentialToStringPipe,
    PmfmNamePipe,
    PmfmValueToStringPipe,
    IsDatePmfmPipe,
    IsComputedPmfmPipe,

    // Components
    ReferentialsPage,
    ReferentialForm,
    VesselsTable,
    VesselPage,
    VesselsPage,
    VesselForm,
    VesselModal,
    ProgramPage,
    StrategyForm,
    StrategiesTable,
    PmfmStrategiesTable,
    SoftwarePage,
    VesselFeaturesHistoryComponent,
    VesselRegistrationHistoryComponent,
    ParameterPage,
    PmfmPage,
    ReferentialTable,
    ReferentialRefTable,
    SelectReferentialModal,
    PmfmFormField,
    PmfmQvFormField
  ],
  exports: [
    TranslateModule,

    // Pipes
    ReferentialToStringPipe,
    PmfmNamePipe,
    PmfmValueToStringPipe,
    IsDatePmfmPipe,
    IsComputedPmfmPipe,

    // Components
    ReferentialsPage,
    ReferentialForm,
    VesselsTable,
    VesselPage,
    VesselForm,
    ProgramPage,
    StrategyForm,
    SoftwarePage,
    VesselsPage,
    ParameterPage,
    PmfmPage,
    ReferentialRefTable,
    SelectReferentialModal,
    PmfmFormField,
    PmfmQvFormField
  ]
})
export class ReferentialModule {
}
