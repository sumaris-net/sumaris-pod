import { Component, OnDestroy, OnInit, Inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { RegisterModal } from '../register/modal/modal-register';
import { Subscription, BehaviorSubject } from 'rxjs';
import { AccountService } from '../services/account.service';
import {Account, Configuration, Department} from '../services/model';
import { TranslateService } from '@ngx-translate/core';
import { PodConfigService } from '../services/podconfig.service';
import { DOCUMENT } from '@angular/platform-browser';
import {fadeInAnimation, isNotNil} from "../../shared/shared.module";

export function getBackgroundImage() {
  return this.bgImage; 
 };

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
export class HomePage implements OnInit, OnDestroy {

  loading = true;
  bgImage: String;
  displayName: String = '';
  isLogin: boolean;
  subscriptions: Subscription[] = [];
  partners = new BehaviorSubject<Department[]>(null);
  logo: String;
  description: String;
  appName: string;
  contentStyle = {};

  constructor(
    @Inject(DOCUMENT) private _document: HTMLDocument,
    public accountService: AccountService,
    public modalCtrl: ModalController,
    public translate: TranslateService,

    public configurationService: PodConfigService
  ) {
    
    this.isLogin = accountService.isLogin();
    if (this.isLogin) {
      this.onLogin(this.accountService.account);
    }

    this.configurationService.getConfs().then(config => {

      this.appName = config.label;

      const title = isNotNil(config.name) ? `${config.label} - ${config.name}` : config.label;
      this._document.getElementById('appTitle').textContent = title;

      const favicon = config.properties && config.properties["sumaris.favicon"];
      if(favicon){
        this._document.getElementById('appFavicon').setAttribute('href', favicon);
      }

      this.logo = config.largeLogo || config.smallLogo;
      this.description = config.name || config.description;

      this.partners.next(config.partners);

      if (config.backgroundImages && config.backgroundImages.length) {
        const bgImage = getRandomImage(config.backgroundImages);
        this.contentStyle = {'background-image' : `url(${bgImage})`};
      }
      else {
        const primaryColor = config.properties && config.properties['sumaris.color.primary'] || '#144391';
        this.contentStyle = {'background-color' : primaryColor};
      }

      this.loading = false;
    });

    // Subscriptions
    this.subscriptions.push(this.accountService.onLogin.subscribe(account => this.onLogin(account)));
    this.subscriptions.push(this.accountService.onLogout.subscribe(() => this.onLogout()));
  };

  ngOnInit() {
    // Workaround needed on Firefox Browser
    const pageElements = document.getElementsByTagName('page-home');
    if (pageElements && pageElements.length == 1) {
      const pageElement: Element = pageElements[0];
      if (pageElement.classList.contains('ion-page-invisible')) {
        console.warn("[home] FIXME Applying workaround on page visibility (see issue #1)");
        pageElement.classList.remove('ion-page-invisible');
      }
    }
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
    this.subscriptions = [];
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
