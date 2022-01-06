import { ChangeDetectionStrategy, Component, Injector, OnDestroy, ViewChild } from '@angular/core';

import { TripService } from '../services/trip.service';
import { TripForm } from './trip.form';
import { SaleForm } from '../sale/sale.form';
import { OperationsTable } from '../operation/operations.table';
import { MeasurementsForm } from '../measurement/measurements.form.component';
import { PhysicalGearTable } from '../physicalgear/physical-gears.table';
import * as momentImported from 'moment';
import { AcquisitionLevelCodes, PmfmIds } from '../../referential/services/model/model.enum';
import { AppRootDataEditor } from '../../data/form/root-data-editor.class';
import { FormGroup, Validators } from '@angular/forms';
import {
  Alerts,
  EntitiesStorage,
  EntityServiceLoadOptions,
  fadeInOutAnimation,
  HistoryPageReference,
  isNil,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
  NetworkService,
  PlatformService,
  PromiseEvent,
  ReferentialRef,
  UsageMode,
} from '@sumaris-net/ngx-components';
import { TripsPageSettingsEnum } from './trips.table';
import { PhysicalGear, Trip } from '../services/model/trip.model';
import { SelectPhysicalGearModal } from '../physicalgear/select-physical-gear.modal';
import { ModalController } from '@ionic/angular';
import { PhysicalGearFilter } from '../services/filter/physical-gear.filter';
import { ProgramProperties } from '../../referential/services/config/program.config';
import { VesselSnapshot } from '../../referential/services/model/vessel-snapshot.model';
import { debounceTime, distinctUntilChanged, filter, first, mergeMap, startWith, tap } from 'rxjs/operators';
import { TableElement } from '@e-is/ngx-material-table';
import { Program } from '../../referential/services/model/program.model';
import { environment } from '../../../environments/environment';
import { ProgramRefService } from '@app/referential/services/program-ref.service';
import { TRIP_FEATURE_NAME } from '@app/trip/services/config/trip.config';
import { Subscription } from 'rxjs';
import { OperationService } from '@app/trip/services/operation.service';
import { ContextService } from '@app/shared/context.service';

const moment = momentImported;

const TripPageTabs = {
  GENERAL: 0,
  PHYSICAL_GEARS: 1,
  OPERATIONS: 2
};
export const TripPageSettingsEnum = {
  PAGE_ID: "trip",
  FEATURE_ID: TRIP_FEATURE_NAME
};

@Component({
  selector: 'app-trip-page',
  templateUrl: './trip.page.html',
  styleUrls: ['./trip.page.scss'],
  animations: [fadeInOutAnimation],
  providers: [
    {provide: AppRootDataEditor, useExisting: TripPage}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripPage extends AppRootDataEditor<Trip, TripService> implements OnDestroy {

  readonly acquisitionLevel = AcquisitionLevelCodes.TRIP;
  showSaleForm = false;
  showGearTable = false;
  showOperationTable = false;
  mobile = false;
  forceMeasurementAsOptional = false;
  settingsId: string;
  devAutoFillData = false;
  private _measurementSubscription: Subscription;

  @ViewChild('tripForm', { static: true }) tripForm: TripForm;
  @ViewChild('saleForm', { static: true }) saleForm: SaleForm;
  @ViewChild('physicalGearsTable', { static: true }) physicalGearsTable: PhysicalGearTable;
  @ViewChild('measurementsForm', { static: true }) measurementsForm: MeasurementsForm;
  @ViewChild('operationsTable', { static: true }) operationsTable: OperationsTable;

  get dirty(): boolean {
    // Ignore operation table, when computing dirty state
    return this._dirty || (this.children?.filter(form => form !== this.operationsTable).findIndex(c => c.dirty) !== -1);
  }

  constructor(
    injector: Injector,
    protected entities: EntitiesStorage,
    protected modalCtrl: ModalController,
    protected platform: PlatformService,
    protected programRef: ProgramRefService,
    protected context: ContextService,
    protected operationService: OperationService,
    public network: NetworkService
  ) {
    super(injector,
      Trip,
      injector.get(TripService),
      {
        pathIdAttribute: 'tripId',
        tabCount: 3,
        autoOpenNextTab: !platform.mobile,
        enableListenChanges: true,
        i18nPrefix: 'TRIP.'
      });
    this.defaultBackHref = "/trips";
    this.mobile = platform.mobile;
    this.settingsId = TripPageSettingsEnum.PAGE_ID;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
    this.devAutoFillData = this.debug && (this.settings.getPageSettings(this.settingsId, 'devAutoFillData') == true) || false;
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

    // Cascade refresh to operation tables
    this.registerSubscription(
      this.onUpdateView
        .pipe(
          filter(_ => !this.loading),
          debounceTime(200)
        )
        .subscribe(() => this.operationsTable.onRefresh.emit()));

    // Before delete gears, check if used in operations
    this.registerSubscription(
      this.physicalGearsTable.onBeforeDeleteRows
        .subscribe(async (event) => {
          const rows = (event.detail.rows as TableElement<PhysicalGear>[]);
          const canDelete = await this.operationService.areUsedPhysicalGears(this.data.id,  rows.map(row => row.currentData.id));
          event.detail.success(canDelete);
          if (!canDelete) {
            await Alerts.showError('TRIP.PHYSICAL_GEAR.ERROR.CANNOT_DELETE_USED_GEAR_HELP',
              this.alertCtrl, this.translate, {
                titleKey: 'TRIP.PHYSICAL_GEAR.ERROR.CANNOT_DELETE'
              });
          }
        }));

    // Allow to show operations tab, when add gear
    this.registerSubscription(
      this.physicalGearsTable.onConfirmEditCreateRow
        .subscribe((_) => this.showOperationTable = true));

    if (this.measurementsForm) {
      this.registerSubscription(
        this.measurementsForm.$pmfms
          .pipe(
            debounceTime(400),
            filter(isNotNil),
            mergeMap(_ => this.measurementsForm.ready())
          )
          .subscribe(_ => this.onMeasurementsFormReady())
      );
    }

    // Auto fill form, in DEV mode
    if (!environment.production) {
      this.registerSubscription(
        this.$program
          .pipe(filter(() => this.isNewData && this.devAutoFillData))
          .subscribe(program => this.setTestValue(program))
      );
    }

  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this._measurementSubscription?.unsubscribe();
  }

  protected registerForms() {
    this.addChildForms([
      this.tripForm,
      this.saleForm,
      this.measurementsForm,
      this.physicalGearsTable,
      this.operationsTable
    ]);
  }

  translateControlPath(controlPath: string): string {
    if (controlPath && controlPath.startsWith('measurement')){
      // TODO
      console.warn('[trip] TODO: translate PMFM control name (from the program pmfms ?)', controlPath);
    }
    return super.translateControlPath(controlPath);
  }

  protected async setProgram(program: Program) {
    if (!program) return; // Skip

    if (this.debug) console.debug(`[trip] Program ${program.label} loaded, with properties: `, program.properties);

    // Trip form
    this.tripForm.showObservers = program.getPropertyAsBoolean(ProgramProperties.TRIP_OBSERVERS_ENABLE);
    if (!this.tripForm.showObservers && this.data?.observers) {
      this.data.observers = []; // make sure to reset data observers, if any
    }
    this.tripForm.showMetiers = program.getPropertyAsBoolean(ProgramProperties.TRIP_METIERS_ENABLE);
    if (!this.tripForm.showMetiers && this.data?.metiers) {
      this.data.metiers = []; // make sure to reset data metiers, if any
    }
    this.tripForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.TRIP_LOCATION_LEVEL_IDS);
    this.tripForm.locationSuggestLengthThreshold = program.getPropertyAsInt(ProgramProperties.TRIP_LOCATION_FILTER_MIN_LENGTH);

    // Sale form
    this.showSaleForm = program.getPropertyAsBoolean(ProgramProperties.TRIP_SALE_ENABLE);

    this.physicalGearsTable.canEditRankOrder = program.getPropertyAsBoolean(ProgramProperties.TRIP_PHYSICAL_GEAR_RANK_ORDER_ENABLE);
    this.forceMeasurementAsOptional = this.isOnFieldMode && program.getPropertyAsBoolean(ProgramProperties.TRIP_ON_BOARD_MEASUREMENTS_OPTIONAL);
    const positionEnabled = program.getPropertyAsBoolean(ProgramProperties.TRIP_POSITION_ENABLE);
    this.operationsTable.showPosition = positionEnabled;
    this.operationsTable.showFishingArea = !positionEnabled;
    const allowParentOperation = program.getPropertyAsBoolean(ProgramProperties.TRIP_ALLOW_PARENT_OPERATION);
    this.operationsTable.allowParentOperation = allowParentOperation;
    this.operationsTable.showMap = this.network.online && program.getPropertyAsBoolean(ProgramProperties.TRIP_MAP_ENABLE);

    // Toggle showMap to false, when offline
    if (this.operationsTable.showMap) {
      const subscription = this.network.onNetworkStatusChanges
        .pipe(filter(status => status === "none"))
        .subscribe(status => {
          this.operationsTable.showMap = false;
          this.markForCheck();
          subscription.unsubscribe(); // Remove the subscription (not need anymore)
        });
      this.registerSubscription(subscription);
    }

    // If new data, enable gears tab
    if (this.isNewData) {
      this.showGearTable = true;
    }

    // Disabled operations tab, while no gear
    // But enable anyway, when parent operation allowed
    this.showOperationTable = this.showOperationTable || allowParentOperation;

    this.markAsReady();
    this.markForCheck();
  }

  protected async onNewEntity(data: Trip, options?: EntityServiceLoadOptions): Promise<void> {
    if (this.isOnFieldMode) {
      data.departureDateTime = moment();

      console.debug("[trip] New entity: set default values...");

      // Fill defaults, using filter applied on trips table
      const searchFilter = this.settings.getPageSettings<any>(TripsPageSettingsEnum.PAGE_ID, TripsPageSettingsEnum.FILTER_KEY);
      if (searchFilter) {

        // Synchronization status
        if (searchFilter.synchronizationStatus && searchFilter.synchronizationStatus !== 'SYNC') {
          data.synchronizationStatus = 'DIRTY';
        }

        // program
        if (searchFilter.program && searchFilter.program.label) {
          data.program = ReferentialRef.fromObject(searchFilter.program);
        }

        // Vessel
        if (searchFilter.vesselSnapshot) {
          data.vesselSnapshot = VesselSnapshot.fromObject(searchFilter.vesselSnapshot);
        }

        // Location
        if (searchFilter.location) {
          data.departureLocation = ReferentialRef.fromObject(searchFilter.location);
        }
      }

      // Listen first opening the operations tab, then save
      this.registerSubscription(
        this.tabGroup.selectedTabChange
          .pipe(
            filter(event => this.showOperationTable && event.index === TripPageTabs.OPERATIONS),
            // Save trip when opening the operation tab
            mergeMap(_ => this.save()),
            filter(saved => saved === true),
            first(),
            // If save succeed, propagate the tripId to the table
            tap(_ => this.operationsTable.setTripId(this.data.id))
          )
          .subscribe()
        );
    }

    // Set contextual program, if any
    if (!data.program) {
      const contextualProgram = this.context.getValue('program') as Program;
      if (contextualProgram?.label) {
        data.program = ReferentialRef.fromObject(contextualProgram);
      }
    }

    this.showGearTable = false;
    this.showOperationTable = false;

    // Propagate program
    const programLabel = data.program && data.program.label;
    this.$programLabel.next(programLabel);

    // Enable forms (do not wait for program load)
    if (!programLabel) this.markAsReady();
  }

  protected async onEntityLoaded(data: Trip, options?: EntityServiceLoadOptions): Promise<void> {
    // program
    const programLabel =  data.program?.label;
    if (programLabel) this.$programLabel.next(programLabel);
  }

  updateViewState(data: Trip, opts?: {onlySelf?: boolean, emitEvent?: boolean; }) {
    super.updateViewState(data, opts);

    // Update tabs state (show/hide)
    this.updateTabsState(data);
  }

  updateTabsState(data: Trip) {
    // Enable gears tab if a program has been selected
    this.showGearTable = !this.isNewData || isNotNilOrBlank(this.$programLabel.getValue());

    // Enable operations tab if has gears
    this.showOperationTable = this.showOperationTable || (this.showGearTable && isNotEmptyArray(data.gears));
  }

  protected async setValue(data: Trip) {
    // Set data to form
    const formPromise = this.tripForm.setValue(data);

    this.saleForm.value = data && data.sale;
    this.measurementsForm.value = data && data.measurements || [];

    // Physical gear table
    this.physicalGearsTable.value = data && data.gears || [];
    this.physicalGearsTable.tripId = data.id;

    // Operations table
    const isNew = isNil(data.id);
    if (!isNew && this.operationsTable) {
      this.operationsTable.setTripId(data.id);
    }

    await formPromise;
  }

  async onOpenOperation({id, row}: { id?: number; row: TableElement<any>; }) {

    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      this.markAsLoading();

     setTimeout(async () => {
        await this.router.navigate(['trips', this.data.id, 'operation', id], {
          queryParams: {}
        });

       this.markAsLoaded();
      });
    }
  }

  async onNewOperation(event?: any) {
    const savePromise: Promise<boolean> = this.isOnFieldMode && this.dirty
      // If on field mode: try to save silently
      ? this.save(event)
      // If desktop mode: ask before save
      : this.saveIfDirtyAndConfirm();

    const savedOrContinue = await savePromise;
    if (savedOrContinue) {
      this.markAsLoading();

      setTimeout(async () => {
        await this.router.navigate(['trips', this.data.id, 'operation', 'new'], {
          queryParams: {}
        });
        this.markAsLoaded();
      });
    }
  }

  // For DEV only
  setTestValue(program: Program) {
    const departureDate = moment().startOf('minutes');
    const returnDate = departureDate.clone().add(15, 'day');
    const trip = Trip.fromObject({
      program,
      departureDateTime: departureDate,
      departureLocation: {id: 11, label: 'FRDRZ', name: 'Douarnenez', entityName: 'Location', __typename: 'ReferentialVO'},
      returnDateTime: returnDate,
      returnLocation: {id: 11, label: 'FRDRZ', name: 'Douarnenez', entityName: 'Location', __typename: 'ReferentialVO'},
      vesselSnapshot: {id: 1, vesselId: 1, name: 'Vessel 1', basePortLocation: {id: 11, label: 'FRDRZ', name: 'Douarnenez', __typename: 'ReferentialVO'} , __typename: 'VesselSnapshotVO'},
      measurements: [
        { numericalValue: 1, pmfmId: 21}, // NB fisherman
        { numericalValue: 1, pmfmId: 188} // GPS_USED
      ]
    });

    this.measurementsForm.value = trip.measurements;
    this.form.patchValue(trip);
  }

  devToggleAutoFillData() {
    this.devAutoFillData = !this.devAutoFillData;
    this.settings.savePageSetting(this.settingsId, this.devAutoFillData, 'devAutoFillData');
  }

  devToggleOfflineMode() {
    if (this.network.offline) {
      this.network.setForceOffline(false);
    }
    else {
      this.network.setForceOffline();
    }
  }

  async copyLocally() {
    if (!this.data) return;

    // Copy the trip
    await this.dataService.copyLocallyById(this.data.id, { withOperations: true });

  }

  /**
   * Open a modal to select a previous gear
   * @param event
   */
  async openSelectPreviousGearsModal(event: PromiseEvent<PhysicalGear>) {
    if (!event || !event.detail.success) return; // Skip (missing callback)

    const trip = Trip.fromObject(this.tripForm.value);
    const vessel = trip.vesselSnapshot;
    const date = trip.departureDateTime || trip.returnDateTime;
    if (!vessel || !date) return; // Skip

    const filter = <PhysicalGearFilter>{
      vesselId: vessel.id,
      endDate: date,
      excludeTripId: trip.id,
      startDate: moment().add(-15, 'day')
      // TODO startDate : endDate - 6 month ?
    };
    const modal = await this.modalCtrl.create({
      component: SelectPhysicalGearModal,
      componentProps: {
        filter,
        allowMultiple: false,
        program: this.$programLabel.getValue(),
        acquisitionLevel: this.physicalGearsTable.acquisitionLevel
      }
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();

    console.debug("[trip] Result of select gear modal:", res);
    if (res && res.data && isNotEmptyArray(res.data)) {
      // Cal resolve callback
      event.detail.success(res.data[0]);
    }
    else {
      // User cancelled
      event.detail.error('CANCELLED');
    }
  }

  /* -- protected methods -- */

  protected get form(): FormGroup {
    return this.tripForm.form;
  }

  protected canUserWrite(data: Trip): boolean {
    return isNil(data.validationDate) && this.dataService.canUserWrite(data);
  }

  protected computeUsageMode(data: Trip): UsageMode {
    return this.settings.isUsageMode('FIELD') || data.synchronizationStatus === 'DIRTY'  ? 'FIELD' : 'DESK';
  }

  protected computeNextTabIndex(): number | undefined {
    return super.computeNextTabIndex() || this.selectedTabIndex;
  }

  protected computeTitle(data: Trip): Promise<string> {

    // new data
    if (!data || isNil(data.id)) {
      return this.translate.get('TRIP.NEW.TITLE').toPromise();
    }

    // Existing data
    return this.translate.get('TRIP.EDIT.TITLE', {
      vessel: data.vesselSnapshot && (data.vesselSnapshot.exteriorMarking || data.vesselSnapshot.name),
      departureDateTime: data.departureDateTime && this.dateFormat.transform(data.departureDateTime) as string
    }).toPromise();
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ... (await super.computePageHistory(title)),
      icon: 'boat'
    };
  }

  protected async getJsonValueToSave(): Promise<any> {
    const json = await super.getJsonValueToSave();

    json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    json.measurements = this.measurementsForm.value;

    if (this.physicalGearsTable.dirty) {
      await this.physicalGearsTable.save();
    }
    json.gears = this.physicalGearsTable.value;

    return json;
  }

  protected getFirstInvalidTabIndex(): number {
    const invalidTabs = [
      this.tripForm.invalid || this.measurementsForm.invalid,
      this.showGearTable && this.physicalGearsTable.invalid,
      this.showOperationTable && this.operationsTable.invalid
    ]

    return invalidTabs.findIndex(invalid => invalid === true);
  }

  /**
   * Configure specific behavior
   */
  protected async onMeasurementsFormReady() {

    // Wait program to be loaded
    await this.ready();

    // DEBUG
    //console.debug('[operation-page] Measurement form is ready');

    // Clean existing subscription (e.g. when acquisition level change, this function can= be called many times)
    this._measurementSubscription?.unsubscribe();
    this._measurementSubscription = new Subscription();

    const formGroup = this.measurementsForm.form as FormGroup;

    // If PMFM "Use of a GPS ?" exists, then use to enable/disable positions or fishing area
    const isGPSUsed = formGroup?.controls[PmfmIds.GPS_USED];
    if (isNotNil(isGPSUsed)) {
      isGPSUsed.setValidators(Validators.required);
      this._measurementSubscription.add(
        isGPSUsed.valueChanges
          .pipe(
            debounceTime(400),
            startWith<any, any>(isGPSUsed.value),
            filter(isNotNil),
            distinctUntilChanged()
          )
          .subscribe(isGPSUsed => {

            if (this.debug) console.debug('[trip] Enable/Disable positions or fishing area, because GPS_USED=' + isGPSUsed);

            // Enable positions, when has gps
            this.operationsTable.showPosition = isGPSUsed;
            // Enable fishing area, when has not gps
            this.operationsTable.showFishingArea = !isGPSUsed;

            this.markForCheck();
          })
      );
    }

  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
