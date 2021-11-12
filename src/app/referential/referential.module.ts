import {NgModule} from '@angular/core';
import {CoreModule} from "@sumaris-net/ngx-components";

import {ReferentialForm} from "./form/referential.form";
import {ProgramPage} from "./program/program.page";
import {StrategiesTable} from "./strategy/strategies.table";
import {SoftwarePage} from "./software/software.page";
import {PmfmPage} from "./pmfm/pmfm.page";
import {ParameterPage} from "./pmfm/parameter.page";
import {PmfmStrategiesTable} from "./strategy/pmfm-strategies.table";
import {SelectReferentialModal} from "./list/select-referential.modal";
import {ReferentialRefTable} from "./list/referential-ref.table";
import {StrategyForm} from "./strategy/strategy.form";
import {PmfmQvFormField} from "./pmfm/pmfm-qv.form-field.component";
import {PmfmFormField} from "./pmfm/pmfm.form-field.component";
import {ReferentialToStringPipe} from "./services/pipes/referential-to-string.pipe";
import {TranslateModule} from "@ngx-translate/core";
import { IsComputedPmfmPipe, IsDatePmfmPipe, IsMultiplePmfmPipe, PmfmNamePipe, PmfmValuePipe } from './pipes/pmfms.pipe';
import {StrategyPage} from "./strategy/strategy.page";

import {TextMaskModule} from "angular2-text-mask";
import {CommonModule} from "@angular/common";
import {ProgramsPage} from "./program/programs.page";
import {SamplingStrategyForm} from "./strategy/sampling/sampling-strategy.form";
import {SamplingStrategyPage} from "./strategy/sampling/sampling-strategy.page";
import {SamplingStrategiesTable} from "./strategy/sampling/sampling-strategies.table";
import {SimpleReferentialTable} from "./list/referential-simple.table";
import {PmfmsTable} from "./pmfm/pmfms.table";
import {SelectPmfmModal} from "./pmfm/select-pmfm.modal";
import {TaxonNamePage} from "./taxon/taxon-name.page";
import {ReferentialsPage} from '@app/referential/list/referentials.page';
import {AppCoreModule} from '@app/core/core.module';
import {StrategiesPage} from './strategy/strategies.page';
import {StrategyModal} from '@app/referential/strategy/strategy.modal';
import { PmfmFormArray } from '@app/referential/pmfm/pmfm.form-array.component';

@NgModule({
  imports: [
    CommonModule,
    TextMaskModule,
    TranslateModule.forChild(),

    AppCoreModule
  ],
  declarations: [
    // Pipes
    ReferentialToStringPipe,
    PmfmNamePipe,
    PmfmValuePipe,
    IsDatePmfmPipe,
    IsComputedPmfmPipe,
    IsMultiplePmfmPipe,

    // Components
    ReferentialsPage,
    ReferentialForm,
    SamplingStrategyForm,
    ProgramsPage,
    ProgramPage,
    SamplingStrategyPage,
    StrategiesPage,
    StrategyPage,
    StrategyForm,
    StrategiesTable,
    SamplingStrategiesTable,
    PmfmStrategiesTable,
    SoftwarePage,
    ParameterPage,
    PmfmPage,
    SimpleReferentialTable,
    ReferentialRefTable,
    SelectReferentialModal,
    PmfmFormField,
    PmfmFormArray,
    PmfmQvFormField,
    PmfmsTable,
    SelectPmfmModal,
    TaxonNamePage,
    StrategyModal
  ],
  exports: [
    TranslateModule,

    // Pipes
    ReferentialToStringPipe,
    PmfmNamePipe,
    PmfmValuePipe,
    IsDatePmfmPipe,
    IsComputedPmfmPipe,

    // Components
    ReferentialsPage,
    ReferentialForm,
    SamplingStrategyForm,
    SamplingStrategyPage,
    StrategiesPage,
    StrategyPage,
    StrategyForm,
    SoftwarePage,
    ProgramsPage,
    ProgramPage,
    ParameterPage,
    PmfmPage,
    ReferentialRefTable,
    SelectReferentialModal,
    PmfmFormField,
    PmfmQvFormField,
    PmfmsTable,
    SelectPmfmModal,
    PmfmStrategiesTable,
    TaxonNamePage,
    StrategyModal,
    PmfmFormArray,
    IsMultiplePmfmPipe,
  ],
})
export class AppReferentialModule {
}
