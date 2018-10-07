import { Component, ViewChild } from '@angular/core';
import { VesselFeatures } from "../../services/model";
import { ModalController } from "@ionic/angular";
import { VesselForm } from '../form/form-vessel';
import { VesselService } from '../../services/vessel-service';
import { AppFormUtils } from '../../../core/core.module';


@Component({
  selector: 'modal-vessel',
  templateUrl: './modal-vessel.html'
})
export class VesselModal {

  loading: boolean = false;

  @ViewChild('formVessel') form: VesselForm;

  constructor(
    private vesselService: VesselService,
    private viewCtrl: ModalController) {
  }

  async onSave(event: any): Promise<any> {

    console.debug("[vessel-modal] Saving new vessel...");

    // Avoid multiple call    
    if (this.form.form.disabled) return;

    if (this.form.invalid) {
      AppFormUtils.logFormErrors(this.form.form);
      return;
    }

    this.loading = true;

    try {
      const json = this.form.value;
      let data = VesselFeatures.fromObject(json);

      this.form.disable();

      const res = await this.vesselService.save(data);
      this.viewCtrl.dismiss(res)
    }
    catch (err) {
      this.form.error = err && err.message || err;
      this.form.enable();
      this.loading = false;
    }
  }

  cancel() {
    this.viewCtrl.dismiss();
  }
}
