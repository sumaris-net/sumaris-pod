import { Component, ViewChild, OnInit } from "@angular/core";
import { Router, ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TripService } from './services/trip.service';
import { MatHorizontalStepper } from "@angular/material";
import { TripPage } from "./trip.page";


@Component({
  selector: 'modal-trip',
  templateUrl: './trip.modal.html',
  styleUrls: ['./trip.modal.scss']
})
export class TripModal extends TripPage {

  @ViewChild('stepper') private stepper: MatHorizontalStepper;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected tripService: TripService,
    protected viewCtrl: ModalController) {
    super(route, router, tripService);
  }

  async save(event: any): Promise<any> {

    try {
      let res = await super.save(event);
      this.viewCtrl.dismiss(res);
    }
    catch (err) {
      // nothing to do
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
