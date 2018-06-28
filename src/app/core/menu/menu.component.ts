import { Component, ViewChild, Input } from '@angular/core';
import { Platform, MenuController, ModalController, NavController } from "ionic-angular";

import { StatusBar } from '@ionic-native/status-bar';
import { SplashScreen } from '@ionic-native/splash-screen';
import { Keyboard } from '@ionic-native/keyboard';


import { Router } from "@angular/router";
import { Account } from "../services/model";
import { AccountService } from "../services/account.service";
import { AboutModal } from '../about/modal-about';
import { AppComponent } from '../../app.component';

import { environment } from "../core.module"


export interface MenuItem {
  title: string;
  path?: string;
  page?: string | any;
  icon?: string;
}

@Component({
  selector: 'app-menu',
  templateUrl: 'menu.component.html'
})
export class MenuComponent {

  private isLogin: boolean;
  private account: Account;

  @Input()
  appVersion: String = environment.version;

  @Input() content: any;

  @Input() side: string = "left";

  @Input() nav: NavController;

  @Input()
  items: Array<MenuItem>;

  constructor(
    protected accountService: AccountService,
    protected router: Router,
    protected menu: MenuController,
    protected modalCtrl: ModalController
  ) {

    this.isLogin = accountService.isLogin();
    if (this.isLogin) {
      this.onLogin(this.accountService.account);
    }

    // subscriptions
    this.accountService.onLogin.subscribe(account => this.onLogin(account));
    this.accountService.onLogout.subscribe(() => this.onLogout());
  }

  onLogin(account: Account) {
    //console.log('[app] Logged account: ', account);
    this.account = account;
    this.isLogin = true;
  }

  onLogout() {
    console.log("[app] logout");
    this.isLogin = false;
    this.account = null;
    this.router.navigate(['']);
  }

  logout(): void {
    this.account = null;
    this.accountService.logout();
  }

  openPage(item): void {
    console.log(item);
    // close the menu when clicking a link from the menu
    this.menu.close();


    if (item.page && this.nav) {
      this.nav.push(item.page, item.params);
    }
    else if (item.path) {
      this.router.navigate([item.path], item.params)
    }
  }

  openAboutModal(event) {
    const modal = this.modalCtrl.create(AboutModal);
    modal.present();
  }
}

