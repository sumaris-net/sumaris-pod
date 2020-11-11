import {Component} from "@angular/core";
import {ModalController} from "@ionic/angular";

import {environment} from '../../../environments/environment';


@Component({
  selector: 'modal-about',
  styleUrls: ['./modal-about.scss'],
  templateUrl: './modal-about.html'
})
export class AboutModal {

    appVersion: String = environment.version;

    constructor(
        protected modalController: ModalController
    ) {
    }

    async close() {
        await this.modalController.dismiss();
    }
}
