import "./vendor";

import {APP_BASE_HREF, CommonModule} from "@angular/common";
import {BrowserModule} from "@angular/platform-browser";
import {CUSTOM_ELEMENTS_SCHEMA, NgModule} from "@angular/core";
import {DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE} from "@angular/material/core";
import {DATE_ISO_PATTERN} from "./core/constants";
import {MomentDateAdapter} from '@angular/material-moment-adapter';
import {SplashScreen} from '@ionic-native/splash-screen/ngx';
import {StatusBar} from '@ionic-native/status-bar/ngx';
import {Keyboard} from '@ionic-native/keyboard/ngx';
import {NativeAudio} from "@ionic-native/native-audio/ngx";
import {Vibration} from '@ionic-native/vibration/ngx';
// App modules
import {AppComponent} from "./app.component";
import {AppRoutingModule} from "./app-routing.module";
import {CoreModule} from "./core/core.module";

import {environment} from "../environments/environment";
import {HttpClientModule} from "@angular/common/http";
import {HTTP} from "@ionic-native/http/ngx";
import {Camera} from "@ionic-native/camera/ngx";
import {Network} from "@ionic-native/network/ngx";
import {AudioManagement} from "@ionic-native/audio-management/ngx";
import {APP_LOCAL_SETTINGS_OPTIONS} from "./core/services/local-settings.service";
import {ConfigOptions, LocalSettings} from "./core/services/model";
import {TripModule} from "./trip/trip.module";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {APP_CONFIG_OPTIONS, ConfigService} from "./core/services/config.service";
import {TripConfigOptions} from "./trip/services/config/trip.config";
import {IonicStorageModule} from "@ionic/storage";
import {InAppBrowser} from "@ionic-native/in-app-browser/ngx";
import {APP_MENU_ITEMS} from "./core/menu/menu.component";
import {APP_HOME_BUTTONS} from "./core/home/home";


@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    CommonModule,
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    // functional modules
    AppRoutingModule,
    CoreModule,
    TripModule,
    IonicStorageModule.forRoot({
      name: 'sumaris',
      driverOrder: ['indexeddb', 'sqlite', 'websql']
    })
  ],
  providers: [
    StatusBar,
    SplashScreen,
    Keyboard,
    Camera,
    HTTP,
    Network,
    NativeAudio,
    Vibration,
    InAppBrowser,
    AudioManagement,
    ConfigService,
    {provide: APP_BASE_HREF, useValue: (environment.baseUrl || '/')},
    //{ provide: ErrorHandler, useClass: IonicErrorHandler },
    {provide: MAT_DATE_LOCALE, useValue: 'en'},
    {
      provide: MAT_DATE_FORMATS, useValue: {
        parse: {
          dateInput: DATE_ISO_PATTERN,
        },
        display: {
          dateInput: 'L',
          monthYearLabel: 'MMM YYYY',
          dateA11yLabel: 'LL',
          monthYearA11yLabel: 'MMMM YYYY',
        }
      }
    },
    {provide: DateAdapter, useClass: MomentDateAdapter, deps: [MAT_DATE_LOCALE, MAT_DATE_FORMATS]},

    { provide: APP_LOCAL_SETTINGS_OPTIONS, useValue: {
        pageHistoryMaxSize: 3
      } as LocalSettings
    },

    // Config options (Core + trip)
    { provide: APP_CONFIG_OPTIONS, useValue: {...ConfigOptions, ...TripConfigOptions}},

    // Menu items
    { provide: APP_MENU_ITEMS, useValue: [
        {title: 'MENU.HOME', path: '/', icon: 'home'},

        // Data entry
        {title: 'MENU.DATA_ENTRY_DIVIDER', profile: 'USER'},
        {title: 'MENU.TRIPS', path: '/trips',
          matIcon: 'explore',
          profile: 'USER',
          ifProperty: 'sumaris.trip.enable',
          titleProperty: 'sumaris.trip.name'
        },
        {
          title: 'MENU.OBSERVED_LOCATIONS', path: '/observations',
          matIcon: 'verified_user',
          profile: 'USER',
          ifProperty: 'sumaris.observedLocation.enable',
          titleProperty: 'sumaris.observedLocation.name'
        },

        // Data extraction
        {title: 'MENU.EXTRACTION_DIVIDER', profile: 'SUPERVISOR'},
        {title: 'MENU.TRIPS', path: '/extraction/table', icon: 'cloud-download', profile: 'SUPERVISOR'},
        {title: 'MENU.MAP', path: '/extraction/map', icon: 'earth', profile: 'SUPERVISOR'},

        // Referential
        {title: 'MENU.REFERENTIAL_DIVIDER', profile: 'USER'},
        {title: 'MENU.VESSELS', path: '/referential/vessels', icon: 'boat', profile: 'USER'},
        {title: 'MENU.REFERENTIAL', path: '/referential/list', icon: 'list', profile: 'ADMIN'},
        {title: 'MENU.USERS', path: '/admin/users', icon: 'people', profile: 'ADMIN'},
        {title: 'MENU.SERVER_SETTINGS', path: '/admin/config', matIcon: 'build', profile: 'ADMIN'},

        // Settings
        {title: '' /*empty divider*/, cssClass: 'flex-spacer'},
        {title: 'MENU.TESTING', path: '/testing', icon: 'code', color: 'danger', ifProperty: 'sumaris.testing.enable',},
        {title: 'MENU.LOCAL_SETTINGS', path: '/settings', icon: 'settings', color: 'medium'},
        {title: 'MENU.ABOUT', action: 'about', matIcon: 'help_outline', color: 'medium', cssClass: 'visible-mobile'},

        // Logout
        {title: 'MENU.LOGOUT', action: 'logout', icon: 'log-out', profile: 'GUEST', color: 'medium hidden-mobile'},
        {title: 'MENU.LOGOUT', action: 'logout', icon: 'log-out', profile: 'GUEST', color: 'danger visible-mobile'}

      ]
    },

    // Home buttons
    { provide: APP_HOME_BUTTONS, useValue: [
        // Data entry
        { title: 'MENU.DATA_ENTRY_DIVIDER', profile: 'USER'},
        { title: 'MENU.TRIPS', path: '/trips',
          matIcon: 'explore',
          profile: 'USER',
          ifProperty: 'sumaris.trip.enable',
          titleProperty: 'sumaris.trip.name'
        },
        { title: 'MENU.OBSERVED_LOCATIONS', path: '/observations',
          matIcon: 'verified_user',
          profile: 'USER',
          ifProperty: 'sumaris.observedLocation.enable',
          titleProperty: 'sumaris.observedLocation.name'
        },
        { title: '' /*empty divider*/, cssClass: 'visible-mobile'}
      ]
    }
  ],
  bootstrap: [AppComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule {
}
