import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {
  LocationLevelIds,
  personsToString,
  ReferentialRef,
  referentialToString,
  vesselFeaturesToString
} from "../services/trip.model";
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService} from "../../core/services/account.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {AppTableDataSource} from "../../core/table/table-datasource.class";
import {debounceTime, startWith, switchMap} from "rxjs/operators";
import {Observable} from "rxjs";
import {ObservedLocationFilter, ObservedLocationService} from "../services/observed-location.service";
import {ObservedLocation} from "../services/trip.model";
import {ObservedLocationValidatorService} from "../services/observed-location.validator";
import {qualityFlagToColor} from "../../referential/services/model";

@Component({
  selector: 'page-observed-locations',
  templateUrl: 'observed-locations.page.html',
  styleUrls: ['observed-locations.page.scss'],
  providers: [
    {provide: ValidatorService, useClass: ObservedLocationValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationsPage extends AppTable<ObservedLocation, ObservedLocationFilter> {

  canEdit: boolean;
  canDelete: boolean;
  isAdmin: boolean;
  filterForm: FormGroup;
  programs: Observable<ReferentialRef[]>;
  locations: Observable<ReferentialRef[]>;

  constructor(
    injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: ValidatorService,
    protected dataService: ObservedLocationService,
    protected referentialRefService: ReferentialRefService,
    protected formBuilder: FormBuilder,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {

    super(route, router, platform, location, modalCtrl, accountService,
      RESERVED_START_COLUMNS
        .concat([
          'quality',
          'program',
          'location',
          'startDateTime',
          'observers',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<ObservedLocation, ObservedLocationFilter>(ObservedLocation, dataService, null, {
        prependNewElements: false,
        suppressErrors: false,
        useRowValidator: false,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      }),
      null,
      injector
    );
    this.i18nColumnPrefix = 'OBSERVED_LOCATION.TABLE.';
    this.filterForm = formBuilder.group({
      'program': [null],
      'startDate': [null],
      'endDate': [null],
      'location': [null]
    });
    this.isAdmin = accountService.isAdmin();
    this.canEdit = this.isAdmin || accountService.isUser();
    this.canDelete = this.isAdmin;
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  };

  ngOnInit() {
    super.ngOnInit();

    // Programs combo (filter)
    this.programs = this.filterForm.controls['program']
      .valueChanges
      .pipe(
        startWith('*'),
        debounceTime(250),
        switchMap(value => this.referentialRefService.suggest(value,
          {
            entityName: 'Program'
          }))
      );

    // Locations combo (filter)
    this.locations = this.filterForm.controls['location']
      .valueChanges
      .pipe(
        debounceTime(250),
        switchMap(value => this.referentialRefService.suggest(value,
            {
              entityName: 'Location',
              levelIds: [LocationLevelIds.AUCTION, LocationLevelIds.PORT]
            }))
      );

    // Update filter when changes
    this.filterForm.valueChanges.subscribe(() => {
      const filter = this.filterForm.value;
      this.filter = {
        programLabel: filter.program && typeof filter.program == "object" && filter.program.label || undefined,
        startDate: filter.startDate,
        endDate: filter.endDate,
        locationId: filter.location && typeof filter.location == "object" && filter.location.id || undefined
      };
    });

    this.onRefresh.subscribe(() => {
      this.filterForm.markAsUntouched();
      this.filterForm.markAsPristine();
    });

    // TODO: remove this
    setTimeout(() => {
      this.loading = false;
      this.markForCheck();
    }, 1000);
  }

  protected openRow(id: number): Promise<boolean> {
    return this.router.navigateByUrl('/observations/' + id);
  }

  protected openNewRowDetail(): Promise<boolean> {
    return this.router.navigateByUrl('/observations/new');
  }

  vesselFeaturesToString = vesselFeaturesToString;
  referentialToString = referentialToString;
  personsToString = personsToString;
  qualityFlagToColor = qualityFlagToColor;

  programToString(item: ReferentialRef) {
    return item && item.label || '';
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
