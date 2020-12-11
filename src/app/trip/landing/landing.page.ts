import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';

import {isNil, isNotEmptyArray, isNotNil} from '../../shared/functions';
import * as momentImported from "moment";
import {Moment} from "moment";
import {LandingForm} from "./landing.form";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {SamplesTable} from "../sample/samples.table";
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
import {ProgramProperties} from "../../referential/services/config/program.config";
import {AppEditorOptions} from "../../core/form/editor.class";
import {EnvironmentService} from "../../../environments/environment.class";
import {Program} from "../../referential/services/model/program.model";

const moment = momentImported;

@Component({
  selector: 'app-landing-page',
  templateUrl: './landing.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: AppEditorOptions,
      useValue: {
        pathIdAttribute: 'landingId'
      }
    }
  ]
})
export class LandingPage extends AppRootDataEditor<Landing, LandingService> implements OnInit {

  protected parent: Trip | ObservedLocation;
  protected dataService: LandingService;
  protected observedLocationService: ObservedLocationService;
  protected tripService: TripService;
  protected referentialRefService: ReferentialRefService;
  protected vesselService: VesselSnapshotService;
  protected platform: PlatformService;

  mobile: boolean;
  showQualityForm = false;

  @ViewChild('landingForm', { static: true }) landingForm: LandingForm;
  @ViewChild('samplesTable', { static: true }) samplesTable: SamplesTable;

  get form(): FormGroup {
    return this.landingForm.form;
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

    this.mobile = this.platform.mobile;
    // FOR DEV ONLY ----
    this.debug = !injector.get(EnvironmentService).production;
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.onProgramChanged.subscribe(program => this.setProgram(program))
    );

    // Use landing date as default dateTime for samples
    this.registerSubscription(
      this.landingForm.form.get('dateTime').valueChanges
        .pipe(throttleTime(200), filter(isNotNil))
        .subscribe((dateTime) => {
          this.samplesTable.defaultSampleDate = dateTime as Moment;
        })
    );
  }

  protected registerForms() {
    this.addChildForms([this.landingForm, this.samplesTable]);
  }

  protected setProgram(program: Program) {
    if (!program) return; // Skip
    if (this.debug) console.debug(`[landing] Program ${program.label} loaded, with properties: `, program.properties);

    // Customize the UI, using program options
    this.landingForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_ID);
    this.samplesTable.modalOptions = {
      ...this.samplesTable.modalOptions,
      maxVisibleButtons: program.getPropertyAsInt(ProgramProperties.MEASUREMENTS_MAX_VISIBLE_BUTTONS)
    };

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

    // Landing as root
    else {
      this.showQualityForm = true;
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
    // Landing as root
    else {
      this.showQualityForm = true;
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
      throw new Error('No parent found in path. Landing without parent is not implemented yet !');
    }
  }

  protected async getValue(): Promise<Landing> {
    const data = await super.getValue();

    if (this.samplesTable.dirty) {
      await this.samplesTable.save();
    }
    data.samples = this.samplesTable.value;

    return data;
  }

  protected async setValue(data: Landing): Promise<void> {

    const isNew = isNil(data.id);
    if (!isNew) {
      this.programSubject.next(data.program.label);
    }

    this.landingForm.program = data.program.label;
    this.landingForm.value = data;

    this.samplesTable.value = data.samples || [];
  }

  updateView(data: Landing | null, opts?: {
    emitEvent?: boolean;
    openTabIndex?: number;
    updateRoute?: boolean;
  }) {
    super.updateView(data, opts);

    if (this.parent) {
      if (this.parent instanceof ObservedLocation) {

        this.landingForm.showProgram = false;
        this.landingForm.showVessel = true;
        this.landingForm.showLocation = false;
        this.landingForm.showDateTime = true;
        this.landingForm.showObservers = true;

      } else if (this.parent instanceof Trip) {

        // Hide some fields
        this.landingForm.showProgram = false;
        this.landingForm.showVessel = false;
        this.landingForm.showLocation = true;
        this.landingForm.showDateTime = true;
        this.landingForm.showObservers = true;

      }
      // Set program
      if (isNil(this.programSubject.getValue()) && this.parent.program) {
        this.programSubject.next(this.parent.program.label);
      }
    } else {

      this.landingForm.showVessel = true;
      this.landingForm.showLocation = true;
      this.landingForm.showDateTime = true;
      this.landingForm.showObservers = true;
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
    const parentUrl = this.getParentPageUrl();
    return `${parentUrl}/landing/${id}`;
  }

  protected getFirstInvalidTabIndex(): number {
    return this.landingForm.invalid ? 0 : (this.samplesTable.invalid ? 1 : -1);
  }

  protected computeUsageMode(landing: Landing): UsageMode {
    return this.settings.isUsageMode('FIELD')
      // Force desktop mode if landing date/time is 1 day later than now
      && (isNil(landing && landing.dateTime) || landing.dateTime.diff(moment(), "day") <= 1) ? 'FIELD' : 'DESK';
  }

  /* -- protected methods -- */

}
