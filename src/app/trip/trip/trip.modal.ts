import {ChangeDetectorRef, Component, ViewChild} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {AlertController, ModalController} from "@ionic/angular";
import {TripService} from '../services/trip.service';
import {MatHorizontalStepper} from "@angular/material";
import {TripPage} from "./trip.page";
import {TranslateService} from '@ngx-translate/core';
import {DateFormatPipe} from "../../shared/shared.module";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {ProgramService} from "../../referential/services/program.service";

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
    protected dataService: TripService,
    protected settingsService: LocalSettingsService,
    protected modalCtrl: ModalController,
    protected programService: ProgramService,
    protected cd: ChangeDetectorRef) {
    super(route, router, alterCtrl, translate, dateFormat, dataService, settingsService, programService, cd);
  }

  async save(event: any): Promise<boolean> {

    try {
      const saved = await super.save(event);
      if (saved) {
        this.modalCtrl.dismiss(this.data);
      }
      return saved;
    }
    catch (err) {
      // nothing to do
      return false;
    }
  }

  async cancel() {
    await this.modalCtrl.dismiss();
  }

  public isEnd(): boolean {
    return this.stepper.selectedIndex === 2;
  }

  public isBeginning(): boolean {
    return this.stepper.selectedIndex === 0;
  }

  public slidePrev() {
    return this.stepper.previous();
  }

  public slideNext() {
    return this.stepper.next();
  }
}
