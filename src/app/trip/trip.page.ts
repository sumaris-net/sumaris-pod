import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, ActivatedRoute } from "@angular/router";
import { AlertController } from "@ionic/angular";

import { TripService } from './services/trip.service';
import { TripForm } from './trip.form';
import { Trip } from './services/trip.model';
import { SaleForm } from './sale/sale.form';
import { OperationTable } from './operation/operations.table';
import { MeasurementsForm } from './measurement/measurements.form';
import { AppTabPage } from '../core/core.module';
import { PhysicalGearTable } from './physicalgear/physicalgears.table';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../environments/environment.prod';
import { Subscription } from 'rxjs-compat';
@Component({
  selector: 'page-trip',
  templateUrl: './trip.page.html',
  styleUrls: ['./trip.page.scss']
})
export class TripPage extends AppTabPage<Trip> implements OnInit {


  protected _enableListenChanges: boolean = false;

  saving: boolean = false;

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
    protected tripService: TripService
  ) {
    super(route, router, alertCtrl, translate);

    // FOR DEV ONLY ----
    this.debug = true;
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
      this.tripService.load(id).first()
        .subscribe(data => {
          this.updateView(data, true);
          this.enable();
          this.loading = false;
          this.startListenChanges();
        });
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

    this.physicalGearTable.value = data && data.gears || [];

    if (updateOperations) {
      this.operationTable && this.operationTable.setTrip(data);
    }

    this.markAsPristine();
    this.markAsUntouched();
  }

  async save(event): Promise<any> {
    if (this.loading || this.saving) return;

    // Copy vessel features, before trying to validate saleForm
    if (this.tripForm.valid && !this.saleForm.empty) {
      this.saleForm.form.controls['vesselFeatures'].setValue(this.tripForm.form.controls['vesselFeatures'].value);
    }

    // Not valid
    if (!this.valid) {
      if (this.debug) console.debug("[page-trip] Form not valid");
      this.submitted = true;
      return;
    }
    this.saving = true;
    this.error = undefined;

    if (this.debug) console.debug("[page-trip] Saving trip...");

    // Update Trip from JSON
    let json = this.tripForm.value;
    json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    this.data.fromObject(json);
    this.data.gears = this.physicalGearTable.value;
    this.data.measurements = this.measurementsForm.value;

    const formDirty = this.dirty;
    const isNew = this.isNewData();
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
          queryParams: this.route.snapshot.queryParams
        });

        // SUbscription to changes
        this.startListenChanges();
      }

      return updatedData;
    }
    catch (err) {
      console.error(err);
      this.submitted = true;
      this.error = err && err.message || err;
    }
    finally {
      this.enable();
      this.submitted = false;
      this.saving = false;
    }
  }


}
