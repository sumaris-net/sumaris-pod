import { Component, OnInit, Input } from '@angular/core';
import { MenuController, ModalController } from "@ionic/angular";

import { Router } from "@angular/router";
import { Account, UserProfileLabel } from "../services/model";
import { AccountService } from "../services/account.service";
import { AboutModal } from '../about/modal-about';

import { environment } from '../../../environments/environment';
import { HomePage } from '../home/home';
import { Subject } from 'rxjs';

export interface MenuItem {
  title: string;
  path?: string;
  page?: string | any;
  icon?: string;
  profile?: UserProfileLabel;
}

@Component({
  selector: 'app-menu',
  templateUrl: 'menu.component.html',
  styleUrls: ['./menu.component.scss']
})
export class MenuComponent implements OnInit {

  public isLogin: boolean;
  public account: Account;

  //filteredItems: Array<MenuItem> = [];
  filteredItems = new Subject<MenuItem[]>();

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

  }

  ngOnInit() {
    // subscriptions
    this.accountService.onLogin.subscribe(account => this.onLogin(account));
    this.accountService.onLogout.subscribe(() => this.onLogout());

    if (this.accountService.isLogin()) {
      this.onLogin(this.accountService.account);
    }
  }

  onLogin(account: Account) {
    console.debug('[menu] Logged account: ', account);
    this.account = account;
    this.isLogin = true;
    this.updateItems();
  }

  onLogout() {
    //console.debug("[menu] logout");
    this.isLogin = false;
    this.account = null;
    this.router.navigate(['']);
    this.updateItems();
  }

  logout(): void {
    this.account = null;
    this.accountService.logout();
  }

  async openAboutModal(event) {
    const modal = await this.modalCtrl.create({ component: AboutModal });
    return modal.present();
  }

  updateItems() {
    if (!this.isLogin) {
      this.filteredItems.next((this.items || []).filter(i => !i.profile || i.profile == 'GUEST'));
    }
    else {
      this.filteredItems.next((this.items || []).filter(i => {
        const res = !i.profile || this.accountService.hasProfile(i.profile);
        if (!res) {
          console.debug("[menu] User does not have profile '" + i.profile + "' need by ", (i.path || i.page));
          this.accountService.hasProfile(i.profile);
        }
        return res;
      }));
    }
  }
}

