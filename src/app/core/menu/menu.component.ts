import { Component, OnInit, Input } from '@angular/core';
import { MenuController, ModalController } from "@ionic/angular";

import { Router } from "@angular/router";
import { Account } from "../services/model";
import { AccountService } from "../services/account.service";
import { AboutModal } from '../about/modal-about';

import { environment } from '../../../environments/environment';
import { HomePage } from '../home/home';


export interface MenuItem {
  title: string;
  path?: string;
  page?: string | any;
  icon?: string;
  requiredProfiles?: string[];
}

@Component({
  selector: 'app-menu',
  templateUrl: 'menu.component.html',
  styleUrls: ['./menu.component.scss']
})
export class MenuComponent implements OnInit {

  public isLogin: boolean;
  public account: Account;

  filteredItems: Array<MenuItem> = [];

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

  }

  ngOnInit() {
    this.filteredItems = (this.items || []).filter(i => !i.requiredProfiles || i.requiredProfiles.indexOf('GUEST') != -1);
  }

  onLogin(account: Account) {
    //console.log('[app] Logged account: ', account);
    this.account = account;
    this.isLogin = true;
    this.filteredItems = (this.items || []).filter(i => !i.requiredProfiles || !!i.requiredProfiles.find(p => this.accountService.hasProfile(p)));
  }

  onLogout() {
    console.debug("[app] logout");
    this.isLogin = false;
    this.account = null;
    this.router.navigate(['']);
  }

  logout(): void {
    this.account = null;
    this.accountService.logout();
  }

  async openAboutModal(event) {
    const modal = await this.modalCtrl.create({ component: AboutModal });
    return modal.present();
  }
}

