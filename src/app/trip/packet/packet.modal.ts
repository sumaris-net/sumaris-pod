import {
  AfterViewInit,
  Component,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";
import {Packet, PacketComposition} from "../services/model/packet.model";
import {ModalController} from "@ionic/angular";
import {Subscription} from "rxjs";
import {PacketForm} from "./packet.form";
import {AppFormUtils} from "../../core/form/form.utils";

@Component({
  selector: 'app-packet-modal',
  templateUrl: './packet.modal.html'
})
export class PacketModal implements OnInit, OnDestroy, AfterViewInit {

  loading = false;
  subscription = new Subscription();

  @ViewChild('packetForm', {static: true}) packetForm: PacketForm;

  @Input() packet: Packet;

  get disabled() {
    return this.packetForm.disabled;
  }

  get enabled() {
    return this.packetForm.enabled;
  }

  get valid() {
    return this.packetForm.valid;
  }


  constructor(
    protected viewCtrl: ModalController
  ) {

  }

  ngOnInit(): void {
    this.enable();
  }

  ngAfterViewInit(): void {

    this.packetForm.setValue(this.packet);

  }

  async onSave(event: any): Promise<any> {

    // Avoid multiple call
    if (this.disabled) return;

    await AppFormUtils.waitWhilePending(this.packetForm);

    if (this.packetForm.invalid) {
      this.packetForm.markAsTouched({emitEvent: true});

      AppFormUtils.logFormErrors(this.packetForm.form);
      return;
    }

    this.loading = true;

    try {
      const value = this.packetForm.value;
      this.disable();
      await this.viewCtrl.dismiss(value);
      this.packetForm.error = null;
    } catch (err) {
      this.packetForm.error = err && err.message || err;
      this.enable();
      this.loading = false;
    }
  }

  disable() {
    this.packetForm.disable();
  }

  enable() {
    this.packetForm.enable();
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

}
