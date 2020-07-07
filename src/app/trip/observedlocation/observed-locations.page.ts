import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {
  AppTable,
  environment, isNil,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController, ModalController} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService} from "../../core/services/account.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {personToString, personsToString} from "../../core/services/model/person.model";
import {ReferentialRef, referentialToString} from "../../core/services/model/referential.model";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {debounceTime, filter, tap} from "rxjs/operators";
import {ObservedLocationFilter, ObservedLocationService} from "../services/observed-location.service";
import {ObservedLocationValidatorService} from "../services/validator/observed-location.validator";
import {LocationLevelIds} from "../../referential/services/model/model.enum";
import {qualityFlagToColor} from "../../data/services/model/model.utils";
import {vesselSnapshotToString} from "../../referential/services/model/vessel-snapshot.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {PlatformService} from "../../core/services/platform.service";
import {ObservedLocation} from "../services/model/observed-location.model";
import {PersonService} from "../../admin/services/person.service";
import {SharedValidators} from "../../shared/validator/validators";
import {StatusIds} from "../../core/services/model/model.enum";

@Component({
  selector: 'app-observed-locations-page',
  templateUrl: 'observed-locations.page.html',
  styleUrls: ['observed-locations.page.scss'],
  providers: [
    {provide: ValidatorService, useExisting: ObservedLocationValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationsPage extends AppTable<ObservedLocation, ObservedLocationFilter> implements OnInit {

  canEdit: boolean;
  canDelete: boolean;
  isAdmin: boolean;
  filterForm: FormGroup;
  filterIsEmpty = true;

  constructor(
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected accountService: AccountService,
    protected dataService: ObservedLocationService,
    protected personService: PersonService,
    protected referentialRefService: ReferentialRefService,
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
          'location',
          'startDateTime',
          'observers',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      new EntitiesTableDataSource<ObservedLocation, ObservedLocationFilter>(ObservedLocation, dataService, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        dataServiceOptions: {
          saveOnlyDirtyRows: true
        }
      })
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
    this.sortBy='startDateTime';
    this.sortDirection='desc';

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

    this.registerSubscription(
      this.onRefresh.subscribe(() => {
        this.filterIsEmpty = ObservedLocationFilter.isEmpty(this.filter);
        this.filterForm.markAsUntouched();
        this.filterForm.markAsPristine();
        this.markForCheck();
      }));

    // Restore filter from settings, or load all rows
    this.restoreFilterOrLoad();
  }

  vesselSnapshotToString = vesselSnapshotToString;
  referentialToString = referentialToString;
  personsToString = personsToString;
  qualityFlagToColor = qualityFlagToColor;

  programToString(item: ReferentialRef) {
    return item && item.label || undefined;
  }

  /* -- protected methods -- */

  protected async restoreFilterOrLoad() {
    console.debug("[observed-locations] Restoring filter from settings...");
    const json = this.settings.getPageSettings(this.settingsId, 'filter');

    // No default filter: load all trips
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
