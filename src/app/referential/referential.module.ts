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
  Person,
  PmfmIds,
  PmfmLabelPatterns,
  PmfmStrategy,
  QualitativeLabels,
  QualityFlagIds,
  Referential,
  ReferentialRef,
  referentialToString,
  qualityFlagToColor,
  StatusIds,
  TaxonGroupIds,
  TaxonomicLevelIds,
  VesselFeatures,
  vesselFeaturesToString
} from './services/model';

import {ReferentialFragments} from './services/referential.queries';
import {ReferentialForm} from "./form/referential.form";
import {ProgramPage} from "./program/list/program.page";

export {
  VesselModal, VesselService, ReferentialService, ProgramService, ReferentialRefService,
  Referential, ReferentialRef, EntityUtils, Department, Person,
  VesselFeatures, PmfmStrategy, QualityFlagIds,
  GearLevelIds, TaxonGroupIds, AcquisitionLevelCodes, StatusIds, PmfmIds, QualitativeLabels, TaxonomicLevelIds,
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
    ProgramPage
  ],
  exports: [
    ReferentialsPage,
    ReferentialForm,
    VesselsPage,
    VesselPage,
    VesselForm,
    ProgramPage
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
