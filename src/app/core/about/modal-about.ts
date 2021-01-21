import {Component, Inject} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {EnvironmentService} from "../../../environments/environment.class";


@Component({
  selector: 'modal-about',
  styleUrls: ['./modal-about.scss'],
  templateUrl: './modal-about.html'
})
export class AboutModal {

    appVersion: string;

    constructor(
        protected modalController: ModalController,
        @Inject(EnvironmentService) protected environment
    ) {
      this.appVersion = environment.version;
    }

    async close() {
        await this.modalController.dismiss();
    }
}
