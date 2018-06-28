import { Component, OnInit } from "@angular/core";
import { ViewController } from "ionic-angular";

import { environment } from "../core.module"


@Component({
    selector: 'modal-about',
    templateUrl: './modal-about.html'
})
export class AboutModal {

    appVersion: String = environment.version;

    constructor(
        protected viewCtrl: ViewController
    ) {
    }

    async cancel() {
        await this.viewCtrl.dismiss();
    }
}