import {Component, OnInit} from "@angular/core";
import { NavParams } from 'ionic-angular';
import { ViewController } from "ionic-angular";

@Component({
    selector: 'table-select-columns',
    templateUrl: './table-select-columns.html'
})
export class TableSelectColumnsComponent implements OnInit {

    columns: [{name?: string, label: string, visible: boolean}];

    constructor(
        private navParams: NavParams,
        private viewCtrl: ViewController) {
    }

    ngOnInit() {
        this.columns = this.navParams.data;
    }

    reorderItems(indexes) {
        let element = this.columns[indexes.from];
        this.columns.splice(indexes.from, 1);
        this.columns.splice(indexes.to, 0, element);
    }

    close() {
        this.viewCtrl.dismiss(this.columns);
    }
}