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
import {ObservedLocationForm} from "./observed-location.form";
import {ObservedLocation} from "../services/trip.model";
import {ObservedLocationService} from "../services/observed-location.service";
import {EntityQualityFormComponent} from "../quality/entity-quality-form.component";
import {LandingsTable} from "../landing/landings.table";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {filter, switchMap} from "rxjs/operators";
import {LocationLevelIds, ProgramProperties} from "../../referential/services/model";
import {ProgramService} from "../../referential/services/program.service";
import {isNotNilOrBlank} from "../../shared/functions";

@Component({
  selector: 'page-observed-location',
  templateUrl: './observed-location.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ObservedLocationPage extends AppTabPage<ObservedLocation> implements OnInit {

  protected _enableListenChanges = (environment.listenRemoteChanges === true);

  programSubject = new Subject<string>();
  title = new Subject<string>();
  saving: boolean = false;
  defaultBackHref: string = "/observations";
  onRefresh = new EventEmitter<any>();
  isOnFieldMode: boolean;

  @ViewChild('observedLocationForm') observedLocationForm: ObservedLocationForm;

  @ViewChild('qualityForm') qualityForm: EntityQualityFormComponent;

  @ViewChild('landingsTable') landingsTable: LandingsTable;

  constructor(
    route: ActivatedRoute,
    router: Router,
    alertCtrl: AlertController,
    translate: TranslateService,
    protected dateFormat: DateFormatPipe,
    protected dataService: ObservedLocationService,
    protected settingsService: LocalSettingsService,
    protected programService: ProgramService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, alertCtrl, translate);

    this.isOnFieldMode = this.settingsService.isUsageMode('FIELD');

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Register forms & tables
    this.registerForms([this.observedLocationForm])
      .registerTables([this.landingsTable]);

    this.disable();

    this.route.params.first().subscribe(async ({id}) => {
      if (!id || id === "new") {
        await this.load();
      }
      else {
        await this.load(+id);
      }
    });

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject.asObservable()
        .pipe(filter(isNotNilOrBlank), switchMap(label => this.programService.watchByLabel(label)))
        .subscribe(program => {
          if (this.debug) console.debug(`[observed-location] Program ${program.label} loaded, with properties: `, program.properties);
          this.observedLocationForm.showEndDateTime = program.getPropertyAsBoolean(ProgramProperties.OBSERVED_LOCATION_END_DATE_TIME_ENABLE, false);
          this.observedLocationForm.locationLevelIds = program.getPropertyAsNumbers(ProgramProperties.OBSERVED_LOCATION_LOCATION_LEVEL_IDS) || [LocationLevelIds.PORT];
        })
    );
  }

  async load(id?: number, options?: any) {
    this.error = null;

    // New data
    if (isNil(id)) {

      // Create using default values
      const data = new ObservedLocation();

      // If is on field mode, fill default values
      if (this.isOnFieldMode) {
        data.startDateTime = moment();
        // TODO : get the default program from local settings ?
        //data.program = ...;
      }

      this.updateView(data);
      this.loading = false;
      this.startListenProgramChanges();
    }

    // Load existing data
    else {
      const data = await this.dataService.load(id);
      this.updateView(data);
      this.loading = false;
      this.startListenRemoteChanges();
    }
  }

  startListenProgramChanges() {

    // If new trip
    if (isNil(this.data.id)) {

      // Listen program changes (only if new data)
      this.registerSubscription(this.observedLocationForm.form.controls['program'].valueChanges
        .subscribe(program => {
          if (EntityUtils.isNotEmpty(program)) {
            console.debug("[observed-location] Propagate program change: " + program.label);
            this.programSubject.next(program.label);
          }
        })
      );
    }
  }

  startListenRemoteChanges() {
    // Listen for changes on server
    if (isNotNil(this.data.id) && this._enableListenChanges) {
      this.registerSubscription(
        this.dataService.listenChanges(this.data.id)
          .subscribe((data: ObservedLocation | undefined) => {
            const newUpdateDate = data && (data.updateDate as Moment) || undefined;
            if (isNotNil(newUpdateDate) && newUpdateDate.isAfter(this.data.updateDate)) {
              if (this.debug) console.debug("[trip] Changes detected on server, at:", newUpdateDate);
              if (!this.dirty) {
                this.updateView(data);
              }
            }
          })
        );
    }
  }

  updateView(data: ObservedLocation | null) {
    console.log("TODO");
    this.data = data;
    this.observedLocationForm.value = data;
    const isNew = this.isNewData;
    if (!isNew) {
      this.observedLocationForm.form.controls['program'].disable();
      this.programSubject.next(data.program.label);
      this.landingsTable.setParent(data);
    }

    // Quality metadata
    this.qualityForm.value = data;

    this.updateTitle();

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

    if (this.debug) console.debug("[observed-location] Saving control...");

    // Get data
    const data = this.observedLocationForm.value;
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
      const isVesselSaved = !this.landingsTable || await this.landingsTable.save();
      if (isVesselSaved && this.landingsTable) {
        this.landingsTable.markAsPristine();
      }

      // Update the view (e.g metadata)
      this.updateView(updatedData);

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
    if (isNotNil(this.data.id) && !this.dataService.canUserWrite(this.data)) {
      if (this.debug) console.warn("[observed-location] Leave form disable (User has NO write access)");
      return;
    }

    if (this.debug) console.debug("[observed-location] Enabling form (User has write access)");
    super.enable();
  }

  protected async saveIfDirtyAndConfirm(): Promise<boolean> {
    if (!this.dirty) return true;

    let confirm = false;
    let cancel = false;
    const translations = this.translate.instant(['COMMON.BTN_SAVE', 'COMMON.BTN_CANCEL', 'COMMON.BTN_ABORT_CHANGES', 'CONFIRM.SAVE', 'CONFIRM.ALERT_HEADER']);
    const alert = await this.alertCtrl.create({
      header: translations['CONFIRM.ALERT_HEADER'],
      message: translations['CONFIRM.SAVE'],
      buttons: [
        {
          text: translations['COMMON.BTN_CANCEL'],
          role: 'cancel',
          cssClass: 'secondary',
          handler: () => {
            cancel = true;
          }
        },
        {
          text: translations['COMMON.BTN_ABORT_CHANGES'],
          cssClass: 'secondary',
          handler: () => { }
        },
        {
          text: translations['COMMON.BTN_SAVE'],
          handler: () => {
            confirm = true; // update upper value
          }
        }
      ]
    });
    await alert.present();
    await alert.onDidDismiss();

    if (!confirm) return !cancel;

    const saved = await this.save(event);
    return saved;
  }

  /**
   * Compute the title
   * @param data
   */
  async updateTitle(data?: ObservedLocation) {
    data = data || this.data;

    let title;
    // new data
    if (!data || isNil(data.id)) {
      title = await this.translate.get('OBSERVED_LOCATION.NEW.TITLE').toPromise();
    }
    // Existing data
    else {
      title = await this.translate.get('OBSERVED_LOCATION.EDIT.TITLE', {
        location: data.location && (data.location.label || data.location.name),
        dateTime: data.startDateTime && this.dateFormat.transform(data.startDateTime) as string
      }).toPromise();
    }

    // Emit the title
    this.title.next(title);
  }

  protected logFormErrors() {
    if (this.debug) console.debug("[observed-location] Form not valid. Detecting where...");
    if (!this.observedLocationForm.empty && this.observedLocationForm.invalid) {
      AppFormUtils.logFormErrors(this.observedLocationForm.form, "[observed-location] [sale-form] ");
    }
    // if (this.measurementsForm.invalid) {
    //   AppFormUtils.logFormErrors(this.measurementsForm.form, "[page-trip] [measurementsForm-form] ");
    // }
  }

  /**
   * Open the first tab that is invalid
   */
  protected openFirstInvalidTab() {
    const tab0Invalid = this.observedLocationForm.invalid;
    const tab1Invalid = this.landingsTable.invalid;

    const invalidTabIndex = tab0Invalid ? 0 : (tab1Invalid ? 1 : this.selectedTabIndex);
    if (this.selectedTabIndex === 0 && !tab0Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    }
    else if (this.selectedTabIndex === 1 && !tab1Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    }
  }

  public async onControl(event: Event) {
    // Stop if data is not valid
    if (!this.valid) {
      // Stop the control
      event && event.preventDefault();

      // Open the first tab in error
      this.openFirstInvalidTab();
    }
    else if (this.dirty) {

      // Stop the control
      event && event.preventDefault();

      console.debug("[observed-location] Saving data, before control...");
      const saved = await this.save(new Event('save'));
      if (saved) {
        // Loop
        this.qualityForm.control(new Event('control'));
      }

    }
  }

  async onOpenLanding(id: number) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/observations/${this.data.id}/landings/${id}`);
    }
  }

  async onNewLanding(event?: any) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/observations/${this.data.id}/landings/new`);
    }
  }

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
