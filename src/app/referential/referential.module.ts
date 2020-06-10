import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule} from '../core/core.module';
import {VesselService} from './services/vessel-service';
import {VesselValidatorService} from './services/vessel.validator';
import {ReferentialRefService} from './services/referential-ref.service';
import {ReferentialService} from './services/referential.service';
import {ReferentialValidatorService} from './services/referential.validator';
import {ProgramService} from './services/program.service';
import {VesselForm} from "./vessel/form/form-vessel";
import {VesselPage} from "./vessel/page/vessel.page";
import {VesselsTable} from "./vessel/list/vessels.table";
import {VesselModal} from "./vessel/modal/modal-vessel";
import {ReferentialsPage} from './list/referentials';
import {
  AcquisitionLevelCodes,
  Department,
  entityToString,
  EntityUtils,
  GearLevelIds,
  getPmfmName,
  LocationLevelIds,
  Person,
  PmfmIds,
  PmfmLabelPatterns,
  PmfmStrategy,
  PmfmUtils,
  QualitativeLabels,
  QualityFlagIds,
  qualityFlagToColor,
  Referential,
  ReferentialRef,
  referentialToString,
  StatusIds,
  TaxonGroupIds,
  TaxonomicLevelIds,
  VesselSnapshot,
  vesselSnapshotToString
} from './services/model';

import {ReferentialFragments} from './services/referential.queries';
import {ReferentialForm} from "./form/referential.form";
import {ProgramPage} from "./program/program.page";
import {ProgramValidatorService} from "./services/validator/program.validator";
import {StrategyValidatorService} from "./services/validator/strategy.validator";
import {StrategiesTable} from "./strategy/strategies.table";
import {SoftwarePage} from "./software/software.page";
import {VesselFeaturesHistoryComponent} from "./vessel/page/vessel-features-history.component";
import {VesselRegistrationHistoryComponent} from "./vessel/page/vessel-registration-history.component";
import {VesselFeaturesValidatorService} from "./services/vessel-features.validator";
import {VesselRegistrationValidatorService} from "./services/vessel-registration.validator";
import {SoftwareValidatorService} from "./services/software.validator";
import {SoftwareService} from "./services/software.service";
import {VesselsPage} from "./vessel/list/vessels.page";
import {PmfmService} from "./services/pmfm.service";
import {ParameterService} from "./services/parameter.service";
import {PmfmValidatorService} from "./services/validator/pmfm.validator";
import {PmfmPage} from "./pmfm/pmfm.page";
import {ParameterPage} from "./pmfm/parameter.page";
import {ParameterValidatorService} from "./services/validator/parameter.validator";
import {ReferentialTable} from "./list/referential.table";
import {ReferentialRoutingModule} from "./referential-routing.module";
import {PmfmStrategiesTable} from "./strategy/pmfm-strategies.table";
import {PmfmStrategyValidatorService} from "./services/validator/pmfm-strategy.validator";
import {SelectReferentialModal} from "./list/select-referential.modal";
import {ReferentialRefTable} from "./list/referential-ref.table";
import {StrategyForm} from "./strategy/strategy.form";

export {
  VesselModal, VesselService, ReferentialService, ProgramService, ReferentialRefService,
  Referential, ReferentialRef, EntityUtils, Department, Person,
  VesselSnapshot, PmfmStrategy, PmfmUtils, QualityFlagIds,
  GearLevelIds, TaxonGroupIds, LocationLevelIds, AcquisitionLevelCodes, StatusIds, PmfmIds, QualitativeLabels, TaxonomicLevelIds,
  ReferentialFragments, PmfmLabelPatterns,
  entityToString, referentialToString, qualityFlagToColor,
  vesselSnapshotToString, getPmfmName
};

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    ReferentialRoutingModule
  ],
  declarations: [
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
    SelectReferentialModal
  ],
  exports: [
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
    SelectReferentialModal
  ],
  entryComponents: [
    VesselModal,
    ReferentialRefTable,
    SelectReferentialModal
  ],
  providers: [
    ReferentialRefService,
    ReferentialService,
    ReferentialValidatorService,
    ProgramService,
    VesselService,
    VesselValidatorService,
    VesselFeaturesValidatorService,
    VesselRegistrationValidatorService,
    ProgramValidatorService,
    StrategyValidatorService,
    PmfmStrategyValidatorService,
    SoftwareService,
    SoftwareValidatorService,
    PmfmService,
    PmfmValidatorService,
    ParameterService,
    ParameterValidatorService
  ]
})
export class ReferentialModule {
}
