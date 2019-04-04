import {Component, OnDestroy, OnInit} from '@angular/core';
import {ModalController} from '@ionic/angular';
import {RegisterModal} from '../register/modal/modal-register';
import {BehaviorSubject, Subscription} from 'rxjs';
import {AccountService} from '../services/account.service';
import {Account, Configuration, Department} from '../services/model';
import {TranslateService} from '@ngx-translate/core';
import {ConfigService} from '../services/config.service';
import {fadeInAnimation} from "../../shared/shared.module";

export function getRandomImage(files : String[]) {
  let imgIndex = Math.floor(Math.random() * files.length)  ;
   return files[imgIndex]; 
 };

@Component({
  moduleId: module.id.toString(),
  selector: 'page-home',
  templateUrl: 'home.html',
  styleUrls: ['./home.scss'],
  animations: [fadeInAnimation]
})
export class HomePage implements OnDestroy {

  loading = true;
  displayName: String = '';
  isLogin: boolean;
  subscriptions: Subscription[] = [];
  partners = new BehaviorSubject<Department[]>(null);
  loadingBanner = true;
  logo: String;
  description: String;
  appName: string;
  contentStyle = {};

  constructor(
    public accountService: AccountService,
    public modalCtrl: ModalController,
    public translate: TranslateService,
    public configurationService: ConfigService
  ) {
    
    this.isLogin = accountService.isLogin();
    if (this.isLogin) {
      this.onLogin(this.accountService.account);
    }

    // Subscriptions
    this.subscriptions.push(this.accountService.onLogin.subscribe(account => this.onLogin(account)));
    this.subscriptions.push(this.accountService.onLogout.subscribe(() => this.onLogout()));
    this.subscriptions.push(this.configurationService.get().subscribe(config => {
      this.onConfigReady(config);
      this.loading = false;
    }));
  };

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.subscriptions = [];
  }

  onConfigReady(config: Configuration) {
    this.appName = config.label;
    this.logo = config.largeLogo || config.smallLogo;
    this.description = config.name;

    const partners = (config.partners || []).filter(p => p && p.logo);
    this.partners.next(partners);
    this.loadingBanner = (partners.length === 0);

    if (config.backgroundImages && config.backgroundImages.length) {
      const bgImage = getRandomImage(config.backgroundImages);
      this.contentStyle = {'background-image' : `url(${bgImage})`};
    }
    else {
      const primaryColor = config.properties && config.properties['sumaris.color.primary'] || '#144391';
      this.contentStyle = {'background-color' : primaryColor};
    }
  }

  onLogin(account: Account) {
    //console.debug('[home] Logged account: ', account);
    this.isLogin = true;
    this.displayName = account &&
      ((account.firstName && (account.firstName + " ") || "") +
        (account.lastName || "")) || "";
  }

  onLogout() {
    //console.log('[home] Logout');
    this.isLogin = false;
    this.displayName = "";
  }

  async register() {
    const modal = await this.modalCtrl.create({ component: RegisterModal });
    return modal.present();
  }

  logout(event: any) {
    this.accountService.logout();
  }

  changeLanguage(locale: string) {
    this.translate.use(locale);
  }
}
