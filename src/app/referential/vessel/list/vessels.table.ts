import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnInit} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {VesselValidatorService} from "../../services/validator/vessel.validator";
import {VesselFilter, VesselService} from "../../services/vessel-service";
import {VesselModal} from "../modal/modal-vessel";
import {Vessel} from "../../services/model/vessel.model";
import {DefaultStatusList, ReferentialRef, referentialToString} from "../../../core/services/model/referential.model";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {AccountService} from "../../../core/services/account.service";
import {Location} from '@angular/common';
import {Observable} from 'rxjs';
import {FormBuilder, FormGroup} from "@angular/forms";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {debounceTime, filter, tap} from "rxjs/operators";
import {SharedValidators} from "../../../shared/validator/validators";
import {isNil, isNotNil, toBoolean} from "../../../shared/functions";
import {statusToColor} from "../../../data/services/model/model.utils";
import {LocationLevelIds} from "../../services/model/model.enum";
import {ReferentialRefService} from "../../services/referential-ref.service";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../../core/table/table.class";
import {EntitiesTableDataSource} from "../../../core/table/entities-table-datasource.class";
import {environment} from "../../../../environments/environment";

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

  @Input()
  set showIdColumn(value: boolean) {
    this.setShowColumn('id', value);
  }

  get showIdColumn(): boolean {
    return this.getShowColumn('id');
  }

  @Input()
  set showVesselTypeColumn(value: boolean) {
    this.setShowColumn('vesselType', value);
  }

  get showVesselTypeColumn(): boolean {
    return this.getShowColumn('vesselType');
  }

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected vesselService: VesselService,
    protected referentialRefService: ReferentialRefService,
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
      new EntitiesTableDataSource<Vessel, VesselFilter>(Vessel, vesselService, environment, null, {
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

    // Locations
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.PORT
      },
      mobile: this.mobile
    });

    // TODO fill vessel types

    // Update filter when changes
    this.registerSubscription(
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
        .subscribe());

    this.registerSubscription(
      this.onRefresh.subscribe(() => {
        this.filterIsEmpty = VesselFilter.isEmpty(this.filter);
        this.filterForm.markAsUntouched();
        this.filterForm.markAsPristine();
        this.markForCheck();
      }));

    // Restore filter from settings, or load all vessels
    this.restoreFilterOrLoad();
  }

  async openNewRowDetail(): Promise<boolean> {
    if (this.loading) return Promise.resolve(false);

    const modal = await this.modalCtrl.create({ component: VesselModal });

    await modal.present();

    const {data} = await modal.onDidDismiss();

    // if new vessel added, refresh the table
    if (isNotNil(data)) this.onRefresh.emit();

    return true;
  }

  referentialToString = referentialToString;
  statusToColor = statusToColor;

  /* -- protected methods -- */

  protected async restoreFilterOrLoad() {
    console.debug("[vessels] Restoring filter from settings...");
    const json = this.settings.getPageSettings(this.settingsId, 'filter');

    // No default filter: load all vessels
    if (isNil(json) || typeof json !== 'object') {
      // To avoid a delay (caused by debounceTime in a previous pipe), to refresh content manually
      this.onRefresh.emit();
      // But set a empty filter, to avoid automatic apply of next filter changes (caused by condition '|| isNil()' in a previous pipe)
      this.filterForm.patchValue({}, {emitEvent: false});
    }
    // Restore the filter (will apply it)
    else {
      this.filterForm.patchValue(json);
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

