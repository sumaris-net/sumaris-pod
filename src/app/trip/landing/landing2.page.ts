import {ChangeDetectionStrategy, Component, Injector, OnInit, Optional, ViewChild} from '@angular/core';

import {isNil, isNotEmptyArray, isNotNil} from '../../shared/functions';
import * as moment from "moment";
import {Moment} from "moment";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {UsageMode} from "../../core/services/model/settings.model";
import {ReferentialUtils} from "../../core/services/model/referential.model";
import {LandingService} from "../services/landing.service";
import {AppRootDataEditor} from "../../data/form/root-data-editor.class";
import {FormGroup} from "@angular/forms";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {ObservedLocationService} from "../services/observed-location.service";
import {TripService} from "../services/trip.service";
import {filter, throttleTime} from "rxjs/operators";
import {Observable} from "rxjs";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {PlatformService} from "../../core/services/platform.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {Landing} from "../services/model/landing.model";
import {Trip} from "../services/model/trip.model";
import {ObservedLocation} from "../services/model/observed-location.model";
import {environment} from "../../../environments/environment";
import {ProgramProperties} from "../../referential/services/config/program.config";
import {AppEditorOptions} from "../../core/form/editor.class";
import {Landing2Form} from "./landing2.form";
import {MatTabGroup} from "@angular/material/tabs";
import {Samples2Table} from "../sample/samples2.table";
import {StrategyService} from "../../referential/services/strategy.service";

@Component({
  selector: 'app-landing2-page',
  templateUrl: './landing2.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: AppEditorOptions,
      useValue: {
        pathIdAttribute: 'landing2Id'
      }
    }
  ]
})
export class Landing2Page extends AppRootDataEditor<Landing, LandingService> implements OnInit {

  protected parent: Trip | ObservedLocation;
  protected dataService: LandingService;
  protected observedLocationService: ObservedLocationService;
  protected tripService: TripService;
  protected referentialRefService: ReferentialRefService;
  protected vesselService: VesselSnapshotService;
  protected platform: PlatformService;
  protected strategyService: StrategyService;


  mobile: boolean;

  @ViewChild('landing2Form', { static: true }) landing2Form: Landing2Form;
  @ViewChild('sample2TabGroup', { static: true }) sample2TabGroup: MatTabGroup;
  @ViewChild('samples2Table', { static: true }) samples2Table: Samples2Table;

  get pmfms(): Observable<PmfmStrategy[]> {
    return this.landing2Form.$pmfms.pipe(filter(isNotNil));
  }

  get form(): FormGroup {
    return this.landing2Form.form;
  }

  constructor(
    injector: Injector,
    options: AppEditorOptions
  ) {
    super(injector, Landing, injector.get(LandingService), options);
    this.observedLocationService = injector.get(ObservedLocationService);
    this.tripService = injector.get(TripService);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.vesselService = injector.get(VesselSnapshotService);
    this.platform = injector.get(PlatformService);
    this.strategyService = injector.get(StrategyService);


    this.mobile = this.platform.mobile;
    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.onProgramChanged
        .subscribe(program => {
          if (this.debug) console.debug(`[landing] Program ${program.label} loaded, with properties: `, program.properties);
          this.landing2Form.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_ID);
          //this.markForCheck();
        }));

    // Use landing date as default dateTime for samples
    this.registerSubscription(
      this.landing2Form.form.controls['dateTime'].valueChanges
        .pipe(throttleTime(200), filter(isNotNil))
        .subscribe((dateTime) => {
          this.samples2Table.defaultSampleDate = dateTime as Moment;
        })
    );
  }

  protected registerForms() {
    this.addChildForms([this.landing2Form, this.samples2Table]);
  }

  protected async onNewEntity(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {

    if (this.isOnFieldMode) {
      data.dateTime = moment();
    }

    data.observedLocationId = options && options.observedLocationId && parseInt(options.observedLocationId);
    data.tripId = options && options.tripId && parseInt(options.tripId);

    this.parent = await this.loadParent(data);

    // Copy from parent into the new object
    if (this.parent) {
      const queryParams = this.route.snapshot.queryParams;
      data.program = this.parent.program;
      data.observers = this.parent.observers;
      if (this.parent instanceof ObservedLocation) {
        data.location = this.parent.location;
        data.dateTime = this.parent.startDateTime || this.parent.endDateTime;
        data.tripId = undefined;

        // Load the vessel, if any
        if (isNotNil(queryParams['vessel'])) {
          const vesselId = +queryParams['vessel'];
          console.debug(`[landing-page] Loading vessel {${vesselId}}...`);
          data.vesselSnapshot = await this.vesselService.load(vesselId, {fetchPolicy: 'cache-first'});
        }

        // Define back link
        this.defaultBackHref = `/observations/${this.parent.id}?tab=1`;
      }
      else if (this.parent instanceof Trip) {
        data.vesselSnapshot = this.parent.vesselSnapshot;
        data.location = this.parent.returnLocation || this.parent.departureLocation;
        data.dateTime = this.parent.returnDateTime || this.parent.departureDateTime;
        data.observedLocationId = undefined;

        // Define back link
        this.defaultBackHref = `/trips/${this.parent.id}?tab=2`;
      }

      // Set rankOrder
      if (isNotNil(queryParams['rankOrder'])) {
        data.rankOrder = +queryParams['rankOrder'];
      }
      else {
        data.rankOrder = 1;
      }
    }

  }

  protected async onEntityLoaded(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {

    this.parent = await this.loadParent(data);

    // Copy not fetched data
    if (this.parent) {
      data.program = ReferentialUtils.isNotEmpty(data.program) && data.program || this.parent.program;
      data.observers = isNotEmptyArray(data.observers) && data.observers || this.parent.observers;

      if (this.parent instanceof ObservedLocation) {
        data.location = data.location || this.parent.location;
        data.dateTime = data.dateTime || this.parent.startDateTime || this.parent.endDateTime;
        data.tripId = undefined;

        // Define back link
        this.defaultBackHref = `/observations/${this.parent.id}?tab=1`;
      }
      else if (this.parent instanceof Trip) {
        data.vesselSnapshot = this.parent.vesselSnapshot;
        data.location = data.location || this.parent.returnLocation || this.parent.departureLocation;
        data.dateTime = data.dateTime || this.parent.returnDateTime || this.parent.departureDateTime;
        data.observedLocationId = undefined;

        this.defaultBackHref = `/trips/${this.parent.id}?tab=2`;
      }
    }

  }

  protected async loadParent(data: Landing): Promise<Trip | ObservedLocation> {

    // Load parent observed location
    if (isNotNil(data.observedLocationId)) {
      console.debug('[landing-page] Loading parent observed location...');
      return await this.observedLocationService.load(data.observedLocationId, {fetchPolicy: 'cache-first'});
    }
    // Load parent trip
    else if (isNotNil(data.tripId)) {
      console.debug('[landing-page] Loading parent trip...');
      return await this.tripService.load(data.tripId, {fetchPolicy: 'cache-first'});
    }
    else {
      throw new Error('No parent found in path. Landing without parent not implemented yet !');
    }
  }

  protected async getValue(): Promise<Landing> {
    const data = await super.getValue();

    if (this.samples2Table.dirty) {
      await this.samples2Table.save();
    }
    //data.samples = data.samples.concat(this.samples2Table.value);
    data.samples =this.samples2Table.value;
    data.samples.map(s => s.rankOrder = 1);

    // set TaxonName to sample
    const taxonName = this.landing2Form.taxonNamesForm.value[0].taxonName;
    if(data.samples){
      data.samples[0].taxonName = taxonName;
    }


    return data;
  }

  protected async setValue(data: Landing): Promise<void> {

    const isNew = isNil(data.id);
    if (!isNew) {
      // FIXME CLT Program subscriber throw invalid pmfms => /!\ program not set
      //this.programSubject.next(data.program.label);
    }

    this.samples2Table.value = data.samples || [];

    const measurementValues = Object.entries(data.measurementValues).map(([key, value]) => {
      return {
        key,
        value
      };
    });
    let strategyLabel : string;
    // FIXME CLT measurement Pmfm Code must be externalized
    measurementValues.forEach((measurementValue) => {
      if (measurementValue.key === "359") {
        strategyLabel = measurementValue.value
      }
    });


    this.landing2Form.program = data.program.label;
    this.landing2Form.value = data;
    // this.landing2Form.strategyLabel = strategyLabel;
    //this.samples2Table.value = data.samples || [];

    let pmfmStrategy =  await this.strategyService.loadByLabel( strategyLabel, { expandedPmfmStrategy : true});
    let pmfmStrategies = pmfmStrategy.pmfmStrategies.filter(pmfmStrategies => pmfmStrategies.pmfmId);
    console.log("pmfmStrategy", pmfmStrategy);

    this.landing2Form.appliedStrategies = pmfmStrategy.appliedStrategies;

    this.samples2Table.appliedPmfmStrategy = pmfmStrategies;
    this.samples2Table.pmfms = pmfmStrategies;

    // Set fishing areas using strategy
    // this.landing2Form.
    //------------------------------------------------------------------------------------------------------------------


  }

  updateView(data: Landing | null, opts?: {
    emitEvent?: boolean;
    openTabIndex?: number;
    updateRoute?: boolean;
  }) {
    super.updateView(data, opts);

    if (this.parent) {
      if (this.parent instanceof ObservedLocation) {

        this.landing2Form.showProgram = false;
        this.landing2Form.showSampleRowCode = true;
        this.landing2Form.showVessel = true;
        this.landing2Form.showLocation = true;
        this.landing2Form.showDateTime = false;
        this.landing2Form.showFishingArea = true;
        this.landing2Form.showTargetSpecies = true;
        this.landing2Form.showComment = true;
        this.landing2Form.showObservers = true;

      } else if (this.parent instanceof Trip) {

        // Hide some fields
        this.landing2Form.showProgram = false;
        this.landing2Form.showVessel = false;
        this.landing2Form.showLocation = true;
        this.landing2Form.showDateTime = true;
        this.landing2Form.showObservers = true;
      }
    } else {

      this.landing2Form.showVessel = true;
      this.landing2Form.showLocation = true;
      this.landing2Form.showDateTime = true;
      this.landing2Form.showObservers = true;
    }
  }

  protected async computeTitle(data: Landing): Promise<string> {
    const titlePrefix = this.parent && this.parent instanceof ObservedLocation &&
      await this.translate.get('LANDING.TITLE_PREFIX', {
        location: (this.parent.location && (this.parent.location.name || this.parent.location.label)),
        date: this.parent.startDateTime && this.dateFormat.transform(this.parent.startDateTime) as string || ''
      }).toPromise() || '';

    // new data
    if (!data || isNil(data.id)) {
      return titlePrefix + (await this.translate.get('LANDING.NEW.TITLE').toPromise());
    }

    // Existing data
    return titlePrefix + (await this.translate.get('LANDING.EDIT.TITLE', {
      vessel: data.vesselSnapshot && (data.vesselSnapshot.exteriorMarking || data.vesselSnapshot.name)
    }).toPromise());
  }

  protected computePageUrl(id: number|'new') {
    let parentUrl = this.getParentPageUrl();
    return `${parentUrl}/landing/${id}`;
  }

  protected getFirstInvalidTabIndex(): number {
  //  return this.landing2Form.invalid ? 0 : (this.samples2Table.invalid ? 1 : -1);
    // return first invalid tabIndex
    return 0;
  }

  protected computeUsageMode(landing: Landing): UsageMode {
    return this.settings.isUsageMode('FIELD')
    && isNotNil(landing && landing.dateTime)
    && landing.dateTime.diff(moment(), "day") <= 1 ? 'FIELD' : 'DESK';
  }

  /* -- protected methods -- */

}
