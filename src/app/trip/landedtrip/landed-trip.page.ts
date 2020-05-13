import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';

import {MeasurementsForm} from '../measurement/measurements.form.component';
import {environment, isNotNil} from '../../core/core.module';
import {EditorDataServiceLoadOptions, fadeInOutAnimation, isNil, isNotEmptyArray, isNotNilOrBlank} from '../../shared/shared.module';
import * as moment from "moment";
import {AcquisitionLevelCodes, PmfmStrategy, ProgramProperties} from "../../referential/services/model";
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
import {MatAutocompleteConfigHolder, MatAutocompleteFieldConfig} from "../../shared/material/material.autocomplete";
import {MatTabChangeEvent, MatTabGroup} from "@angular/material/tabs";
import {ProductsTable} from "../product/products.table";
import {Product} from "../services/model/product.model";
import {LandedSaleForm} from "../sale/landed-sale.form";
import {PacketsTable} from "../packet/packets.table";
import {Packet} from "../services/model/packet.model";
import {OperationGroup, Trip} from "../services/model/trip.model";
import {ObservedLocation} from "../services/model/observed-location.model";
import {ProductSale} from "../services/model/product-sale.model";
import {PacketSale} from "../services/model/packet-sale.model";
import {fillRankOrder, isRankOrderValid} from "../services/model/base.model";

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

  selectedCatchTabIndex = 0;

  showOperationGroupTab = false;
  showCatchTab = false;
  showSaleTab = false;
  showExpenseTab = false;

  autocompleteHelper: MatAutocompleteConfigHolder;
  autocompleteFields: { [key: string]: MatAutocompleteFieldConfig };

  // List of trip's metier, used to populate operation group's metier combobox
  $metiers = new BehaviorSubject<MetierRef[]>(null);

  // List of trip's operation groups, use to populate product filter
  $operationGroups = new BehaviorSubject<OperationGroup[]>(null);
  catchFilterForm: FormGroup;

  operationGroupAttributes = ['metier.label', 'metier.name'];

  productSalePmfms: PmfmStrategy[];

  @ViewChild('tripForm', {static: true}) tripForm: TripForm;
  @ViewChild('measurementsForm', {static: true}) measurementsForm: MeasurementsForm;
  @ViewChild('operationGroupTable', {static: true}) operationGroupTable: OperationGroupTable;

  @ViewChild('landedSaleForm', {static: true}) landedSaleForm: LandedSaleForm;

  @ViewChild('catchTabGroup', {static: true}) catchTabGroup: MatTabGroup;
  // @ViewChild('saleTabGroup', {static: true}) saleTabGroup: MatTabGroup;

  @ViewChild('productsTable', {static: true}) productsTable: ProductsTable;
  @ViewChild('packetsTable', {static: true}) packetsTable: PacketsTable;


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
        .subscribe(async program => {
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
        this.packetsTable.onRefresh.emit();
        //this.landedSaleForm.onRefresh.emit();// TODO ? le onRefresh sur les sous tableaux ?
      })
    );

  }

  protected registerFormsAndTables() {
    this.registerForms([this.tripForm, this.measurementsForm,
      this.landedSaleForm //, this.saleMeasurementsForm
    ])
      .registerTables([this.operationGroupTable, this.productsTable, this.packetsTable]);
  }


  async load(id?: number, options?: EditorDataServiceLoadOptions): Promise<void> {

    this.observedLocationId = options && options.observedLocationId || this.observedLocationId;
    this.defaultBackHref = `/observations/${this.observedLocationId}`;

    super.load(id, {isLandedTrip: true, ...options});
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

        data.observedLocationId = observedLocation.id;

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

    if (this.isOnFieldMode) {
      data.departureDateTime = moment();
      data.returnDateTime = moment();
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

      // fixme trouver un meilleur moment pour charger les pmfms
      this.productSalePmfms = await this.programService.loadProgramPmfms(data.program.label, {acquisitionLevel: AcquisitionLevelCodes.PRODUCT_SALE});

    }
    this.measurementsForm.value = data && data.measurements || [];

    // Operations table
    const operationGroups = data && data.operationGroups || [];
    this.operationGroupTable.value = operationGroups;
    this.$operationGroups.next(operationGroups);

    let products: Product[] = [];
    let packets: Packet[] = [];
    operationGroups.forEach(operationGroup => {
      products = products.concat(operationGroup.products);
      packets = packets.concat(operationGroup.packets);
    });

    // Fix products and packets rank orders (reset if rank order are invalid, ie. from SIH)
    if (!isRankOrderValid(products))
      fillRankOrder(products, 1);
    if (!isRankOrderValid(packets))
      fillRankOrder(packets, 1);

    // Sale
    if (data && data.sale && this.productSalePmfms) {

      // fix sale startDateTime
      data.sale.startDateTime = !data.sale.startDateTime ? data.returnDateTime : data.sale.startDateTime;

      this.landedSaleForm.value = data.sale;
      // this.saleMeasurementsForm.value = data.sale.measurements || [];


      // Dispatch product and packet sales
      data.sale.products.forEach(saleProduct => {
        if (isNil(saleProduct.batchId)) {
          // = product
          const productFound = products.find(product => ProductSale.isSaleOfProduct(product, saleProduct, this.productSalePmfms));
          if (productFound) {
            productFound.productSales.push(ProductSale.toProductSale(saleProduct, this.productSalePmfms));
          }
        } else {
          // = packet
          const packetFound = packets.find(packet => PacketSale.isSaleOfPacket(packet, saleProduct));
          if (packetFound) {
            packetFound.addPacketSale(PacketSale.toPacketSale(saleProduct, this.productSalePmfms));
          }
        }
      });

      // need fill products.productSales.rankOrder
      products.forEach(p => fillRankOrder(p.productSales));
    }

    // Products table
    this.productsTable.value = products;

    // Packets table
    this.packetsTable.value = packets;


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
    await (this.dataService as TripService).copyToOffline(this.data.id, {isLandedTrip: true, withOperationGroup: true});

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

    // parent link
    json.landingId = this.data.landingId;
    json.observedLocationId = this.data.observedLocationId;

    // recopy vesselSnapshot (disabled control)
    json.vesselSnapshot = this.data.vesselSnapshot;

    // json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    json.measurements = this.measurementsForm.value;

    const operationGroups: OperationGroup[] = this.operationGroupTable.value || [];

    // Get products and packets
    const products = this.productsTable.value || [];
    const packets = this.packetsTable.value || [];

    // Sale
    json.sale = this.landedSaleForm.value;
    if (json.sale && this.productSalePmfms) {
      // json.sale.measurements = this.saleMeasurementsForm.value;

      // Merge sales from products and packets
      const existingProducts: any[] = json.sale.products || [];
      const newProducts: Product[] = [];

      // Parse ProductSales
      products.forEach(product => {
        (product.productSales || []).forEach(productSale => {
          const existingProduct = existingProducts.find(p => p.id === productSale.id);
          if (existingProduct) {
            // merge existing product
            const toMerge = productSale.asObject(this.productSalePmfms);
            newProducts.push({...existingProduct, ...toMerge});
          } else {
            // new sale product
          }
        });
      });

      console.debug(newProducts);
    }

    // Affect in each operation group : products and packets
    operationGroups.forEach(operationGroup => {
      operationGroup.products = products.filter(product => operationGroup.equals(product.parent as OperationGroup));
      operationGroup.packets = packets.filter(packet => operationGroup.equals(packet.parent as OperationGroup));
    });

    json.operationGroups = operationGroups;
    json.gears = operationGroups.map(operationGroup => operationGroup.physicalGear);


    // todo affect others tables

    return json;
  }

  async save(event, options?: any): Promise<boolean> {

    const saveOptions: TripServiceSaveOption = {
      isLandedTrip: true // indicate service to reload with LandedTrip query
    };

    // Save children in-memory datasources
    if (this.productsTable.dirty) {
      await this.productsTable.save();
      this.operationGroupTable.markAsDirty();
    }
    if (this.packetsTable.dirty) {
      await this.packetsTable.save();
      this.operationGroupTable.markAsDirty();
    }
    if (this.operationGroupTable.dirty) {
      await this.operationGroupTable.save();
      saveOptions.withOperationGroup = true;
    }

    // todo same for products, productsale, expense

    return await super.save(event, {...options, ...saveOptions});
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

  /**
   * Update route with correct url
   * workaround for #185
   *
   * @param data
   * @param queryParams
   */
  protected async updateRoute(data: Trip, queryParams: any): Promise<boolean> {
    return await this.router.navigateByUrl(`${this.defaultBackHref}/trip/${data.id}`, {
      replaceUrl: true,
      queryParams: this.queryParams
    });
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

  // todo à suppr
  onCatchTabChange($event: MatTabChangeEvent) {
    super.onSubTabChange($event);
    if (!this.loading) {
      // todo On each tables, confirm editing row
      // this.productTABLE.confirmEditCreate();
      // this.batchTABLE.confirmEditCreate();
    }
  }

  onSaleTabChange($event: MatTabChangeEvent) {
    super.onSubTabChange($event);
    if (!this.loading) {
      // todo On each tables, confirm editing row
      // this.productsSAleTABLE.confirmEditCreate();
      // this.batchSaleTABLE.confirmEditCreate();
    }
  }

}
