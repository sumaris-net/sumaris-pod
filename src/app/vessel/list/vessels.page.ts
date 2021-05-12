import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewChild} from "@angular/core";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {AccountService} from "../../core/services/account.service";
import {Location} from '@angular/common';
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {VesselsTable} from "./vessels.table";
import {VESSEL_FEATURE_NAME} from "../services/config/vessel.config";

export const VesselsPageSettingsEnum = {
  PAGE_ID: "vessels",
  FEATURE_ID: VESSEL_FEATURE_NAME
};


@Component({
  selector: 'app-vessels-page',
  templateUrl: 'vessels.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselsPage implements OnInit {

  canEdit: boolean;
  canDelete: boolean;

  @ViewChild('table', { static: true }) table: VesselsTable;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef
  ) {
    const isAdmin = this.accountService.isAdmin();
    this.canEdit = isAdmin || this.accountService.isUser();
    this.canDelete = isAdmin;
  }

  ngOnInit() {
    this.table.settingsId = VesselsPageSettingsEnum.PAGE_ID;
  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  async onOpenRow({id, row}) {
    return await this.router.navigateByUrl(`/vessels/${row.currentData.id}` );
  }
}

