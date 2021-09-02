import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, Optional} from '@angular/core';
import {OperationValidatorService} from '../services/validator/operation.validator';
import * as momentImported from 'moment';
import {Moment} from 'moment';
import {
  AccountService,
  AppForm,
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
  UsageMode
} from '@sumaris-net/ngx-components';
import {AbstractControl, FormGroup, RequiredValidator, ValidationErrors, Validators} from '@angular/forms';
import {TranslateService} from '@ngx-translate/core';
import {defaultOperationTypesList, Operation, OperationType, PhysicalGear, Trip, VesselPosition} from '../services/model/trip.model';
import {BehaviorSubject, merge, Subject} from 'rxjs';
import {distinctUntilChanged} from 'rxjs/operators';
import {METIER_DEFAULT_FILTER, MetierService} from '@app/referential/services/metier.service';
import {ReferentialRefService} from '@app/referential/services/referential-ref.service';
import {Geolocation} from '@ionic-native/geolocation/ngx';
import {OperationService} from '@app/trip/services/operation.service';
import {ModalController} from '@ionic/angular';
import {SelectOperationModal} from '@app/trip/operation/select-operation.modal';
import {Program} from '@app/referential/services/model/program.model';
import {ProgramProperties} from '@app/referential/services/config/program.config';
import {AcquisitionLevelCodes, QualityFlagIds} from '@app/referential/services/model/model.enum';


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
  private _operationTypesList = defaultOperationTypesList;

  $program = new Subject<Program>();
  operationTypeById: { [id: number]: OperationType; };

  startProgram: Date | Moment;
  enableGeolocation: boolean;
  latLongFormat: string;
  hasChildOperation = false;
  parentOperationLabel: string;
  mobile: boolean;
  distance: number;
  $useLinkedOperation = new BehaviorSubject<boolean>(false);
  operationId: number;
  maxDistanceWarning: number;
  maxDistanceError: number;
  typeOperation = 0;
  distanceError: boolean;
  distanceWarning: boolean;
  enableMetierFilter = false;
  showMetierFilter = false;

  @Input() showComment = true;
  @Input() showError = true;

  @Input() usageMode: UsageMode;

  @Input() defaultLatitudeSign: '+' | '-';
  @Input() defaultLongitudeSign: '+' | '-';
  @Input() programLabel: string;
  @Input() acquisitionLevel: BehaviorSubject<string>;

  @Input()
  set operationTypesList(values: OperationType[]) {
    this._operationTypesList = values;

    // Fill statusById
    this.operationTypeById = {};
    this.operationTypesList.forEach((operationType) => this.operationTypeById[operationType.id] = operationType);
  }

  get operationTypesList(): OperationType[] {
    return this._operationTypesList;
  }

  get trip(): Trip {
    return this._trip;
  }

  set trip(value: Trip) {
    this.setTrip(value);
  }

  constructor(
    protected dateFormat: DateFormatPipe,
    protected validatorService: OperationValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected operationService: OperationService,
    protected metierService: MetierService,
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

    this.registerSubscription(
      this.$useLinkedOperation.subscribe((value) => {
        if (value === true) {
          this.onOperationTypeChanged(this.form.get('operationTypeId').value);
          this.setParentOperationLabel(this.form.get('parentOperation').value);
        }
      })
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

  ngAfterViewInit() {
    this.$program.subscribe((value) => {
        this.programLabel = value.label;
        this.startProgram = value.creationDate;
        if (value.getProperty(ProgramProperties.TRIP_OPERATION_LINKED) === 'true') {
          this.$useLinkedOperation.next(true);
          // Fill OperationTypeById
          if (this._operationTypesList && !this.operationTypeById) {
            this.operationTypeById = {};
            this._operationTypesList.forEach((operationType) => this.operationTypeById[operationType.id] = operationType);
          }

          // Listen operation Type
          this.registerSubscription(
            this.form.get('operationTypeId').valueChanges
              .pipe(
                distinctUntilChanged((o1, o2) => EntityUtils.equals(o1, o2, 'id'))
              )
              .subscribe((operationType) => this.onOperationTypeChanged(operationType))
          );
          this.registerSubscription(
            this.form.get('parentOperation').valueChanges
              .subscribe((res) => this.setParentOperationLabel(res))
          );
        } else {
          this.$useLinkedOperation.next(false);
        }

        this.showMetierFilter = value.getPropertyAsBoolean(ProgramProperties.TRIP_FILTER_METIER);
        if (this.showMetierFilter !== this.enableMetierFilter) {
          this.toggleMetierFilter(null);
        }
      }
    );
  }

  setValue(data: Operation, opts?: { emitEvent?: boolean; onlySelf?: boolean; }) {
    // Use label and name from metier.taxonGroup
    if (data && data.metier) {
      data.metier = data.metier.clone(); // Leave original object unchanged
      data.metier.label = data.metier.taxonGroup && data.metier.taxonGroup.label || data.metier.label;
      data.metier.name = data.metier.taxonGroup && data.metier.taxonGroup.name || data.metier.name;
    }
    this.operationId = data.id;
    super.setValue(data, opts);
  }

  setTrip(trip: Trip) {
    this._trip = trip;

    if (trip) {
      // Propagate physical gears
      this._physicalGearsSubject.next((trip.gears || []).map(ps => ps.clone()));

      // Use trip physical gear Object (if possible)
      const physicalGearControl = this.form.get('physicalGear');
      let physicalGear = physicalGearControl.value;
      if (physicalGear && isNotNil(physicalGear.id)) {
        physicalGear = (trip.gears || []).find(g => g.id === physicalGear.id) || physicalGear;
        if (physicalGear) physicalGearControl.patchValue(physicalGear);
      }

      this.setValidators();
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
      const endDateTimeControlName = !this.$useLinkedOperation.getValue() || (this.typeOperation && this.typeOperation === 1) ? 'endDateTime' : 'fishingStartDateTime';
      this.form.get(endDateTimeControlName).setValue(moment(), {emitEvent: false, onlySelf: true});
    }
    this.form.markAsDirty({onlySelf: true});
    this.updateDistance();
    this.form.updateValueAndValidity();
    this.markForCheck();
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

    const endDate = this.form.get('fishingEndDateTime').value || this.trip.returnDateTime;
    const parentOperation = this.form.get('parentOperation').value;

    const modal = await this.modalCtrl.create({
      component: SelectOperationModal,
      componentProps: {
        filter: {
          programLabel: this.programLabel,
          vesselId: this._trip.vesselSnapshot.id,
          excludedIds: isNotNil(this.operationId) ? [this.operationId] : null,
          excludeChildOperation: true,
          hasNoChildOperation: true,
          endDate: endDate,
          startDate: this._trip.departureDateTime.add(-15, 'day'),
          includedIds: isNotNil(parentOperation) ? [parentOperation.id] : null
        },
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

  async setParentOperationLabel(operation: Operation) {
    console.debug('[operation-form] Update parent operation label');
    const parentOperationLabelControl = this.form.get('parentOperationLabel');
    if (operation) {
      this.parentOperationLabel = await this.translate.get('TRIP.OPERATION.EDIT.TITLE_NO_RANK', {
        startDateTime: operation.startDateTime && this.dateFormat.transform(operation.startDateTime, {time: true}) as string
      }).toPromise() as string;
    } else {
      this.parentOperationLabel = '';
    }
    parentOperationLabelControl.patchValue(this.parentOperationLabel);
  }

  async addParentOperation() {
    const operation = await this.openSelectOperationModal();

    if (isNotNil(operation)) {
      const metierControl = this.form.get('metier');
      const physicalGearControl = this.form.get('physicalGear');
      const startPositionControl = this.form.get('startPosition');
      const endPositionControl = this.form.get('endPosition');
      const parentOperationControl = this.form.get('parentOperation');
      const startDateTimeControl = this.form.get('startDateTime');
      const fishingStartDateTimeControl = this.form.get('fishingStartDateTime');

      parentOperationControl.patchValue(operation);
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

      this.form.get('fishingEndDateTime').setAsyncValidators(async (control) => {
        const fishingEndDateTime = fromDateISOString(control.value);

        // Make sure fishingEndDateTime > fishingStartDateTime
        if (fishingEndDateTime && operation.fishingStartDateTime.isBefore(fishingEndDateTime) === false) {
          console.warn(`[operation] Invalid operation fishingEndDateTime: before fishingStartDateTime! `, fishingEndDateTime, operation.fishingStartDateTime);
          return <ValidationErrors>{msg: this.translate.instant('TRIP.OPERATION.ERROR.FIELD_DATE_BEFORE_PARENT_OPERATION')};
        }
        // OK: clear existing errors
        SharedValidators.clearError(control, 'msg');
        return null;
      });
      this.markAsDirty();
    }
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
    const enableMetier = hasPhysicalGear && this.form.enabled && isNotEmptyArray(gears) || this.$useLinkedOperation.getValue();
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
      res = await this.metierService.loadAll(0, 100, null, null,
        {
          entityName: 'Metier',
          ...METIER_DEFAULT_FILTER,
          vesselId: this.trip.vesselSnapshot.id,
          startDate: this.startProgram as Moment,
          endDate: moment(),
          searchJoin: 'TaxonGroup',
          programLabel: this.programLabel,
          gearIds: [gear.id],
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

  protected onOperationTypeChanged(operationType) {
    const qualityFlagIdControl = this.form.get('qualityFlagId');
    this.typeOperation = operationType;

    console.debug('[operation-form] type operation has changed', operationType);

    if (operationType === 0 && this.form.get('parentOperation').value && this.form.get('operationTypeId').pristine) {
      this.form.get('operationTypeId').patchValue(1);
      return;
    }
    //virage
    else if (operationType === 1) {
      this.hasChildOperation = true;
      this.acquisitionLevel.next(AcquisitionLevelCodes.CHILD_OPERATION);

      qualityFlagIdControl.patchValue(null);

      if (!this.form.get('parentOperation').value) {
        this.form.get('fishingEndDateTime').patchValue(this.form.get('startDateTime').value);
        this.form.get('startDateTime').patchValue(null);
      }
    }
    //filage
    else {
      this.hasChildOperation = false;
      this.acquisitionLevel.next(AcquisitionLevelCodes.OPERATION);

      qualityFlagIdControl.patchValue(QualityFlagIds.NOT_COMPLETED);

      this.form.get('parentOperation').patchValue(null);
    }
    this.setValidators();
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
    // Add validator on trip date

    let endDateTimeControlName; //
    let disabledEndDateTimeControlName;

    if (!(this.$useLinkedOperation.getValue()) || this.typeOperation === 1) {
      endDateTimeControlName = 'endDateTime';
      disabledEndDateTimeControlName = 'fishingStartDateTime';
    } else {
      endDateTimeControlName = 'fishingStartDateTime';
      disabledEndDateTimeControlName = 'endDateTime';
    }

    //Start date end child operation
    if (this.typeOperation === 1) {
      this.form.get('fishingEndDateTime').setValidators(Validators.required);
      this.form.get('parentOperationLabel').setValidators(Validators.required);
    } else {
      this.form.get('parentOperationLabel').clearValidators();
      if (this.$useLinkedOperation.getValue()) {
        this.form.get('fishingEndDateTime').clearValidators();
      }
    }

    this.form.get(disabledEndDateTimeControlName).clearAsyncValidators();
    SharedValidators.clearError(this.form.get(disabledEndDateTimeControlName), 'required');

    this.form.get(endDateTimeControlName).setAsyncValidators(async (control) => {
      if (this.usageMode !== 'FIELD' && !control.value) {
        return <ValidationErrors>{required: true};
      }
      if (!control.touched) return null;

      const endDateTime = fromDateISOString(control.value);

      // Make sure trip.departureDateTime < operation.endDateTime
      if (endDateTime && this.trip.departureDateTime && this.trip.departureDateTime.isBefore(endDateTime) === false) {
        console.warn(`[operation] Invalid operation ` + endDateTimeControlName + ` : before the trip! `, endDateTime, this.trip.departureDateTime);
        return <ValidationErrors>{msg: this.translate.instant('TRIP.OPERATION.ERROR.FIELD_DATE_BEFORE_TRIP')};
      }
      // Make sure operation.endDateTime < trip.returnDateTime
      else if (endDateTime && this.trip.returnDateTime && endDateTime.isBefore(this.trip.returnDateTime) === false) {
        console.warn(`[operation] Invalid operation ` + endDateTimeControlName + `: after the trip! `, endDateTime, this.trip.returnDateTime);
        return <ValidationErrors>{msg: this.translate.instant('TRIP.OPERATION.ERROR.FIELD_DATE_AFTER_TRIP')};
      }

      // OK: clear existing errors
      SharedValidators.clearError(control, 'msg');
      SharedValidators.clearError(control, 'required');
      return null;
    });
    this.form.updateValueAndValidity();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}
