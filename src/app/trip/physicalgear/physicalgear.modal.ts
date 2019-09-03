import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from "@angular/core";
import {AlertController, ModalController} from "@ionic/angular";
import {AcquisitionLevelCodes} from "../../referential/services/model";
import {PhysicalGear} from "../services/trip.model";
import {PhysicalGearForm} from "./physicalgear.form";
import {BehaviorSubject} from "rxjs";
import {TranslateService} from "@ngx-translate/core";
import {PlatformService} from "../../core/services/platform.service";

@Component({
  selector: 'app-physical-gear-modal',
  templateUrl: './physicalgear.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PhysicalGearModal implements OnInit {

  loading = false;
  originalData: PhysicalGear;
  $title = new BehaviorSubject<string>(undefined);

  @Input() acquisitionLevel: string;

  @Input() program: string;

  @Input() disabled = false;

  @Input() isNew = false;

  @Input() set value(value: PhysicalGear) {
    this.originalData = value;
  }

  @Input() mobile: boolean;

  @ViewChild('form') form: PhysicalGearForm;

  constructor(
    protected alertCtrl: AlertController,
    protected viewCtrl: ModalController,
    protected translate: TranslateService,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef
  ) {

    // Default values
    this.acquisitionLevel = AcquisitionLevelCodes.PHYSICAL_GEAR;
    this.mobile = platform.mobile;
  }

  ngOnInit() {
    if (this.disabled) {
      this.form.disable();
    }

    this.form.value = this.originalData || new PhysicalGear();

    // Compute the title
    this.computeTitle();

  }


  async cancel() {
    await this.saveIfDirtyAndConfirm();
  }

  async save(event?: any): Promise<boolean> {
    if (!this.form.valid || this.loading) return false;
    this.loading = true;

    // Nothing to save: just leave
    if (!this.form.dirty) {
      await this.viewCtrl.dismiss();
      return;
    }

    try {
      const gear = this.form.value;
      return await this.viewCtrl.dismiss(gear);
    }
    catch (err) {
      this.loading = false;
      this.form.error = err && err.message || err;
      return false;
    }
  }

  /* -- protected functions -- */

  protected async saveIfDirtyAndConfirm(): Promise<boolean> {
    if (!this.form.dirty) return true;

    let confirm = false;
    let cancel = false;
    const translations = this.translate.instant(['COMMON.BTN_SAVE', 'COMMON.BTN_CANCEL', 'COMMON.BTN_ABORT_CHANGES', 'CONFIRM.SAVE', 'CONFIRM.ALERT_HEADER']);
    const alert = await this.alertCtrl.create({
      header: translations['CONFIRM.ALERT_HEADER'],
      message: translations['CONFIRM.SAVE'],
      buttons: [
        {
          text: translations['COMMON.BTN_CANCEL'],
          role: 'cancel',
          cssClass: 'secondary',
          handler: () => {
            cancel = true;
          }
        },
        {
          text: translations['COMMON.BTN_ABORT_CHANGES'],
          cssClass: 'secondary',
          handler: () => {
          }
        },
        {
          text: translations['COMMON.BTN_SAVE'],
          handler: () => {
            confirm = true; // update upper value
          }
        }
      ]
    });
    await alert.present();
    await alert.onDidDismiss();

    if (!confirm) {
      if (cancel) return false; // cancel

      // abort changes
      await this.viewCtrl.dismiss();
    }

    await this.save(event);
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected async computeTitle() {
    if (this.isNew || !this.originalData) {
      this.$title.next(await this.translate.get('TRIP.PHYSICAL_GEAR.NEW.TITLE').toPromise());
    }
    else {
      this.$title.next(await this.translate.get('TRIP.PHYSICAL_GEAR.EDIT.TITLE', this.originalData).toPromise());
    }
  }
}
