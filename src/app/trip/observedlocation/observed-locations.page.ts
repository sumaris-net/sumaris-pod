import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnInit} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {environment, isNil, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/core.module";
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController, ModalController} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService} from "../../core/services/account.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {FormBuilder} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {personToString} from "../../core/services/model/person.model";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {debounceTime, filter, tap} from "rxjs/operators";
import {ObservedLocationFilter, ObservedLocationService} from "../services/observed-location.service";
import {ObservedLocationValidatorService} from "../services/validator/observed-location.validator";
import {LocationLevelIds} from "../../referential/services/model/model.enum";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {PlatformService} from "../../core/services/platform.service";
import {ObservedLocation} from "../services/model/observed-location.model";
import {PersonService} from "../../admin/services/person.service";
import {SharedValidators} from "../../shared/validator/validators";
import {StatusIds} from "../../core/services/model/model.enum";
import {Trip} from "../services/model/trip.model";
import {NetworkService} from "../../core/services/network.service";
import {UserEventService} from "../../social/services/user-event.service";
import {AppRootTable} from "../../data/table/root-table.class";
import {OBSERVED_LOCATION_FEATURE_NAME} from "../services/config/trip.config";
import {TripFilter} from "../services/trip.service";


export const ObservedLocationsPageSettingsEnum = {
  PAGE_ID: "observedLocations",
  FILTER_KEY: "filter",
  FEATURE_NAME: OBSERVED_LOCATION_FEATURE_NAME
};

@Component({
  selector: 'app-observed-locations-page',
  templateUrl: 'observed-locations.page.html',
  styleUrls: ['observed-locations.page.scss'],
  providers: [
    {provide: ValidatorService, useExisting: ObservedLocationValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationsPage extends AppRootTable<ObservedLocation, ObservedLocationFilter> implements OnInit {

  highlightedRow: TableElement<Trip>;

  constructor(
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected dataService: ObservedLocationService,
    protected personService: PersonService,
    protected referentialRefService: ReferentialRefService,
    protected formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat([
          'quality',
          'program',
          'location',
          'startDateTime',
          'observers',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      dataService,
      new EntitiesTableDataSource<ObservedLocation, ObservedLocationFilter>(ObservedLocation, dataService, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        dataServiceOptions: {
          saveOnlyDirtyRows: true
        }
      }),
      null,
      injector
    );
    this.i18nColumnPrefix = 'OBSERVED_LOCATION.TABLE.';
    this.filterForm = formBuilder.group({
      program: [null, SharedValidators.entity],
      location: [null, SharedValidators.entity],
      startDate: [null, SharedValidators.validDate],
      endDate: [null, SharedValidators.validDate],
      synchronizationStatus: [null],
      recorderDepartment: [null, SharedValidators.entity],
      recorderPerson: [null, SharedValidators.entity]
      // TODO: add observer filter ?
      //,'observer': [null]
    });
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.autoLoad = false;
    this.defaultSortBy = 'startDateTime';
    this.defaultSortDirection = 'desc';

    this.settingsId = ObservedLocationsPageSettingsEnum.PAGE_ID;
    this.featureId = ObservedLocationsPageSettingsEnum.FEATURE_NAME;

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
        levelIds: [LocationLevelIds.AUCTION, LocationLevelIds.PORT]
      },
      mobile: this.mobile
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
          synchronizationStatus: json.synchronizationStatus || undefined,
          recorderDepartmentId: json.recorderDepartment && typeof json.recorderDepartment === "object" && json.recorderDepartment.id || undefined,
          recorderPersonId: json.recorderPerson && typeof json.recorderPerson === "object" && json.recorderPerson.id || undefined
        }, {emitEvent: this.mobile || isNil(this.filter)})),

        // Save filter in settings (after a debounce time)
        debounceTime(1000),
        tap(json => this.settings.savePageSetting(this.settingsId, json, 'filter'))
    )
    .subscribe());
  }

  /* -- protected methods -- */

  protected isFilterEmpty = ObservedLocationFilter.isEmpty;

  protected openRow(id: number): Promise<boolean> {
    return this.router.navigateByUrl('/observations/' + id);
  }

  protected openNewRowDetail(): Promise<boolean> {
    return this.router.navigateByUrl('/observations/new');
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
