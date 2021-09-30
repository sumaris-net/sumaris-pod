import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {FormControl, FormGroup} from '@angular/forms';
import {AppForm} from '@sumaris-net/ngx-components';
import moment, {Moment} from 'moment';
import {DateAdapter} from '@angular/material/core';

@Component({
  selector: 'app-strategy-modal',
  templateUrl: './strategy.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategyModal extends AppForm<Moment> implements OnInit {

  constructor(
    protected viewCtrl: ModalController,
    protected cd: ChangeDetectorRef,
    protected dateAdapter: DateAdapter<Moment>,
  ) {
    super(dateAdapter);
    this.form = new FormGroup({
      year: new FormControl()
    })
  }

  ngOnInit() {
    super.ngOnInit();
    this.form.get('year').setValue(moment());
    this.form.enable();
  }

  protected async computeTitle(): Promise<string> {
    return "REFERENTIAL.ENTITY.DUPLICATE_STRATEGY";
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  async validDate() {
    await this.viewCtrl.dismiss(this.form.get('year').value);
  }
}
