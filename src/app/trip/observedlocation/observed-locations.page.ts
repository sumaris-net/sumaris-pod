import {Component} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {
  EntityUtils,
  LocationLevelIds, personsToString,
  ReferentialRef,
  referentialToString,
  vesselFeaturesToString
} from "../services/trip.model";
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService} from "../../core/services/account.service";
import {TripValidatorService} from "../services/trip.validator";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {AppTableDataSource} from "../../core/table/table-datasource.class";
import {debounceTime, mergeMap} from "rxjs/operators";
import {Observable} from "rxjs";
import {ObservedLocationFilter, ObservedLocationService} from "../services/observed-location.service";
import {ObservedLocation} from "../services/observed-location.model";
import {ObservedLocationValidatorService} from "../services/observed-location.validator";

@Component({
  selector: 'page-observed-locations',
  templateUrl: 'observed-locations.page.html',
  styleUrls: ['observed-locations.page.scss'],
  providers: [
    { provide: ValidatorService, useClass: ObservedLocationValidatorService }
  ]
})
export class ObservedLocationsPage extends AppTable<ObservedLocation, ObservedLocationFilter> {

  canEdit: boolean;
  canDelete: boolean;
  isAdmin: boolean;
  filterForm: FormGroup;
  programs: Observable<ReferentialRef[]>;
  locations: Observable<ReferentialRef[]>;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: TripValidatorService,
    protected dataService: ObservedLocationService,
    protected referentialRefService: ReferentialRefService,
    protected formBuilder: FormBuilder,
    protected alertCtrl: AlertController,
    protected translate: TranslateService
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
      new AppTableDataSource<ObservedLocation, ObservedLocationFilter>(ObservedLocation, dataService, validatorService, {
        prependNewElements: false,
        useRowValidator: false,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      })
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

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  };

  ngOnInit() {
    super.ngOnInit();

    // Programs combo (filter)
    this.programs = this.filterForm.controls['program']
      .valueChanges
      .startWith('')
      .pipe(
        debounceTime(250),
        mergeMap(value => {
          if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
          value = (typeof value === "string" && value !== '*') && value || undefined;
          return this.referentialRefService.loadAll(0, !value ? 30 : 10, undefined, undefined,
            {
              entityName: 'Program',
              searchText: value as string
            }).first().map(({data}) => data);
        })
      );

    // Locations combo (filter)
    this.locations = this.filterForm.controls['location']
      .valueChanges
      .pipe(
        debounceTime(250),
        mergeMap(value => {
          if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
          value = (typeof value === "string" && value !== '*') && value || undefined;
          return this.referentialRefService.loadAll(0, !value ? 30 : 10, undefined, undefined,
            {
              entityName: 'Location',
              levelId: LocationLevelIds.AUCTION,
              searchText: value as string
            }).first().map(({data}) => data);
        }));

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
  }

  async deleteSelection(confirm?: boolean) {
    if (this.loading) return;

    if (!confirm) {
      const translations = this.translate.instant(['COMMON.YES', 'COMMON.NO', 'CONFIRM.DELETE', 'CONFIRM.ALERT_HEADER']);
      const alert = await this.alertCtrl.create({
        header: translations['CONFIRM.ALERT_HEADER'],
        message: translations['CONFIRM.DELETE'],
        buttons: [
          {
            text: translations['COMMON.NO'],
            role: 'cancel',
            cssClass: 'secondary',
            handler: () => { }
          },
          {
            text: translations['COMMON.YES'],
            handler: () => {
              confirm = true; // update upper value
            }
          }
        ]
      });
      await alert.present();
      await alert.onDidDismiss();
    }

    if (confirm) {
      await super.deleteSelection();
    }
  }

  protected openEditRowDetail(id: number): Promise<boolean> {
    return this.router.navigateByUrl('/observations/' + id);
  }

  protected openNewRowDetail(): Promise<boolean> {
    return this.router.navigateByUrl('/observations/new');
  }

  vesselFeaturesToString = vesselFeaturesToString;
  referentialToString = referentialToString;
  personsToString = personsToString;

  programToString(item: ReferentialRef) {
    return item && item.label || '';
  }
}
