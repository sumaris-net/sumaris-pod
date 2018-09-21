import { Component, OnInit, Input } from '@angular/core';
import { TripValidatorService } from "./services/trip.validator";
import { Trip, Referential, VesselFeatures, LocationLevelIds, vesselFeaturesToString } from "./services/trip.model";
import { ModalController, Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap, debounceTime } from 'rxjs/operators';
import { merge } from "rxjs/observable/merge";
import { AppForm } from '../core/core.module';
import { VesselModal, ReferentialService, VesselService } from "../referential/referential.module";
import { referentialToString, EntityUtils } from '../referential/services/model';

@Component({
  selector: 'form-trip',
  templateUrl: './trip.form.html',
  styleUrls: ['./trip.form.scss']
})
export class TripForm extends AppForm<Trip> implements OnInit {

  vessels: Observable<VesselFeatures[]>;
  locations: Observable<Referential[]>;

  @Input() showComment: boolean = true;
  @Input() showError: boolean = true;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: Platform,
    protected tripValidatorService: TripValidatorService,
    protected vesselService: VesselService,
    protected referentialService: ReferentialService,
    protected modalCtrl: ModalController
  ) {

    super(dateAdapter, platform, tripValidatorService.getFormGroup());
  }

  ngOnInit() {
    // Combo: vessels
    this.vessels = this.form.controls['vesselFeatures']
      .valueChanges
      .pipe(
        debounceTime(250),
        mergeMap(value => {
          if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
          value = (typeof value === "string") && value || undefined;
          return this.vesselService.loadAll(0, 10, undefined, undefined,
            { searchText: value as string }
          );
        }));

    // Combo: sale location
    this.locations =
      merge(
        this.form.controls.departureLocation.valueChanges,
        this.form.controls.returnLocation.valueChanges
      )
        .pipe(
          debounceTime(250),
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
  }

  async addVesselModal(): Promise<any> {
    const modal = await this.modalCtrl.create({ component: VesselModal });
    modal.onDidDismiss().then(res => {
      // if new vessel added, use it
      if (res && res.data instanceof VesselFeatures) {
        console.debug("[trip-form] New vessel added : updating form...", res.data);
        this.form.controls['vesselFeatures'].setValue(res.data);
      }
      else {
        console.debug("[trip-form] No vessel added (user cancelled)");
      }
    });
    return modal.present();
  }

  vesselFeaturesToString = vesselFeaturesToString;
  referentialToString = referentialToString;
}
