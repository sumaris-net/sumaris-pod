import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { SaleValidatorService } from "../validator/validators";
import { FormGroup } from "@angular/forms";
import { Sale, Referential, VesselFeatures, LocationLevelIds, referentialToString, entityToString, vesselFeaturesToString } from "../../services/model";
import { Platform } from '@ionic/angular';
import { Moment } from 'moment/moment';
import { AppForm } from '../../../core/core.module';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { VesselService, ReferentialService } from '../../../referential/referential.module';
import { merge } from "rxjs/observable/merge";

@Component({
  selector: 'form-sale',
  templateUrl: './form-sale.html'
})
export class SaleForm extends AppForm<Sale> implements OnInit {

  vessels: Observable<VesselFeatures[]>;
  locations: Observable<Referential[]>;
  saleTypes: Observable<Referential[]>;

  @Input() showVessel: boolean = true;
  @Input() showEndDateTime: boolean = true;
  @Input() showComment: boolean = true;
  @Input() showButtons: boolean = true;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: Platform,
    protected saleValidatorService: SaleValidatorService,
    protected vesselService: VesselService,
    protected referentialService: ReferentialService
  ) {
    super(dateAdapter, platform, saleValidatorService.getFormGroup());
  }

  ngOnInit() {
    // Combo: vessels (if need)
    if (this.showVessel) {
      this.vessels = this.form.controls['vesselFeatures']
        .valueChanges
        .pipe(
          mergeMap(value => {
            if (!value) return Observable.empty();
            if (typeof value == "object") return Observable.of([]);
            return this.vesselService.loadAll(0, 10, undefined, undefined,
              { searchText: value as string }
            );
          }));
    }

    // Combo: sale locations
    this.locations = this.form.controls['saleLocation']
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
            {
              entityName: 'Location'
            });
        }));

    // Combo: sale types
    this.saleTypes = this.form.controls['saleType']
      .valueChanges
      .pipe(
        mergeMap(value => {
          if (!value) return Observable.empty();
          if (typeof value != "string" || value.length < 2) return Observable.of([]);
          return this.referentialService.loadAll(0, 10, undefined, undefined,
            { searchText: value as string },
            { entityName: 'SaleType' });
        }));
  }

  public get empty(): any {
    let value = this.value;
    return (!value.saleLocation || !value.saleLocation.id)
      && (!value.startDateTime)
      && (!value.endDateTime)
      && (!value.saleType || !value.saleType.id)
      && (!value.comments || !value.comments.length);
  }

  entityToString = entityToString;
  vesselFeaturesToString = vesselFeaturesToString;
  referentialToString = referentialToString;
}
