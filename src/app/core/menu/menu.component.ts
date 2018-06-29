import { Component, ViewChild, Input } from '@angular/core';
import { Platform, MenuController, ModalController, Menu } from "@ionic/angular";

import { Router } from "@angular/router";
import { Account } from "../services/model";
import { AccountService } from "../services/account.service";
import { AboutModal } from '../about/modal-about';
import { AppComponent } from '../../app.component';

import { environment } from '../../../environments/environment';
import { HomePage } from '../home/home';


export interface MenuItem {
  title: string;
  path?: string;
  page?: string | any;
  icon?: string;
}

@Component({
  selector: 'app-menu',
  templateUrl: 'menu.component.html',
  styleUrls: ['./menu.component.scss']
})
export class MenuComponent {

  public isLogin: boolean;
  public account: Account;

  @Input()
  appVersion: String = environment.version;

  @Input() content: any;

  @Input() side: string = "left";

  root: any = HomePage;

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
    //this.toto.
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

    if (item.path) {
      this.router.navigate([item.path], item.params)
    }
  }

  async openAboutModal(event) {
    const modal = await this.modalCtrl.create({ component: AboutModal });
    return modal.present();
  }
}

