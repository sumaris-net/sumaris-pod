import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit} from '@angular/core';
import {OperationValidatorService} from "../services/operation.validator";
import {fromDateISOString, Operation, PhysicalGear, Trip} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {Observable} from 'rxjs';
import {debounceTime, map, mergeMap, tap} from 'rxjs/operators';
import {merge} from "rxjs/observable/merge";
import {AccountService, AppForm, IReferentialRef} from '../../core/core.module';
import {
  EntityUtils,
  ReferentialRef,
  ReferentialRefService,
  referentialToString
} from '../../referential/referential.module';
import {UsageMode} from "../../core/services/model";
import {FormGroup} from "@angular/forms";
import * as moment from "moment";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {TranslateService} from "@ngx-translate/core";
import {isNilOrBlank} from "../../shared/functions";

@Component({
  selector: 'form-operation',
  templateUrl: './operation.form.html',
  styleUrls: ['./operation.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationForm extends AppForm<Operation> implements OnInit {

  private _trip: Trip;

  metiers: Observable<ReferentialRef[]>;
  physicalGears: Observable<PhysicalGear[]>;

  onFocusPhysicalGear: EventEmitter<any> = new EventEmitter<any>();
  onFocusMetier: EventEmitter<any> = new EventEmitter<any>();
  enableGps: boolean;
  latLongFormat: string;

  @Input() showComment = true;
  @Input() showError = true;

  @Input() usageMode: UsageMode;

  get trip() {
    return this._trip;
  }

  set trip(trip: Trip) {
    this._trip = trip;

    if (trip) {
      // Use trip physical gear Object (if possible)
      let physicalGear = this.form.get("physicalGear").value;
      if (physicalGear && physicalGear.id) {
        physicalGear = (trip.gears || []).find(g => g.id === physicalGear.id) || physicalGear;
        if (physicalGear) {
          this.form.controls["physicalGear"].patchValue(physicalGear);
        }
      }

      // Add validator on trip date
      this.form.get('endDateTime').setAsyncValidators(async (control) => {
        const endDateTime = fromDateISOString(control.value);
        // Make sure: departureDateTime < endDateTime < returnDateTime
        if (endDateTime && ((trip.departureDateTime && endDateTime.isBefore(trip.departureDateTime))
          || (trip.returnDateTime && endDateTime.isAfter(trip.returnDateTime)))) {
          return {msg: await this.translate.get('TRIP.OPERATION.ERROR.FIELD_DATE_OUTSIDE_TRIP').toPromise() };
        }
        return null;
      });
    }
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected physicalGearValidatorService: OperationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {

    super(dateAdapter, physicalGearValidatorService.getFormGroup(), settings);

  }

  ngOnInit() {
    this.usageMode = this.usageMode || (this.settings.isUsageMode('FIELD') ? 'FIELD' : 'DESK');
    this.enableGps = (this.usageMode === 'FIELD'); /* TODO: && platform has sensor */
    this.latLongFormat = this.settings.latLongFormat;

    // Taxon group combo
    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTargetSpecies(value, options)
    });

    // Combo: physicalGears
    this.physicalGears =
      merge(
        this.form.get('physicalGear').valueChanges.pipe(debounceTime(300)),
        this.onFocusPhysicalGear.pipe(map((_) => this.form.get('physicalGear').value))
      )
        .pipe(
          map(value => {
            // Display the selected object
            if (EntityUtils.isNotEmpty(value)) {
              if (this.form.enabled) this.form.controls["metier"].enable();
              else this.form.controls["metier"].disable();
              return [value];
            }
            // Skip if no trip (or no physical gears)
            if (!this.trip || !this.trip.gears || !this.trip.gears.length) {
              this.form.controls["metier"].disable();
              return [];
            }
            value = (typeof value === "string" && value !== "*") && value || undefined;
            // Display all trip gears
            if (!value) return this.trip.gears;
            // Search on label or name
            const ucValue = value.toUpperCase();
            return this.trip.gears.filter(g => g.gear &&
              (g.gear.label && g.gear.label.toUpperCase().indexOf(ucValue) != -1)
              || (g.gear.name && g.gear.name.toUpperCase().indexOf(ucValue) != -1)
            );
          }),
          tap(res => this.updateImplicitValue('physicalGear', res))
        );

    // Combo: metiers
    this.metiers = merge(
      this.form.get('metier').valueChanges.pipe(debounceTime(300)),
      this.onFocusMetier.pipe(map((_) => this.form.get('metier').value))
    )
      .pipe(
        mergeMap(async (value) => {
          const physicalGear = this.form.get('physicalGear').value;
          if (!physicalGear || !physicalGear.gear) return [];
          return this.referentialRefService.suggest(value, {
              entityName: 'Metier',
              levelId: physicalGear && physicalGear.gear && physicalGear.gear.id || null
            });
        }),
        tap(res => this.updateImplicitValue('metier', res))
      );
  }



  physicalGearToString(physicalGear: PhysicalGear) {
    return physicalGear && physicalGear.id ? ("#" + physicalGear.rankOrder + " - " + referentialToString(physicalGear.gear)) : undefined;
  }

  /**
   * Get the position by GPS sensor
   * @param fieldName
   */
  fillPosition(fieldName: string) {
    const positionGroup = this.form.controls[fieldName];
    if (positionGroup && positionGroup instanceof FormGroup) {
      positionGroup.patchValue(this.getGPSPosition(), {emitEvent: false, onlySelf: true});
    }
    // Set also the end date time
    if (fieldName == 'endPosition') {
      this.form.controls['endDateTime'].setValue(moment(), {emitEvent: false, onlySelf: true});
    }
    this.form.markAsDirty({onlySelf: true});
    this.form.updateValueAndValidity();
    this.markForCheck();
  }

  /**
   * Get the position by GPS sensor
   * @param fieldName
   */
  getGPSPosition(): { latitude: number, longitude: number } {
    // TODO : access GPS sensor
    console.log("TODO: get GPS position use FAKE values !!");
    return {
      latitude: 50.11,
      longitude: 0.11
    };
  }

  copyPosition(source: string, target: string) {
    const value = this.form.get(source).value;

    this.form.get(target).patchValue({
      latitude: value.latitude,
      longitude: value.longitude
    }, {emitEvent: true});
    this.markAsDirty();
  }

  referentialToString = referentialToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected async suggestTargetSpecies(value: any, options?: any): Promise<IReferentialRef[]> {
    const physicalGear = this.form.get('physicalGear').value;

    // IF taxonGroup column exists: gear must be filled first
    if (isNilOrBlank(value) && EntityUtils.isEmpty(physicalGear)) return [];

    return this.referentialRefService.suggest(value,
      {
        entityName: "Metier",
        searchJoin: "TaxonGroup",
        searchAttribute: options && options.searchAttribute,
        levelId: physicalGear && physicalGear.gear && physicalGear.gear.id || undefined
      });
  }
}
