import {
  AfterViewInit,
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {Subscription} from "rxjs";
import {AppFormUtils} from "../../core/form/form.utils";
import {Packet} from "../services/model/packet.model";
import {PacketSaleForm} from "./packet-sale.form";
import {PmfmStrategy} from "../../referential/services/model";

@Component({
  selector: 'app-packet-sale-modal',
  templateUrl: './packet-sale.modal.html'
})
export class PacketSaleModal implements OnInit, OnDestroy, AfterViewInit {

  loading = false;
  subscription = new Subscription();

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
    return this.packetSaleForm.valid;
  }


  constructor(
    protected viewCtrl: ModalController
  ) {

  }

  ngOnInit(): void {
    this.enable();
  }

  ngAfterViewInit(): void {

    setTimeout(() => {
      this.packetSaleForm.setValue(Packet.fromObject(this.packet));
    });

  }

  async onSave(event: any): Promise<any> {

    // Avoid multiple call
    if (this.disabled) return;

    await AppFormUtils.waitWhilePending(this.packetSaleForm);

    if (this.packetSaleForm.invalid) {
      AppFormUtils.logFormErrors(this.packetSaleForm.form);
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
