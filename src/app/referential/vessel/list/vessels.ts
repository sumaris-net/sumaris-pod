import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from "@angular/core";
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

@Component({
  selector: 'page-vessels',
  templateUrl: 'vessels.html',
  styleUrls: ['./vessels.scss'],
  providers: [
    { provide: ValidatorService, useClass: VesselValidatorService }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselsPage extends AppTable<Vessel, VesselFilter> implements OnInit {

  canEdit: boolean;
  canDelete: boolean;
  isAdmin: boolean;
  filterForm: FormGroup;
  filterIsEmpty = true;
  locations: Observable<ReferentialRef[]>;
  vesselTypes: Observable<ReferentialRef[]>;
  statusList = DefaultStatusList;
  statusById: any;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected vesselValidatorService: VesselValidatorService,
    protected vesselService: VesselService,
    private formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'status',
          'features.exteriorMarking',
          'registration.registrationCode',
          'features.startDate',
          'features.endDate',
          'features.name',
          'vesselType',
          'features.basePortLocation',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Vessel, VesselFilter>(Vessel, vesselService, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      })
    );
    this.i18nColumnPrefix = 'VESSEL.';
    this.filterForm = formBuilder.group({
      'date': [null, SharedValidators.validDate],
      'searchText': [null],
      'statusId': [null]
    });
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.autoLoad = false;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);
  }

  ngOnInit() {

    super.ngOnInit();

    this.isAdmin = this.accountService.isAdmin();
    this.canEdit = this.isAdmin || this.accountService.isUser();
    this.canDelete = this.isAdmin;
    if (this.debug) console.debug("[vessels-page] Can user edit table ? " + this.canEdit);

    // TODO fill locations
    // TODO fill vessel types

    // Update filter when changes
    this.filterForm.valueChanges
      .pipe(
        debounceTime(250),
        filter(() => this.filterForm.valid),
        // Applying the filter
        tap(json => this.setFilter({
          date: json.date,
          searchText: json.searchText,
          statusId: json.statusId
        }, {emitEvent: this.mobile || isNil(this.filter)})),
        // Save filter in settings (after a debounce time)
        debounceTime(1000),
        tap(json => this.settings.savePageSetting(this.settingsId, json, 'filter'))
      )
      .subscribe();

    this.onRefresh.subscribe(() => {
      this.filterForm.markAsUntouched();
      this.filterForm.markAsPristine();
      this.markForCheck();
    });

    // Restore filter from settings, or load all vessels
    this.restoreFilterOrLoad();
  }

  async openNewRowDetail(): Promise<any> {
    if (this.loading) return Promise.resolve();

    const modal = await this.modalCtrl.create({ component: VesselModal });
    // if new vessel added, refresh the table
    modal.onDidDismiss().then(res => {
      if (res) this.onRefresh.emit();
    });
    return modal.present();
  }

  protected async openRow(id: number, row?: TableElement<Vessel>): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    return await this.router.navigateByUrl(`/referential/vessels/${row.currentData.id}` );
  }

  referentialToString = referentialToString;
  statusToColor = statusToColor;

  /* -- protected methods -- */

  protected async restoreFilterOrLoad() {
    const json = this.settings.getPageSettings(this.settingsId, 'filter');

    // No default filter: load all vessels
    if (isNil(json) || typeof json !== 'object') {
      this.onRefresh.emit();
    }
    // Restore the filter (will apply it)
    else {
      this.filterForm.patchValue(json);
    }
  }

  setFilter(json: VesselFilter, opts?: { emitEvent: boolean }) {
    super.setFilter(json, opts);

    this.filterIsEmpty = VesselFilter.isEmpty(json);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

