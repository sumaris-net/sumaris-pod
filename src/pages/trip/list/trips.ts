import { Component, EventEmitter, OnInit, Output, ViewChild, OnDestroy } from "@angular/core";
import { MatPaginator, MatSort } from "@angular/material";
import { merge } from "rxjs/observable/merge";
import { Observable } from 'rxjs';
import { startWith, switchMap, mergeMap } from "rxjs/operators";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource } from "../../../app/material/material.table";
import { TripValidatorService } from "../validator/validators";
import { TripService, TripFilter } from "../../../services/trip-service";
import { SelectionModel } from "@angular/cdk/collections";
import { TripModal } from "../modal/modal-trip";
import { Trip, Referential, VesselFeatures, LocationLevelIds } from "../../../services/model";
import { Subscription } from "rxjs";
import { ModalController, Platform } from "ionic-angular";
import { Router, ActivatedRoute } from "@angular/router";
import { VesselService } from '../../../services/vessel-service';
import { AccountService } from '../../../services/account-service';
import { TableSelectColumnsComponent } from '../../../components/table/table-select-columns';
import { Location } from '@angular/common';
import { ViewController } from "ionic-angular";
import { PopoverController } from 'ionic-angular';
import { AppTable } from "../../../app/table/table";
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { ReferentialService } from "../../../services/referential-service";
import { MatButtonToggleGroup } from "@angular/material";

@Component({
  selector: 'page-trips',
  templateUrl: 'trips.html',
  providers: [
    { provide: ValidatorService, useClass: TripValidatorService }
  ],
})
export class TripsPage extends AppTable<Trip, TripFilter> implements OnInit, OnDestroy {

  filterForm: FormGroup;
  vessels: Observable<VesselFeatures[]>;
  locations: Observable<Referential[]>;

  @ViewChild(MatButtonToggleGroup) clickModeGroup: MatButtonToggleGroup;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected tripValidatorService: TripValidatorService,
    protected tripService: TripService,
    protected vesselService: VesselService,
    protected referentialService: ReferentialService,
    private formBuilder: FormBuilder
  ) {
    super(route, router, platform, location, modalCtrl, accountService, tripValidatorService,
      new AppTableDataSource<Trip, TripFilter>(Trip, tripService, tripValidatorService),
      ['select', 'id',
        'vessel',
        'departureLocation',
        'departureDateTime',
        'returnDateTime',
        'comments'],
      {} // filter
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

    this.clickModeGroup.valueChange.subscribe((value) => {
      this.inlineEdition = (value === "edit");
    });

    // Combo: sale locations
    this.locations = this.filterForm.controls.location
      .valueChanges
      .pipe(
        mergeMap(value => {
          if (!value) return Observable.empty();
          if (typeof value != "string" || value.length < 2) return Observable.of([]);
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

  addRowModal(): Promise<any> {
    if (this.loading) return;

    let modal = this.modalCtrl.create(TripModal);
    modal.onDidDismiss(res => {
      // if new trip added, refresh the table
      if (res) this.onRefresh.emit();
    });
    return modal.present();
  }


}

