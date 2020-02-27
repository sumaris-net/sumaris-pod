import {AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {isNil, isNotNil, Vessel} from "../../services/model";
import { ModalController } from "@ionic/angular";
import { VesselForm } from '../form/form-vessel';
import { VesselService } from '../../services/vessel-service';
import { AppFormUtils } from '../../../core/core.module';
import {ConfigService} from "../../../core/services/config.service";
import {Subscription} from "rxjs";
import {AccountService} from "../../../core/services/account.service";
import {ConfigOptions} from "../../../core/services/model";


@Component({
  selector: 'modal-vessel',
  templateUrl: './modal-vessel.html'
})
export class VesselModal implements OnInit, OnDestroy {

  loading = false;
  subscription = new Subscription();

  @Input() defaultStatus: number;

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
            const defaultStatus = config.properties[ConfigOptions.VESSEL_DEFAULT_STATUS.key];
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
      this.formVessel.markAsTouched({emitEvent: true});

      AppFormUtils.logFormErrors(this.formVessel.form);
      return;
    }

    this.loading = true;

    try {
      const json = this.formVessel.value;
      const data = Vessel.fromObject(json);

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
    this.formVessel.setValue(Vessel.fromObject({}));
    this.formVessel.markAsPristine();
    this.formVessel.markAsUntouched();
  }
}
