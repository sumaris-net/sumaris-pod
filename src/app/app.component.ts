import {Component, Inject} from '@angular/core';
import {Platform} from "@ionic/angular";
import {MenuItem} from './core/menu/menu.component';
import {HomePage} from './core/home/home';
import {AccountService, DataService, isNotNil} from './core/core.module';
import {ReferentialRefService} from './referential/referential.module';
import { PodConfigService } from './core/services/podconfig.service';
import {DOCUMENT} from "@angular/platform-browser";
import {Configuration} from "./core/services/model";
// import { StatusBar } from "@ionic-native/status-bar";
// import { SplashScreen } from "@ionic-native/splash-screen";
// import { Keyboard } from "@ionic-native/keyboard";
// import { AccountFieldDef, AccountService } from './core/core.module';
// import { Referential } from './core/services/model';
// import { DataService } from './shared/shared.module';


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {

  root: any = HomePage;
  logo: String;
  appName: String;
  menuItems: Array<MenuItem> = [
    { title: 'MENU.HOME', path: '/', icon: 'home' },
    { title: 'MENU.TRIPS', path: '/trips', icon: 'pin', profile: 'GUEST'},
    { title: 'MENU.EXTRACTIONS', path: '/extraction/table', icon: 'download', profile: 'SUPERVISOR' },
    { title: 'MENU.ADMINISTRATION_DIVIDER', profile: 'USER' },
    { title: 'MENU.USERS', path: '/admin/users', icon: 'people', profile: 'ADMIN' },
    { title: 'MENU.VESSELS', path: '/referential/vessels', icon: 'boat', profile: 'USER' },
    { title: 'MENU.REFERENTIALS', path: '/referential/list', icon: 'list', profile: 'ADMIN' },
    { title: 'MENU.PODCONFIG', path: '/admin/podconfig', icon: 'settings', profile: 'ADMIN' }

  ];

  constructor(
    @Inject(DOCUMENT) private _document: HTMLDocument,
    private platform: Platform,
    private accountService: AccountService,
    private referentialRefService: ReferentialRefService,
    private configurationService: PodConfigService
    // TODO: waiting ionic-native release
    // private statusBar: StatusBar, 
    // private splashScreen: SplashScreen,
    // private keyboard: Keyboard
  ) {

    platform.ready()
      .then(() => this.configurationService.getConfs())
      .then((config) => {
        this.onConfigReady(config)

        console.info("[app] Setting cordova plugins...");

        /*
        statusBar.styleDefault();
        splashScreen.hide();

        statusBar.overlaysWebView(false);

        // Control Keyboard
        keyboard.disableScroll(true);
        */

        this.addAccountFields();
      });

  }

  public onActivate(event) {
    // Make sure to scroll on top before changing state
    // See https://stackoverflow.com/questions/48048299/angular-5-scroll-to-top-on-every-route-click
    let scrollToTop = window.setInterval(() => {
      let pos = window.pageYOffset;
      if (pos > 0) {
        window.scrollTo(0, pos - 20); // how far to scroll on each step
      } else {
        window.clearInterval(scrollToTop);
      }
    }, 16);
  }

  protected onConfigReady(config: Configuration) {

    this.logo = config.smallLogo || config.largeLogo;
    this.appName = config.label;

    // Set document title
    const title = isNotNil(config.name) ? `${config.label} - ${config.name}` : config.label;
    this._document.getElementById('appTitle').textContent = title;

    // Set document favicon
    const favicon = config.properties && config.properties["sumaris.favicon"];
    if (isNotNil(favicon)){
      this._document.getElementById('appFavicon').setAttribute('href', favicon);
    }

    // this.updateTheme({
    //   colors: {
    //     primary: config.properties["sumaris.color.primary"],
    //     secondary: config.properties["sumaris.color.secondary"],
    //     tertiary: config.properties["sumaris.color.tertiary"]
    //   }
    // });

  }

  protected updateTheme(options: {colors?: {primary?: string; secondary?: string; tertiary?: string;}}) {
    if (!options)  return;

    console.info("[app] Changing theme colors ", options);

    // Settings colors
    if (options.colors) {
      Object.getOwnPropertyNames(options.colors).forEach(color => {
        if (color !== undefined && color !== null) {
          document.documentElement.style.setProperty(`--ion-color-${color}`, color);
        }
      });
    }
  }

  protected addAccountFields() {

    console.debug("[app] Add additional account fields...");

    // Add account field: department
    this.accountService.addAdditionalAccountField({
      name: 'department',
      label: 'USER.DEPARTMENT',
      required: true,
      dataService: this.referentialRefService as DataService<any, any>,
      dataFilter: { entityName: 'Department' },
      updatable: {
        registration: true,
        account: false
      }
    });
  }
}

