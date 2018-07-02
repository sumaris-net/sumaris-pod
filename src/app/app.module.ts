
import "./vendor";

import { APP_BASE_HREF } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import { ErrorHandler, NgModule } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { MAT_DATE_FORMATS, MAT_DATE_LOCALE, DateAdapter } from "@angular/material";
import { DATE_ISO_PATTERN } from "./core/constants";
import { MomentDateAdapter } from '@angular/material-moment-adapter';
import { Routes, RouterModule } from '@angular/router';


// App modules
import { AppComponent } from "./app.component";
import { AppRoutingModule } from "./app-routing.module";
import { CoreModule } from "./core/core.module";
import { AdminModule } from "./admin/admin.module";
import { ReferentialModule } from "./referential/referential.module";
import { TripModule } from "./trip/trip.module";
import { environment } from "../environments/environment.prod";


@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    AppRoutingModule,
    CoreModule,
    AdminModule,
    ReferentialModule,
    TripModule,
    IonicModule.forRoot()
  ],
  bootstrap: [AppComponent],
  providers: [
    //StatusBar,
    //SplashScreen,
    //Keyboard,
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
    }
  ]
})
export class AppModule {
}
