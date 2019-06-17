import {Component, Inject} from '@angular/core';
import {MenuItem} from './core/menu/menu.component';
import {AccountService, isNotNil} from './core/core.module';
import {ReferentialRefService} from './referential/referential.module';
import {ConfigService} from './core/services/config.service';
import {DOCUMENT} from "@angular/common";
import {Configuration} from "./core/services/model";
import {SplashScreen} from '@ionic-native/splash-screen/ngx';
import {StatusBar} from '@ionic-native/status-bar/ngx';
import {Keyboard} from "@ionic-native/keyboard/ngx";
import {PlatformService} from "./core/services/platform.service";


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {

  logo: String;
  appName: String;
  menuItems: Array<MenuItem> = [
    {title: 'MENU.HOME', path: '/', icon: 'home'},

    // Data entry
    {title: 'MENU.DATA_ENTRY_DIVIDER', profile: 'USER'},
    {title: 'MENU.TRIPS', path: '/trips', icon: 'pin', profile: 'USER'},
    {
      title: 'MENU.OBSERVED_LOCATIONS', path: '/observations',
      matIcon: 'verified_user',
      profile: 'USER'
    },

    // Data extraction
    {title: 'MENU.EXTRACTION_DIVIDER', profile: 'SUPERVISOR'},
    {title: 'MENU.TRIPS', path: '/extraction', icon: 'download', profile: 'SUPERVISOR'},
    {title: 'MENU.MAP', path: '/map', icon: 'globe', profile: 'SUPERVISOR'},

    // Referential
    {title: 'MENU.REFERENTIAL_DIVIDER', profile: 'USER'},
    {title: 'MENU.VESSELS', path: '/referential/vessels', icon: 'boat', profile: 'USER'},
    {title: 'MENU.REFERENTIAL', path: '/referential/list', icon: 'list', profile: 'ADMIN'},
    {title: 'MENU.USERS', path: '/admin/users', icon: 'people', profile: 'ADMIN'},
    {title: 'MENU.SERVER_SETTINGS', path: '/admin/config', matIcon: 'build', profile: 'ADMIN'},

    // Settings
    {title: '' /*empty divider*/},
    {title: 'MENU.LOCAL_SETTINGS', path: '/settings', icon: 'settings'},
    {title: 'MENU.ABOUT', action: 'about', matIcon: 'help_outline', cssClass: 'visible xs visible-sm'},
    {title: 'MENU.LOGOUT', action: 'logout', icon: 'log-out', profile: 'GUEST', cssClass: 'ion-color-danger'}

  ];

  constructor(
    @Inject(DOCUMENT) private _document: HTMLDocument,
    private platform: PlatformService,
    private accountService: AccountService,
    private referentialRefService: ReferentialRefService,
    private configurationService: ConfigService
  ) {

    this.platform.ready().then(() => {

      // Listen for config changed
      this.configurationService.config.subscribe(config => this.onConfigChanged(config));

      // Add additional account fields
      this.addAccountFields();

    });
  }

  public onActivate(event) {
    // Make sure to scroll on top before changing state
    // See https://stackoverflow.com/questions/48048299/angular-5-scroll-to-top-on-every-route-click
    const scrollToTop = window.setInterval(() => {
      let pos = window.pageYOffset;
      if (pos > 0) {
        window.scrollTo(0, pos - 20); // how far to scroll on each step
      } else {
        window.clearInterval(scrollToTop);
      }
    }, 16);
  }

  protected onConfigChanged(config: Configuration) {

    this.logo = config.smallLogo || config.largeLogo;
    this.appName = config.label;

    // Set document title
    const title = isNotNil(config.name) ? `${config.label} - ${config.name}` : config.label;
    this._document.getElementById('appTitle').textContent = title;

    // Set document favicon
    const favicon = config.properties && config.properties["sumaris.favicon"];
    if (isNotNil(favicon)) {
      this._document.getElementById('appFavicon').setAttribute('href', favicon);
    }

    if (config.properties) {
      this.updateTheme({
        colors: {
          primary: config.properties["sumaris.color.primary"],
          secondary: config.properties["sumaris.color.secondary"],
          tertiary: config.properties["sumaris.color.tertiary"]
        }
      });
    }

  }


  protected updateTheme(options: { colors?: { primary?: string; secondary?: string; tertiary?: string; } }) {
    if (!options) return;

    console.info("[app] Changing theme colors ", options);

    // Settings colors
    if (options.colors) {
      Object.getOwnPropertyNames(options.colors).forEach(color => {
        if (color !== undefined && color !== null) {
          // TODO
          //document.documentElement.style.removeProperty(`--ion-color-${color}`);
          //document.documentElement.style.setProperty(`--ion-color-${color}`, color);
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
      dataService: this.referentialRefService,
      dataFilter: {entityName: 'Department'},
      updatable: {
        registration: true,
        account: false
      }
    });
  }
}

