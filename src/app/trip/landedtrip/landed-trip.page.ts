import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';

import {isNotNil, Landing, ObservedLocation, PhysicalGear, Trip} from '../services/trip.model';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {environment} from '../../core/core.module';
import {
  EditorDataServiceLoadOptions,
  fadeInOutAnimation,
  isNil,
  isNotEmptyArray,
  isNotNilOrBlank
} from '../../shared/shared.module';
import * as moment from "moment";
import {AcquisitionLevelCodes, ProgramProperties} from "../../referential/services/model";
import {AppDataEditorPage} from "../form/data-editor-page.class";
import {FormBuilder, FormGroup} from "@angular/forms";
import {NetworkService} from "../../core/services/network.service";
import {LandingService} from "../services/landing.service";
import {TripForm} from "../trip/trip.form";
import {BehaviorSubject} from "rxjs";
import {MetierRef} from "../../referential/services/model/taxon.model";
import {TripService, TripServiceSaveOption} from "../services/trip.service";
import {HistoryPageReference, UsageMode} from "../../core/services/model";
import {EntityStorage} from "../../core/services/entities-storage.service";
import {ObservedLocationService} from "../services/observed-location.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {isEmptyArray} from "../../shared/functions";
import {OperationGroupTable} from "../operationgroup/operation-groups.table";
import {OperationGroup} from "../services/model/trip.model";
import {MatAutocompleteConfigHolder, MatAutocompleteFieldConfig} from "../../shared/material/material.autocomplete";
import {MatTabChangeEvent, MatTabGroup} from "@angular/material";
import {ProductsTable} from "../product/products.table";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {Product} from "../services/model/product.model";
import {arraysAreEqual} from "tslint/lib/utils";

@Component({
  selector: 'app-landed-trip-page',
  templateUrl: './landed-trip.page.html',
  styleUrls: ['./landed-trip.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandedTripPage extends AppDataEditorPage<Trip, TripService> implements OnInit {

  readonly acquisitionLevel = AcquisitionLevelCodes.TRIP;
  observedLocationId: number;
  landingId: number;
  landing: Landing;

  selectedCatchTabIndex = 0;
  selectedSaleTabIndex = 0;

  showOperationGroupTab = false;
  showCatchTab = false;
  showSaleTab = false;
  showExpenseTab = false;

  autocompleteHelper: MatAutocompleteConfigHolder;
  autocompleteFields: {[key: string]: MatAutocompleteFieldConfig};

  // List of trip's metier, used to populate operation group's metier combobox
  $metiers = new BehaviorSubject<MetierRef[]>(null);

  // List of trip's operation groups, use to populate product filter
  $operationGroups = new BehaviorSubject<OperationGroup[]>(null);
  catchFilterForm: FormGroup;

  operationGroupAttributes = ['metier.label', 'metier.name'];

  @ViewChild('tripForm', {static: true}) tripForm: TripForm;
  @ViewChild('measurementsForm', {static: true}) measurementsForm: MeasurementsForm;
  @ViewChild('operationGroupTable', { static: true }) operationGroupTable: OperationGroupTable;
  @ViewChild('catchTabGroup', {static: true}) catchTabGroup: MatTabGroup;
  @ViewChild('productsTable', {static: true}) productsTable: ProductsTable;


  constructor(
    injector: Injector,
    protected entities: EntityStorage,
    protected landingService: LandingService,
    protected observedLocationService: ObservedLocationService,
    protected vesselService: VesselSnapshotService,
    public network: NetworkService, // Used for DEV (to debug OFFLINE mode)
    protected formBuilder: FormBuilder
  ) {
    super(injector,
      Trip,
      injector.get(TripService));
    this.idAttribute = 'tripId';
    // this.defaultBackHref = "/trips";

    this.autocompleteHelper = new MatAutocompleteConfigHolder(this.settings && {
      getUserAttributes: (a, b) => this.settings.getFieldDisplayAttributes(a, b)
    });
    this.autocompleteFields = this.autocompleteHelper.fields;

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.onProgramChanged
        .subscribe(program => {
          if (this.debug) console.debug(`[landedTrip] Program ${program.label} loaded, with properties: `, program.properties);
          this.tripForm.showObservers = program.getPropertyAsBoolean(ProgramProperties.TRIP_OBSERVERS_ENABLE);
          if (!this.tripForm.showObservers) {
            this.data.observers = []; // make sure to reset data observers, if any
          }
          this.tripForm.showMetiers = program.getPropertyAsBoolean(ProgramProperties.TRIP_METIERS_ENABLE);
          if (!this.tripForm.showMetiers) {
            this.data.metiers = []; // make sure to reset data metiers, if any
          } else {
            this.tripForm.metiersForm.valueChanges.subscribe(value => {
              const metiers = ((value || []) as MetierRef[]).filter(metier => isNotNilOrBlank(metier));
              if (JSON.stringify(metiers) !== JSON.stringify(this.$metiers.value || [])) {
                if (this.debug) console.debug('[landedTrip-page] metiers array has changed', metiers);
                this.$metiers.next(metiers);
              }
            });
          }
        })
    );

    this.catchFilterForm = this.formBuilder.group({
      operationGroup: [null]
    });

    // Init operationGroupFilter combobox
    this.autocompleteHelper.add('operationGroupFilter', {
      showAllOnFocus: true,
      items: this.$operationGroups,
      attributes: this.operationGroupAttributes
    });




    // Cascade refresh to operation tables
    this.registerSubscription(
      this.onUpdateView.subscribe(() => {
        this.operationGroupTable.onRefresh.emit();
        this.productsTable.onRefresh.emit();
      })
    );

  }

  protected registerFormsAndTables() {
    this.registerForms([this.tripForm, this.measurementsForm])
      .registerTables([this.operationGroupTable, this.productsTable]);
  }


  async load(id?: number, options?: EditorDataServiceLoadOptions): Promise<void> {

    this.observedLocationId = options.observedLocationId;
    this.defaultBackHref = `/observations/${this.observedLocationId}`;

    super.load(id, {isLandedTrip: true, ...options});
  }

  protected async onEntityLoaded(data: Trip, options?: EditorDataServiceLoadOptions): Promise<void> {

    // load parent landing
    this.loadLandingByTrip(data);

  }

  protected async onNewEntity(data: Trip, options?: EditorDataServiceLoadOptions): Promise<void> {

    // Read options and query params
    console.info(options);
    if (options && options.observedLocationId) {

      console.debug("[landedTrip-page] New entity: settings defaults...");
      this.observedLocationId = parseInt(options.observedLocationId);
      const observedLocation = await this.getObservedLocationById(this.observedLocationId);

      // Fill default values
      if (observedLocation) {

        // program
        data.program = observedLocation.program;
        this.programSubject.next(data.program.label);

        // location
        const location = observedLocation.location;
        data.departureLocation = location;
        data.returnLocation = location;

        // observers
        if (!isEmptyArray(observedLocation.observers)) {
          data.observers = observedLocation.observers;
        }
      }
    } else {
      throw new Error("[landedTrip-page] the observedLocationId must be present");
    }

    const queryParams = this.route.snapshot.queryParams;
    // Load the vessel, if any
    if (isNotNil(queryParams['vessel'])) {
      const vesselId = +queryParams['vessel'];
      console.debug(`[landedTrip-page] Loading vessel {${vesselId}}...`);
      data.vesselSnapshot = await this.vesselService.load(vesselId, {fetchPolicy: 'cache-first'});
    }

    if (isNotNilOrBlank(queryParams['landing']) && queryParams['landing'] !== 'null') {
      this.loadLandingById(parseInt(queryParams['landing']));
    }

    if (this.isOnFieldMode) {
      data.departureDateTime = moment();
      data.returnDateTime = moment();
    }

  }

  protected loadLandingByTrip(data: Trip) {

    // Load parent landing
    if (isNotNil(data.id)) {
      console.debug('[landedTrip-page] Loading parent landing...');
      this.registerSubscription(
        this.landingService.watchAll(0, 1, undefined, undefined,
          {tripId: data.id},
          {fetchPolicy: 'cache-first'})
          .subscribe(value => {
            this.landing = value && value.data && value.data[0];
            this.landingId = this.landing && this.landing.id;
          }));
    } else {
      throw new Error('No parent found in path. landed trip without parent not implemented yet !');
    }
  }

  protected loadLandingById(landingId: number) {

    // Load parent landing
    if (isNotNil(landingId)) {
      console.debug(`[landedTrip-page] Loading parent landing ${landingId}...`);
      this.landingId = landingId;
      this.landingService.load(landingId, {fetchPolicy: "cache-first"}).then(value => this.landing = value);
    } else {
      throw new Error('No parent found in path. landed trip without parent not implemented yet !');
    }
  }

  protected async getObservedLocationById(observedLocationId: number): Promise<ObservedLocation> {

    // Load parent landing
    if (isNotNil(observedLocationId)) {
      console.debug(`[landedTrip-page] Loading parent observed location ${observedLocationId}...`);
      return this.observedLocationService.load(observedLocationId, {fetchPolicy: "cache-first"});
    } else {
      throw new Error('No parent found in path. landed trip without parent not implemented yet !');
    }
  }

  updateViewState(data: Trip) {
    super.updateViewState(data);

    if (this.isNewData) {
      this.hideTabs();
    } else {
      this.showTabs();
    }
  }

  private showTabs() {
    this.showOperationGroupTab = true;
    this.showCatchTab = true;
    this.showSaleTab = true;
    this.showExpenseTab = true;
  }

  private hideTabs() {
    this.showOperationGroupTab = false;
    this.showCatchTab = false;
    this.showSaleTab = false;
    this.showExpenseTab = false;
  }

  protected async setValue(data: Trip): Promise<void> {

    this.tripForm.value = data;
    const isNew = isNil(data.id);
    if (!isNew) {
      this.programSubject.next(data.program.label);
      this.$metiers.next(data.metiers);
    }
    this.measurementsForm.value = data && data.measurements || [];

    // Operations table
    const operationGroups = data && data.operationGroups || [];
    this.operationGroupTable.value = operationGroups;
    this.$operationGroups.next(operationGroups);

    // Products table
    let products: Product[] = [];
    operationGroups.forEach(operationGroup => products = products.concat(operationGroup.products));
    this.productsTable.value = products;

    // todo set other tables
  }

  onOperationGroupChange() {
    this.$operationGroups.next(this.operationGroupTable.value);
  }

  // todo attention à cette action
  async onOpenOperationGroup({id, row}) {

    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      this.loading = true;
      try {
        await this.router.navigateByUrl(`/trips/${this.data.id}/operations/${id}`);
      } finally {
        this.loading = false;
      }
    }
  }

  // todo attention à cette action
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

  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean }): boolean {
    const enabled = super.enable(opts);

    // Leave program & vessel controls disabled
    this.form.controls['program'].disable(opts);
    this.form.controls['vesselSnapshot'].disable(opts);

    return enabled;
  }

  devToggleOfflineMode() {
    if (this.network.offline) {
      this.network.setForceOffline(false);
    } else {
      this.network.setForceOffline();
    }
  }

  async devDownloadToLocal() {
    if (!this.data) return;

    // Copy the trip
    await (this.dataService as TripService).downloadToLocal(this.data.id, {isLandedTrip: true, withOperationGroup: true});

  }

  /* -- protected methods -- */

  protected get form(): FormGroup {
    return this.tripForm.form;
  }

  protected canUserWrite(data: Trip): boolean {
    return isNil(data.validationDate) && this.dataService.canUserWrite(data);
  }

  protected computeUsageMode(data: Trip): UsageMode {
    return this.settings.isUsageMode('FIELD') || data.synchronizationStatus === 'DIRTY' ? 'FIELD' : 'DESK';
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

  /**
   * Called by super.save()
   */
  protected async getJsonValueToSave(): Promise<any> {
    const json = await super.getJsonValueToSave();

    // recopy vesselSnapshot (disabled control)
    json.vesselSnapshot = this.data.vesselSnapshot;

    // json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    json.measurements = this.measurementsForm.value;

    const operationGroups: OperationGroup[] = this.operationGroupTable.value || [];
    json.operationGroups = operationGroups;
    json.gears = operationGroups.map(value => value.physicalGear);


    // todo affect others tables

    return json;
  }

  async save(event, options?: any): Promise<boolean> {

    const saveOptions: TripServiceSaveOption = {
      isLandedTrip: true // indicate service to reload with LandedTrip query
    };

    // Save children in-memory datasources
    if (this.operationGroupTable.dirty) {
      await this.operationGroupTable.save();
      saveOptions.withOperationGroup = true;
    }

    // todo same for products, productsale, expense

    const saved = await super.save(event, {...options, ...saveOptions} );

    if (saved) {
      // save landing
      if (!this.landing) {

        const observedLocation = await this.getObservedLocationById(this.observedLocationId);

        // create new landing
        this.landing = new Landing();
        this.landing.observedLocationId = this.observedLocationId;
        this.landing.program = observedLocation.program;
        this.landing.location = observedLocation.location;
        this.landing.vesselSnapshot = this.data.vesselSnapshot;
        // this.landing.synchronizationStatus = "DIRTY";
      }

      // update landing to trip link
      if (isNil(this.landing.tripId)) {
        this.landing.tripId = this.data.id;
      }

      // update other properties
      this.landing.dateTime = this.data.returnDateTime;
      this.landing.observers = this.data.observers;

      this.landing = await this.landingService.save(this.landing);
    }

    return saved;
  }


  /**
   * Get the first invalid tab
   */
  protected getFirstInvalidTabIndex(): number {
    const tab0Invalid = this.tripForm.invalid || this.measurementsForm.invalid;
    return 0; // test


    // const tab1Invalid = !tab0Invalid && this.physicalGearTable.invalid;
    // const tab2Invalid = !tab1Invalid && this.operationTable.invalid;

    // return tab0Invalid ? 0 : (tab1Invalid ? 1 : (tab2Invalid ? 2 : this.selectedTabIndex));
  }

  protected addToPageHistory(page: HistoryPageReference) {
    super.addToPageHistory({...page, icon: 'boat'});
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  filter($event: Event) {
    console.debug('[landed-trip.page] filter : ', $event);

  }

  // todo see operation.page.ts
  onCatchTabChange($event: MatTabChangeEvent) {
    super.onSubTabChange($event);
    if (!this.loading) {
      // todo On each tables, confirm editing row
      // this.productTABLE.confirmEditCreate();
      // this.batchTABLE.confirmEditCreate();
    }
  }

}
