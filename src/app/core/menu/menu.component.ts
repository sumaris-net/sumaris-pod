import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from '@angular/core';
import {AlertController, IonSplitPane, MenuController, ModalController} from "@ionic/angular";

import {Router} from "@angular/router";
import {Account, Configuration, UserProfileLabel} from "../services/model";
import {AccountService} from "../services/account.service";
import {AboutModal} from '../about/modal-about';

import {environment} from '../../../environments/environment';
import {HomePage} from '../home/home';
import {fadeInAnimation} from '../../shared/material/material.animations';
import {TranslateService} from "@ngx-translate/core";
import {isNotNilOrBlank} from "../../shared/functions";
import {BehaviorSubject, merge, Subscription} from "rxjs";
import {ConfigService} from "../services/config.service";
import {filter, mergeMap, tap, throttleTime} from "rxjs/operators";

export interface MenuItem {
  title: string;
  path?: string;
  page?: string | any;
  action?: string | any;
  icon?: string;
  matIcon?: string;
  profile?: UserProfileLabel;
  exactProfile?: UserProfileLabel;
  cssClass?: string;
  // A config property, to enable the menu item
  ifProperty?: string;
  // A config property, to override the title
  titleProperty?: string;
}

const SPLIT_PANE_SHOW_WHEN = 'lg';

@Component({
  selector: 'app-menu',
  templateUrl: 'menu.component.html',
  styleUrls: ['./menu.component.scss'],
  animations: [fadeInAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MenuComponent implements OnInit {

  private _debug = false;
  private _subscription = new Subscription();
  public loading = true;
  public isLogin = false;
  public account: Account;
  public splitPaneOpened: boolean;
  public config: Configuration;

  @Input() logo: String;

  @Input() appName: String;

  $filteredItems = new BehaviorSubject<MenuItem[]>(undefined);

  @Input()
  appVersion: String = environment.version;

  @Input() content: any;

  @Input() side: string = "left";

  root: any = HomePage;

  @Input()
  items: Array<MenuItem>;

  @ViewChild('splitPane', { static: true }) splitPane: IonSplitPane;

  constructor(
    protected accountService: AccountService,
    protected router: Router,
    protected menu: MenuController,
    protected modalCtrl: ModalController,
    protected alertController: AlertController,
    protected translate: TranslateService,
    protected configService: ConfigService,
    protected cd: ChangeDetectorRef
  ) {

    //this._debug = !environment.production;
  }

  async ngOnInit() {
    this.splitPane.when = SPLIT_PANE_SHOW_WHEN;
    this.splitPaneOpened = true;

    // Update component when refresh is need (=login events or config changed)

    this._subscription.add(
      merge(
        this.accountService.onLogin,
        this.accountService.onLogout,
        this.configService.config
          .pipe(
            tap(config => this.config = config)
          )
        )
        .pipe(
          mergeMap(() => this.accountService.ready()),
          throttleTime(200)
        )
        .subscribe(account => {
          if (this.accountService.isLogin()) {
            this.onLogin(this.accountService.account);

          } else {
            this.onLogout(true);
          }
        }));
  }

  async onLogin(account: Account) {
    console.info('[menu] Update using logged account');
    this.account = account;
    this.isLogin = true;
    //this.splitPaneOpened = true;
    //this.splitPane.when = SPLIT_PANE_SHOW_WHEN;
    await this.refreshMenuItems();

    setTimeout(() => {
      this.loading = false;
      this.markForCheck();
    }, 500);
  }

  async onLogout(skipRedirect?: boolean) {
    if (!skipRedirect) console.debug("[menu] logout");
    this.isLogin = false;
    //this.splitPaneOpened = false;
    //this.splitPane.when = false;
    this.account = null;
    await this.refreshMenuItems();

    // Wait the end of fadeout, to reset the account
    if (!skipRedirect) {
      await this.router.navigate(['']);
    }

    //setTimeout(() => {
      this.loading = false;
      this.markForCheck();
    //}, 1000);

  }

  async logout() {

    const translations = await this.translate.get([
      'AUTH.LOGOUT.CONFIRM_TITLE',
      'AUTH.LOGOUT.CONFIRM_MESSAGE',
      'COMMON.BTN_CANCEL',
      'AUTH.LOGOUT.BTN_CONFIRM'
    ]).toPromise();
    const alert = await this.alertController.create({
      header: translations['AUTH.LOGOUT.CONFIRM_TITLE'],
      message: translations['AUTH.LOGOUT.CONFIRM_MESSAGE'],
      buttons: [
        {
          text: translations['COMMON.BTN_CANCEL'],
          role: 'cancel',
          cssClass: 'secondary'
        }, {
          text: translations['AUTH.LOGOUT.BTN_CONFIRM'],
          cssClass: 'ion-color-primary',
          handler: () => {
            this.accountService.logout();
          }
        }
      ]
    });

    await alert.present();
  }

  async openAboutModal(event) {
    const modal = await this.modalCtrl.create({component: AboutModal});
    return modal.present();
  }

  toggleSplitPane($event: MouseEvent) {
    if ($event.defaultPrevented) return;
    this.splitPaneOpened = !this.splitPaneOpened;
    if (!this.splitPaneOpened) {
      this.splitPane.when = false;
    } else {
      this.splitPane.when = SPLIT_PANE_SHOW_WHEN;
    }
    $event.preventDefault();
  }

  async doAction(action: string, event: UIEvent) {
    switch (action) {
      case 'logout':
        await this.logout();
        break;
      case 'about':
        await this.openAboutModal(event);
        break;
      default:
        throw new Error('Unknown action: ' + action);
    }
  }

  /* -- protected methods -- */

  protected refreshMenuItems() {
    if (this._debug) console.debug("[menu] Updating menu...");

    const filteredItems = (this.items || [])
      .filter((item) => this.filterMenuItem(item))
      .map(item => {
        // Replace title using properties
        if (isNotNilOrBlank(item.titleProperty) && this.config) {
          const title = this.config.properties[item.titleProperty];
          if (title) return { ...item, title}; // Create a copy, to keep the original item.title
        }
        return item;
      });

    this.$filteredItems.next(filteredItems);
  }

  protected filterMenuItem(item: MenuItem): boolean {
    if (item.profile) {
      const hasProfile = this.isLogin && this.accountService.hasMinProfile(item.profile);
      if (!hasProfile) {
        if (this._debug) console.debug("[menu] User does not have minimal profile '" + item.profile + "' for ", (item.path || item.page));
        return false;
      }
    }

    else if (item.exactProfile) {
      const hasExactProfile =  this.isLogin && this.accountService.hasExactProfile(item.profile);
      if (!hasExactProfile) {
        if (this._debug) console.debug("[menu] User does not have exact profile '" + item.exactProfile + "' for ", (item.path || item.page));
        return false;
      }
    }

    // If enable by config
    if (item.ifProperty) {
      const isEnableByConfig = this.config && this.config.properties[item.ifProperty] === 'true';
      if (!isEnableByConfig) {
        if (this._debug) console.debug("[menu] Config property '" + item.ifProperty + "' not 'true' for ", (item.path || item.page));
        return false;
      }
    }

    return true;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

