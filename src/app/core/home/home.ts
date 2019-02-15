import { Component, OnDestroy, OnInit, Inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { RegisterModal } from '../register/modal/modal-register';
import { Subscription, BehaviorSubject } from 'rxjs';
import { AccountService } from '../services/account.service';
import { Account, Department } from '../services/model';
import { TranslateService } from '@ngx-translate/core';
import { PodConfigService } from '../services/podconfig.service';
import { DOCUMENT } from '@angular/platform-browser';

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
  styleUrls: ['./home.scss']
})
export class HomePage implements OnInit, OnDestroy {

  bgImage: String;
  displayName: String = '';
  isLogin: boolean;
  subscriptions: Subscription[] = [];
  partners = new BehaviorSubject<Department[]>(null);
  logo: String;
  appName: string;
  partLogos: string[];

  constructor(
    @Inject(DOCUMENT) private _document: HTMLDocument,
    public accountService: AccountService,
    public activatedRoute: ActivatedRoute,
    public modalCtrl: ModalController,
    public translate: TranslateService,

    public configurationService: PodConfigService
  ) {
    
    this.isLogin = accountService.isLogin();
    if (this.isLogin) {
      this.onLogin(this.accountService.account);
    }
 
    this.configurationService.getConfs().then(conf => {
      this.appName = conf.label;
      this._document.getElementById('appTitle').textContent = conf.name;

      let fav = conf.properties["favicon"];
      if(fav){
        this._document.getElementById('appFavicon').setAttribute('href', fav); 
      }

      this.logo = conf.logo;
      this.partners.next(conf.partners);
      this.bgImage = getRandomImage(conf.backgroundImages);
    })

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
