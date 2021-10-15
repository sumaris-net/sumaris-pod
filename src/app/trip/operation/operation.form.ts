import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Optional, Output } from '@angular/core';
import { OperationValidatorService } from '../services/validator/operation.validator';
import * as momentImported from 'moment';
import { Moment } from 'moment';
import {
  AccountService,
  AppForm, AppFormUtils,
  DateFormatPipe,
  EntityUtils,
  fromDateISOString,
  IReferentialRef,
  isNil,
  isNotEmptyArray,
  isNotNil,
  LocalSettingsService,
  PlatformService,
  ReferentialRef,
  ReferentialUtils,
  SharedValidators,
  toBoolean,
  UsageMode
} from '@sumaris-net/ngx-components';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { Operation, PhysicalGear, Trip, VesselPosition } from '../services/model/trip.model';
import { BehaviorSubject, merge, Observable, Subscription } from 'rxjs';
import { distinctUntilChanged, startWith } from 'rxjs/operators';
import { METIER_DEFAULT_FILTER, MetierService } from '@app/referential/services/metier.service';
import { ReferentialRefService } from '@app/referential/services/referential-ref.service';
import { Geolocation } from '@ionic-native/geolocation/ngx';
import { OperationService } from '@app/trip/services/operation.service';
import { ModalController } from '@ionic/angular';
import { SelectOperationModal } from '@app/trip/operation/select-operation.modal';
import { QualityFlagIds } from '@app/referential/services/model/model.enum';
import { PmfmService } from '@app/referential/services/pmfm.service';
import { Router } from '@angular/router';
import { SubBatch } from '@app/trip/services/model/subbatch.model';

const moment = momentImported;


export const IS_CHILD_OPERATION_ITEMS = Object.freeze([
  {
    value: false,
    label: "TRIP.OPERATION.EDIT.TYPE.PARENT"
  },
  {
    value: true,
    label: "TRIP.OPERATION.EDIT.TYPE.CHILD"
  }
]);

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
  private _showMetierFilter = false;

  startProgram: Date | Moment;
  enableGeolocation: boolean;
  latLongFormat: string;
  mobile: boolean;
  distance: number;
  maxDistanceWarning: number;
  maxDistanceError: number;
  distanceError: boolean;
  distanceWarning: boolean;
  enableMetierFilter = false;

  isChildOperationItems = IS_CHILD_OPERATION_ITEMS;
  $isChildOperation = new BehaviorSubject<boolean>(undefined);
  $parentOperationLabel = new BehaviorSubject<string>('');

  @Input() showComment = true;
  @Input() showError = true;

  @Input() set showMetierFilter(value: boolean) {
    this._showMetierFilter = value;
    // Change metier filter button
    if (this._showMetierFilter !== this.enableMetierFilter) {
      this.toggleMetierFilter(null);
    }
  }

  get showMetierFilter(): boolean {
    return this._showMetierFilter;
  }

  @Input() allowParentOperation: boolean;
  @Input() usageMode: UsageMode;
  @Input() defaultLatitudeSign: '+' | '-';
  @Input() defaultLongitudeSign: '+' | '-';
  @Input() programLabel: string;

  get trip(): Trip {
    return this._trip;
  }

  set trip(value: Trip) {
    this.setTrip(value);
  }

  get isChildOperation(): boolean {
    return this.$isChildOperation.value === true;
  }


  get parentControl(): FormControl {
    return this.form.get('parentOperation') as FormControl;
  }

  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean }) {
    super.enable(opts);
  }

  @Output() onParentChanges = new EventEmitter<Operation>();

  constructor(
    protected dateFormat: DateFormatPipe,
    protected router: Router,
    protected validatorService: OperationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected operationService: OperationService,
    protected metierService: MetierService,
    protected pmfmService: PmfmService,
    protected settings: LocalSettingsService,
    protected translate: TranslateService,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef,
    @Optional() protected geolocation: Geolocation
  ) {
    super(dateFormat, validatorService.getFormGroup(), settings);
    this.mobile = this.settings.mobile;
  }

  ngOnInit() {
    this.usageMode = this.settings.isOnFieldMode(this.usageMode) ? 'FIELD' : 'DESK';
    this.latLongFormat = this.settings.latLongFormat;

    this.enableGeolocation = (this.usageMode === 'FIELD') && this.settings.mobile;
    this.allowParentOperation = toBoolean(this.allowParentOperation, false);

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

    // Listen parent operation
    this.registerSubscription(
      this.form.get('parentOperation').valueChanges
        .subscribe(value => this.onParentOperationChanged(value))
    );

    this.registerSubscription(
      merge(
        this.form.get('startPosition').valueChanges,
        this.form.get('endPosition').valueChanges
      ).subscribe(
        () => this.updateDistance()
      )
    );
  }

  setValue(data: Operation, opts?: { emitEvent?: boolean; onlySelf?: boolean; }) {
    const isNew = isNil(data?.id);

    // Use label and name from metier.taxonGroup
    if (!isNew && data.metier) {
      data.metier = data.metier.clone(); // Leave original object unchanged
      data.metier.label = data.metier.taxonGroup && data.metier.taxonGroup.label || data.metier.label;
      data.metier.name = data.metier.taxonGroup && data.metier.taxonGroup.name || data.metier.name;
    }
    this.onIsChildOperationChanged(isNotNil(data.parentOperation?.id));
    super.setValue(data, opts);
  }

  setTrip(trip: Trip) {
    this._trip = trip;

    if (trip) {
      // Propagate physical gears
      this._physicalGearsSubject.next((trip.gears || []).map(ps => PhysicalGear.fromObject(ps).clone()));

      // Use trip physical gear Object (if possible)
      const physicalGearControl = this.form.get('physicalGear');
      let physicalGear = physicalGearControl.value;
      if (physicalGear && isNotNil(physicalGear.id)) {
        physicalGear = (trip.gears || []).find(g => g.id === physicalGear.id) || physicalGear;
        if (physicalGear) physicalGearControl.patchValue(physicalGear);
      }
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
      const coords = await this.operationService.getGeoCoordinates();
      positionGroup.patchValue(coords, {emitEvent: false, onlySelf: true});
    }
    // Set also the end date time
    if (fieldName === 'endPosition') {
      const endDateTimeControlName = this.isChildOperation ? 'endDateTime' : 'fishingStartDateTime';
      this.form.get(endDateTimeControlName).setValue(moment(), {emitEvent: false, onlySelf: true});
    }
    this.form.markAsDirty({onlySelf: true});
    this.updateDistance();
    this.form.updateValueAndValidity();
    this.markForCheck();
    this.checkDistanceValidity();
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

  async openSelectOperationModal(): Promise<Operation> {

    const value = this.form.value as Partial<Operation>;
    const endDate = value.fishingEndDateTime || this.trip.returnDateTime;
    const parentOperation = value.parentOperation;
    const startDate = fromDateISOString(this._trip.departureDateTime).clone().add(-15, 'day');

    const modal = await this.modalCtrl.create({
      component: SelectOperationModal,
      componentProps: {
        filter: {
          programLabel: this.programLabel,
          vesselId: this._trip.vesselSnapshot.id,
          excludedIds: isNotNil(value.id) ? [value.id] : null,
          excludeChildOperation: true,
          hasNoChildOperation: true,
          endDate,
          startDate,
          includedIds: isNotNil(parentOperation) ? [parentOperation.id] : null,
          gearIds: this._physicalGearsSubject.getValue().map(physicalGear => physicalGear.gear.id)
        },
        physicalGears: this._physicalGearsSubject.getValue(),
        programLabel: this.programLabel,
        enableGeolocation: this.enableGeolocation
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    await modal.present();

    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug('[operation-form] Modal result: ', data);

    return (data instanceof Operation) ? data : undefined;
  }

  async onParentOperationChanged(parentOperation?: Operation) {
    parentOperation = parentOperation || this.form.get('parentOperation').value;
    if (this.debug) console.debug('[operation-form] Parent operation changed: ', parentOperation);

    this.onParentChanges.emit(parentOperation);

    // Compute parent operation label
    let parentLabel = '';
    if (isNotNil(parentOperation?.id)) {
      parentLabel = await this.translate.get('TRIP.OPERATION.EDIT.TITLE_NO_RANK', {
        startDateTime: parentOperation.startDateTime && this.dateFormat.transform(parentOperation.startDateTime, {time: true}) as string
      }).toPromise() as string;
    }
    this.$parentOperationLabel.next(parentLabel);

  }

  async addParentOperation(): Promise<Operation> {
    const operation = await this.openSelectOperationModal();

    // User cancelled
    if (!operation) {
      this.parentControl.markAsTouched();
      this.parentControl.markAsDirty();
      this.markForCheck();
      return;
    }

    const metierControl = this.form.get('metier');
    const physicalGearControl = this.form.get('physicalGear');
    const startPositionControl = this.form.get('startPosition');
    const endPositionControl = this.form.get('endPosition');
    const startDateTimeControl = this.form.get('startDateTime');
    const fishingStartDateTimeControl = this.form.get('fishingStartDateTime');

    this.parentControl.setValue(operation)

    if (this._trip.id === operation.tripId) {
      physicalGearControl.patchValue(operation.physicalGear);
      metierControl.patchValue(operation.metier);
    } else {
      const physicalGear = this._physicalGearsSubject.getValue().filter((value) => {
        return value.gear.id === operation.physicalGear.gear.id;
      });

      if (physicalGear.length === 1) {
        physicalGearControl.setValue(physicalGear[0]);
        const metiers = await this.loadMetiers(operation.physicalGear);

        const metier = metiers.filter((value) => {
          return value.id === operation.metier.id;
        });

        if (metier.length === 1) {
          metierControl.patchValue(metier[0]);
        }
      } else if (physicalGear.length === 0) {
        console.warn('[operation-form] no matching physical gear on trip');
      } else {
        console.warn('[operation-form] several matching physical gear on trip');
      }
    }

    this.setPosition(startPositionControl, operation.startPosition);
    this.setPosition(endPositionControl, operation.endPosition);

    startDateTimeControl.patchValue(operation.startDateTime);
    fishingStartDateTimeControl.patchValue(operation.fishingStartDateTime);
    this.form.get('qualityFlagId').patchValue(null);

    this.markAsDirty();

    return operation;
  }

  checkDistanceValidity() {
    if (this.maxDistanceError && this.maxDistanceError > 0 && this.distance > this.maxDistanceError) {
      console.error('Too long distance (> ' + this.maxDistanceError + ') between start and end positions');
      this.setPositionError(true, false);
    } else if (this.maxDistanceWarning && this.maxDistanceWarning > 0 && this.distance > this.maxDistanceWarning) {
      console.warn('Too long distance (> ' + this.maxDistanceWarning + ') between start and end positions');
      this.setPositionError(false, true);
    } else {
      this.setPositionError(false, false);
    }
  }

  toggleMetierFilter($event) {
    if ($event) $event.preventDefault();
    this.enableMetierFilter = !this.enableMetierFilter;
    const physicalGear = this.form.get('physicalGear').value;

    if (physicalGear) {
      // Refresh metiers
      this.loadMetiers(physicalGear);
    }
  }

  async updateParentOperation() {
    //console.debug(this.form.get('parentOperation'));
    const parent = this.parentControl.value;

    if (parent) {
      await this.router.navigateByUrl(`/trips/${parent.tripId}/operation/${parent.id}`);
    }
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
    const enableMetier = hasPhysicalGear && this.form.enabled && isNotEmptyArray(gears) || this.allowParentOperation;
    if (enableMetier) {
      if (metierControl.disabled) metierControl.enable();
    } else {
      if (metierControl.enabled) metierControl.disable();
    }

    if (hasPhysicalGear) {
      // Refresh metiers
      await this.loadMetiers(physicalGear);
    }
  }

  protected async loadMetiers(physicalGear?: PhysicalGear | any): Promise<ReferentialRef[]> {

    // No gears selected: skip
    if (EntityUtils.isEmpty(physicalGear, 'id')) return undefined;

    const gear = physicalGear && physicalGear.gear;
    console.debug('[operation-form] Loading Metier ref items for the gear: ' + (gear && gear.label));

    let res;
    if (this.enableMetierFilter) {
      res = await this.operationService.loadPracticedMetier(0, 100, null, null,
        {
          ...METIER_DEFAULT_FILTER,
          searchJoin: 'TaxonGroup',
          vesselId: this.trip.vesselSnapshot.id,
          startDate: this.startProgram as Moment,
          endDate: moment(),
          programLabel: this.programLabel,
          gearIds: gear && [gear.id],
          levelId: gear && gear.id || undefined
        },
        {
          withTotal: false
        });
    } else {
      res = await this.referentialRefService.loadAll(0, 100, null, null,
        {
          entityName: 'Metier',
          ...METIER_DEFAULT_FILTER,
          searchJoin: 'TaxonGroup',
          levelId: gear && gear.id || undefined
        },
        {
          withTotal: false
        });
    }

    const metiers = res.data;

    if (this.enableMetierFilter && metiers.length === 0) {
      this.toggleMetierFilter(null);
      return;
    }

    const metierControl = this.form.get('metier');

    const metier = metierControl.value;
    if (ReferentialUtils.isNotEmpty(metier)) {
      // Find new reference, by ID
      let updatedMetier = (metiers || []).find(m => m.id === metier.id);

      // If not found : retry using the label (WARN: because of searchJoin, label = taxonGroup.label)
      updatedMetier = updatedMetier || (metiers || []).find(m => m.label === metier.label);

      // Update the metier, if not found (=reset) or ID changed
      if (!updatedMetier || !ReferentialUtils.equals(metier, updatedMetier)) {
        metierControl.patchValue(updatedMetier);
      }
    }
    this._metiersSubject.next(metiers);
    if (metiers.length === 1 && ReferentialUtils.isEmpty(metier)) {
      metierControl.patchValue(metiers[0]);
    }
    return res.data;
  }

  onIsChildOperationChanged(isChildOperation: boolean) {
    isChildOperation = isChildOperation === true;

    if (this.$isChildOperation.value !== isChildOperation){

      this.$isChildOperation.next(isChildOperation);
      console.debug('[operation-form] Is child operation ? ', isChildOperation);

      // Virage
      if (isChildOperation) {

        this.parentControl.enable();

        if (!this.parentControl.value) {
          // Keep filled values
          this.form.get('fishingEndDateTime').patchValue(this.form.get('startDateTime').value);

          // Propage to page, that there is an operation
          setTimeout(() => this.onParentChanges.next(new Operation()), 600);

          // Select a parent (or same if user cancelled)
          this.addParentOperation();
        }
      }

      // Filage or other case
      else {
        this.form.patchValue({
          qualityFlagId: QualityFlagIds.NOT_COMPLETED,
          parentOperation: null
        });
        SharedValidators.clearError(this.parentControl, 'required');
        this.parentControl.disable();
      }

      this.setValidators();
    }
  }

  protected setPosition(positionControl: AbstractControl, position?: VesselPosition) {
    const latitudeControl = positionControl.get('latitude');
    const longitudeControl = positionControl.get('longitude');

    if (isNil(latitudeControl) || isNil(longitudeControl)) {
      console.warn('[operation-form] This control does not contains longitude or latitude field');
      return;
    }
    latitudeControl.patchValue(position && position.latitude || null);
    longitudeControl.patchValue(position && position.longitude || null);
  }

  protected updateDistance() {
    const latitude1 = this.form.get('startPosition').get('latitude').value;
    const longitude1 = this.form.get('startPosition').get('longitude').value;
    const latitude2 = this.form.get('endPosition').get('latitude').value;
    const longitude2 = this.form.get('endPosition').get('longitude').value;

    this.distance = this.operationService.getDistanceBetweenPositions({latitude: latitude1, longitude: longitude1}, {latitude: latitude2, longitude: longitude2});
  }

  protected setPositionError(hasError: boolean, hasWarning: boolean) {
    if (hasError) {
      this.form.get('endPosition').get('longitude').setErrors({tooLong: true});
      this.form.get('endPosition').get('latitude').setErrors({tooLong: true});
      this.form.get('startPosition').get('longitude').setErrors({tooLong: true});
      this.form.get('startPosition').get('latitude').setErrors({tooLong: true});
    } else {
      SharedValidators.clearError(this.form.get('endPosition').get('longitude'), 'tooLong');
      SharedValidators.clearError(this.form.get('endPosition').get('latitude'), 'tooLong');
      SharedValidators.clearError(this.form.get('startPosition').get('longitude'), 'tooLong');
      SharedValidators.clearError(this.form.get('startPosition').get('latitude'), 'tooLong');
    }

    this.distanceError = hasError;
    this.distanceWarning = hasWarning;

    if (this.form.get('endPosition').touched || this.form.get('startPosition').touched) {
      this.form.get('endPosition').markAllAsTouched();
    }
  }

  protected setValidators() {
    // Add validator on date
    let endDateTimeControlName;
    let disabledEndDateTimeControlName;
    const childOperation = this.form.get('childOperation').value;

    if (!this.allowParentOperation || this.isChildOperation) {
      endDateTimeControlName = 'endDateTime';
      disabledEndDateTimeControlName = 'fishingStartDateTime';
    } else {
      endDateTimeControlName = 'fishingStartDateTime';
      disabledEndDateTimeControlName = 'endDateTime';
    }

    // Start date end child operation
    if (this.isChildOperation) {
      //this.parentControl.setValidators(Validators.required);
      this.form.get('fishingEndDateTime').setValidators(Validators.required);
      this.form.get('fishingEndDateTime').setAsyncValidators(async (control) => {
        const fishingEndDateTime = fromDateISOString(control.value);
        const fishingStartDateTime = fromDateISOString((control.parent as FormGroup).get('fishingStartDateTime').value);
        // Error if fishingEndDateTime <= fishingStartDateTime
        if (fishingStartDateTime && fishingEndDateTime?.isSameOrBefore(fishingStartDateTime)) {
          console.warn(`[operation] Invalid operation fishingEndDateTime: before fishingStartDateTime! `, fishingEndDateTime, fishingStartDateTime);
          return <ValidationErrors>{ msg: this.translate.instant('TRIP.OPERATION.ERROR.FIELD_DATE_BEFORE_PARENT_OPERATION') };
        }
        // OK: clear existing errors
        SharedValidators.clearError(control, 'msg');
        return null;
      });
    } else {
      //this.parentControl.clearValidators();
      this.form.get('fishingEndDateTime').clearValidators();
      this.form.get('fishingEndDateTime').clearAsyncValidators();
    }

    this.form.get(disabledEndDateTimeControlName).clearAsyncValidators();
    SharedValidators.clearError(this.form.get(disabledEndDateTimeControlName), 'required');

    this.form.get(endDateTimeControlName).setAsyncValidators(async (control) => {
      if (this.usageMode !== 'FIELD' && !control.value) {
        return <ValidationErrors>{required: true};
      }
      if (!control.touched && !control.dirty) return null;

      const endDateTime = fromDateISOString(control.value);

      // Make sure trip.departureDateTime < operation.endDateTime
      if (endDateTime && this.trip.departureDateTime && this.trip.departureDateTime.isBefore(endDateTime) === false) {
        console.warn(`[operation] Invalid operation ${endDateTimeControlName}: before the trip!`, endDateTime, this.trip.departureDateTime);
        return <ValidationErrors>{msg: this.translate.instant('TRIP.OPERATION.ERROR.FIELD_DATE_BEFORE_TRIP')};
      }
      // Make sure operation.endDateTime < trip.returnDateTime
      else if (endDateTime && this.trip.returnDateTime && endDateTime.isBefore(this.trip.returnDateTime) === false) {
        console.warn(`[operation] Invalid operation ${endDateTimeControlName}: after the trip! `, endDateTime, this.trip.returnDateTime);
        return <ValidationErrors>{msg: this.translate.instant('TRIP.OPERATION.ERROR.FIELD_DATE_AFTER_TRIP')};
      } else if (childOperation != null && endDateTime && endDateTime.isBefore(childOperation.fishingEndDateTime) === false) {
        console.warn(`[operation] Invalid operation ${endDateTimeControlName}: after the child operation's start! `, endDateTime, childOperation.fishingEndDateTime);
        return <ValidationErrors>{msg: this.translate.instant('TRIP.OPERATION.ERROR.FIELD_DATE_AFTER_CHILD_OPERATION')};
      }

      // OK: clear existing errors
      SharedValidators.clearError(control, 'msg');
      SharedValidators.clearError(control, 'required');
      return null;
    });

    this.form.updateValueAndValidity();
    this.markForCheck();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}
