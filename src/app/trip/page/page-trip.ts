import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, ActivatedRoute, Params, NavigationEnd } from "@angular/router";
import { MatTabChangeEvent } from "@angular/material";
import { TripService } from '../services/trip-service';
import { TripForm } from '../form/form-trip';
import { Trip, Sale } from '../services/model';
import { FormGroup } from '@angular/forms';
import { SaleForm } from '../sale/form/form-sale';
import { OperationTable } from '../operation/table/table-operations';
import { Observable } from "rxjs-compat";
import { PhysicalGearForm } from '../physicalGear/form/form-physical-gear';

@Component({
  selector: 'page-trip',
  templateUrl: './page-trip.html',
  styleUrls: ['./page-trip.scss']
})
export class TripPage implements OnInit {

  selectedTabIndex: number = 0; // TODO

  id: any;
  error: string;
  loading: boolean = true;
  saving: boolean = false;
  data: Trip;

  public get dirty(): boolean {
    return this.tripForm.dirty || this.saleForm.dirty || this.gearForm.dirty || this.operationTable.dirty;
  }

  @ViewChild('tripForm') tripForm: TripForm;

  @ViewChild('saleForm') saleForm: SaleForm;

  @ViewChild('gearForm') gearForm: PhysicalGearForm;

  @ViewChild('operationTable') operationTable: OperationTable;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected tripService: TripService
  ) {
  }


  public get valid(): boolean {
    return this.tripForm.form.valid && (this.saleForm.form.valid || this.saleForm.empty) && this.gearForm.form.valid;
  }

  ngOnInit() {
    // Make sure template has a form
    if (!this.tripForm || !this.saleForm) throw "[TripPage] no form for value setting";

    this.disable();

    // Listen route parameters
    this.route.queryParams.subscribe(res => {
      const tabIndex = res["tab"];
      if (tabIndex !== undefined) {
        this.selectedTabIndex = parseInt(tabIndex);
      }
    });

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

  async load(id?: number) {
    this.error = null;
    if (id) {
      let obs = this.tripService.load(id);
      if (obs['subscribe']) {
        const obs2 = obs as Observable<Trip | null>;
        obs2.subscribe(data => {
          this.data = data;
          this.updateView(this.data, true);
          this.enable();
          this.loading = false;
        });
      }
    }
    else {
      this.data = new Trip();
      this.updateView(this.data, true);
      this.enable();
      this.loading = false;
    }
  }

  updateView(data: Trip | null, updateOperations?: boolean) {
    this.data = data;
    this.tripForm.setValue(data);
    this.saleForm.setValue(data && data.sale);
    this.gearForm.setValue(data && data.gears && data.gears[0]);
    if (updateOperations) {
      this.operationTable && this.operationTable.setTrip(data);
    }
    this.markAsPristine();
    this.markAsUntouched();
  }

  async save(event): Promise<any> {
    if (this.loading || this.saving || !this.valid) return;
    this.saving = true;

    console.log("[page-trip] Saving...");

    // Update Trip from JSON
    let json = this.tripForm.value;
    json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    json.gears = [this.gearForm.value];
    this.data.fromObject(json);

    const tripDirty = this.tripForm.dirty || this.saleForm.dirty || this.gearForm.dirty;
    this.disable();

    try {
      // Save trip form (with sale) 
      const updatedData = tripDirty ? await this.tripService.save(this.data) : this.data;
      this.markAsPristine();
      this.markAsUntouched();

      // Save operations
      const isOperationSaved = !this.operationTable || await this.operationTable.save();
      isOperationSaved && this.operationTable && this.operationTable.markAsPristine();

      // Update the view (e.g metadata)
      this.updateView(updatedData, false);
      return updatedData;
    }
    catch (err) {
      console.error(err);
      this.error = err && err.message || err;
    }
    finally {
      this.enable();
      this.saving = false;
    }
  }

  public disable() {
    this.tripForm.disable();
    this.saleForm.disable();
    this.gearForm.disable();
  }

  public enable() {
    this.tripForm.enable();
    this.saleForm.enable();
    this.gearForm.enable();
  }

  public markAsPristine() {
    this.tripForm.markAsPristine();
    this.saleForm.markAsPristine();
    this.gearForm.markAsPristine();
  }

  public markAsUntouched() {
    this.tripForm.markAsUntouched();
    this.saleForm.markAsUntouched();
    this.gearForm.markAsUntouched();
  }

  async cancel() {
    // reload
    this.loading = true;
    await this.load(this.data.id);

  }

  onTabChange(event: MatTabChangeEvent) {
    const queryParams: Params = Object.assign({}, this.route.snapshot.queryParams);
    queryParams['tab'] = event.index;
    this.router.navigate(['.'], {
      relativeTo: this.route,
      queryParams: queryParams
    });
  }


}
