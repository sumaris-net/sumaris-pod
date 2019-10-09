import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy} from '@angular/core';
import {ModalController} from '@ionic/angular';
import {RegisterModal} from '../register/modal/modal-register';
import {BehaviorSubject, Subscription} from 'rxjs';
import {AccountService} from '../services/account.service';
import {Account, Configuration, Department, HistoryPageReference} from '../services/model';
import {TranslateService} from '@ngx-translate/core';
import {ConfigService} from '../services/config.service';
import {fadeInAnimation} from "../../shared/shared.module";
import {PlatformService} from "../services/platform.service";
import {LocalSettingsService} from "../services/local-settings.service";
import {debounceTime} from "rxjs/operators";

export function getRandomImage(files: String[]) {
  const imgIndex = Math.floor(Math.random() * files.length);
  return files[imgIndex];
}

@Component({
  moduleId: module.id.toString(),
  selector: 'page-home',
  templateUrl: 'home.html',
  styleUrls: ['./home.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [fadeInAnimation]
})
export class HomePage implements OnDestroy {

  loading = true;
  showSpinner = true;
  displayName: String = '';
  isLogin: boolean;
  subscription = new Subscription();
  partners = new BehaviorSubject<Department[]>(null);
  loadingBanner = true;
  logo: String;
  description: String;
  appName: string;
  contentStyle = {};
  pageHistory: HistoryPageReference[] = [];

  get currentLocaleCode(): string {
    return (this.translate.currentLang || this.translate.defaultLang).substr(0,2);
  }

  constructor(
    private accountService: AccountService,
    private modalCtrl: ModalController,
    private translate: TranslateService,
    private configService: ConfigService,
    private platform: PlatformService,
    private cd: ChangeDetectorRef,
    public settings: LocalSettingsService
  ) {

    this.showSpinner = !this.platform.started;
    console.log("TODO start home page")
    this.platform.ready().then(() => {
      console.log("TODO checkis ready !")
      this.start()
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  async register() {
    const modal = await this.modalCtrl.create({component: RegisterModal});
    return modal.present();
  }

  logout(event: any) {
    this.accountService.logout();
  }

  changeLanguage(locale: string) {
    this.settings.apply({locale: locale})
      .then(() => {
        this.markForCheck();
      });
  }

  getPagePath(page: HistoryPageReference) {
    return page && page.path;
  }

  /* -- protected method  -- */

  protected async start() {
    this.isLogin = this.accountService.isLogin();
    if (this.isLogin) {
      this.onLogin(this.accountService.account);
    }

    // Listen login/logout events
    this.subscription.add(this.accountService.onLogin.subscribe(account => this.onLogin(account)));
    this.subscription.add(this.accountService.onLogout.subscribe(() => this.onLogout()));

    // Listen remote config changes
    this.subscription.add(this.configService.config.subscribe(config => {
      this.onConfigChanged(config);
      this.markForCheck();

      setTimeout(() => {
        this.loading = false;
        this.markForCheck();
      }, 500);
    }));

    // Listen settings changes
    const settings = await this.settings.ready();
    this.pageHistory = settings && settings.pageHistory || [];
    this.subscription.add(
      this.settings.onChange
        .pipe(
          // Add a delay, to avoid to apply changes to often
          debounceTime(3000)
        )
        .subscribe(history => {
          console.debug("[home] Page history has been updated");
          this.markForCheck();
        }));
  }

  protected onConfigChanged(config: Configuration) {
    console.debug("[home] Configuration changed:", config);

    this.appName = config.label;
    this.logo = config.largeLogo || config.smallLogo;
    this.description = config.name;

    const partners = (config.partners || []).filter(p => p && p.logo);
    this.partners.next(partners);
    this.loadingBanner = (partners.length === 0);

    if (config.backgroundImages && config.backgroundImages.length) {
      const bgImage = getRandomImage(config.backgroundImages);
      this.contentStyle = {'background-image': `url(${bgImage})`};
    } else {
      const primaryColor = config.properties && config.properties['sumaris.color.primary'] || '#144391';
      this.contentStyle = {'background-color': primaryColor};
    }
  }

  protected onLogin(account: Account) {
    //console.debug('[home] Logged account: ', account);
    this.isLogin = true;
    this.displayName = account &&
      ((account.firstName && (account.firstName + " ") || "") +
        (account.lastName || "")) || "";
    this.markForCheck();
  }

  protected onLogout() {
    //console.log('[home] Logout');
    this.isLogin = false;
    this.displayName = "";
    this.markForCheck();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
