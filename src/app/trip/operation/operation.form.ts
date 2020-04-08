import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit} from '@angular/core';
import {OperationValidatorService} from "../services/operation.validator";
import {fromDateISOString, isNotNil, Operation, PhysicalGear, Trip} from "../services/trip.model";
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {AppForm, IReferentialRef} from '../../core/core.module';
import {EntityUtils, ReferentialRefService} from '../../referential/referential.module';
import {UsageMode} from "../../core/services/model";
import {FormGroup} from "@angular/forms";
import * as moment from "moment";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {TranslateService} from "@ngx-translate/core";
import {isNilOrBlank, suggestFromArray} from "../../shared/functions";
import {AccountService} from "../../core/services/account.service";
import {PlatformService} from "../../core/services/platform.service";
import {SharedValidators} from "../../shared/validator/validators";

@Component({
  selector: 'form-operation',
  templateUrl: './operation.form.html',
  styleUrls: ['./operation.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationForm extends AppForm<Operation> implements OnInit {

  private _trip: Trip;

  enableGeolocation: boolean;
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
      const physicalGearControl = this.form.get("physicalGear");
      let physicalGear = physicalGearControl.value;
      if (physicalGear && isNotNil(physicalGear.id)) {
        physicalGear = (trip.gears || []).find(g => g.id === physicalGear.id) || physicalGear;
        if (physicalGear) physicalGearControl.patchValue(physicalGear);
      }

      // Add validator on trip date
      this.form.get('endDateTime').setAsyncValidators(async (control) => {
        if (!control.touched) return;
        const endDateTime = fromDateISOString(control.value);
        // Make sure: departureDateTime < endDateTime < returnDateTime
        if (endDateTime && ((trip.departureDateTime && endDateTime.isBefore(trip.departureDateTime))
          || (trip.returnDateTime && endDateTime.isAfter(trip.returnDateTime)))) {
          return {msg: await this.translate.get('TRIP.OPERATION.ERROR.FIELD_DATE_OUTSIDE_TRIP').toPromise() };
        }
        else {
          SharedValidators.clearError(control, 'msg');
        }
        return null;
      });
    }
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: OperationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected translate: TranslateService,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, validatorService.getFormGroup(), settings);
  }

  ngOnInit() {
    this.usageMode = this.usageMode || (this.settings.isUsageMode('FIELD') ? 'FIELD' : 'DESK');
    this.latLongFormat = this.settings.latLongFormat;

    this.enableGeolocation = (this.usageMode === 'FIELD') &&
      (this.platform.is('mobile') || this.platform.is('mobileweb'));

    // Combo: physicalGears
    this.registerAutocompleteField('physicalGear', {
      suggestFn: (value, options) => this.suggestPhysicalGear(value, options),
      attributes: ['rankOrder'].concat(this.settings.getFieldDisplayAttributes('gear').map(key => 'gear.' + key))
    });

    // Taxon group combo
    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value, options) => this.suggestTargetSpecies(value, options),
      showPanelOnFocus: true,
      showAllOnFocus: true
    });
  }

  /**
   * Get the position by GPS sensor
   * @param fieldName
   */
  async fillPosition(fieldName: string) {

    const positionGroup = this.form.controls[fieldName];
    if (positionGroup && positionGroup instanceof FormGroup) {
      const coords = await this.getGeoCoordinates();
      positionGroup.patchValue(coords, {emitEvent: false, onlySelf: true});
    }
    // Set also the end date time
    if (fieldName === 'endPosition') {
      this.form.controls['endDateTime'].setValue(moment(), {emitEvent: false, onlySelf: true});
    }
    this.form.markAsDirty({onlySelf: true});
    this.form.updateValueAndValidity();
    this.markForCheck();
  }

  /**
   * Get the position by geo loc sensor
   */
  getGeoCoordinates(): Promise<{ latitude: number; longitude: number; }> {
    return new Promise<{ latitude: number; longitude: number; }>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        (position: Position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          });
        },
        (err) => {
          console.error(err);
          reject(err);
        },
        // Options
        { maximumAge: 3000, timeout: 1000, enableHighAccuracy: true }
      );
    });
  }

  copyPosition(source: string, target: string) {
    const value = this.form.get(source).value;

    this.form.get(target).patchValue({
      latitude: value.latitude,
      longitude: value.longitude
    }, {emitEvent: true});
    this.markAsDirty();
  }

  /* -- protected methods -- */

  protected async suggestPhysicalGear(value: any, options?: any): Promise<PhysicalGear[]> {
    // Display the selected object
    if (EntityUtils.isNotEmpty(value)) {
      if (this.form.enabled) this.form.controls["metier"].enable();
      else this.form.controls["metier"].disable();
      return [value];
    }
    // Skip if no trip (or no physical gears)
    if (!this._trip || !this._trip.gears || !this._trip.gears.length) {
      this.form.controls["metier"].disable();
      return [];
    }

    return suggestFromArray<PhysicalGear>(this._trip.gears, value, options);
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

  protected markForCheck() {
    this.cd.markForCheck();
  }

}
