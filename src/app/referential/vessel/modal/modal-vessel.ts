import { Component, ViewChild } from '@angular/core';
import { VesselFeatures } from "../../services/model";
import { ViewController } from "ionic-angular";
import { VesselForm } from '../form/form-vessel';
import { VesselService } from '../../services/vessel-service';


@Component({
  selector: 'modal-vessel',
  templateUrl: './modal-vessel.html'
})
export class VesselModal {

  loading: boolean = false;

  @ViewChild('form') private form: VesselForm;

  constructor(
    private vesselService: VesselService,
    private viewCtrl: ViewController) {
  }

  onSave(json: any): Promise<any> {

    // Avoid multiple call    
    if (this.form.form.disabled) return;
    this.form.disable();

    let data = new VesselFeatures();
    data.fromObject(json);

    return this.vesselService.save(data)
      .then((res) => this.viewCtrl.dismiss(res))
      .catch(err => {
        this.form.error = err && err.message || err;
        this.form.enable();
      });
  }

  cancel() {
    this.viewCtrl.dismiss();
  }
}
