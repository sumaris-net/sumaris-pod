import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, ActivatedRoute, Params } from "@angular/router";
import { MatTabChangeEvent } from "@angular/material";
import { TripService } from './services/trip.service';
import { TripForm } from './trip.form';
import { Trip } from './services/trip.model';
import { SaleForm } from './sale/sale.form';
import { OperationTable } from './operation/operations.table';
import { MeasurementsForm } from './measurement/measurements.form';
import { AppForm, AppTable, AppTabPage } from '../core/core.module';
import { PhysicalGearTable } from './physicalgear/physicalgears.table';

@Component({
  selector: 'page-trip',
  templateUrl: './trip.page.html',
  styleUrls: ['./trip.page.scss']
})
export class TripPage extends AppTabPage<Trip> implements OnInit {


  saving: boolean = false;

  @ViewChild('tripForm') tripForm: TripForm;

  @ViewChild('saleForm') saleForm: SaleForm;

  @ViewChild('physicalGearTable') physicalGearTable: PhysicalGearTable;

  @ViewChild('measurementsForm') measurementsForm: MeasurementsForm;

  @ViewChild('operationTable') operationTable: OperationTable;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected tripService: TripService
  ) {
    super(route, router);
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
    if (id) {
      this.tripService.load(id)
        .subscribe(data => {
          this.updateView(data, true);
          this.enable();
          this.loading = false;
        });
    }
    else {
      this.updateView(new Trip(), true);
      this.enable();
      this.loading = false;
    }
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
    if (this.tripForm.valid) {
      this.saleForm.form.controls['vesselFeatures'].setValue(this.tripForm.form.controls['vesselFeatures'].value);
    }

    // Not valid
    if (!this.valid) {
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

  async cancel() {
    // reload
    this.loading = true;
    await this.load(this.data.id);
  }

}
