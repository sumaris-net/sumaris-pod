import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';

import {fromDateISOString, isNotNil, Landing, Trip} from '../services/trip.model';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {environment} from '../../core/core.module';
import {EditorDataServiceLoadOptions, fadeInOutAnimation, isNil} from '../../shared/shared.module';
import {EntityQualityFormComponent} from "../quality/entity-quality-form.component";
import * as moment from "moment";
import {ProgramProperties} from "../../referential/services/model";
import {AppDataEditorPage} from "../form/data-editor-page.class";
import {FormGroup} from "@angular/forms";
import {NetworkService} from "../../core/services/network.service";
import {LandingService} from "../services/landing.service";
import {TripForm} from "../trip/trip.form";
import {LandedTripService} from "../services/landed-trip.service";
import {OperationGroupTable} from "../operationgroup/operation-groups.table";
import {BehaviorSubject} from "rxjs";
import {MetierRef} from "../../referential/services/model/taxon.model";

@Component({
  selector: 'app-landed-trip-page',
  templateUrl: './landed-trip.page.html',
  styleUrls: ['./landed-trip.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandedTripPage extends AppDataEditorPage<Trip, LandedTripService> implements OnInit {

  landingId: number;
  landing: Landing;

  metiersSubject = new BehaviorSubject<MetierRef[]>(null);

  @ViewChild('tripForm', {static: true}) tripForm: TripForm;

  // @ViewChild('saleForm', {static: true}) saleForm: SaleForm;

  @ViewChild('measurementsForm', {static: true}) measurementsForm: MeasurementsForm;

  @ViewChild('operationGroupTable', { static: true }) operationGroupTable: OperationGroupTable;

  @ViewChild('qualityForm', {static: true}) qualityForm: EntityQualityFormComponent;

  constructor(
    injector: Injector,
    protected landingService: LandingService,
    public network: NetworkService // Used for DEV (to debug OFFLINE mode)
  ) {
    super(injector,
      Trip,
      injector.get(LandedTripService));
    this.idAttribute = 'tripId';


    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.onProgramChanged
        .subscribe(program => {
          if (this.debug) console.debug(`[landedTrip-page] Program ${program.label} loaded, with properties: `, program.properties);
          this.tripForm.showObservers = program.getPropertyAsBoolean(ProgramProperties.TRIP_OBSERVERS_ENABLE);
        })
    );
  }

  async load(id?: number, options?: EditorDataServiceLoadOptions): Promise<void> {

    // get landing id from url
    this.landingId = options && options.landingId && parseInt(options.landingId);
    this.defaultBackHref = `/observations/${options.observedLocationId}`;

    super.load(id, options);
  }

  protected registerFormsAndTables() {
    this.registerForms([this.tripForm, /*this.saleForm,*/ this.measurementsForm]);
    // .registerTables([this.operationTable]);
  }

  protected async onNewEntity(data: Trip, options?: EditorDataServiceLoadOptions): Promise<void> {

    this.landing = await this.loadLandingById(this.landingId);

    // assert this landing is loaded and has no trip
    if (isNil(this.landing)) {
      throw new Error(`The parent landing ${this.landingId} can't be load !`);
    }
    if (isNotNil(this.landing.tripId)) {
      throw new Error(`The parent landing ${this.landingId} has already a trip : ${this.landing.tripId}`);
    }

    // get program from landing
    data.program = this.landing.program;

    if (this.isOnFieldMode) {
      data.departureDateTime = moment();

      // TODO : get the default program from local settings ?
      //data.program = ...
    }

  }

  protected async onEntityLoaded(data: Trip, options?: EditorDataServiceLoadOptions): Promise<void> {

    // load parent landing
    this.loadLandingByTrip(data);

  }

  protected loadLandingByTrip(data: Trip) {

    // Load parent landing
    if (isNotNil(data.id)) {
      console.debug('[landedTrip-page] Loading parent landing...');
      this.registerSubscription(
        this.landingService.watchAll(0, 1, undefined, undefined,
          {tripId: data.id},
          {fetchPolicy: 'cache-first'})
          .subscribe(value => this.landing = value && value.data && value.data[0]));
    } else {
      throw new Error('No parent found in path. landed trip without parent not implemented yet !');
    }
  }

  protected async loadLandingById(landingId: number): Promise<Landing> {

    // Load parent landing
    if (isNotNil(landingId)) {
      console.debug('[landedTrip-page] Loading parent landing...');
      return this.landingService.load(landingId, {fetchPolicy: "cache-first"});
    } else {
      throw new Error('No parent found in path. landed trip without parent not implemented yet !');
    }
  }

  updateViewState(data: Trip) {
    super.updateViewState(data);

    if (this.isNewData) {
      // this.showOperationTable = false;
    } else {
      // this.showOperationTable = true;
    }
  }

  protected async setValue(data: Trip): Promise<void> {

    this.tripForm.value = data;
    const isNew = isNil(data.id);
    if (!isNew) {
      this.programSubject.next(data.program.label);
    }

    // Listen metiers
    this.registerSubscription(
      this.tripForm.form.controls['metiers'].valueChanges.subscribe(value => {
        this.metiersSubject.next(this.tripForm.form.controls['metiers'].value || []);
      })
    );


    // this.saleForm.value = data && data.sale;
    this.measurementsForm.value = data && data.measurements || [];
    //this.measurementsForm.updateControls();



    // Physical gear table
    // this.physicalGearTable.value = data && data.gears || [];

    // Operations table
    if (!isNew && this.operationGroupTable) {
      this.operationGroupTable.setTripId(data.id);
    }
  }

  onOpenOperationGroup({id, row}) {
    this.loading = true;
    this.saveIfDirtyAndConfirm()
      .then((savedOrContinue) => {
        if (savedOrContinue) {
          return this.router.navigateByUrl(`/trips/${this.data.id}/operations/${id}`);
        }
      })
      .then(() => this.loading = false);
  }

  async onNewOperationGroup(event?: any) {
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
      } finally {
        this.loading = false;
        this.markForCheck();
      }
    }
  }

  // For DEV only
  devFillFakeTrip(event?: UIEvent) {
    const trip = {
      program: {
        id: 10,
        label: 'ADAP-MER',
        name: 'Application d’assistance à l’auto-échantillonnage en mer',
        __typename: 'ProgramVO'
      },
      departureDateTime: fromDateISOString('2019-01-01T12:00:00.000Z'),
      departureLocation: {
        id: 11,
        label: 'FRDRZ',
        name: 'Douarnenez',
        entityName: 'Location',
        __typename: 'ReferentialVO'
      },
      returnDateTime: fromDateISOString('2019-01-05T12:00:00.000Z'),
      returnLocation: {id: 11, label: 'FRDRZ', name: 'Douarnenez', entityName: 'Location', __typename: 'ReferentialVO'},
      vesselSnapshot: {
        id: 1,
        vesselId: 1,
        name: 'Vessel 1',
        basePortLocation: {id: 11, label: 'FRDRZ', name: 'Douarnenez', __typename: 'ReferentialVO'},
        __typename: 'VesselSnapshotVO'
      },
      measurements: [
        {numericalValue: '1', pmfmId: 21}
      ]
    };

    this.form.patchValue(trip);
  }

  devToggleOfflineMode(event?: UIEvent) {
    if (this.network.offline) {
      this.network.setConnectionType('unknown');
    } else {
      this.network.setConnectionType('none');
    }
  }

  /* -- protected methods -- */

  protected get form(): FormGroup {
    return this.tripForm.form;
  }

  protected canUserWrite(data: Trip): boolean {
    return isNil(data.validationDate) && this.dataService.canUserWrite(data);
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

    // json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    json.measurements = this.measurementsForm.value;

    // if (this.physicalGearTable.dirty) {
    //   await this.physicalGearTable.save();
    // }
    // json.gears = this.physicalGearTable.value;

    return json;
  }

  /**
   * Get the first invalid tab
   */
  protected getFirstInvalidTabIndex(): number {

    // TODO
    return 0;

    // const tab0Invalid = this.tripForm.invalid || this.measurementsForm.invalid;
    // const tab1Invalid = !tab0Invalid && this.physicalGearTable.invalid;
    // const tab2Invalid = !tab1Invalid && this.operationTable.invalid;

    // return tab0Invalid ? 0 : (tab1Invalid ? 1 : (tab2Invalid ? 2 : this.selectedTabIndex));
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
