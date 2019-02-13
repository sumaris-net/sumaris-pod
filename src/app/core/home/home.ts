import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { RegisterModal } from '../register/modal/modal-register';
import { Subscription, BehaviorSubject } from 'rxjs';
import { AccountService } from '../services/account.service';
import { Account, Department } from '../services/model';
import { TranslateService } from '@ngx-translate/core';
import { PodConfigService }from '../services/podconfig.service';



export function getRandomImage() {

  const kinds = {
    'ray': 7,
    'boat': 3
  };
  const kind = (Math.random() < 0.3) ? 'ray' : 'boat';
  const imageCount = kinds[kind];

  if (imageCount == 0) return getRandomImage();
  var imageIndex = Math.floor(Math.random() * imageCount) + 1;
  return './assets/img/bg/' + kind + '-' + imageIndex + '.jpg';
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
  departements = new BehaviorSubject<Department[]>(null);
 

  constructor(
    public accountService: AccountService,
    public activatedRoute: ActivatedRoute,
    public modalCtrl: ModalController,
    public translate: TranslateService,
    public configurationService: PodConfigService
  ) {
    this.bgImage = getRandomImage();
    this.isLogin = accountService.isLogin();
    if (this.isLogin) {
      this.onLogin(this.accountService.account);
    }  
 
    this.configurationService.getDepartments().then(de =>{
      this.departements.next(de);
      console.log("depService.logos " +  de .map(d=>d.logo) );
     } );
    
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
