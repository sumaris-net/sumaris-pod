import {Component, Input, OnInit} from '@angular/core';
import {SaleValidatorService} from "../services/sale.validator";
import {
  entityToString,
  EntityUtils,
  LocationLevelIds,
  ReferentialRef,
  referentialToString,
  Sale,
  VesselSnapshot,
  vesselSnapshotToString
} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {AppForm} from '../../core/core.module';
import {DateAdapter} from "@angular/material";
import {Observable, of} from 'rxjs';
import {debounceTime, map, mergeMap, switchMap} from 'rxjs/operators';
import {ReferentialRefService, VesselService} from '../../referential/referential.module';
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";

@Component({
  selector: 'form-sale',
  templateUrl: './sale.form.html',
  styleUrls: ['./sale.form.scss']
})
export class SaleForm extends AppForm<Sale> implements OnInit {

  vessels: Observable<VesselSnapshot[]>;
  locations: Observable<ReferentialRef[]>;
  saleTypes: Observable<ReferentialRef[]>;

  @Input() required: boolean = true;
  @Input() showError: boolean = true;
  @Input() showVessel: boolean = true;
  @Input() showEndDateTime: boolean = true;
  @Input() showComment: boolean = true;
  @Input() showButtons: boolean = true;

  get empty(): any {
    const value = this.value;
    return (!value.saleLocation || !value.saleLocation.id)
      && (!value.startDateTime)
      && (!value.endDateTime)
      && (!value.saleType || !value.saleType.id)
      && (!value.comments || !value.comments.length);
  }

  get valid(): any {
    return this.form && (this.required ? this.form.valid : (this.form.valid || this.empty));
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected saleValidatorService: SaleValidatorService,
    // protected vesselService: VesselSnapshotService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService
  ) {
    super(dateAdapter, saleValidatorService.getFormGroup(), settings);
  }

  ngOnInit() {
    super.ngOnInit();

    // Set if required or not
    this.saleValidatorService.setRequired(this.form, this.required);

    // TODO Combo: vessels (if need)
    // if (this.showVessel) {
    //   this.vessels = this.form.controls['vesselSnapshot']
    //     .valueChanges
    //     .pipe(
    //       mergeMap(value => {
    //         if (EntityUtils.isNotEmpty(value)) return of([value]);
    //         value = (typeof value === "string") && value || undefined;
    //         return this.vesselService.watchAll(0, 10, undefined, undefined,
    //           {searchText: value as string}
    //         ).pipe(map(({data}) => data));
    //       }));
    // } else {
      this.form.controls['vesselSnapshot'].clearValidators();
    // }

    // Combo: sale locations
    this.locations = this.form.controls['saleLocation']
      .valueChanges
      .pipe(
        debounceTime(250),
        switchMap(value => this.referentialRefService.suggest(value, {
          entityName: 'Location',
          levelId: LocationLevelIds.PORT
        }))
      );

    // Combo: sale types
    this.saleTypes = this.form.controls['saleType']
      .valueChanges
      .pipe(
        debounceTime(250),
        switchMap(value => this.referentialRefService.suggest(value, {entityName: 'SaleType'}))
      );
  }

  entityToString = entityToString;
  vesselSnapshotToString = vesselSnapshotToString;
  referentialToString = referentialToString;
}
