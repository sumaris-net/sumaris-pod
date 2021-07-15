import {AfterViewInit, Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Packet} from '../services/model/packet.model';
import {ModalController} from '@ionic/angular';
import {Subject, Subscription} from 'rxjs';
import {PacketForm} from './packet.form';
import {AppFormUtils} from '@sumaris-net/ngx-components';
import {TranslateService} from '@ngx-translate/core';

@Component({
  selector: 'app-packet-modal',
  templateUrl: './packet.modal.html'
})
export class PacketModal implements OnInit, OnDestroy {

  loading = false;
  subscription = new Subscription();
  $title = new Subject<string>();

  @ViewChild('packetForm', {static: true}) packetForm: PacketForm;

  @Input() packet: Packet;

  get disabled() {
    return this.packetForm.disabled;
  }

  get enabled() {
    return this.packetForm.enabled;
  }

  get valid() {
    return this.packetForm && this.packetForm.valid || false;
  }


  constructor(
    protected viewCtrl: ModalController,
    protected translate: TranslateService
  ) {

  }

  ngOnInit(): void {
    this.enable();
    this.updateTitle();
    setTimeout(() => this.packetForm.setValue(this.packet))
  }

  protected async updateTitle() {
    const title = await this.translate.get('PACKET.COMPOSITION.TITLE', {rankOrder: this.packet.rankOrder}).toPromise();
    this.$title.next(title);
  }

  async onSave(event: any): Promise<any> {

    // Avoid multiple call
    if (this.disabled) return;

    await AppFormUtils.waitWhilePending(this.packetForm);

    if (this.packetForm.invalid) {
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
