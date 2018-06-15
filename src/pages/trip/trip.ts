import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from "@angular/router";
import { TripService } from '../../services/trip-service';
import { TripForm } from './form/form-trip';
import { Trip, Sale } from '../../services/model';
import { FormGroup } from '@angular/forms';
import { SaleForm } from '../sale/form/form-sale';

@Component({
  selector: 'page-trip',
  templateUrl: './trip.html'
})
export class TripPage implements OnInit {

  protected error: string;
  protected loading: boolean = true;
  protected saving: boolean = false;
  protected data: Trip;

  @ViewChild('tripForm') protected tripForm: TripForm;

  @ViewChild('saleForm') protected saleForm: SaleForm;

  constructor(
    protected route: ActivatedRoute,
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
    this.route.params.subscribe(res => {
      this.load(parseInt(res["id"]));
    });
  }

  async load(id: number | null) {
    this.data = id ? await this.tripService.load(id) : new Trip();
    this.updateView(this.data);
    this.tripForm.enable();
    this.saleForm.enable();
    this.loading = false;

  }

  updateView(data: Trip | null) {
    this.data = data;
    this.tripForm.setValue(data);
    this.saleForm.setValue(data && data.sale);
  }

  async save(event): Promise<any> {
    if (this.loading || this.saving || !this.valid) return;
    this.saving = true;

    // Update Trip from JSON
    let json = this.tripForm.value;
    json.sale = !this.saleForm.empty ? this.saleForm.value : null;
    this.data.fromObject(json);

    this.tripForm.disable();
    this.saleForm.disable();

    try {
      const res = await this.tripService.save(this.data);
      this.updateView(res);
      this.tripForm.markAsPristine();
      this.tripForm.markAsUntouched();
      this.saleForm.markAsPristine();
      this.saleForm.markAsUntouched();
      return res;
    }
    catch (err) {
      this.error = err && err.message || err;
      return Promise.reject(err);
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



}
