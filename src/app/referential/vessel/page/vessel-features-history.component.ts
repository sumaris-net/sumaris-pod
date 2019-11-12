import {ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {ValidatorService} from "angular4-material-table";
import {VesselValidatorService} from "../../services/vessel.validator";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../../core/table/table.class";
import {Entity, referentialToString, VesselFeatures} from "../../services/model";
import {DefaultStatusList} from "../../../core/services/model";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService} from "../../../core/services/account.service";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {VesselFilter, VesselService} from "../../services/vessel-service";
import {FormBuilder} from "@angular/forms";
import {AppTableDataSource} from "../../../core/table/table-datasource.class";
import {environment} from "../../../../environments/environment";
import {VesselHistoryService} from "../../services/vessel-history-service";

@Component({
  selector: 'app-vessel-features-history-table',
  templateUrl: './vessel-features-history.component.html',
  styleUrls: ['./vessel-features-history.component.scss'],
  providers: [
    { provide: ValidatorService, useClass: VesselValidatorService }
  ],
})
export class VesselFeaturesHistoryComponent extends AppTable<VesselFeatures, VesselFilter> implements OnInit {

  referentialToString = referentialToString;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected vesselValidatorService: VesselValidatorService,
    protected vesselService: VesselHistoryService,
    protected cd: ChangeDetectorRef) {

    super(route, router, platform, location, modalCtrl, settings,
      // columns
      // RESERVED_START_COLUMNS
      //   .concat(
          ['id',
          'exteriorMarking',
          'startDate',
          'endDate',
          'name',
          'basePortLocation',
          'comments']
        // )
      //  .concat(RESERVED_END_COLUMNS)
      ,
      new AppTableDataSource<VesselFeatures, VesselFilter>(VesselFeatures, vesselService, vesselValidatorService, {
        prependNewElements: false,
        suppressErrors: environment.production,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      })
    );

    this.i18nColumnPrefix = 'VESSEL.';

    this.autoLoad = false;
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;

  }

}
