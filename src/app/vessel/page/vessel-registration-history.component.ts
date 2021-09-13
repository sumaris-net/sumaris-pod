import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnInit} from '@angular/core';
import {AppTable}  from "@sumaris-net/ngx-components";

import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService}  from "@sumaris-net/ngx-components";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {EntitiesTableDataSource}  from "@sumaris-net/ngx-components";
import {VesselRegistrationService} from "../services/vessel-registration.service";
import {VesselRegistrationPeriod} from "../services/model/vessel.model";
import {referentialToString}  from "@sumaris-net/ngx-components";
import {environment} from "../../../environments/environment";
import {VesselRegistrationFilter} from "../services/filter/vessel.filter";

@Component({
  selector: 'app-vessel-registration-history-table',
  templateUrl: './vessel-registration-history.component.html',
  styleUrls: ['./vessel-registration-history.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselRegistrationHistoryComponent extends AppTable<VesselRegistrationPeriod, VesselRegistrationFilter> implements OnInit {

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
    dataService: VesselRegistrationService,
    protected cd: ChangeDetectorRef
  ) {

    super(route, router, platform, location, modalCtrl, settings,
      // columns
      ['id',
        'startDate',
        'endDate',
        'registrationCode',
        'registrationLocation']
      ,
      new EntitiesTableDataSource<VesselRegistrationPeriod>(VesselRegistrationPeriod, dataService, null, {
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

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
