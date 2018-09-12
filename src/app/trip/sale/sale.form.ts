import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { SaleValidatorService } from "../services/sale.validator";
import { Sale, Referential, VesselFeatures, LocationLevelIds, referentialToString, entityToString, vesselFeaturesToString } from "../services/model";
import { Platform } from '@ionic/angular';
import { Moment } from 'moment/moment';
import { AppForm } from '../../core/core.module';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { VesselService, ReferentialService } from '../../referential/referential.module';

@Component({
  selector: 'form-sale',
  templateUrl: './sale.form.html',
  styleUrls: ['./sale.form.scss']
})
export class SaleForm extends AppForm<Sale> implements OnInit {

  vessels: Observable<VesselFeatures[]>;
  locations: Observable<Referential[]>;
  saleTypes: Observable<Referential[]>;

  @Input() required: boolean = true;
  @Input() showError: boolean = true;
  @Input() showVessel: boolean = true;
  @Input() showEndDateTime: boolean = true;
  @Input() showComment: boolean = true;
  @Input() showButtons: boolean = true;

  get empty(): any {
    let value = this.value;
    return (!value.saleLocation || !value.saleLocation.id)
      && (!value.startDateTime)
      && (!value.endDateTime)
      && (!value.saleType || !value.saleType.id)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.required ? this.form.valid : (this.form.valid || this.empty);
  }

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
            if (typeof value == "object") return Observable.of([value]);
            return this.vesselService.loadAll(0, 10, undefined, undefined,
              { searchText: value as string }
            );
          }));
    }
    else {
      this.form.controls['vesselFeatures'].clearValidators();
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
          if (typeof value == "object") return Observable.of([value]);
          //if (!value) return Observable.empty();
          //if (typeof value != "string" || value.length < 2) return Observable.of([]);
          return this.referentialService.loadAll(0, 10, undefined, undefined,
            { searchText: value as string },
            { entityName: 'SaleType' });
        }));
  }

  entityToString = entityToString;
  vesselFeaturesToString = vesselFeaturesToString;
  referentialToString = referentialToString;
}
