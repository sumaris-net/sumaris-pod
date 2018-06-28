import { Component, Input, Output, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { Navbar, NavController } from 'ionic-angular';

@Component({
  selector: 'app-toolbar',
  templateUrl: 'toolbar.html'
})
export class ToolbarComponent implements OnInit {

  @Input()
  title: string = '';

  @Input()
  color: string = '';

  @Input()
  class: string = '';

  @Input()
  hasValidate: boolean = true;

  @Output()
  onValidate: EventEmitter<any> = new EventEmitter<any>();

  @ViewChild('navbar') public navBar: NavController;

  constructor(
  ) {
  }

  ionViewDidLoad() {
    console.debug('[toolbar] page loaded');
  }

  ngOnInit() {
    this.hasValidate = this.hasValidate && this.onValidate.observers.length > 0;

    //console.log(this.navBar);
    /*this.navBar.backButtonClick = (e:UIEvent)=>{
     console.log("Back clicked !");
     //this.navBar.conpop();
     history.back();
    }*/

    //this.navBar.hideBackButton = false;
  }


  public push(page: any, params?: any) {
    this.navBar.push(page, params);
  }
}
