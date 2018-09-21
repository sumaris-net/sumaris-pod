import { Component, OnInit, OnDestroy } from "@angular/core";
import { Observable } from 'rxjs';
import { mergeMap } from "rxjs/operators";
import { ValidatorService } from "angular4-material-table";
import { AppTable, AppTableDataSource, AccountService } from "../core/core.module";
import { TripValidatorService } from "./services/trip.validator";
import { TripService, TripFilter } from "./services/trip.service";
import { TripModal } from "./trip.modal";
import { Trip, Referential, VesselFeatures, LocationLevelIds } from "./services/trip.model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { FormGroup, FormBuilder } from "@angular/forms";
import { VesselService, ReferentialService, vesselFeaturesToString, referentialToString } from "../referential/referential.module";

@Component({
  selector: 'page-trips',
  templateUrl: 'trips.page.html',
  providers: [
    { provide: ValidatorService, useClass: TripValidatorService }
  ],
  styleUrls: ['./trips.page.scss']
})
export class TripsPage extends AppTable<Trip, TripFilter> implements OnInit, OnDestroy {

  filterForm: FormGroup;
  vessels: Observable<VesselFeatures[]>;
  locations: Observable<Referential[]>;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: TripValidatorService,
    protected dataService: TripService,
    protected vesselService: VesselService,
    protected referentialService: ReferentialService,
    protected formBuilder: FormBuilder
  ) {

    super(route, router, platform, location, modalCtrl, accountService,
      ['select', 'id',
        'vessel',
        'departureLocation',
        'departureDateTime',
        'returnDateTime',
        'comments',
        'actions'],
      new AppTableDataSource<Trip, TripFilter>(Trip, dataService, validatorService)
    );
    this.i18nColumnPrefix = 'TRIP.';
    this.filterForm = formBuilder.group({
      'startDate': [null],
      'endDate': [null],
      'location': [null]
    });
  };

  ngOnInit() {
    super.ngOnInit();

    // Combo: sale locations
    this.locations = this.filterForm.controls.location
      .valueChanges
      .pipe(
        mergeMap(value => {
          if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
          value = (typeof value === "string") && value || undefined;
          return this.referentialService.loadAll(0, 10, undefined, undefined,
            {
              levelId: LocationLevelIds.PORT,
              searchText: value as string
            },
            { entityName: 'Location' });
        }));

    // Update filter when changes
    this.filterForm.valueChanges.subscribe(() => {
      const filter = this.filterForm.value;
      this.filter = {
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

  protected openEditRowDetail(id: number): Promise<boolean> {
    return this.router.navigateByUrl('/trips/' + id);
  }

  protected openNewRowDetail(): Promise<boolean> {
    return this.router.navigateByUrl('/trips/new');
  }

  // Not USED - remane in onAddRowDetail() if need)
  async onAddRowDetailUsingModal(): Promise<any> {
    if (this.loading) return Promise.resolve();

    const modal = await this.modalCtrl.create({ component: TripModal });
    // if new trip added, refresh the table
    modal.onDidDismiss().then(res => res && this.onRefresh.emit());
    return modal.present();
  }

  vesselFeaturesToString = vesselFeaturesToString;
  referentialToString = referentialToString;
}

