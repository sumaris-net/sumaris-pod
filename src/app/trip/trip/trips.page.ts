import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from "@angular/core";
import {Observable} from 'rxjs';
import {ValidatorService} from "angular4-material-table";
import {
  AccountService,
  AppTable,
  AppTableDataSource,
  LocalSettingsService,
  personsToString, PlatformService,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {TripValidatorService} from "../services/trip.validator";
import {TripFilter, TripService} from "../services/trip.service";
import {TripModal} from "./trip.modal";
import {LocationLevelIds, ReferentialRef, Trip, VesselFeatures} from "../services/trip.model";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {FormBuilder, FormGroup} from "@angular/forms";
import {
  qualityFlagToColor,
  ReferentialRefService,
  referentialToString,
  vesselFeaturesToString
} from "../../referential/referential.module";
import {debounceTime, startWith, switchMap} from "rxjs/operators";
import {TranslateService} from "@ngx-translate/core";

@Component({
  selector: 'page-trips',
  templateUrl: 'trips.page.html',
  providers: [
    {provide: ValidatorService, useClass: TripValidatorService}
  ],
  styleUrls: ['./trips.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripsPage extends AppTable<Trip, TripFilter> implements OnInit, OnDestroy {

  canEdit: boolean;
  canDelete: boolean;
  isAdmin: boolean;
  filterForm: FormGroup;
  programs: Observable<ReferentialRef[]>;
  locations: Observable<ReferentialRef[]>;
  vessels: Observable<VesselFeatures[]>;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected accountService: AccountService,
    protected validatorService: ValidatorService,
    protected dataService: TripService,
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
          'vessel',
          'departureLocation',
          'departureDateTime',
          'returnDateTime',
          'observers',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Trip, TripFilter>(Trip, dataService, null, {
        prependNewElements: false,
        suppressErrors: false,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      })
    );
    this.i18nColumnPrefix = 'TRIP.TABLE.';
    this.filterForm = formBuilder.group({
      'program': [null],
      'startDate': [null],
      'endDate': [null],
      'location': [null]
    });
    this.inlineEdition = false;

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  };

  ngOnInit() {
    super.ngOnInit();

    this.isAdmin = this.accountService.isAdmin();
    this.canEdit = this.isAdmin || this.accountService.isUser();
    this.canDelete = this.isAdmin;
    if (this.debug) console.debug("[trips-page] Can user edit table ? " + this.canEdit);

    // Programs combo (filter)
    this.programs = this.filterForm.controls['program']
      .valueChanges
      .pipe(
        startWith('*'),
        debounceTime(250),
        switchMap(value => this.referentialRefService.suggest(value, {entityName: 'Program'}))
      );

    // Locations combo (filter)
    this.locations = this.filterForm.controls['location']
      .valueChanges
      .pipe(
        debounceTime(250),
        switchMap(value => this.referentialRefService.suggest(value, {
          entityName: 'Location',
          levelId: LocationLevelIds.PORT
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
      this.markForCheck();
    });
  }

  // Not USED - remane in onAddRowDetail() if need)
  async onAddRowDetailUsingModal(): Promise<any> {
    if (this.loading) return Promise.resolve();

    const modal = await this.modalCtrl.create({component: TripModal});
    // if new trip added, refresh the table
    modal.onDidDismiss().then(res => res && this.onRefresh.emit());
    return modal.present();
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
            handler: () => {
            }
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

  vesselFeaturesToString = vesselFeaturesToString;
  referentialToString = referentialToString;
  personsToString = personsToString;
  qualityFlagToColor = qualityFlagToColor;

  programToString(item: ReferentialRef) {
    return item && item.label || undefined;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

