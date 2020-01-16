import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';

import {TripService} from '../services/trip.service';
import {TripForm} from './trip.form';
import {fromDateISOString, ReferentialRef, Trip, VesselSnapshot} from '../services/trip.model';
import {SaleForm} from '../sale/sale.form';
import {OperationTable} from '../operation/operations.table';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {AppFormUtils, environment} from '../../core/core.module';
import {PhysicalGearTable} from '../physicalgear/physicalgears.table';
import {EditorDataServiceLoadOptions, fadeInOutAnimation, isNil} from '../../shared/shared.module';
import {EntityQualityFormComponent} from "../quality/entity-quality-form.component";
import * as moment from "moment";
import {ProgramProperties} from "../../referential/services/model";
import {AppDataEditorPage} from "../form/data-editor-page.class";
import {FormGroup} from "@angular/forms";
import {NetworkService} from "../../core/services/network.service";
import {TripsPageSettingsEnum} from "./trips.page";
import {EntityStorage} from "../../core/services/entities-storage.service";
import {DataQualityService} from "../services/base.service";
import {HistoryPageReference, UsageMode} from "../../core/services/model";
import {TripValidatorService} from "../services/trip.validator";

@Component({
  selector: 'app-trip-page',
  templateUrl: './trip.page.html',
  styleUrls: ['./trip.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripPage extends AppDataEditorPage<Trip, TripService> implements OnInit {


  showSaleForm = false;
  showGearTable = false;
  showOperationTable = false;

  @ViewChild('tripForm', { static: true }) tripForm: TripForm;

  @ViewChild('saleForm', { static: true }) saleForm: SaleForm;

  @ViewChild('physicalGearTable', { static: true }) physicalGearTable: PhysicalGearTable;

  @ViewChild('measurementsForm', { static: true }) measurementsForm: MeasurementsForm;

  @ViewChild('operationTable', { static: true }) operationTable: OperationTable;

  constructor(
    injector: Injector,
    protected entities: EntityStorage,
    protected tripValidatorService: TripValidatorService,
    public network: NetworkService // Used for DEV (to debug OFFLINE mode)
  ) {
    super(injector,
      Trip,
      injector.get(TripService));
    this.idAttribute = 'tripId';
    this.defaultBackHref = "/trips";

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
        })
    );

    // Cascade refresh to operation tables
    this.onUpdateView.subscribe(() => this.operationTable.onRefresh.emit());
  }

  protected registerFormsAndTables() {
    this.registerForms([this.tripForm, this.saleForm, this.measurementsForm])
      .registerTables([this.physicalGearTable, this.operationTable]);
  }

  protected async onNewEntity(data: Trip, options?: EditorDataServiceLoadOptions): Promise<void> {
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

    this.showGearTable = false;
    this.showOperationTable = false;

  }

  updateViewState(data: Trip) {
    super.updateViewState(data);

    if (this.isNewData) {
      this.showGearTable = false;
      this.showOperationTable = false;
    }
    else {
      this.showGearTable = true;
      this.showOperationTable = true;
    }
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
      this.operationTable.setTrip(data);
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

  async devDownloadToLocal() {
    if (!this.data) return;

    // Copy the trip
    await (this.dataService as TripService).downloadToLocal(this.data.id, { withOperations: true });

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

  protected addToPageHistory(page: HistoryPageReference) {
    super.addToPageHistory({ ...page, icon: 'boat'});
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
