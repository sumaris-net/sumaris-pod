import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';

import {EntityUtils, environment} from '../../core/core.module';
import {isNil, isNotNil} from '../../shared/shared.module';
import * as moment from "moment";
import {LandingForm} from "./landing.form";
import {Landing, ObservedLocation, Trip, vesselFeaturesToString} from "../services/trip.model";
import {LocationLevelIds, ProgramProperties} from "../../referential/services/model";
import {SamplesTable} from "../sample/samples.table";
import {UsageMode} from "../../core/services/model";
import {LandingFilter, LandingService} from "../services/landing.service";
import {AppEditorPage} from "../form/editor-page.class";
import {FormGroup} from "@angular/forms";
import {LoadEditorDataOptions} from "../../shared/services/data-service.class";
import {ObservedLocationService} from "../services/observed-location.service";
import {TripService} from "../services/trip.service";
import {isEmptyArray, isNotEmptyArray} from "../../shared/functions";
import {TableElement} from "angular4-material-table";

@Component({
  selector: 'app-landing-page',
  templateUrl: './landing.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingPage extends AppEditorPage<Landing, LandingFilter> implements OnInit {

  protected parent: Trip | ObservedLocation;

  @ViewChild('landingForm') landingForm: LandingForm;

  @ViewChild('samplesTable') samplesTable: SamplesTable;

  protected async getValue(): Promise<Landing> {
    const data = await super.getValue();

    if (this.samplesTable.dirty) {
      await this.samplesTable.save();
    }
    data.samples = this.samplesTable.value;

    return data;
  }

  constructor(
    injector: Injector,
    protected dataService: LandingService,
    protected observedLocationService: ObservedLocationService,
    protected tripService: TripService
  ) {
    super(injector, Landing, dataService);

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    //this.selectedTabIndex = 1;
    //this.showGeneralTab = false;

    // Watch program, to configure tables from program properties
    this.onProgramChanged
      .subscribe(program => {
        if (this.debug) console.debug(`[landing] Program ${program.label} loaded, with properties: `, program.properties);
        //this.showGeneralTab = program.getPropertyAsBoolean(ProgramProperties.LANDING_DETAILS_ENABLE, false);
        //if (!this.showGeneralTab && this.selectedTabIndex === 0) {
        //  this.selectedTabIndex = 1;
        //}
        this.landingForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_IDS) || [LocationLevelIds.PORT];
        this.markForCheck();
      });
  }

  protected registerFormsAndTables() {
    this.registerForms([this.landingForm])
      .registerTables([this.samplesTable]);
  }

  protected async onNewEntity(data: Landing, options?: LoadEditorDataOptions): Promise<void> {
    if (this.isOnFieldMode) {
      data.dateTime = moment();
    }

    data.observedLocationId = options && options.observedLocationId && parseInt(options.observedLocationId);
    data.tripId = options && options.tripId && parseInt(options.tripId);

    await this.loadParent(data, options);
  }

  protected async onEntityLoaded(data: Landing, options?: LoadEditorDataOptions): Promise<void> {
    await this.loadParent(data, options);
  }

  protected async loadParent(data: Landing, options?: LoadEditorDataOptions) {
    console.debug('[landing-page] Loading parent entity');

    // Load parent observed location
    let parent: Trip | ObservedLocation = this.parent;
    if (isNotNil(data.observedLocationId)) {
      // Load parent
      parent = await this.observedLocationService.load(data.observedLocationId, {fetchPolicy: 'cache-first'});
      data.tripId = undefined;

      // Copy from parent
      data.program = parent.program;
      data.location = parent.location;
      data.dateTime = parent.startDateTime || parent.endDateTime;
      data.observers = parent.observers;

      // Hide some fields
      this.landingForm.showProgram = EntityUtils.isEmpty(data.program);
      this.landingForm.showLocation = EntityUtils.isEmpty(data.location);
      this.landingForm.showDateTime = isNil(data.dateTime);
      this.landingForm.showObservers = isEmptyArray(data.observers);
    }
    // Load parent trip
    else if (isNotNil(data.tripId)) {
      // Load parent
      parent = await this.tripService.load(data.tripId, {fetchPolicy: 'cache-first'});
      data.observedLocationId = undefined;

      // Copy from parent
      data.program = parent.program;
      data.vesselFeatures = parent.vesselFeatures;
      data.location = parent.returnLocation || parent.returnLocation;
      data.dateTime = parent.returnDateTime || parent.departureDateTime;
      data.observers = parent.observers;

      // Hide some fields
      this.landingForm.showProgram = EntityUtils.isEmpty(data.program);
      this.landingForm.showVessel = isNil(data.vesselFeatures);
      this.landingForm.showLocation = EntityUtils.isEmpty(data.location);
      this.landingForm.showDateTime = isNil(data.dateTime);
      this.landingForm.showObservers = isEmptyArray(data.observers);
    }
    this.parent = parent;
  }

  protected async setValue(data: Landing): Promise<void> {

    // Update the back ref link
    if (this.parent) {
      if (this.parent instanceof ObservedLocation) {
        this.defaultBackHref = `/observations/${this.parent.id}?tab=1`;
      } else if (this.parent instanceof Trip) {
        this.defaultBackHref = `/trips/${this.parent.id}`;
      }
    } else {
      this.defaultBackHref = null;
    }

    this.landingForm.value = data;
    this.samplesTable.value = data.samples || [];
  }

  protected async computeTitle(data: Landing): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return await this.translate.get('LANDING.NEW.TITLE').toPromise();
    }

    // Existing data
    return await this.translate.get('LANDING.EDIT.TITLE', {
      vessel: vesselFeaturesToString(data.vesselFeatures)
    }).toPromise();
  }


  protected get form(): FormGroup {
    return this.landingForm.form;
  }

  protected getFirstInvalidTabIndex(): number {
    return this.landingForm.invalid ? 0 : (this.samplesTable.invalid ? 1 : -1);
  }

  async onOpenSample(event: CustomEvent<{id: number; row: TableElement<Landing>}>) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/observations/${this.data.id}/landing/${event.detail.id}`);
    }
  }

  async onNewSample(event?: any) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/observations/${this.data.id}/landing/new`);
    }
  }

  protected computeUsageMode(landing: Landing): UsageMode {
    return this.settings.isUsageMode('FIELD')
    && isNotNil(landing && landing.dateTime)
    && landing.dateTime.diff(moment(), "day") <= 1 ? 'FIELD' : 'DESK';
  }


  /* -- protected methods -- */

}
