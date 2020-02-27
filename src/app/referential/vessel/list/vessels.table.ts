import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {
  AppTable,
  AppTableDataSource,
  environment,
  isNil,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../../core/core.module";
import {VesselValidatorService} from "../../services/vessel.validator";
import {VesselFilter, VesselService} from "../../services/vessel-service";
import {VesselModal} from "../modal/modal-vessel";
import {ReferentialRef, referentialToString, statusToColor, Vessel} from "../../services/model";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {AccountService} from "../../../core/services/account.service";
import {Location} from '@angular/common';
import {Observable} from 'rxjs';
import {FormBuilder, FormGroup} from "@angular/forms";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {DefaultStatusList} from "../../../core/services/model";
import {debounceTime, filter, tap} from "rxjs/operators";
import {SharedValidators} from "../../../shared/validator/validators";
import {toBoolean} from "../../../shared/functions";

@Component({
  selector: 'app-vessels-table',
  templateUrl: 'vessels.table.html',
  styleUrls: ['./vessels.table.scss'],
  providers: [
    { provide: ValidatorService, useClass: VesselValidatorService }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselsTable extends AppTable<Vessel, VesselFilter> implements OnInit {

  isAdmin: boolean;
  filterForm: FormGroup;
  filterIsEmpty = true;
  locations: Observable<ReferentialRef[]>;
  vesselTypes: Observable<ReferentialRef[]>;
  statusList = DefaultStatusList;
  statusById: any;

  @Input() canEdit: boolean;
  @Input() canDelete: boolean;
  @Input() showFabButton = false;
  @Input() showError = true;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected vesselService: VesselService,
    protected cd: ChangeDetectorRef,
    formBuilder: FormBuilder,
    injector: Injector
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
      }),
      null,
      injector
    );
    this.i18nColumnPrefix = 'VESSEL.';
    this.filterForm = formBuilder.group({
      'date': [null, SharedValidators.validDate],
      'searchText': [null],
      'statusId': [null]
    });
    this.autoLoad = false;
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);
  }

  ngOnInit() {

    super.ngOnInit();

    const isAdmin = this.accountService.isAdmin();
    this.canEdit = toBoolean(this.canEdit, (isAdmin || this.accountService.isUser()));
    this.canDelete = toBoolean(this.canDelete, isAdmin);
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

