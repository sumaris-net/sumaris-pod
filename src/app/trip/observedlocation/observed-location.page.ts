import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController} from "@ionic/angular";

import {AccountService, AppFormUtils, AppTabPage, EntityUtils} from '../../core/core.module';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../../../environments/environment';
import {Subject} from 'rxjs';
import {DateFormatPipe, isNil, isNotNil} from '../../shared/shared.module';
import * as moment from "moment";
import {Moment} from "moment";
import {ObservedLocationForm} from "./observed-location.form";
import {ObservedLocation} from "../services/observed-location.model";
import {ObservedLocationService} from "../services/observed-location.service";
import {MeasurementsForm} from "../measurement/measurements.form.component";
import {EntityQualityFormComponent} from "../quality/entity-quality-form.component";
import {ObservedVesselsTable} from "./observed-vessels.table";
import {LocalSettingsService} from "../../core/services/local-settings.service";

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
  showVesselTable = false;
  onRefresh = new EventEmitter<any>();
  isOnFieldMode: boolean;

  @ViewChild('observedLocationForm') observedLocationForm: ObservedLocationForm;

  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

  @ViewChild('qualityForm') qualityForm: EntityQualityFormComponent;

  @ViewChild('vesselTable') vesselTable: ObservedVesselsTable;

  constructor(
    route: ActivatedRoute,
    router: Router,
    alertCtrl: AlertController,
    translate: TranslateService,
    protected dateFormat: DateFormatPipe,
    protected dataService: ObservedLocationService,
    protected settingsService: LocalSettingsService,
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
    this.registerForms([this.observedLocationForm, this.measurementsForm])
      .registerTables([this.vesselTable]);

    this.disable();

    this.route.params.first().subscribe(res => {
      const id = res && res["observedLocationId"];
      if (!id || id === "new") {
        this.load();
      }
      else {
        this.load(parseInt(id));
      }
    });
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
      this.showVesselTable = false;
      this.startListenProgramChanges();
    }

    // Load existing data
    else {
      const data = await this.dataService.load(id);
      this.updateView(data);
      this.loading = false;
      this.showVesselTable = true;
      this.startListenRemoteChanges();
    }
  }

  startListenProgramChanges() {

    // If new trip
    if (isNil(this.data.id)) {

      // Listen program changes (only if new data)
      if (this.observedLocationForm && this.observedLocationForm.form) {
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
                this.updateView(data, true);
              }
            }
          })
        );
    }
  }

  updateView(data: ObservedLocation | null, updateVessels?: boolean) {
    this.data = data;
    this.observedLocationForm.value = data;
    const isSaved = isNotNil(data.id);
    if (isSaved) {
      this.observedLocationForm.form.controls['program'].disable();
      this.programSubject.next(data.program.label);
    }
    this.measurementsForm.value = data && data.measurements || [];
    this.measurementsForm.updateControls();

    this.showVesselTable = isSaved;
    if (updateVessels && this.vesselTable) {
      this.vesselTable.setParent(data);
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

    // Update ObservedLocation from JSON
    let json = this.observedLocationForm.value;
    json.measurements = this.measurementsForm.value;

    this.data.fromObject(json);

    const formDirty = this.dirty;
    const isNew = this.isNewData();

    this.disable();

    try {
      // Save saleControl form (with sale)
      const updatedData = formDirty ? await this.dataService.save(this.data) : this.data;
      if (formDirty) {
        this.markAsPristine();
        this.markAsUntouched();
      }

      // Save vessels
      const isVesselSaved = !this.vesselTable || await this.vesselTable.save();
      if (isVesselSaved && this.vesselTable) {
        this.vesselTable.markAsPristine();
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
    if (isNotNil(this.data.id) && !this.dataService.canUserWrite(this.data)) {
      if (this.debug) console.warn("[observed-location] Leave form disable (User has NO write access)");
      return;
    }
    if (this.debug) console.debug("[observed-location] Enabling form (User has write access)");
    super.enable();
    // Leave program disable once saved
    if (isNotNil(this.data.id)) {
      this.observedLocationForm.form.controls['program'].disable();
    }
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
    if (this.measurementsForm.invalid) {
      AppFormUtils.logFormErrors(this.measurementsForm.form, "[page-trip] [measurementsForm-form] ");
    }
  }

  /**
   * Open the first tab that is invalid
   */
  protected openFirstInvalidTab() {
    const tab0Invalid = this.observedLocationForm.invalid || this.measurementsForm.invalid;
    //TODO
    const tab1Invalid = false;// this.physicalGearTable.invalid;
    const tab2Invalid = false;// this.operationTable.invalid;

    const invalidTabIndex = tab0Invalid ? 0 : (tab1Invalid ? 1 : (tab2Invalid ? 2 : this.selectedTabIndex));
    if (this.selectedTabIndex === 0 && !tab0Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    }
    else if (this.selectedTabIndex === 1 && !tab1Invalid) {
      this.selectedTabIndex = invalidTabIndex;
    }
    else if (this.selectedTabIndex === 2 && !tab2Invalid) {
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

  /* -- protected methods -- */

  protected markForCHeck() {
    this.cd.markForCheck();
  }
}
