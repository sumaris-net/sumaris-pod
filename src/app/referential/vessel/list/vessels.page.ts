import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit, ViewChild} from "@angular/core";
import { ValidatorService, TableElement } from "angular4-material-table";
import {
  AppTableDataSource,
  AppTable,
  AppFormUtils,
  RESERVED_START_COLUMNS,
  RESERVED_END_COLUMNS, StatusIds, isNil, environment
} from "../../../core/core.module";
import { VesselValidatorService } from "../../services/vessel.validator";
import { VesselService, VesselFilter } from "../../services/vessel-service";
import { VesselModal } from "../modal/modal-vessel";
import {
  Referential,
  toDateISOString,
  fromDateISOString,
  referentialToString,
  ReferentialRef,
  Vessel, statusToColor
} from "../../services/model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { AccountService } from "../../../core/services/account.service";
import { Location } from '@angular/common';
import { Observable } from 'rxjs';
import { FormGroup, FormBuilder } from "@angular/forms";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {DefaultStatusList} from "../../../core/services/model";
import {debounceTime, filter, tap} from "rxjs/operators";
import {SharedValidators} from "../../../shared/validator/validators";
import {LandingsTable} from "../../../trip/landing/landings.table";
import {VesselsTable} from "./vessels.table";

@Component({
  selector: 'app-vessels-page',
  templateUrl: 'vessels.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselsPage {

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


  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  async onOpenRow({id, row}) {
    return await this.router.navigateByUrl(`/referential/vessels/${row.currentData.id}` );
  }
}

