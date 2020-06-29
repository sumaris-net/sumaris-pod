import {ChangeDetectorRef, Component, Injector, OnInit} from '@angular/core';
import {ValidatorService} from "angular4-material-table";
import {VesselValidatorService} from "../../services/validator/vessel.validator";
import {AppTable} from "../../../core/table/table.class";
import {VesselFeatures} from "../../services/model/vessel.model";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService} from "../../../core/services/account.service";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {VesselFilter} from "../../services/vessel-service";
import {AppTableDataSource} from "../../../core/table/table-datasource.class";
import {environment} from "../../../../environments/environment";
import {VesselFeaturesService} from "../../services/vessel-features.service";
import {VesselFeaturesValidatorService} from "../../services/validator/vessel-features.validator";
import {referentialToString} from "../../../core/services/model/referential.model";

@Component({
  selector: 'app-vessel-features-history-table',
  templateUrl: './vessel-features-history.component.html',
  styleUrls: ['./vessel-features-history.component.scss'],
  providers: [
    {provide: ValidatorService, useClass: VesselValidatorService}
  ],
})
export class VesselFeaturesHistoryComponent extends AppTable<VesselFeatures, VesselFilter> implements OnInit {

  referentialToString = referentialToString;
  isAdmin: boolean;

  constructor(
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected vesselFeaturesValidator: VesselFeaturesValidatorService,
    protected vesselFeaturesService: VesselFeaturesService,
    protected cd: ChangeDetectorRef) {

    super(route, router, platform, location, modalCtrl, settings,
      // columns
      ['id',
        'startDate',
        'endDate',
        'exteriorMarking',
        'name',
        'administrativePower',
        'lengthOverAll',
        'grossTonnageGt',
        'basePortLocation',
        'comments'],
      new AppTableDataSource<VesselFeatures, VesselFilter>(VesselFeatures, vesselFeaturesService, vesselFeaturesValidator, {
        prependNewElements: false,
        suppressErrors: environment.production,
        dataServiceOptions: {
          saveOnlyDirtyRows: true
        }
      }),
      null,
      injector
    );

    this.i18nColumnPrefix = 'VESSEL.';

    this.autoLoad = false;
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;

  }

  ngOnInit() {
    super.ngOnInit();

    this.isAdmin = this.accountService.isAdmin();
  }

}
