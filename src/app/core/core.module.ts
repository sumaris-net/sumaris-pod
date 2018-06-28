import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreRoutingModule } from './core-routing.module';
import { RouterModule } from '@angular/router';

import { AccountService } from './services/account.service';
import { AuthGuardService } from './services/auth-guard.service';
import { CryptoService } from './services/crypto.service';
import { AuthForm } from './auth/form/form-auth';
import { AuthModal } from './auth/modal/modal-auth';
import { AboutModal } from './about/modal-about';

import { RegisterConfirmPage } from "./register/confirm/confirm";
import { AccountPage } from "./account/account";
import { SharedModule } from '../shared/shared.module';
import { AppForm } from './form/form.class';
import { FormMetadataComponent } from './form/form-metadata.component';
import { AppTable } from './table/table.class';
import { AppTableDataSource } from './table/table-datasource.class';
import { TableSelectColumnsComponent } from './table/table-select-columns.component';
import { MenuComponent } from './menu/menu.component';
import { IonicApp, IonicErrorHandler, IonicModule } from "ionic-angular";
import { HttpClient, HttpClientModule } from "@angular/common/http";
import { TranslateModule, TranslateService, TranslateLoader } from "@ngx-translate/core";
import { TranslateHttpLoader } from "@ngx-translate/http-loader";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicStorageModule } from '@ionic/storage';
import { HomePage } from './home/home';
import { RegisterForm } from './register/form/form-register';
import { RegisterModal } from './register/modal/modal-register';
import { AppGraphQLModule } from './graphql/graphql.module';
import { DateAdapter } from "@angular/material";
import * as moment from "moment/moment";

import { BrowserModule } from "@angular/platform-browser";
import { environment } from '../../environments/environment';

export { environment, AppForm, AppTable, AppTableDataSource, TableSelectColumnsComponent, AccountService, AuthGuardService, FormMetadataComponent }

export function createTranslateLoader(http: HttpClient) {
    return new TranslateHttpLoader(http, './assets/i18n/', '.json');
}

@NgModule({
    imports: [
        CommonModule,
        BrowserModule,
        RouterModule,
        //CoreRoutingModule,
        AppGraphQLModule,
        SharedModule,
        HttpClientModule,
        ReactiveFormsModule,
        IonicStorageModule.forRoot(),
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: (createTranslateLoader),
                deps: [HttpClient]
            }
        })
    ],

    declarations: [
        MenuComponent,
        HomePage,
        // Auth & Register
        AuthForm,
        AuthModal,
        RegisterForm,
        RegisterModal,
        RegisterConfirmPage,
        AccountPage,

        // Components
        TableSelectColumnsComponent,
        AboutModal,
        FormMetadataComponent
    ],
    exports: [
        CommonModule,
        BrowserModule,
        SharedModule,
        RouterModule,
        AppGraphQLModule,
        HomePage,
        AuthForm,
        AuthModal,
        TableSelectColumnsComponent,
        FormMetadataComponent,
        MenuComponent,
        ReactiveFormsModule,
        TranslateModule,
        AboutModal
    ],
    entryComponents: [
        RegisterModal,
        AuthModal,
        TableSelectColumnsComponent,
        FormMetadataComponent,
        AboutModal
    ],
    providers: [
        AccountService,
        AuthGuardService,
        CryptoService
    ]
})
export class CoreModule {

    constructor(
        private translate: TranslateService,
        private accountService: AccountService,
        private dateAdapter: DateAdapter<any>) {

        console.info("[core] Starting module...");

        // this language will be used as a fallback when a translation isn't found in the current language
        translate.setDefaultLang(environment.defaultLocale);

        // When locale changes, apply to date adapter
        translate.onLangChange.subscribe(event => {
            if (event && event.lang) {

                // Config date adapter
                dateAdapter.setLocale(event.lang);

                // config moment lib
                try {
                    const momentLocale: string = event.lang.substr(0, 2);
                    moment.locale(momentLocale);
                    console.debug('[app] Use locale {' + event.lang + '}');
                }
                // If error, fallback to en
                catch (err) {
                    dateAdapter.setLocale('en');
                    moment.locale('en');
                    console.warn('[app] Unknown local for moment lib. Using default [en]');
                }

            }
        });

        accountService.onLogin.subscribe(account => {
            if (account.settings && account.settings.locale && account.settings.locale != translate.currentLang) {
                this.translate.use(account.settings.locale);
            }
        });
    }

}