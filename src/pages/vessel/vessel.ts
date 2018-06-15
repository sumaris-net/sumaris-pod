import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from "@angular/router";
import { VesselService } from '../../services/vessel-service';
import { VesselForm } from './form/form-vessel';
import { VesselFeatures } from '../../services/model';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'page-vessel',
  templateUrl: './vessel.html'
})
export class VesselPage implements OnInit {

  loading: boolean = true;
  data: VesselFeatures;

  @ViewChild('form') private form: VesselForm;

  constructor(
    private route: ActivatedRoute,
    private vesselService: VesselService
  ) {
  }

  ngOnInit() {
    // Make sure template has a form
    if (!this.form) throw "[VesselPage] no form for value setting";

    this.route.params.subscribe(res => {
      this.load(parseInt(res["id"]));
    });
  }

  async load(id: number) {
    const vessel = await this.vesselService.loadByVesselFeaturesId(id);
    this.updateView(vessel);
    this.loading = false;
  }

  updateView(data: VesselFeatures | null) {
    this.form.setValue(data);
    this.data = data;
  }

  save(event, json): Promise<any> {
    if (this.loading) return;

    // Update Vessel from JSON
    this.data.fromObject(json);

    return this.vesselService.save(this.data)
      .then((data) => {
        this.updateView(data);
        this.form.markAsPristine();
      });
  }


}
