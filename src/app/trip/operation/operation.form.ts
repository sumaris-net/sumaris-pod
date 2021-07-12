import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, Optional} from '@angular/core';
import {OperationValidatorService} from '../services/validator/operation.validator';
import * as momentImported from 'moment';
import {Moment} from 'moment';
import {DateAdapter} from '@angular/material/core';
import {
  AccountService,
  AppForm,
  EntityUtils,
  fromDateISOString,
  IReferentialRef,
  isNotEmptyArray,
  isNotNil,
  LocalSettingsService,
  PlatformService,
  ReferentialRef,
  ReferentialUtils,
  SharedValidators,
  UsageMode
} from '@sumaris-net/ngx-components';
import {FormGroup, ValidationErrors} from '@angular/forms';
import {TranslateService} from '@ngx-translate/core';
import {Operation, PhysicalGear, Trip} from '../services/model/trip.model';
import {BehaviorSubject} from 'rxjs';
import {distinctUntilChanged} from 'rxjs/operators';
import {METIER_DEFAULT_FILTER} from '../../referential/services/metier.service';
import {ReferentialRefService} from '../../referential/services/referential-ref.service';
import {Geolocation} from '@ionic-native/geolocation/ngx';
import {GeolocationOptions} from '@ionic-native/geolocation';

const moment = momentImported;

@Component({
  selector: 'app-form-operation',
  templateUrl: './operation.form.html',
  styleUrls: ['./operation.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationForm extends AppForm<Operation> implements OnInit {

  private _trip: Trip;
  private _physicalGearsSubject = new BehaviorSubject<PhysicalGear[]>(undefined);
  private _metiersSubject = new BehaviorSubject<IReferentialRef[]>(undefined);

  enableGeolocation: boolean;
  latLongFormat: string;

  mobile: boolean;

  @Input() showComment = true;
  @Input() showError = true;

  @Input() usageMode: UsageMode;

  @Input() defaultLatitudeSign: '+'|'-';
  @Input() defaultLongitudeSign: '+'|'-';

  get trip(): Trip {
    return this._trip;
  }

  set trip(value: Trip) {
    this.setTrip(value);
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected validatorService: OperationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected translate: TranslateService,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef,
    @Optional() protected geolocation: Geolocation
  ) {
    super(dateAdapter, validatorService.getFormGroup(), settings);
    this.mobile = this.settings.mobile;
  }

  ngOnInit() {
    this.usageMode = this.settings.isOnFieldMode(this.usageMode) ? 'FIELD' : 'DESK';
    this.latLongFormat = this.settings.latLongFormat;

    this.enableGeolocation = (this.usageMode === 'FIELD') && this.settings.mobile;

    // Combo: physicalGears
    const physicalGearAttributes = ['rankOrder'].concat(this.settings.getFieldDisplayAttributes('gear').map(key => 'gear.' + key));
    this.registerAutocompleteField('physicalGear', {
      items: this._physicalGearsSubject,
      attributes: physicalGearAttributes,
      mobile: this.mobile
    });

    // Taxon group combo
    this.registerAutocompleteField('taxonGroup', {
      items: this._metiersSubject,
      mobile: this.mobile
    });

    // Listen physical gear, to enable/disable metier
    this.registerSubscription(
      this.form.get('physicalGear').valueChanges
        .pipe(
          distinctUntilChanged((o1, o2) => EntityUtils.equals(o1, o2, 'id'))
        )
        .subscribe((physicalGear) => this.onPhysicalGearChanged(physicalGear))
    );
  }

  setValue(data: Operation, opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {
    // Use label and name from metier.taxonGroup
    if (data && data.metier) {
      data.metier = data.metier.clone(); // Leave original object unchanged
      data.metier.label = data.metier.taxonGroup && data.metier.taxonGroup.label || data.metier.label;
      data.metier.name = data.metier.taxonGroup && data.metier.taxonGroup.name || data.metier.name;
    }
    super.setValue(data, opts);
  }

  setTrip(trip: Trip) {
    this._trip = trip;

    if (trip) {
      // Propagate physical gears
      this._physicalGearsSubject.next((trip.gears || []).map(ps => ps.clone()));

      // Use trip physical gear Object (if possible)
      const physicalGearControl = this.form.get("physicalGear");
      let physicalGear = physicalGearControl.value;
      if (physicalGear && isNotNil(physicalGear.id)) {
        physicalGear = (trip.gears || []).find(g => g.id === physicalGear.id) || physicalGear;
        if (physicalGear) physicalGearControl.patchValue(physicalGear);
      }

      // Add validator on trip date
      this.form.get('endDateTime').setAsyncValidators(async(control)  => {
        if (!control.touched) return null;
        const endDateTime = fromDateISOString(control.value);

        // Make sure trip.departureDateTime < operation.endDateTime
        if (endDateTime && trip.departureDateTime && trip.departureDateTime.isBefore(endDateTime) === false) {
          console.warn(`[operation] Invalid operation endDateTime: before the trip! `, endDateTime, trip.departureDateTime);
          return <ValidationErrors>{msg: this.translate.instant('TRIP.OPERATION.ERROR.FIELD_DATE_BEFORE_TRIP')};
        }
        // Make sure operation.endDateTime < trip.returnDateTime
        else if (endDateTime && trip.returnDateTime && endDateTime.isBefore(trip.returnDateTime) === false) {
          console.warn(`[operation] Invalid operation endDateTime: after the trip! `, endDateTime, trip.returnDateTime);
          return <ValidationErrors>{msg: this.translate.instant('TRIP.OPERATION.ERROR.FIELD_DATE_AFTER_TRIP')};
        }

        // OK: clear existing errors
        SharedValidators.clearError(control, 'msg');
        return null;
      });
    }
  }

  /**
   * Get the position by GPS sensor
   * @param fieldName
   */
  async onFillPositionClick(event: UIEvent, fieldName: string) {

    if (event) {
      event.preventDefault();
      event.stopPropagation(); // Avoid focus into the longitude field
    }
    const positionGroup = this.form.controls[fieldName];
    if (positionGroup && positionGroup instanceof FormGroup) {
      const coords = await this.getGeoCoordinates();
      positionGroup.patchValue(coords, {emitEvent: false, onlySelf: true});
    }
    // Set also the end date time
    if (fieldName === 'endPosition') {
      this.form.get('endDateTime').setValue(moment(), {emitEvent: false, onlySelf: true});
    }
    this.form.markAsDirty({onlySelf: true});
    this.form.updateValueAndValidity();
    this.markForCheck();
  }

  /**
   * Get the position by geo loc sensor
   */
  async getGeoCoordinates(options?: GeolocationOptions): Promise<{ latitude: number; longitude: number; }> {
    options = {
        maximumAge: 30000/*30s*/,
        timeout: 10000/*10s*/,
        enableHighAccuracy: true,
        ...options
      };

    // Use ionic-native plugin
    if (this.geolocation != null) {
      try {
        const res = await this.geolocation.getCurrentPosition(options);
        return {
          latitude: res.coords.latitude,
          longitude: res.coords.longitude
        };
      }
      catch (err) {
        console.error(err);
        throw err;
      }
    }

    // Or fallback to navigator
    return new Promise<{ latitude: number; longitude: number; }>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition((res) => {
          resolve({
            latitude: res.coords.latitude,
            longitude: res.coords.longitude
          });
        },
        (err) => {
          console.error(err);
          reject(err);
        },
        options
      );
    });
  }

  copyPosition(event: UIEvent, source: string, target: string) {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    const value = this.form.get(source).value;

    this.form.get(target).patchValue({
      latitude: value.latitude,
      longitude: value.longitude
    }, {emitEvent: true});
    this.markAsDirty();
  }

  /* -- protected methods -- */

  protected async onPhysicalGearChanged(physicalGear) {
    const metierControl = this.form.get('metier');
    const physicalGearControl = this.form.get('physicalGear');

    const hasPhysicalGear = EntityUtils.isNotEmpty(physicalGear, 'id');
    const gears = this._physicalGearsSubject.getValue() || this._trip && this._trip.gears;
    // Use same trip's gear Object (if found)
    if (hasPhysicalGear && isNotEmptyArray(gears)) {
      physicalGear = (gears || []).find(g => g.id === physicalGear.id);
      physicalGearControl.patchValue(physicalGear, {emitEvent: false});
    }

    // Change metier status, if need
    const enableMetier = hasPhysicalGear && this.form.enabled && isNotEmptyArray(gears);
    if (enableMetier) {
      if (metierControl.disabled) metierControl.enable();
    }
    else {
      if (metierControl.enabled) metierControl.disable();
    }

    if (hasPhysicalGear) {

      // Refresh metiers
      const metiers = await this.loadMetiers(physicalGear);
      this._metiersSubject.next(metiers);

      const metier = metierControl.value;
      if (ReferentialUtils.isNotEmpty(metier)) {
        // Find new reference, by ID
        let updatedMetier = (metiers || []).find(m => m.id === metier.id);

        // If not found : retry using the label (WARN: because of searchJoin, label = taxonGroup.label)
        updatedMetier = updatedMetier || (metiers || []).find(m => m.label === metier.label);

        // Update the metier, if not found (=reset) or ID changed
        if (!updatedMetier || !ReferentialUtils.equals(metier, updatedMetier)) {
          metierControl.setValue(updatedMetier);
        }
      }
    }
  }

  protected async loadMetiers(physicalGear?: PhysicalGear|any): Promise<ReferentialRef[]> {

    // No gears selected: skip
    if (EntityUtils.isEmpty(physicalGear, 'id')) return undefined;

    const gear = physicalGear && physicalGear.gear;
    console.debug('[operation-form] Loading Metier ref items for the gear: ' + (gear && gear.label));

    const res = await this.referentialRefService.loadAll(0, 100, null, null,
      {
        entityName: "Metier",
        ...METIER_DEFAULT_FILTER,
        searchJoin: "TaxonGroup",
        levelId: gear && gear.id || undefined
      },
      {
        withTotal: false
      });

    return res.data;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}
