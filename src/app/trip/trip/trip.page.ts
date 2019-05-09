import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController} from "@ionic/angular";

import {TripService} from '../services/trip.service';
import {TripForm} from './trip.form';
import {EntityUtils, Trip} from '../services/trip.model';
import {SaleForm} from '../sale/sale.form';
import {OperationTable} from '../operation/operations.table';
import {MeasurementsForm} from '../measurement/measurements.form.component';
import {AccountService, AppFormUtils, AppTabPage, environment} from '../../core/core.module';
import {PhysicalGearTable} from '../physicalgear/physicalgears.table';
import {TranslateService} from '@ngx-translate/core';
import {Subject} from 'rxjs';
import {DateFormatPipe, isNil, isNotNil} from '../../shared/shared.module';
import {EntityQualityFormComponent} from "../quality/entity-quality-form.component";
import * as moment from "moment";
import {Moment} from "moment";
import {LocalSettingsService} from "../../core/services/local-settings.service";

@Component({
  selector: 'page-trip',
  templateUrl: './trip.page.html',
  styleUrls: ['./trip.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripPage extends AppTabPage<Trip> implements OnInit {

  protected _enableListenChanges = (environment.listenRemoteChanges === true);

  programSubject = new Subject<string>();
  title = new Subject<string>();
  saving: boolean = false;
  defaultBackHref = "/trips";
  showOperationTable = false;
  showGearTable = false;
  onRefresh = new EventEmitter<any>();
  isOnFieldMode: boolean;

  @ViewChild('tripForm') tripForm: TripForm;

  @ViewChild('saleForm') saleForm: SaleForm;

  @ViewChild('physicalGearTable') physicalGearTable: PhysicalGearTable;

  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

  @ViewChild('operationTable') operationTable: OperationTable;

  @ViewChild('qualityForm') qualityForm: EntityQualityFormComponent;

  constructor(
    route: ActivatedRoute,
    router: Router,
    alertCtrl: AlertController,
    translate: TranslateService,
    protected dateFormat: DateFormatPipe,
    protected accountService: AccountService,
    protected dataService: TripService,
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
    this.registerForms([this.tripForm, this.saleForm, this.measurementsForm])
      .registerTables([this.physicalGearTable, this.operationTable]);

    this.disable();

    this.route.params.first().subscribe(res => {
      const id = res && res["tripId"];
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
      const data = new Trip();

      // If is on field mode, fill default values
      if (this.isOnFieldMode) {
        data.departureDateTime = moment();
        // TODO : get the default program from local settings ?
        //data.program = ...
      }

      this.updateView(data, true);
      this.loading = false;
      this.showGearTable = false;
      this.showOperationTable = false;
      this.startListenProgramChanges();
    }

    // Load existing data
    else {
      const data = await this.dataService.load(id);
      this.updateView(data, true);
      this.loading = false;
      this.showGearTable = true;
      this.showOperationTable = true;
      this.startListenRemoteChanges();
    }
  }

  startListenProgramChanges() {

    // If new trip
    if (isNil(this.data.id)) {

      // Listen program changes (only if new data)
      if (this.tripForm && this.tripForm.form) {
        this.registerSubscription(this.tripForm.form.controls['program'].valueChanges
          .subscribe(program => {
            if (EntityUtils.isNotEmpty(program)) {
              console.debug("[trip] Propagate program change: " + program.label);
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
          .subscribe((data: Trip | undefined) => {
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

  updateView(data: Trip | null, updateOperations?: boolean) {
    this.data = data;
    this.tripForm.value = data;
    const isSaved = isNotNil(data.id);
    if (isSaved) {
      this.tripForm.form.controls['program'].disable();
      this.programSubject.next(data.program.label);
    }
    this.saleForm.value = data && data.sale;
    this.measurementsForm.value = data && data.measurements || [];
    this.measurementsForm.updateControls();

    // Physical gear table
    this.physicalGearTable.value = data && data.gears || [];
    this.showGearTable = isSaved;

    // Operations table
    this.showOperationTable = isSaved;
    if (updateOperations && this.operationTable) {
      this.operationTable.setTrip(data);
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

    // Copy vessel features, before trying to validate saleForm
    if (this.tripForm.valid && !this.saleForm.empty) {
      this.saleForm.form.controls['vesselFeatures'].setValue(this.tripForm.form.controls['vesselFeatures'].value);
    }

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

    if (this.debug) console.debug("[page-trip] Saving trip...");

    // Update Trip from JSON
    let json = this.tripForm.value;
    json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    json.measurements = this.measurementsForm.value;

    this.data.fromObject(json);

    const formDirty = this.dirty;
    const isNew = this.isNewData();

    // Update gears, from table
    await this.physicalGearTable.save();
    this.data.gears = this.physicalGearTable.value;

    this.disable();

    try {
      // Save trip form (with sale)
      const updatedData = formDirty ? await this.dataService.save(this.data) : this.data;
      if (formDirty) {
        this.markAsPristine();
        this.markAsUntouched();
      }

      // Save operations
      const isOperationSaved = !this.operationTable || await this.operationTable.save();
      if (isOperationSaved && this.operationTable) {
        this.operationTable.markAsPristine();
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
      if (this.debug) console.warn("[trip] Leave form disable (User has NO write access)");
      return;
    }
    if (this.debug) console.debug("[trip] Enabling form (User has write access)");
    super.enable();
    // Leave program disable once saved
    if (isNotNil(this.data.id)) {
      this.tripForm.form.controls['program'].disable();
    }
  }

  async onOperationClick(opeId: number) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/trips/${this.data.id}/operations/${opeId}`);
    }
  }

  async onNewOperationClick(event?: any) {
    const savedOrContinue = await this.saveIfDirtyAndConfirm();
    if (savedOrContinue) {
      await this.router.navigateByUrl(`/trips/${this.data.id}/operations/new`);
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
  async updateTitle(data?: Trip) {
    data = data || this.data;

    let title;
    // new data
    if (!data || isNil(data.id)) {
      title = await this.translate.get('TRIP.NEW.TITLE').toPromise();
    }
    // Existing data
    else {
      title = await this.translate.get('TRIP.EDIT.TITLE', {
        vessel: data.vesselFeatures && (data.vesselFeatures.exteriorMarking || data.vesselFeatures.name),
        departureDateTime: data.departureDateTime && this.dateFormat.transform(data.departureDateTime) as string
      }).toPromise();
    }

    // Emit the title
    this.title.next(title);
  }

  protected logFormErrors() {
    if (this.debug) console.debug("[page-trip] Form not valid. Detecting where...");
    if (this.tripForm.invalid) {
      AppFormUtils.logFormErrors(this.tripForm.form, "[page-trip] ");
    }
    if (!this.saleForm.empty && this.saleForm.invalid) {
      AppFormUtils.logFormErrors(this.saleForm.form, "[page-trip] [sale-form] ");
    }
    if (this.physicalGearTable.invalid) {
      AppFormUtils.logFormErrors(this.physicalGearTable.gearForm.form, "[page-trip] [gear-form] ");
      AppFormUtils.logFormErrors(this.physicalGearTable.gearForm.measurementsForm.form, "[page-trip] [gear-measurements-form] ");
    }
    if (this.measurementsForm.invalid) {
      AppFormUtils.logFormErrors(this.measurementsForm.form, "[page-trip] [measurementsForm-form] ");
    }
  }

  /**
   * Open the first tab that is invalid
   */
  protected openFirstInvalidTab() {
    const tab0Invalid = this.tripForm.invalid || this.measurementsForm.invalid;
    const tab1Invalid = this.physicalGearTable.invalid;
    const tab2Invalid = this.operationTable.invalid;

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

      console.debug("[trip] Saving data, before control...");
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
