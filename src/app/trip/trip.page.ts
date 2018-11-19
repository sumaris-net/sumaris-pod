import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, ActivatedRoute } from "@angular/router";
import { AlertController } from "@ionic/angular";

import { TripService } from './services/trip.service';
import { TripForm } from './trip.form';
import { Trip } from './services/trip.model';
import { SaleForm } from './sale/sale.form';
import { OperationTable } from './operation/operations.table';
import { MeasurementsForm } from './measurement/measurements.form';
import { AppTabPage, AppFormUtils, AccountService } from '../core/core.module';
import { PhysicalGearTable } from './physicalgear/physicalgears.table';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../environments/environment';
import { Subscription, Subject } from 'rxjs';
import { DateFormatPipe } from '../shared/pipes/date-format.pipe';
import { isNil } from '../core/services/model';
@Component({
  selector: 'page-trip',
  templateUrl: './trip.page.html',
  styleUrls: ['./trip.page.scss']
})
export class TripPage extends AppTabPage<Trip> implements OnInit {


  protected _enableListenChanges: boolean = false;

  title = new Subject<string>();
  saving: boolean = false;
  defaultBackHref: string = "/trips";

  @ViewChild('tripForm') tripForm: TripForm;

  @ViewChild('saleForm') saleForm: SaleForm;

  @ViewChild('physicalGearTable') physicalGearTable: PhysicalGearTable;

  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

  @ViewChild('operationTable') operationTable: OperationTable;

  constructor(
    route: ActivatedRoute,
    router: Router,
    alertCtrl: AlertController,
    translate: TranslateService,
    protected dateFormat: DateFormatPipe,
    protected tripService: TripService
  ) {
    super(route, router, alertCtrl, translate);

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    // Register forms & tables
    this.registerForms([this.tripForm, this.saleForm, this.measurementsForm])
      .registerTables([this.physicalGearTable, this.operationTable]);

    this.disable();

    this.route.params.subscribe(res => {
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
    // new
    if (!id) {

      // Create using default values
      const data = Trip.fromObject({
        program: { label: environment.defaultProgram }
      });

      this.updateView(data, true);
      this.enable();
      this.loading = false;
    }

    // Load
    else {
      const data = await this.tripService.load(id).first().toPromise();
      this.updateView(data, true);
      this.enable();
      this.loading = false;
      this.startListenChanges();
    }
  }

  startListenChanges() {
    if (!this._enableListenChanges) return;

    const subscription = this.tripService.listenChanges(this.data.id)
      .subscribe((data: Trip | undefined) => {
        if (data && data.updateDate) {
          if (this.debug) console.debug("[trip] Detected update on server", data.updateDate, this.data.updateDate);
        }
      });

    // Add log when closing
    if (this.debug) subscription.add(() => console.debug('[trip] [WS] Stop to listen changes'));

    //.subscribe(data => this.updateView(data, true));
    this.registerSubscription(subscription);
  }

  updateView(data: Trip | null, updateOperations?: boolean) {
    this.data = data;
    this.tripForm.value = data;
    this.saleForm.value = data && data.sale;
    this.measurementsForm.value = data && data.measurements || [];
    this.measurementsForm.updateControls();

    this.physicalGearTable.value = data && data.gears || [];

    if (updateOperations) {
      this.operationTable && this.operationTable.setTrip(data);
    }

    this.updateTitle();

    this.markAsPristine();
    this.markAsUntouched();
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
      if (this.debug) {
        console.debug("[page-trip] Form not valid. Detecting where...");
        if (this.tripForm.invalid) {
          AppFormUtils.logFormErrors(this.saleForm.form, "[page-trip] [gear-form] ");
        }
        if (!this.saleForm.empty && this.saleForm.invalid) {
          AppFormUtils.logFormErrors(this.saleForm.form, "[page-trip] [sale-form] ");
        }
        if (this.physicalGearTable.invalid) {
          AppFormUtils.logFormErrors(this.physicalGearTable.gearForm.form, "[page-trip] [gear-form] ");
          AppFormUtils.logFormErrors(this.physicalGearTable.gearForm.measurementsForm.form, "[page-trip] [gear-measurementsForm] ");

        }
      }
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
      const updatedData = formDirty ? await this.tripService.save(this.data) : this.data;
      formDirty && this.markAsPristine();
      formDirty && this.markAsUntouched();

      // Save operations
      const isOperationSaved = !this.operationTable || await this.operationTable.save();
      isOperationSaved && this.operationTable && this.operationTable.markAsPristine();

      // Update the view (e.g metadata)
      this.updateView(updatedData, false);

      // Update route location
      if (isNew) {
        this.router.navigate(['../' + updatedData.id], {
          relativeTo: this.route,
          queryParams: this.route.snapshot.queryParams,
          replaceUrl: true // replace the current satte in history
        });

        // SUbscription to changes
        this.startListenChanges();
      }

      return true;
    }
    catch (err) {
      console.error(err);
      this.submitted = true;
      this.error = err && err.message || err;
      return false;
    }
    finally {
      this.enable();
      this.submitted = false;
      this.saving = false;
    }
  }

  enable() {
    if (!this.data) return false;
    // If not a new trip, check user can write
    if ((this.data.id || this.data.id === 0) && !this.tripService.canUserWrite(this.data)) {
      if (this.debug) console.warn("[trip] Leave form disable (User has NO write access)");
      return;
    }
    if (this.debug) console.debug("[trip] Enabling form (User has write access)");
    super.enable();
  }

  async onOperationClick(opeId: number) {
    const savedOrContinu = await this.saveIfDirtyAndConfirm();

    if (savedOrContinu) {
      this.router.navigateByUrl('/operations/' + this.data.id + '/' + opeId);
    }
  }

  async onNewOperationClick(event?: any) {
    const savedOrContinu = await this.saveIfDirtyAndConfirm();
    if (savedOrContinu) {
      this.router.navigateByUrl('/operations/' + this.data.id + '/new');
    }
  }

  protected async saveIfDirtyAndConfirm(): Promise<boolean> {
    if (!this.dirty) return true;

    let confirm = false;
    let cancel = false;
    const translations = this.translate.instant(['COMMON.BTN_SAVE', 'COMMON.BTN_CANCEL', 'COMMON.BTN_NOT_SAVE', 'CONFIRM.SAVE', 'CONFIRM.ALERT_HEADER']);
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
          text: translations['COMMON.BTN_NOT_SAVE'],
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

    // new trip
    let title;
    if (!data || isNil(data.id)) {
      title = await this.translate.get('TRIP.NEW.TITLE').toPromise();
    }
    // Existing trip
    else {
      title = await this.translate.get('TRIP.EDIT.TITLE', {
        vessel: data.vesselFeatures && (data.vesselFeatures.exteriorMarking || data.vesselFeatures.name),
        departureDateTime: data.departureDateTime && this.dateFormat.transform(data.departureDateTime) as string
      }).toPromise();
    }

    // Emit the title
    this.title.next(title);
  }
}
