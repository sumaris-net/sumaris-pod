import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy} from '@angular/core';
import {ModalController} from '@ionic/angular';
import {RegisterModal} from '../register/modal/modal-register';
import {BehaviorSubject, Subscription} from 'rxjs';
import {AccountService} from '../services/account.service';
import {Account, Configuration, Department, HistoryPageReference, LocalSettings} from '../services/model';
import {TranslateService} from '@ngx-translate/core';
import {ConfigService} from '../services/config.service';
import {fadeInAnimation, isNotNilOrBlank, slideUpDownAnimation} from "../../shared/shared.module";
import {PlatformService} from "../services/platform.service";
import {LocalSettingsService} from "../services/local-settings.service";
import {debounceTime, startWith} from "rxjs/operators";
import {AuthModal} from "../auth/modal/modal-auth";
import {environment} from "../../../environments/environment";
import {InAppBrowser} from "@ionic-native/in-app-browser/ngx";

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
  animations: [fadeInAnimation, slideUpDownAnimation]
})
export class HomePage implements OnDestroy {

  loading = true;
  showSpinner = true;
  displayName: String = '';
  isLogin: boolean;
  subscription = new Subscription();
  $partners = new BehaviorSubject<Department[]>(null);
  loadingBanner = true;
  logo: String;
  description: String;
  appName: string;
  isWeb: boolean;
  contentStyle = {};
  pageHistory: HistoryPageReference[] = [];
  appPlatformName: string;
  appInstallName: string;
  appInstallUrl: string;

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
    public settings: LocalSettingsService,
    private browser: InAppBrowser
  ) {

    this.showSpinner = !this.platform.started;
    this.platform.ready().then(() => this.start());
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  async login() {
    const modal = await this.modalCtrl.create({component: AuthModal});
    return modal.present();
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

  downloadApp(event: UIEvent) {
    event.preventDefault();

    if (this.appInstallUrl) {
      console.info(`[home] Opening App download link: ${this.appInstallUrl}`);
      this.browser.create(this.appInstallUrl, '_system', 'location=yes');
      return false;
    }
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

    // Listen pod config
    this.subscription.add(this.configService.config.subscribe(config => this.onConfigLoaded(config)));

    // Listen settings changes
    this.subscription.add(
      this.settings.onChange
        .pipe(
          // Add a delay, to avoid to apply changes to often
          debounceTime(3000),
          // Start with current settings
          startWith(await this.settings.ready())
        )
        .subscribe(res => this.onSettingsChanged(res)));

  }

  protected onConfigLoaded(config: Configuration) {
    console.debug("[home] Configuration loaded:", config);

    this.appName = config.label || environment.defaultAppName || 'SUMARiS';
    this.logo = config.largeLogo || config.smallLogo;
    this.description = config.name;
    this.isWeb = this.platform.isWebOrDesktop() ;

    const partners = (config.partners || []).filter(p => p && p.logo);
    this.$partners.next(partners);
    this.loadingBanner = (partners.length === 0);

    if (config.backgroundImages && config.backgroundImages.length) {
      const bgImage = getRandomImage(config.backgroundImages);
      this.contentStyle = {'background-image': `url(${bgImage})`};
    } else {
      const primaryColor = config.properties && config.properties['sumaris.color.primary'] || '#144391';
      this.contentStyle = {'background-color': primaryColor};
    }

    this.markForCheck();

    // If mobile web: show "download app" button
    if (this.platform.is('mobileweb')) {

      // Android
      if (this.platform.is('android')) {

        setTimeout(() => {
          this.appPlatformName = 'Android';
          const apkLink = config.properties['sumaris.android.install.url'];
          if (isNotNilOrBlank(apkLink)) {
            this.appInstallUrl = apkLink;
            this.appInstallName = this.appName;
          }
          else {
            this.appInstallName = environment.defaultAppName || 'SUMARiS';
            this.appInstallUrl =  environment.defaultAndroidInstallUrl || null;
          }
          this.markForCheck();
        }, 1000); // Add a delay, for animation
      }

      // TODO: other mobile platforms
      // else if (...)
    }

    // If first load, hide the loading indicator
    if (this.loading) {
      setTimeout(() => {
        this.loading = false;
        this.markForCheck();
      }, 500); // Add a delay, for animation
    }

  }

  protected onSettingsChanged(settings: LocalSettings) {
    if (settings.pageHistory !== this.pageHistory) {
      console.debug("[home] Page history loaded");
      this.pageHistory = settings.pageHistory || [];
    }
    // Always force a refresh (same history array, but content may have changed)
    this.markForCheck();
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
    this.isLogin = false;
    this.displayName = "";
    this.pageHistory = [];
    this.markForCheck();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
