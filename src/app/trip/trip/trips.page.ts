import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {
  AppTable,
  AppTableDataSource,
  environment,
  isNil,
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
import {catchError, debounceTime, filter, map, tap, throttleTime} from "rxjs/operators";
import {TranslateService} from "@ngx-translate/core";
import {SharedValidators} from "../../shared/validator/validators";
import {PlatformService} from "../../core/services/platform.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {AccountService} from "../../core/services/account.service";
import {NetworkService} from "../../core/services/network.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {BehaviorSubject} from "rxjs";
import {SynchronizationStatus} from "../services/model/base.model";

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

  importing = false;
  $importProgression = new BehaviorSubject<number>(0);
  hasOfflineData = false;

  synchronizationStatusList: SynchronizationStatus[] = ['DIRTY', 'SYNC'];

  get synchronizationStatus(): SynchronizationStatus {
    return this.filterForm.controls.synchronizationStatus.value;
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

  toggleOfflineMode(event?: UIEvent) {
    if (this.network.offline) {
      this.network.setConnectionType('unknown');
    }
    else {
      this.network.setConnectionType('none');
      this.filterForm.patchValue({synchronizationStatus: 'DIRTY'}, {emitEvent: false/*avoid refresh*/});
      this.hasOfflineData = true;
    }
    // Refresh table
    this.onRefresh.emit();
  }

  async initOfflineMode(event?: UIEvent) {
    if (this.importing) return; // skip

    if (this.network.offline) {
      this.error = "ERRORS.IMPORT_NEED_ONLINE_NETWORK";
      this.markForCheck();

      // Reset error after 10s
      setTimeout(() => {
        this.error = null;
        this.markForCheck();
      }, 10000);
      return;
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
      this.hasOfflineData = this.hasOfflineData || success;
      this.importing = false;
      this.markForCheck();
    }
  }

  setSynchronizationStatus(synchronizationStatus: SynchronizationStatus) {
    console.debug("[trips] Applying filter to synchronization status: " + synchronizationStatus);
    this.filterForm.patchValue({synchronizationStatus}, {emitEvent: false});
    this.setFilter({ ...this.filter, synchronizationStatus}, {emitEvent: true});

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

    // No default filter: load all trips
    if (!tripFilter || TripFilter.isEmpty(tripFilter)) {
      this.hasOfflineData = await this.service.hasOfflineData();
      if (this.hasOfflineData) {
        this.filterForm.patchValue({
          synchronizationStatus: 'DIRTY'
        });
      }
      else {
        this.onRefresh.emit();
      }
    }
    // Restore the filter (will apply it)
    else {
      this.hasOfflineData = synchronizationStatus !== 'SYNC' || (await this.service.hasOfflineData());
      tripFilter.synchronizationStatus = synchronizationStatus;
      this.filterForm.patchValue(tripFilter);
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }



}

