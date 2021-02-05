import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnDestroy, OnInit} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {TripValidatorService} from "../services/validator/trip.validator";
import {TripFilter, TripService} from "../services/trip.service";
import {AlertController, ModalController} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {FormBuilder} from "@angular/forms";
import {debounceTime, filter, tap} from "rxjs/operators";
import {TranslateService} from "@ngx-translate/core";
import {SharedValidators} from "../../shared/validator/validators";
import {PlatformService} from "../../core/services/platform.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {AccountService} from "../../core/services/account.service";
import {NetworkService} from "../../core/services/network.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {personToString} from "../../core/services/model/person.model";
import {Trip} from "../services/model/trip.model";
import {PersonService} from "../../admin/services/person.service";
import {StatusIds} from "../../core/services/model/model.enum";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {LocationLevelIds} from "../../referential/services/model/model.enum";
import {UserEventService} from "../../social/services/user-event.service";
import {TripTrashModal} from "./trash/trip-trash.modal";
import {TRIP_FEATURE_NAME} from "../services/config/trip.config";
import {AppRootTable} from "../../data/table/root-table.class";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {isNil} from "../../shared/functions";
import {environment} from "../../../environments/environment";

export const TripsPageSettingsEnum = {
  PAGE_ID: "trips",
  FILTER_KEY: "filter",
  FEATURE_NAME: TRIP_FEATURE_NAME
};

@Component({
  selector: 'app-trips-table',
  templateUrl: 'trips.table.html',
  styleUrls: ['./trips.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: TripValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripTable extends AppRootTable<Trip, TripFilter> implements OnInit, OnDestroy {

  highlightedRow: TableElement<Trip>;

  constructor(
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected dataService: TripService,
    protected userEventService: UserEventService,
    protected personService: PersonService,
    protected referentialRefService: ReferentialRefService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected formBuilder: FormBuilder,
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
        dataService,
      new EntitiesTableDataSource<Trip, TripFilter>(Trip, dataService, environment, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        dataServiceOptions: {
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
      synchronizationStatus: [null],
      recorderDepartment: [null, SharedValidators.entity],
      recorderPerson: [null, SharedValidators.entity]
      // TODO: add observer filter ?
      //,'observer': [null]
    });

    this.autoLoad = false;
    this.defaultSortBy = 'departureDateTime';
    this.defaultSortDirection = 'desc';

    this.settingsId = TripsPageSettingsEnum.PAGE_ID; // Fixed value, to be able to reuse it in the editor page
    this.featureId = TripsPageSettingsEnum.FEATURE_NAME;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Programs combo (filter)
    this.registerAutocompleteField('program', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Program'
      },
      mobile: this.mobile
    });

    // Locations combo (filter)
    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.PORT
      },
      mobile: this.mobile
    });

    // Combo: vessels
    this.registerAutocompleteField('vesselSnapshot', {
      service: this.vesselSnapshotService,
      attributes: this.settings.getFieldDisplayAttributes('vesselSnapshot', ['exteriorMarking', 'name']),
      filter: {
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }
    });

    // Combo: recorder department
    this.registerAutocompleteField('department', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Department'
      },
      mobile: this.mobile
    });

    // Combo: recorder person
    this.registerAutocompleteField('person', {
      service: this.personService,
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE]
      },
      attributes: ['lastName', 'firstName', 'department.name'],
      displayWith: personToString,
      mobile: this.mobile
    });

    // Update filter when changes
    this.registerSubscription(
      this.filterForm.valueChanges
        .pipe(
          debounceTime(250),
          filter(() => this.filterForm.valid),
          // Applying the filter
          tap(json => this.setFilter({
              programLabel: json.program && typeof json.program === "object" && json.program.label || undefined,
              startDate: json.startDate,
              endDate: json.endDate,
              locationId: json.location && typeof json.location === "object" && json.location.id || undefined,
              vesselId:  json.vesselSnapshot && typeof json.vesselSnapshot === "object" && json.vesselSnapshot.id || undefined,
              synchronizationStatus: json.synchronizationStatus || undefined,
              recorderDepartmentId: json.recorderDepartment && typeof json.recorderDepartment === "object" && json.recorderDepartment.id || undefined,
              recorderPersonId: json.recorderPerson && typeof json.recorderPerson === "object" && json.recorderPerson.id || undefined
            }, {emitEvent: this.mobile || isNil(this.filter)})),
          // Save filter in settings (after a debounce time)
          debounceTime(500),
          tap(json => this.settings.savePageSetting(this.settingsId, json, TripsPageSettingsEnum.FILTER_KEY))
        )
        .subscribe());

    // Restore filter from settings, or load all
    this.restoreFilterOrLoad();
  }

  clickRow(event: MouseEvent|undefined, row: TableElement<Trip>): boolean {
    this.highlightedRow = row;
    return super.clickRow(event, row);
  }

  async openTrashModal(event?: UIEvent) {
    console.debug('[trips] Opening trash modal...');
    const modal = await this.modalCtrl.create({
      component: TripTrashModal,
      componentProps: {
        synchronizationStatus: this.filter.synchronizationStatus
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res) return; // CANCELLED
  }

  /* -- protected methods -- */

  protected isFilterEmpty = TripFilter.isEmpty;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}