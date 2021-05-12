import {ChangeDetectionStrategy, ChangeDetectorRef, Component, ViewChild} from "@angular/core";
import {AccountService} from "../../core/services/account.service";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from "@angular/common";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {ReferentialsPage} from "../list/referentials.page";

export enum AnimationState {
  ENTER = 'enter',
  LEAVE = 'leave'
}

@Component({
  selector: 'app-program-page',
  templateUrl: './programs.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProgramsPage {

  canEdit: boolean;
  canDelete: boolean;

  @ViewChild('table', { static: true }) table: ReferentialsPage;

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
    this.canEdit = isAdmin || this.accountService.isSupervisor();
    this.canDelete = isAdmin;
  }

  /* -- protected methods -- */


}

