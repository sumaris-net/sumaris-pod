import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnInit} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController} from "@ionic/angular";
import {Location} from "@angular/common";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {FormBuilder} from "@angular/forms";
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
import {AppRootTable} from "../../data/table/root-table.class";
import {OBSERVED_LOCATION_FEATURE_NAME, TRIP_CONFIG_OPTIONS} from "../services/config/trip.config";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {isNil, isNotNilOrBlank} from "../../shared/functions";
import {environment} from "../../../environments/environment";
import {ConfigService} from "../../core/services/config.service";
import {BehaviorSubject} from "rxjs";


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

  highlightedRow: TableElement<ObservedLocation>;
  $title = new BehaviorSubject<string>('');

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
    protected configService: ConfigService,
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
      new EntitiesTableDataSource<ObservedLocation, ObservedLocationFilter>(ObservedLocation, dataService, environment, null, {
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
    this.autoLoad = false;
    this.defaultSortBy = 'startDateTime';
    this.defaultSortDirection = 'desc';

    this.settingsId = ObservedLocationsPageSettingsEnum.PAGE_ID; // Fixed value, to be able to reuse it in the editor page
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
          debounceTime(500),
          tap(json => this.settings.savePageSetting(this.settingsId, json, ObservedLocationsPageSettingsEnum.FILTER_KEY))
        )
        .subscribe());

    this.registerSubscription(
      this.configService.config.subscribe(config => {
        const title = config && config.getProperty(TRIP_CONFIG_OPTIONS.OBSERVED_LOCATION_NAME);
        this.$title.next(title);
      })
    );

    // Restore filter from settings, or load all
    this.restoreFilterOrLoad();
  }

  clickRow(event: MouseEvent|undefined, row: TableElement<ObservedLocation>): boolean {
    this.highlightedRow = row;
    return super.clickRow(event, row);
  }


  async openTrashModal(event?: UIEvent) {
    console.debug('[observed-locations] Opening trash modal...');
    // TODO BLA
    /*const modal = await this.modalCtrl.create({
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
    if (!res) return; // CANCELLED*/
  }

  /* -- protected methods -- */

  protected isFilterEmpty = ObservedLocationFilter.isEmpty;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
