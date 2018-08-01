import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, ActivatedRoute, Params, NavigationEnd } from "@angular/router";
import { MatTabChangeEvent } from "@angular/material";
import { OperationService } from '../../services/operation-service';
import { OperationForm } from '../form/form-operation';
import { Operation, Trip } from '../../services/model';
import { FormGroup } from '@angular/forms';
import { Observable } from "rxjs-compat";
import { TripService } from '../../services/trip-service';
import { MeasurementList } from '../../measurement/list/list-measurements';

@Component({
  selector: 'page-operation',
  templateUrl: './page-operation.html',
  styleUrls: ['./page-operation.scss']
})
export class OperationPage implements OnInit {

  selectedTabIndex: number = 0; // TODO

  trip: Trip;
  data: Operation;
  error: string;
  loading: boolean = true;
  saving: boolean = false;

  public get dirty(): boolean {
    return this.opeForm.dirty || this.measurementList.dirty;
  }

  public get valid(): boolean {
    return this.opeForm.form.valid && this.measurementList.form.valid;
  }

  @ViewChild('opeForm') opeForm: OperationForm;

  @ViewChild('measurementList') measurementList: MeasurementList;


  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected operationService: OperationService,
    protected tripService: TripService
  ) {
  }


  ngOnInit() {
    // Make sure template has a form
    if (!this.opeForm) throw "no form for value setting";

    this.disable();

    // Listen route parameters
    this.route.queryParams.subscribe(res => {
      const tabIndex = res["tab"];
      if (tabIndex !== undefined) {
        this.selectedTabIndex = parseInt(tabIndex);
      }
    });

    this.route.params.subscribe(res => {
      const tripId = res && res["tripId"];
      const id = res && res["opeId"];
      if (!id || id === "new") {
        this.load(tripId);
      }
      else {
        this.load(tripId, parseInt(id));
      }
    });

  }

  async load(tripId: number, id?: number) {
    this.error = null;

    // Existing operation
    if (id) {
      console.debug("[page-operation] Loading operation...");
      this.operationService.load(id)
        .subscribe(data => {
          if (!data || !data.tripId) {
            console.error("Unable to load operation with id:" + id);
            this.error = "TRIP.OPERATION.ERROR.LOAD_OPERATION_ERROR";
            this.loading = false;
            return;
          }

          this.tripService.load(data.tripId)
            .subscribe(trip => {
              this.updateView(data, trip);
              this.enable();
              this.loading = false;
            });
        });
    }

    // New operation
    else {
      this.tripService.load(tripId)
        .subscribe(trip => {
          this.updateView(new Operation(), trip);
          this.enable();
          this.loading = false;
        });
    }
  }

  updateView(data: Operation | null, trip?: Trip) {
    this.data = data;
    this.opeForm.value = data;
    if (trip) {
      this.trip = trip;
      this.opeForm.setTrip(trip);
    }

    this.measurementList.value = data.measurements || [];

    this.markAsPristine();
    this.markAsUntouched();
  }

  async save(event): Promise<any> {
    if (this.loading || this.saving || !this.valid) return;
    this.saving = true;

    console.log("[page-operation] Saving...");

    // Update entity from JSON
    let json = this.opeForm.value;
    this.data.fromObject(json);
    this.data.tripId = this.trip.id;

    const formDirty = this.opeForm.dirty;
    const measurementsDirty = this.measurementList.dirty;

    if (measurementsDirty) {
      this.data.measurements = this.measurementList.value;
    }

    this.disable();

    try {
      // Save trip form (with sale) 
      const updatedData = formDirty || measurementsDirty ? await this.operationService.save(this.data) : this.data;
      this.markAsPristine();
      this.markAsUntouched();

      // Update the view (e.g metadata)
      this.updateView(updatedData);
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
    this.opeForm.disable();
    this.measurementList.disable();
  }

  public enable() {
    this.opeForm.enable();
    this.measurementList.enable();
  }

  public markAsPristine() {
    this.opeForm.markAsPristine();
    this.measurementList.markAsPristine();
  }

  public markAsUntouched() {
    this.opeForm.markAsUntouched();
    this.measurementList.markAsUntouched();
  }

  async cancel() {
    // reload
    this.loading = true;
    await this.load(this.trip.id, this.data.id);
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
