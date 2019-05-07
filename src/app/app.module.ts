import "./vendor";

import {APP_BASE_HREF} from "@angular/common";
import {BrowserModule} from "@angular/platform-browser";
import {NgModule} from "@angular/core";
import {IonicModule} from "@ionic/angular";
import {DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE, MAT_TABS_CONFIG} from "@angular/material";
import {DATE_ISO_PATTERN} from "./core/constants";
import {MomentDateAdapter} from '@angular/material-moment-adapter';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { Keyboard } from '@ionic-native/keyboard/ngx';
// App modules
import {AppComponent} from "./app.component";
import {AppRoutingModule} from "./app-routing.module";
import {CoreModule} from "./core/core.module";
import {AdminModule} from "./admin/admin.module";

import {ReferentialModule} from "./referential/referential.module";
import {TripModule} from "./trip/trip.module";
import {environment} from "../environments/environment";
import {NgxMaterialTimepickerTheme, TimepickerDirective} from "ngx-material-timepicker";
import {HttpClientModule} from "@angular/common/http";
import {HTTP} from "@ionic-native/http/ngx";

@NgModule({
  declarations: [
    AppComponent 
  ],
  imports: [
    AppRoutingModule,
    BrowserModule,
    HttpClientModule,
    IonicModule.forRoot(),
    // functional modules
    CoreModule,
    AdminModule,
    ReferentialModule,
    TripModule
  ],
  bootstrap: [AppComponent],
  providers: [
    StatusBar,
    SplashScreen,
    Keyboard,
    HTTP,
    { provide: APP_BASE_HREF, useValue: (environment.baseUrl || '/') },
    //{ provide: ErrorHandler, useClass: IonicErrorHandler },
    { provide: MAT_DATE_LOCALE, useValue: 'en' },
    { provide: DateAdapter, useClass: MomentDateAdapter, deps: [MAT_DATE_LOCALE] },
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
    {
      provide: MAT_TABS_CONFIG,
      useValue: {
        //animationDuration: '0ms'
      }
    }
  ]
})
export class AppModule {
}
