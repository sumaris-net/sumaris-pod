import {Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController} from "@ionic/angular";

import {AccountService, AppFormUtils, AppTabPage, ReferentialRef} from '../../core/core.module';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../../../environments/environment';
import {Subject} from 'rxjs';
import {DateFormatPipe, isNil, isNotNil} from '../../shared/shared.module';
import * as moment from "moment";
import {ObservedLocationForm} from "./observed-location.form";
import {ObservedLocation} from "../services/observed-location.model";
import {ObservedLocationService} from "../services/observed-location.service";
import {MeasurementsForm} from "../measurement/measurements.form.component";
import {EntityQualityMetadataComponent} from "../quality/entity-quality-metadata.component";

@Component({
  selector: 'page-observed-location',
  templateUrl: './observed-location.page.html'
})
export class ObservedLocationPage extends AppTabPage<ObservedLocation> implements OnInit {

  protected _enableListenChanges: boolean = false; // TODO: enable

  title = new Subject<string>();
  saving: boolean = false;
  defaultBackHref: string = "/observations";
  showOperationTable = false;
  onRefresh = new EventEmitter<any>();

  @ViewChild('observedLocationForm') observedLocationForm: ObservedLocationForm;

  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

  @ViewChild('qualityForm') qualityForm: EntityQualityMetadataComponent;

  constructor(
    route: ActivatedRoute,
    router: Router,
    alertCtrl: AlertController,
    translate: TranslateService,
    protected dateFormat: DateFormatPipe,
    protected accountService: AccountService,
    protected dataService: ObservedLocationService
  ) {
    super(route, router, alertCtrl, translate);

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Register forms & tables
    this.registerForms([this.observedLocationForm, this.measurementsForm]);
      //.registerTables([this.physicalGearTable, this.operationTable]);

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

    // New saleControl
    if (isNil(id)) {

      // Create using default values
      const data = new ObservedLocation();

      const isOnFieldMode = this.accountService.isUsageMode('FIELD');
      // If is on field mode, fill default values
      if (isOnFieldMode) {
        data.startDateTime = moment();
        data.program = ReferentialRef.fromObject({label: environment.defaultProgram});
      }

      this.updateView(data);
      this.loading = false;
      this.showOperationTable = false;
    }

    // Load existing saleControl
    else {
      const data = await this.dataService.load(id).first().toPromise();
      this.updateView(data);
      this.loading = false;
      this.showOperationTable = true;
      this.startListenChanges();
    }
  }

  startListenChanges() {
    if (!this._enableListenChanges) return;

    // TODO
    /*const subscription = this.dataService.listenChanges(this.data.id)
      .subscribe((data: ObservedLocation | undefined) => {
        const newUpdateDate = data && (data.updateDate as Moment)|| undefined;
        if (isNotNil(newUpdateDate) && newUpdateDate.isAfter(this.data.updateDate)) {
          if (this.debug) console.debug("[sale-control] Detected update on server", newUpdateDate);
          if (!this.dirty) {
            this.updateView(data);
          }
        }
      });

    // Add log when closing
    if (this.debug) subscription.add(() => console.debug('[sale-control] [WS] Stop to listen changes'));

    this.registerSubscription(subscription);*/
  }

  updateView(data: ObservedLocation | null) {
    this.data = data;
    this.observedLocationForm.value = data;
    this.measurementsForm.value = data && data.measurements || [];
    this.measurementsForm.updateControls();

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

    if (this.debug) console.debug("[page-sale-control] Saving control...");

    // Update ObservedLocation from JSON
    let json = this.observedLocationForm.value;
    json.measurements = this.measurementsForm.value;

    this.data.fromObject(json);

    const formDirty = this.dirty;
    const isNew = this.isNewData();

    // TODO Update vessels, from table
    //await this.vesselTable.save();
    //this.data.vessels = this.vesselTable.value;

    this.disable();

    try {
      // Save saleControl form (with sale)
      const updatedData = formDirty ? await this.dataService.save(this.data) : this.data;
      formDirty && this.markAsPristine();
      formDirty && this.markAsUntouched();


      // Update the view (e.g metadata)
      this.updateView(updatedData);

      // Update route location
      if (isNew) {
        this.router.navigate(['../' + updatedData.id], {
          relativeTo: this.route,
          queryParams: this.route.snapshot.queryParams,
          replaceUrl: true // replace the current state in history
        });

        // Subscription to changes
        this.startListenChanges();
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
    if ((this.data.id || this.data.id === 0) && !this.dataService.canUserWrite(this.data)) {
      if (this.debug) console.warn("[sale-control] Leave form disable (User has NO write access)");
      return;
    }
    if (this.debug) console.debug("[sale-control] Enabling form (User has write access)");
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

    // new saleControl
    let title;
    if (!data || isNil(data.id)) {
      title = await this.translate.get('OBSERVED_LOCATION.NEW.TITLE').toPromise();
    }
    // Existing saleControl
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
    if (this.debug) console.debug("[page-sale-control] Form not valid. Detecting where...");
    if (!this.observedLocationForm.empty && this.observedLocationForm.invalid) {
      AppFormUtils.logFormErrors(this.observedLocationForm.form, "[page-sale-control] [sale-form] ");
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
    // Stop if saleControl is not valid
    if (!this.valid) {
      // Stop the control
      event && event.preventDefault();

      // Open the first tab in error
      this.openFirstInvalidTab();
    }
    else if (this.dirty) {

      // Stop the control
      event && event.preventDefault();

      console.debug("[sale-control] Saving control, before settings as controlled...");
      const saved = await this.save(new Event('save'));
      if (saved) {
        // Loop
        this.qualityForm.control(new Event('control'));
      }

    }
  }
}
