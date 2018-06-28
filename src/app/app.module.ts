
import "./vendor";

import { APP_BASE_HREF } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { ErrorHandler, NgModule } from "@angular/core";
import { SplashScreen } from "@ionic-native/splash-screen";
import { StatusBar } from "@ionic-native/status-bar";
import { Keyboard } from "@ionic-native/keyboard";
import { IonicApp, IonicErrorHandler, IonicModule } from "ionic-angular";
import { MAT_DATE_FORMATS, MAT_DATE_LOCALE, DateAdapter } from "@angular/material";
import { DATE_ISO_PATTERN } from "./core/constants";
import { MomentDateAdapter } from '@angular/material-moment-adapter';


import { AppComponent } from "./app.component";
import { CoreModule } from "./core/core.module";
import { AdminModule } from "./admin/admin.module";
import { ReferentialModule } from "./referential/referential.module";
import { AppRoutingModule } from "./app-routing.module";
import { TripModule } from "./trip/trip.module";

import { Routes, RouterModule } from '@angular/router';
@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    CoreModule,
    AdminModule,
    ReferentialModule,
    TripModule,
    IonicModule.forRoot(AppComponent)
  ],
  bootstrap: [IonicApp],
  providers: [
    StatusBar,
    SplashScreen,
    Keyboard,
    { provide: APP_BASE_HREF, useValue: '/' },
    { provide: ErrorHandler, useClass: IonicErrorHandler },
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
    }
  ]
})
export class AppModule {
}
