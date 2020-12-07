import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';

import {TripService} from '../services/trip.service';
import {TripForm} from './trip.form';
import {SaleForm} from '../sale/sale.form';
import {OperationsTable} from '../operation/operations.table';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {environment, fromDateISOString, ReferentialRef} from '../../core/core.module';
import {PhysicalGearTable} from '../physicalgear/physical-gears.table';
import {EntityServiceLoadOptions, fadeInOutAnimation, isNil, isNotEmptyArray} from '../../shared/shared.module';
import * as moment from "moment";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {AppRootDataEditor} from "../../data/form/root-data-editor.class";
import {FormGroup} from "@angular/forms";
import {NetworkService} from "../../core/services/network.service";
import {TripsPageSettingsEnum} from "./trips.table";
import {EntitiesStorage} from "../../core/services/storage/entities-storage.service";
import {HistoryPageReference, UsageMode} from "../../core/services/model/settings.model";
import {PhysicalGear, Trip} from "../services/model/trip.model";
import {SelectPhysicalGearModal} from "../physicalgear/select-physical-gear.modal";
import {ModalController} from "@ionic/angular";
import {PhysicalGearFilter} from "../services/physicalgear.service";
import {PromiseEvent} from "../../shared/events";
import {ProgramProperties} from "../../referential/services/config/program.config";
import {VesselSnapshot} from "../../referential/services/model/vessel-snapshot.model";
import {PlatformService} from "../../core/services/platform.service";
import {filter} from "rxjs/operators";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {TableElement} from "@e-is/ngx-material-table";
import {Alerts} from "../../shared/alerts";
import {AddToPageHistoryOptions} from "../../core/services/local-settings.service";

const TripPageTabs = {
  GENERAL: 0,
  PHYSICAL_GEARS: 1,
  OPERATIONS: 2
}

@Component({
  selector: 'app-trip-page',
  templateUrl: './trip.page.html',
  styleUrls: ['./trip.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripPage extends AppRootDataEditor<Trip, TripService> implements OnInit {

  readonly acquisitionLevel = AcquisitionLevelCodes.TRIP;
  showSaleForm = false;
  showGearTable = false;
  showOperationTable = false;
  mobile = false;
  forceMeasurementAsOptional = false;

  @ViewChild('tripForm', { static: true }) tripForm: TripForm;

  @ViewChild('saleForm', { static: true }) saleForm: SaleForm;

  @ViewChild('physicalGearsTable', { static: true }) physicalGearTable: PhysicalGearTable;

  @ViewChild('measurementsForm', { static: true }) measurementsForm: MeasurementsForm;

  @ViewChild('operationsTable', { static: true }) operationTable: OperationsTable;

  constructor(
    injector: Injector,
    protected entities: EntitiesStorage,
    protected modalCtrl: ModalController,
    protected platform: PlatformService,
    public network: NetworkService // Used for DEV (to debug OFFLINE mode)
  ) {
    super(injector,
      Trip,
      injector.get(TripService),
      {
        pathIdAttribute: 'tripId',
        tabCount: 3,
        autoOpenNextTab: !platform.mobile
      });
    this.defaultBackHref = "/trips";
    this.mobile = platform.mobile;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.onProgramChanged
        .subscribe(program => {
          if (this.debug) console.debug(`[trip] Program ${program.label} loaded, with properties: `, program.properties);
          this.showSaleForm = program.getPropertyAsBoolean(ProgramProperties.TRIP_SALE_ENABLE);
          this.tripForm.showObservers = program.getPropertyAsBoolean(ProgramProperties.TRIP_OBSERVERS_ENABLE);
          if (!this.tripForm.showObservers) {
            this.data.observers = []; // make sure to reset data observers, if any
          }
          this.tripForm.showMetiers = program.getPropertyAsBoolean(ProgramProperties.TRIP_METIERS_ENABLE);
          if (!this.tripForm.showMetiers) {
            this.data.metiers = []; // make sure to reset data metiers, if any
          }
          this.physicalGearTable.canEditRankOrder = program.getPropertyAsBoolean(ProgramProperties.TRIP_PHYSICAL_GEAR_RANK_ORDER_ENABLE);
          this.forceMeasurementAsOptional = this.isOnFieldMode && program.getPropertyAsBoolean(ProgramProperties.TRIP_ON_BOARD_MEASUREMENTS_OPTIONAL);
          this.operationTable.showMap = this.network.online && program.getPropertyAsBoolean(ProgramProperties.TRIP_MAP_ENABLE);

          if (this.isNewData) {
            // If new data, enable gears tab
            this.showGearTable = true;
            // BUT leave operation gear have been filled
            this.showOperationTable = false;
          }
        })
    );

    // Cascade refresh to operation tables
    this.onUpdateView.subscribe(() => this.operationTable.onRefresh.emit());

    this.registerSubscription(
      this.physicalGearTable.onBeforeDeleteRows
        .subscribe(async (event) => {
          const rows = (event.detail.rows as TableElement<PhysicalGear>[]);
          const usedGearIds = await this.operationTable.getUsedPhysicalGearIds();
          const usedGears = rows.map(row => row.currentData)
            .filter(gear => usedGearIds.includes(gear.id));

          const canDelete = (usedGears.length == 0);
          event.detail.success(canDelete);
          if (!canDelete) {
            await Alerts.showError('TRIP.PHYSICAL_GEAR.ERROR.CANNOT_DELETE_USED_GEAR_HELP',
              this.alertCtrl, this.translate, {
                titleKey: 'TRIP.PHYSICAL_GEAR.ERROR.CANNOT_DELETE'
              });
          }
        }));

    this.registerSubscription(
      this.physicalGearTable.onConfirmEditCreateRow
        .subscribe((_) => this.showOperationTable = true));
  }

  protected registerForms() {
    this.addChildForms([
      this.tripForm, this.saleForm, this.measurementsForm,
      this.physicalGearTable, this.operationTable
    ]);
  }

  protected async onNewEntity(data: Trip, options?: EntityServiceLoadOptions): Promise<void> {
    if (this.isOnFieldMode) {
      data.departureDateTime = moment();

      console.debug("[trip] New entity: settings defaults...");

      // Fil defaults, using filter applied on trips table
      const tripFilter = this.settings.getPageSettings<any>(TripsPageSettingsEnum.PAGE_ID, TripsPageSettingsEnum.FILTER_KEY);
      if (tripFilter) {

        // Synchronization status
        if (tripFilter.synchronizationStatus && tripFilter.synchronizationStatus !== 'SYNC') {
          data.synchronizationStatus = 'DIRTY';
        }

        // program
        if (tripFilter.program && tripFilter.program.label) {
          data.program = ReferentialRef.fromObject(tripFilter.program);
          this.programSubject.next(data.program.label);
        }

        // Vessel
        if (tripFilter.vesselSnapshot) {
          data.vesselSnapshot = VesselSnapshot.fromObject(tripFilter.vesselSnapshot);
        }

        // Location
        if (tripFilter.location) {
          data.departureLocation = ReferentialRef.fromObject(tripFilter.location);
        }
      }
    }

    // If on field mode
    if (this.isOnFieldMode) {
      // Listen first opening the operations tab, then save
      this.tabGroup.selectedTabChange
        .pipe(
          filter(event => event.index === TripPageTabs.OPERATIONS)
        )
        .subscribe(event => this.save());
    }

    this.showGearTable = false;
    this.showOperationTable = false;
  }

  updateViewState(data: Trip) {
    super.updateViewState(data);

    // Update tabs state (show/hide)
    this.updateTabsState(data);
  }

  updateTabsState(data: Trip) {
    // Enable gears tab if a program has been selected
    this.showGearTable = !this.isNewData || ReferentialUtils.isNotEmpty(this.programSubject.getValue());

    // ENable operation tab if has gears
    this.showOperationTable = this.showGearTable && isNotEmptyArray(data.gears);
  }

  protected async setValue(data: Trip): Promise<void> {

    this.tripForm.value = data;
    const isNew = isNil(data.id);
    if (!isNew) {
      this.programSubject.next(data.program.label);
    }
    this.saleForm.value = data && data.sale;
    this.measurementsForm.value = data && data.measurements || [];
    //this.measurementsForm.updateControls();

    // Physical gear table
    this.physicalGearTable.value = data && data.gears || [];

    // Operations table
    if (!isNew && this.operationTable) {
      this.operationTable.setTripId(data.id, {emitEvent: false});
    }
  }

  async onOpenOperation({id, row}) {

    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      this.loading = true;
      try {
        await this.router.navigateByUrl(`/trips/${this.data.id}/operations/${id}`);
      }
      finally {
        this.loading = false;
      }
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
      this.loading = true;
      this.markForCheck();
      try {
        await this.router.navigateByUrl(`/trips/${this.data.id}/operations/new`);
      }
      finally {
        this.loading = false;
        this.markForCheck();
      }
    }
  }

  // For DEV only
  devFillFakeTrip(event?: UIEvent) {
    const trip = {
      program: {id: 10, label: 'ADAP-MER', name: 'Application d’assistance à l’auto-échantillonnage en mer', __typename: 'ProgramVO'},
      departureDateTime: fromDateISOString('2019-01-01T12:00:00.000Z'),
      departureLocation: {id: 11, label: 'FRDRZ', name: 'Douarnenez', entityName: 'Location', __typename: 'ReferentialVO'},
      returnDateTime: fromDateISOString('2019-01-05T12:00:00.000Z'),
      returnLocation: {id: 11, label: 'FRDRZ', name: 'Douarnenez', entityName: 'Location', __typename: 'ReferentialVO'},
      vesselSnapshot: {id: 1, vesselId: 1, name: 'Vessel 1', basePortLocation: {id: 11, label: 'FRDRZ', name: 'Douarnenez', __typename: 'ReferentialVO'} , __typename: 'VesselSnapshotVO'},
      measurements: [
        { numericalValue: '1', pmfmId: 21}
      ]
    };

    this.form.patchValue(trip);
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
      excludeTripId: trip.id
      // TODO startDate : endDate - 6 month ?
    };
    const modal = await this.modalCtrl.create({
      component: SelectPhysicalGearModal,
      componentProps: {
        filter,
        allowMultiple: false,
        program: this.programSubject.getValue(),
        acquisitionLevel: this.physicalGearTable.acquisitionLevel
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

  /**
   * Compute the title
   * @param data
   */
  protected async computeTitle(data: Trip) {

    // new data
    if (!data || isNil(data.id)) {
      return await this.translate.get('TRIP.NEW.TITLE').toPromise();
    }

    // Existing data
    return await this.translate.get('TRIP.EDIT.TITLE', {
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

    if (this.physicalGearTable.dirty) {
      await this.physicalGearTable.save();
    }
    json.gears = this.physicalGearTable.value;

    return json;
  }

  /**
   * Get the first invalid tab
   */
  protected getFirstInvalidTabIndex(): number {
    const tab0Invalid = this.tripForm.invalid || this.measurementsForm.invalid;
    const tab1Invalid = !tab0Invalid && this.physicalGearTable.invalid;
    const tab2Invalid = !tab1Invalid && this.operationTable.invalid;

    return tab0Invalid ? 0 : (tab1Invalid ? 1 : (tab2Invalid ? 2 : this.selectedTabIndex));
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
