import { Component, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Subject, Subscription } from 'rxjs';
import { AppFormUtils } from '@sumaris-net/ngx-components';
import { Packet } from '../services/model/packet.model';
import { PacketSaleForm } from './packet-sale.form';
import { PmfmStrategy } from '@app/referential/services/model/pmfm-strategy.model';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-packet-sale-modal',
  templateUrl: './packet-sale.modal.html'
})
export class PacketSaleModal implements OnInit, OnDestroy {

  loading = false;
  subscription = new Subscription();
  $title = new Subject<string>();

  @ViewChild('packetSaleForm', {static: true}) packetSaleForm: PacketSaleForm;

  @Input() packet: Packet;
  @Input() packetSalePmfms: PmfmStrategy[];

  get disabled() {
    return this.packetSaleForm.disabled;
  }

  get enabled() {
    return this.packetSaleForm.enabled;
  }

  get valid() {
    return this.packetSaleForm && this.packetSaleForm.valid || false;
  }


  constructor(
    protected viewCtrl: ModalController,
    protected translate: TranslateService
  ) {

  }

  ngOnInit(): void {
    this.enable();
    this.updateTitle();
    setTimeout(() => this.packetSaleForm.setValue(Packet.fromObject(this.packet)));
  }

  protected async updateTitle() {
    const title = await this.translate.get('PACKET.SALE.TITLE', {rankOrder: this.packet.rankOrder}).toPromise();
    this.$title.next(title);
  }

  async onSave(event: any): Promise<any> {

    // Avoid multiple call
    if (this.disabled) return;

    await AppFormUtils.waitWhilePending(this.packetSaleForm);

    if (this.packetSaleForm.invalid) {
      AppFormUtils.logFormErrors(this.packetSaleForm.form);
      this.packetSaleForm.markAsTouched({emitEvent: true});
      return;
    }

    this.loading = true;

    try {
      const value = this.packetSaleForm.value;
      this.disable();
      await this.viewCtrl.dismiss(value);
      this.packetSaleForm.error = null;
    } catch (err) {
      console.error(err);
      this.packetSaleForm.error = err && err.message || err;
      this.enable();
      this.loading = false;
    }
  }

  disable() {
    this.packetSaleForm.disable();
  }

  enable() {
    this.packetSaleForm.enable();
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

}
