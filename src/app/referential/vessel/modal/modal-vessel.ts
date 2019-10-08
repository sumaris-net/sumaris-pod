import {Component, OnInit, ViewChild} from '@angular/core';
import { VesselFeatures } from "../../services/model";
import { ModalController } from "@ionic/angular";
import { VesselForm } from '../form/form-vessel';
import { VesselService } from '../../services/vessel-service';
import { AppFormUtils } from '../../../core/core.module';


@Component({
  selector: 'modal-vessel',
  templateUrl: './modal-vessel.html'
})
export class VesselModal implements OnInit {

  loading = false;

  get disabled() {
    return this.formVessel.disabled;
  }

  get enabled() {
    return this.formVessel.enabled;
  }

  get valid() {
    return this.formVessel.valid;
  }

  @ViewChild('formVessel', { static: true }) formVessel: VesselForm;

  constructor(
    private vesselService: VesselService,
    private viewCtrl: ModalController) {
  }

  ngOnInit(): void {
    this.enable(); // Enable the vessel form, by default
  }

  async onSave(event: any): Promise<any> {

    console.debug("[vessel-modal] Saving new vessel...");

    // Avoid multiple call
    if (this.disabled) return;

    if (this.formVessel.invalid) {
      this.formVessel.markAsTouched({emitEvent: true});

      AppFormUtils.logFormErrors(this.formVessel.form);
      return;
    }

    this.loading = true;

    try {
      const json = this.formVessel.value;
      const data = VesselFeatures.fromObject(json);

      this.disable();

      const savedData = await this.vesselService.save(data);
      await this.viewCtrl.dismiss(savedData);
      this.formVessel.error = null;
    }
    catch (err) {
      this.formVessel.error = err && err.message || err;
      this.enable();
      this.loading = false;
    }
  }

  disable() {
    this.formVessel.disable();
  }

  enable() {
    this.formVessel.enable();
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  onReset(event: any) {
    this.formVessel.setValue(VesselFeatures.fromObject({}));
    this.formVessel.markAsPristine();
    this.formVessel.markAsUntouched();
  }
}
