import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { TripValidatorService } from "../validator/validators";
import { FormGroup } from "@angular/forms";
import { Trip, Referential, VesselFeatures, LocationLevelIds, vesselFeaturesToString } from "../services/model";
import { ModalController, Platform } from "@ionic/angular";
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap, startWith } from 'rxjs/operators';
import { merge } from "rxjs/observable/merge";
import { AppForm } from '../../core/core.module';
import { VesselModal, ReferentialService, VesselService } from "../../referential/referential.module";
import { referentialToString } from '../../referential/services/model';

@Component({
  selector: 'form-trip',
  templateUrl: './form-trip.html',
  styleUrls: ['./form-trip.scss']
})
export class TripForm extends AppForm<Trip> implements OnInit {

  vessels: Observable<VesselFeatures[]>;
  locations: Observable<Referential[]>;

  @Input() showComment: boolean = true;

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
        mergeMap(value => {
          if (!value) return Observable.empty();
          if (typeof value == "object") return Observable.of([value]);
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
          mergeMap(value => {
            if (!value) return Observable.empty();
            if (typeof value == "object") return Observable.of([value]);
            if (typeof value != "string" || value.length < 2) return Observable.of([]);
            return this.referentialService.loadAll(0, 10, undefined, undefined,
              {
                levelId: LocationLevelIds.PORT,
                searchText: value as string
              },
              { entityName: 'Location' });
          }));
  }

  public async  addVesselModal(): Promise<any> {
    const modal = await this.modalCtrl.create({ component: VesselModal });
    modal.onDidDismiss(res => {
      // if new vessel added, use it
      if (res) {
        if (res instanceof VesselFeatures) {
          let json = this.form.value;
          json.vesselFeatures = res;
          this.form.setValue(json);
        }
      }
    });
    return modal.present();
  }

  vesselFeaturesToString = vesselFeaturesToString;
  referentialToString = referentialToString;
}
