import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController} from "@ionic/angular";

import {AppFormUtils, AppTabPage, EntityUtils} from '../../core/core.module';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../../../environments/environment';
import {Subject} from 'rxjs';
import {DateFormatPipe, isNil, isNotNil} from '../../shared/shared.module';
import * as moment from "moment";
import {Moment} from "moment";
import {LandingForm} from "./landing.form";
import {Landing, vesselFeaturesToString} from "../services/trip.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {filter, switchMap} from "rxjs/operators";
import {LocationLevelIds, ProgramProperties} from "../../referential/services/model";
import {ProgramService} from "../../referential/services/program.service";
import {isNotNilOrBlank} from "../../shared/functions";
import {SamplesTable} from "../sample/samples.table";
import {UsageMode} from "../../core/services/model";
import {LandingService} from "../services/landing.service";

@Component({
  selector: 'app-landing-page',
  templateUrl: './landing.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingPage extends AppTabPage<Landing> implements OnInit {

  protected _enableListenChanges = (environment.listenRemoteChanges === true);

  programSubject = new Subject<string>();
  title = new Subject<string>();
  saving = false;
  defaultBackHref: string;
  showSamples = false;
  onRefresh = new EventEmitter<any>();
  usageMode: UsageMode;

  @ViewChild('landingForm') landingForm: LandingForm;

  @ViewChild('samplesTable') samplesTable: SamplesTable;

  constructor(
    route: ActivatedRoute,
    router: Router,
    alertCtrl: AlertController,
    translate: TranslateService,
    protected dateFormat: DateFormatPipe,
    protected dataService: LandingService,
    protected settings: LocalSettingsService,
    protected programService: ProgramService,
    protected landingService: LandingService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, alertCtrl, translate);

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Register forms & tables
    this.registerForms([this.landingForm])
      .registerTables([this.samplesTable]);

    this.disable();

    this.route.params.first().subscribe(async ({id, parentId}) => {
      if (isNil(id) || id === "new") {
        await this.load(undefined, {parentId});
      }
      else {
        await this.load(+id, {parentId});
      }
    });

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject.asObservable()
        .pipe(filter(isNotNilOrBlank), switchMap(label => this.programService.watchByLabel(label)))
        .subscribe(program => {
          if (this.debug) console.debug(`[landing] Program ${program.label} loaded, with properties: `, program.properties);
          this.landingForm.showLocation = false;
          this.landingForm.showDateTime = false;
          this.landingForm.showProgram = false;
          this.landingForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_IDS) || [LocationLevelIds.PORT];
        })
    );
  }

  async load(id?: number, options?:  { parentId: number }) {
    this.error = null;

    // New data
    if (isNil(id)) {
      if (!options || isNil(options.parentId)) throw new Error("Missing argument 'options.parentId'!");

      const landing = await this.landingService.load(id);

      this.usageMode = this.computeUsageMode(landing);

      // Create using default values
      const data = new Landing();

      // If is on field mode, fill default values
      if (this.usageMode === 'FIELD') {
        data.landingDateTime = moment();
        // TODO : get the default program from local settings ?
        //data.program = ...;
      }

      this.updateView(data);
      this.loading = false;
      this.showSamples = false;
      this.startListenProgramChanges();
    }

    // Load existing data
    else {
      const data = await this.dataService.load(id);
      this.updateView(data, true);
      this.loading = false;
      this.showSamples = true;
      this.startListenRemoteChanges();
    }
  }

  startListenProgramChanges() {

    // If new trip
    if (isNil(this.data.id)) {

      // Listen program changes (only if new data)
      this.registerSubscription(this.landingForm.form.controls['program'].valueChanges
        .subscribe(program => {
          if (EntityUtils.isNotEmpty(program)) {
            console.debug("[landing] Propagate program change: " + program.label);
            this.programSubject.next(program.label);
          }
        })
      );
    }
  }

  startListenRemoteChanges() {
    // Listen for changes on server
    if (isNotNil(this.data.id) && this._enableListenChanges) {
      // this.registerSubscription(
      //   this.dataService.listenChanges(this.data.id)
      //     .subscribe((data: Landing | undefined) => {
      //       const newUpdateDate = data && (data.updateDate as Moment) || undefined;
      //       if (isNotNil(newUpdateDate) && newUpdateDate.isAfter(this.data.updateDate)) {
      //         if (this.debug) console.debug("[trip] Changes detected on server, at:", newUpdateDate);
      //         if (!this.dirty) {
      //           this.updateView(data, true);
      //         }
      //       }
      //     })
      //   );
    }
  }

  updateView(data: Landing | null, updateLandings?: boolean) {
    this.data = data;
    this.landingForm.value = data;
    const isSaved = isNotNil(data.id);
    if (isSaved) {
      this.landingForm.form.controls['program'].disable();
      this.programSubject.next(data.program.label);
    }

    this.showSamples = isSaved;
    if (updateLandings && this.samplesTable) {
      //this.samplesTable.setParent(data);
    }

    // Quality metadata
    //this.qualityForm.value = data;

    this.updateTitle();

    // Compute the default back href
    this.defaultBackHref = `/observations/${data.observedLocationId}?tab=1`;

    this.markAsPristine();
    this.markAsUntouched();

    if (isNotNil(this.data.validationDate)) {
      this.disable();
    } else {
      this.enable();
    }

    this.onRefresh.emit();
  }

  async save(event): Promise<boolean> {
    if (this.loading || this.saving) return false;

    // Not valid
    if (!this.valid) {
      this.markAsTouched();
      this.logFormErrors();
      this.openFirstInvalidTab();

      this.submitted = true;
      return false;
    }
    this.saving = true;
    this.error = undefined;

    if (this.debug) console.debug("[landing] Saving control...");

    // Get data
    const data = this.landingForm.value;
    const formDirty = this.dirty;
    const isNew = this.isNewData;

    this.disable();

    try {
      // Save saleControl form (with sale)
      const updatedData = formDirty ? await this.dataService.save(data) : this.data;
      if (formDirty) {
        this.markAsPristine();
        this.markAsUntouched();
      }

      // Save vessels
      const isVesselSaved = !this.samplesTable || await this.samplesTable.save();
      if (isVesselSaved && this.samplesTable) {
        this.samplesTable.markAsPristine();
      }

      // Update the view (e.g metadata)
      this.updateView(updatedData, isNew/*will update tripId in filter*/);

      // Is first save
      if (isNew) {

        // Open the gear tab
        this.selectedTabIndex = 1;
        const queryParams = Object.assign({}, this.route.snapshot.queryParams, {tab: this.selectedTabIndex});

        // Update route location
        this.router.navigate(['../' + updatedData.id], {
          relativeTo: this.route,
          queryParams: queryParams,
          replaceUrl: true // replace the current state in history
        });

        // Subscription to remote changes
        this.startListenRemoteChanges();
      }

      this.submitted = false;
      return true;
    }
    catch (err) {
      console.error(err);
      this.submitted = true;
      this.error = err && err.message || err;
      this.enable();
      return false;
    }
    finally {
      this.saving = false;
    }
  }

  enable() {
    if (!this.data || isNotNil(this.data.validationDate)) return false;

    // If not a new data, check user can write
    if (isNotNil(this.data.id) && !this.programService.canUserWrite(this.data)) {
      if (this.debug) console.warn("[landing] Leave form disable (User has NO write access)");
      return;
    }

    if (this.debug) console.debug("[landing] Enabling form (User has write access)");
    super.enable();
  }

  /**
   * Compute the title
   * @param data
   */
  async updateTitle(data?: Landing) {
    data = data || this.data;

    let title;
    // new data
    if (!data || isNil(data.id)) {
      title = await this.translate.get('LANDING.NEW.TITLE').toPromise();
    }
    // Existing data
    else {
      title = await this.translate.get('LANDING.EDIT.TITLE', {
        vessel: vesselFeaturesToString(data.vesselFeatures)
      }).toPromise();
    }

    // Emit the title
    this.title.next(title);
  }

  protected logFormErrors() {
    if (this.debug) console.debug("[landing] Form not valid. Detecting where...");
    if (!this.landingForm.empty && this.landingForm.invalid) {
      AppFormUtils.logFormErrors(this.landingForm.form, "[landing] [sale-form] ");
    }
    // if (this.measurementsForm.invalid) {
    //   AppFormUtils.logFormErrors(this.measurementsForm.form, "[page-trip] [measurementsForm-form] ");
    // }
  }

  /**
   * Open the first tab that is invalid
   */
  protected openFirstInvalidTab() {
    const tab0Invalid = this.landingForm.invalid;
    const tab1Invalid = this.samplesTable.invalid;

    const invalidTabIndex = tab0Invalid ? 0 : (tab1Invalid ? 1 : this.selectedTabIndex);
    if (this.selectedTabIndex === 0 && !tab0Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    }
    else if (this.selectedTabIndex === 1 && !tab1Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    }
  }

  async onOpenSample(id: number) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/observations/${this.data.id}/landing/${id}`);
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
    && isNotNil(landing && landing.landingDateTime)
    && landing.landingDateTime.diff(moment(), "day") <= 1 ? 'FIELD' : 'DESK';
  }


  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
