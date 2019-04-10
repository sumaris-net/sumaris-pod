import {Component, ViewChild} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController, ModalController} from "@ionic/angular";
import {TripService} from '../services/trip.service';
import {MatHorizontalStepper} from "@angular/material";
import {TripPage} from "./trip.page";
import {TranslateService} from '@ngx-translate/core';
import {DateFormatPipe} from "../../shared/shared.module";
import {AccountService} from "../../core/services/account.service";

@Component({
  selector: 'modal-trip',
  templateUrl: './trip.modal.html',
  styleUrls: ['./trip.modal.scss']
})
export class TripModal extends TripPage {

  @ViewChild('stepper') private stepper: MatHorizontalStepper;

  constructor(
    route: ActivatedRoute,
    router: Router,
    alterCtrl: AlertController,
    translate: TranslateService,
    protected dateFormat: DateFormatPipe,
    protected tripService: TripService,
    protected accountService: AccountService,
    protected viewCtrl: ModalController) {
    super(route, router, alterCtrl, translate, dateFormat, accountService, tripService);
  }

  async save(event: any): Promise<boolean> {

    try {
      let saved = await super.save(event);
      if (saved) {
        this.viewCtrl.dismiss(this.data);
      }
      return saved;
    }
    catch (err) {
      // nothing to do
      return false;
    }
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  public isEnd(): boolean {
    return this.stepper.selectedIndex == 2;
  }

  public isBeginning(): boolean {
    return this.stepper.selectedIndex == 0;
  }

  public slidePrev() {
    return this.stepper.previous();
  }

  public slideNext() {
    return this.stepper.next();
  }
}
