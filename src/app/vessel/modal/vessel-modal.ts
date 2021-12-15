import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Vessel } from '../services/model/vessel.model';
import { IonContent, ModalController } from '@ionic/angular';
import { VesselForm } from '../form/form-vessel';
import { VesselService } from '../services/vessel-service';
import { AppFormUtils, ConfigService, isNotNil } from '@sumaris-net/ngx-components';
import { Subscription } from 'rxjs';
import { VESSEL_CONFIG_OPTIONS } from '@app/vessel/services/config/vessel.config';
import { SynchronizationStatus } from '@app/data/services/model/model.utils';

export interface VesselModalOptions {
  defaultStatus?: number;
  canEditStatus?: boolean;
}

@Component({
  selector: 'vessel-modal',
  templateUrl: './vessel-modal.html'
})
export class VesselModal implements OnInit, OnDestroy, VesselModalOptions {

  loading = false;
  subscription = new Subscription();

  @Input() defaultStatus: number;
  @Input() canEditStatus = true;

  @Input() synchronizationStatus: SynchronizationStatus|null = null;

  get disabled() {
    return this.formVessel.disabled;
  }

  get enabled() {
    return this.formVessel.enabled;
  }

  get valid() {
    return this.formVessel.valid;
  }

  @ViewChild(VesselForm, {static: true}) formVessel: VesselForm;

  @ViewChild(IonContent, {static: true}) content: IonContent;

  constructor(
    private vesselService: VesselService,
    private configService: ConfigService,
    private viewCtrl: ModalController) {
  }

  ngOnInit(): void {
    this.enable(); // Enable the vessel form, by default

    if (isNotNil(this.defaultStatus)) {
      this.formVessel.defaultStatus = this.defaultStatus;
    }
    else {
      // Get default status by config
      this.subscription.add(
        this.configService.config.subscribe(config => {
          if (config && config.properties) {
            const defaultStatus = config.properties[VESSEL_CONFIG_OPTIONS.VESSEL_DEFAULT_STATUS.key];
            if (defaultStatus) {
              this.formVessel.defaultStatus = +defaultStatus;
            }
          }
        })
      );
    }
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  async onSave(event: any): Promise<any> {

    console.debug("[vessel-modal] Saving new vessel...");

    // Avoid multiple call
    if (this.disabled) return;

    await AppFormUtils.waitWhilePending(this.formVessel);

    if (this.formVessel.invalid) {
      this.formVessel.markAllAsTouched();
      AppFormUtils.logFormErrors(this.formVessel.form);
      return;
    }

    this.loading = true;

    try {
      const json = this.formVessel.value;
      const data = Vessel.fromObject(json);

      // Applying the input synchronisation status, if any (need for offline storage)
      if (this.synchronizationStatus) {
        data.synchronizationStatus = this.synchronizationStatus;
      }

      this.disable();
      this.formVessel.error = null;

      const savedData = await this.vesselService.save(data);
      return await this.viewCtrl.dismiss(savedData);
    }
    catch (err) {
      this.formVessel.error = err && err.message || err;
      this.enable();
      this.loading = false;
      this.scrollToTop();
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
    this.formVessel.setValue(Vessel.fromObject({}));
    this.formVessel.markAsPristine();
    this.formVessel.markAsUntouched();
    this.scrollToTop();
  }

  protected async scrollToTop(duration?: number) {
    if (this.content) {
      return this.content.scrollToTop(duration);
    }
  }
}
