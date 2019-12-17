import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {
  AppTable,
  AppTableDataSource,
  environment,
  isNil, isNotNil,
  personsToString,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  StatusIds
} from "../../core/core.module";
import {TripValidatorService} from "../services/trip.validator";
import {TripFilter, TripService} from "../services/trip.service";
import {LocationLevelIds, ReferentialRef, Trip} from "../services/trip.model";
import {AlertController, ModalController} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {FormBuilder, FormGroup} from "@angular/forms";
import {qualityFlagToColor, ReferentialRefService, referentialToString} from "../../referential/referential.module";
import {catchError, debounceTime, distinctUntilChanged, filter, map, tap, throttleTime} from "rxjs/operators";
import {TranslateService} from "@ngx-translate/core";
import {SharedValidators} from "../../shared/validator/validators";
import {PlatformService} from "../../core/services/platform.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {AccountService} from "../../core/services/account.service";
import {ConnectionType, NetworkService} from "../../core/services/network.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {BehaviorSubject} from "rxjs";
import {SynchronizationStatus} from "../services/model/base.model";
import {concatPromises} from "../../shared/observables";

export const TripsPageSettingsEnum = {
  PAGE_ID: "trips",
  FILTER_KEY: "filter"
};

@Component({
  selector: 'app-trips-page',
  templateUrl: 'trips.page.html',
  styleUrls: ['./trips.page.scss'],
  providers: [
    {provide: ValidatorService, useExisting: TripValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripsPage extends AppTable<Trip, TripFilter> implements OnInit, OnDestroy {

  canEdit: boolean;
  canDelete: boolean;
  isAdmin: boolean;
  filterForm: FormGroup;
  filterIsEmpty = true;
  offline = false;

  importing = false;
  $importProgression = new BehaviorSubject<number>(0);
  hasOfflineMode = false;

  synchronizationStatusList: SynchronizationStatus[] = ['DIRTY', 'SYNC'];

  get synchronizationStatus(): SynchronizationStatus {
    return this.filterForm.controls.synchronizationStatus.value || 'SYNC' /*= the default status*/;
  }

  constructor(
    public network: NetworkService,
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected accountService: AccountService,
    protected service: TripService,
    protected referentialRefService: ReferentialRefService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected formBuilder: FormBuilder,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {

    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat([
          'quality',
          'program',
          'vessel',
          'departureLocation',
          'departureDateTime',
          'returnDateTime',
          'observers',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Trip, TripFilter>(Trip, service, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      }),
      null,
      injector
    );
    this.i18nColumnPrefix = 'TRIP.TABLE.';
    this.filterForm = formBuilder.group({
      program: [null, SharedValidators.entity],
      vesselSnapshot: [null, SharedValidators.entity],
      location: [null, SharedValidators.entity],
      startDate: [null, SharedValidators.validDate],
      endDate: [null, SharedValidators.validDate],
      synchronizationStatus: [null]
    });
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.autoLoad = false;
    this.settingsId = TripsPageSettingsEnum.PAGE_ID; // Fix value, to be able to reuse it in the trip page

    // FOR DEV ONLY ----
    this.debug = !environment.production;

  }

  ngOnInit() {
    super.ngOnInit();

    this.isAdmin = this.accountService.isAdmin();
    this.canEdit = this.isAdmin || this.accountService.isUser();
    this.canDelete = this.isAdmin;

    // Listen network
    this.offline = this.network.offline;
    this.registerSubscription(
      this.network.onNetworkStatusChanges
        .pipe(
          filter(isNotNil),
          distinctUntilChanged()
        )
        .subscribe((type) => this.onNetworkStatusChanged(type)));

    // Programs combo (filter)
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Program'
      }
    });

    // Locations combo (filter)
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.PORT
      }
    });

    // Combo: vessels
    this.registerAutocompleteField('vesselSnapshot', {
      service: this.vesselSnapshotService,
      attributes: this.settings.getFieldDisplayAttributes('vesselSnapshot', ['exteriorMarking', 'name']),
      filter: {
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }
    });

    // Update filter when changes
    this.filterForm.valueChanges
      .pipe(
        debounceTime(250),
        filter(() => this.filterForm.valid),
        // Applying the filter
        tap(json => {
          this.setFilter({
            programLabel: json.program && typeof json.program === "object" && json.program.label || undefined,
            startDate: json.startDate,
            endDate: json.endDate,
            locationId: json.location && typeof json.location === "object" && json.location.id || undefined,
            vesselId:  json.vesselSnapshot && typeof json.vesselSnapshot === "object" && json.vesselSnapshot.id || undefined,
            synchronizationStatus: json.synchronizationStatus || undefined,
          }, {emitEvent: this.mobile || isNil(this.filter)});
        }),
        // Save filter in settings (after a debounce time)
        debounceTime(1000),
        tap(json => this.settings.savePageSetting(this.settingsId, json, TripsPageSettingsEnum.FILTER_KEY))
      )
      .subscribe();

    this.onRefresh.subscribe(() => {
      this.filterForm.markAsUntouched();
      this.filterForm.markAsPristine();
      this.markForCheck();
    });

    // Restore filter from settings, or load all trips
    this.restoreFilterOrLoad();
  }

  setFilter(json: TripFilter, opts?: { emitEvent: boolean; }) {
    super.setFilter(json, opts);

    this.filterIsEmpty = TripFilter.isEmpty(json);
  }

  onNetworkStatusChanged(type: ConnectionType) {
    const offline = type === "none";
    if (this.offline !== offline) {

      // Update the property used in template
      this.offline = offline;
      this.markForCheck();

      // When offline, change synchronization status to DIRTY
      if (this.offline && this.synchronizationStatus === 'SYNC') {
        this.setSynchronizationStatus('DIRTY');
      }
    }
  }

  toggleOfflineMode(event?: UIEvent) {
    if (this.network.offline) {
      this.network.setForceOffline(false);
    }
    else {
      this.network.setForceOffline();
      this.filterForm.patchValue({synchronizationStatus: 'DIRTY'}, {emitEvent: false/*avoid refresh*/});
      this.hasOfflineMode = true;
    }
    // Refresh table
    this.onRefresh.emit();
  }

  async prepareOfflineMode(event?: UIEvent) {
    if (this.importing) return; // skip

    if (this.network.offline) {
      return this.showToast({
        message: "ERROR.NETWORK_REQUIRED",
        error: true,
        showCloseButton: true
      });
    }

    this.$importProgression.next(0);

    let success = false;
    try {

      await new Promise((resolve, reject) => {
        // Run the import
        this.service.executeImport({maxProgression: 100})
          .pipe(
            filter(value => value > 0),
            map((progress) => {
              if (!this.importing) {
                this.importing = true;
                this.markForCheck();
              }
              return Math.min(Math.trunc(progress), 100);
            }),
            catchError(err => {
              reject(err);
              throw err;
            }),
            throttleTime(100)
          )
          .subscribe(progression => this.$importProgression.next(progression))
          .add(() => resolve());
      });

      // Enable sync status button
      this.setSynchronizationStatus('DIRTY');
      this.showToast({message: 'NETWORK.IMPORTATION_SUCCEED'});
      success = true;
    }
    catch (err) {
      this.error = err && err.message || err;
    }
    finally {
      this.hasOfflineMode = this.hasOfflineMode || success;
      this.importing = false;
      this.markForCheck();
    }
  }

  async setSynchronizationStatus(synchronizationStatus: SynchronizationStatus) {
    if (!synchronizationStatus) return; // Skip if empty

    // Make sure network is UP
    if (this.offline && synchronizationStatus === 'SYNC') {
      return this.showToast({
        message: "ERROR.NETWORK_REQUIRED",
        error: true,
        showCloseButton: true
      });
    }

    console.debug("[trips] Applying filter to synchronization status: " + synchronizationStatus);
    this.error = null;
    this.filterForm.patchValue({synchronizationStatus}, {emitEvent: false});
    const json = { ...this.filter, synchronizationStatus};
    this.setFilter(json, {emitEvent: true});

    // Save filter to settings (need to be done here, because new trip can stored filter)
    await this.settings.savePageSetting(this.settingsId, json, TripsPageSettingsEnum.FILTER_KEY);
  }

  async synchronizeSelection() {
    if (!this._enable) return;
    if (this.loading || this.selection.isEmpty()) return;


    if (this.debug) console.debug("[trips] Starting synchronization...");

    const rowsToSync = this.selection.selected.slice();
    const tripIds = rowsToSync.map(row => row.currentData.id).filter(id => id < 0);

    try {
      this.loading = true;
      await concatPromises(tripIds.map(tripId => () => this.service.synchronizeById(tripId)));
      this.selection.clear();

      // Success message
      this.showToast({
        message: 'INFO.SYNCHRONIZATION_SUCCEED'
      });
    } catch (err) {
      this.error = err && err.message || err;
    }
    finally {
      this.onRefresh.emit();
    }
  }

  referentialToString = referentialToString;
  personsToString = personsToString;
  qualityFlagToColor = qualityFlagToColor;

  programToString(item: ReferentialRef) {
    return item && item.label || undefined;
  }

  /* -- protected methods -- */

  protected async restoreFilterOrLoad() {
    const json = this.settings.getPageSettings(this.settingsId, TripsPageSettingsEnum.FILTER_KEY);

    const synchronizationStatus = json && json.synchronizationStatus;
    const tripFilter = json && typeof json === 'object' && {...json, synchronizationStatus: undefined} || undefined;

    this.hasOfflineMode = (synchronizationStatus && synchronizationStatus !== 'SYNC') ||
      (this.settings.hasOfflineFeature() || await this.service.hasOfflineData());

    // No default filter, nor synchronizationStatus
    if (TripFilter.isEmpty(tripFilter) && !synchronizationStatus) {
      // If offline data, show it (will refresh)
      if (this.hasOfflineMode) {
        this.filterForm.patchValue({
          synchronizationStatus: 'DIRTY'
        });
      }
      // No offline data: default load (online trips)
      else {
        this.onRefresh.emit();
      }
    }
    // Restore the filter (will apply it)
    else {
      // Force offline
      if (this.network.offline && this.hasOfflineMode && synchronizationStatus === 'SYNC') {
        this.filterForm.patchValue({
          ...tripFilter,
          synchronizationStatus: 'DIRTY'
        });
      }
      else {
        this.filterForm.patchValue({...tripFilter, synchronizationStatus});
      }
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }



}

