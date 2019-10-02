import "./vendor";

import {APP_BASE_HREF} from "@angular/common";
import {BrowserModule} from "@angular/platform-browser";
import {NgModule} from "@angular/core";
import {IonicModule} from "@ionic/angular";
import {
  DateAdapter,
  MAT_AUTOCOMPLETE_DEFAULT_OPTIONS,
  MAT_AUTOCOMPLETE_SCROLL_STRATEGY,
  MAT_DATE_FORMATS,
  MAT_DATE_LOCALE
} from "@angular/material";
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
import {AdminModule} from "./admin/admin.module";

import {ReferentialModule} from "./referential/referential.module";
import {TripModule} from "./trip/trip.module";
import {environment} from "../environments/environment";
import {HttpClientModule} from "@angular/common/http";
import {HTTP} from "@ionic-native/http/ngx";
import {LeafletModule} from "@asymmetrik/ngx-leaflet";
import {Camera} from "@ionic-native/camera/ngx";
import {CacheModule} from "ionic-cache";
import {Network} from "@ionic-native/network/ngx";
import {CloseScrollStrategy, Overlay} from "@angular/cdk/overlay";
import {AudioManagement} from "@ionic-native/audio-management/ngx";

export function scrollFactory(overlay: Overlay): () => CloseScrollStrategy {
  return () => overlay.scrollStrategies.close();
}


@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    AppRoutingModule,
    BrowserModule,
    HttpClientModule,
    IonicModule.forRoot(),
    CacheModule.forRoot(),
    LeafletModule.forRoot(),
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
    Camera,
    HTTP,
    Network,
    NativeAudio,
    Vibration,
    AudioManagement,
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
    {provide: MAT_AUTOCOMPLETE_DEFAULT_OPTIONS, useValue: {
        autoActiveFirstOption: true
      }
    },
    { provide: MAT_AUTOCOMPLETE_SCROLL_STRATEGY, useFactory: scrollFactory, deps: [Overlay] }
  ]
})
export class AppModule {
}
