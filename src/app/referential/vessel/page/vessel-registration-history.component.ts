import {ChangeDetectorRef, Component, Inject, Injector, OnInit} from '@angular/core';
import {ValidatorService} from "@e-is/ngx-material-table";
import {VesselValidatorService} from "../../services/validator/vessel.validator";
import {AppTable} from "../../../core/table/table.class";

import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService} from "../../../core/services/account.service";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {VesselFilter} from "../../services/vessel-service";
import {EntitiesTableDataSource} from "../../../core/table/entities-table-datasource.class";
import {VesselRegistrationService} from "../../services/vessel-registration.service";
import {VesselRegistrationValidatorService} from "../../services/validator/vessel-registration.validator";
import {VesselRegistration} from "../../services/model/vessel.model";
import {referentialToString} from "../../../core/services/model/referential.model";
import {EnvironmentService} from "../../../../environments/environment.class";

@Component({
  selector: 'app-vessel-registration-history-table',
  templateUrl: './vessel-registration-history.component.html',
  styleUrls: ['./vessel-registration-history.component.scss'],
  providers: [
    {provide: ValidatorService, useClass: VesselValidatorService}
  ],
})
export class VesselRegistrationHistoryComponent extends AppTable<VesselRegistration, VesselFilter> implements OnInit {

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
    protected vesselRegistrationValidator: VesselRegistrationValidatorService,
    protected vesselRegistrationService: VesselRegistrationService,
    protected cd: ChangeDetectorRef,
    @Inject(EnvironmentService) protected environment) {

    super(route, router, platform, location, modalCtrl, settings,
      // columns
      ['id',
        'startDate',
        'endDate',
        'registrationCode',
        'registrationLocation']
      ,
      new EntitiesTableDataSource<VesselRegistration, VesselFilter>(VesselRegistration, vesselRegistrationService, environment, vesselRegistrationValidator, {
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
