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
import {VesselPage} from "./vessel/page/page-vessel";
import {VesselsPage} from "./vessel/list/vessels";
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
  VesselFeatures,
  vesselFeaturesToString
} from './services/model';

import {ReferentialFragments} from './services/referential.queries';
import {ReferentialForm} from "./form/referential.form";
import {ProgramPage} from "./program/program.page";
import {ProgramValidatorService} from "./services/validator/program.validator";
import {StrategyValidatorService} from "./services/validator/strategy.validator";
import {StrategiesTable} from "./program/strategies.table";
import {SoftwarePage} from "./software/software.page";
import {VesselFeaturesHistoryComponent} from "./vessel/page/vessel-features-history.component";
import {VesselRegistrationHistoryComponent} from "./vessel/page/vessel-registration-history.component";

export {
  VesselModal, VesselService, ReferentialService, ProgramService, ReferentialRefService,
  Referential, ReferentialRef, EntityUtils, Department, Person,
  VesselFeatures, PmfmStrategy, PmfmUtils, QualityFlagIds,
  GearLevelIds, TaxonGroupIds, LocationLevelIds, AcquisitionLevelCodes, StatusIds, PmfmIds, QualitativeLabels, TaxonomicLevelIds,
  ReferentialFragments, PmfmLabelPatterns,
  entityToString, referentialToString, qualityFlagToColor,
  vesselFeaturesToString, getPmfmName
};

@NgModule({
  imports: [
    CommonModule,
    CoreModule
  ],
  declarations: [
    ReferentialsPage,
    ReferentialForm,
    VesselsPage,
    VesselPage,
    VesselForm,
    VesselModal,
    ProgramPage,
    StrategiesTable,
    SoftwarePage,
    VesselFeaturesHistoryComponent,
    VesselRegistrationHistoryComponent
  ],
  exports: [
    ReferentialsPage,
    ReferentialForm,
    VesselsPage,
    VesselPage,
    VesselForm,
    ProgramPage,
    SoftwarePage
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
    VesselValidatorService,
    ProgramValidatorService,
    StrategyValidatorService
  ]
})
export class ReferentialModule {
}
