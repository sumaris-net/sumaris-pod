import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, ActivatedRoute, Params } from "@angular/router";
import { MatTabChangeEvent } from "@angular/material";
import { TripService } from '../services/trip-service';
import { TripForm } from '../form/form-trip';
import { Trip, Sale } from '../services/model';
import { FormGroup } from '@angular/forms';
import { SaleForm } from '../sale/form/form-sale';
import { OperationTable } from '../operation/table/table-operations';
import { Observable } from "rxjs";
import { slideInOutAnimation } from '../../shared/material/material.module';

@Component({
  selector: 'page-trip',
  templateUrl: './page-trip.html',

  // make fade in animation available to this component
  animations: [slideInOutAnimation],

  // attach the fade in animation to the host (root) element of this component
  host: { '[@slideInOutAnimation]': '' }
})
export class TripPage implements OnInit {

  selectedTabIndex: number = 0; // TODO

  protected error: string;
  protected loading: boolean = true;
  protected saving: boolean = false;
  protected data: Trip;

  public get dirty(): boolean {
    return this.tripForm.dirty || this.saleForm.dirty || this.operationTable.dirty;
  }

  @ViewChild('tripForm') protected tripForm: TripForm;

  @ViewChild('saleForm') protected saleForm: SaleForm;

  @ViewChild('operationTable') protected operationTable: OperationTable;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected tripService: TripService
  ) {


  }

  public get valid(): boolean {
    return this.tripForm.form.valid && (this.saleForm.form.valid || this.saleForm.empty);
  }

  ngOnInit() {
    // Make sure template has a form
    if (!this.tripForm || !this.saleForm) throw "[TripPage] no form for value setting";

    this.tripForm.disable();
    this.saleForm.disable();

    // Listen route parameters
    this.route.queryParams.subscribe(res => {
      const tabIndex = res["tab"];
      if (tabIndex !== undefined) {
        this.selectedTabIndex = parseInt(tabIndex);
      }
    });

    this.route.params.subscribe(res => {
      const id = res && res["id"];
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
          this.updateView(this.data);
          this.tripForm.enable();
          this.saleForm.enable();
          this.loading = false;
        });
      }
    }
    else {
      this.data = new Trip();
      this.updateView(this.data);
      this.tripForm.enable();
      this.saleForm.enable();
      this.loading = false;
    }
  }

  updateView(data: Trip | null) {
    this.data = data;
    this.tripForm.setValue(data);
    this.saleForm.setValue(data && data.sale);
    this.operationTable && this.operationTable.setValue(data);
  }

  async save(event): Promise<any> {
    if (this.loading || this.saving || !this.valid) return;
    this.saving = true;

    console.log("Saving...");

    // Update Trip from JSON
    let json = this.tripForm.value;
    json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    this.data.fromObject(json);

    this.tripForm.disable();
    this.saleForm.disable();
    //this.operationTable.disable()

    try {
      const updatedData = this.tripForm.dirty || this.saleForm.dirty ? await this.tripService.save(this.data) : this.data;
      const isOperationSaved = !this.operationTable || await this.operationTable.save();

      this.updateView(updatedData);
      this.tripForm.markAsPristine();
      this.tripForm.markAsUntouched();
      this.saleForm.markAsPristine();
      this.saleForm.markAsUntouched();
      isOperationSaved && this.operationTable && this.operationTable.markAsPristine();
      return updatedData;
    }
    catch (err) {
      console.error(err);
      this.error = err && err.message || err;
      //return Promise.reject(err);
    }
    finally {
      this.tripForm.enable();
      this.saleForm.enable();
      this.saving = false;
    }
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
