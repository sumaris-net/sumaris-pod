import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {AccountService} from './services/account.service';
import {AccountValidatorService} from './services/validator/account.validator';
import {UserSettingsValidatorService} from './services/validator/user-settings.validator';
import {BaseEntityService} from './services/base.data-service.class';
import {AuthForm} from './auth/form/form-auth';
import {AuthModal} from './auth/modal/modal-auth';
import {AboutModal} from './about/modal-about';
import {RegisterConfirmPage} from "./register/confirm/confirm";
import {AccountPage} from "./account/account";
import {
  fromDateISOString,
  isNil,
  isNotNil,
  joinPropertiesPath,
  LoadResult,
  nullIfUndefined,
  SharedModule,
  EntitiesService,
  toDateISOString
} from '../shared/shared.module';
import {AppForm} from './form/form.class';
import {AppTabEditor} from './form/tab-editor.class';
import {EntityMetadataComponent} from './form/entity-metadata.component';
import {FormButtonsBarComponent} from './form/form-buttons-bar.component';
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from './table/table.class';
import {EntitiesTableDataSource} from './table/entities-table-datasource.class';
import {TableSelectColumnsComponent} from './table/table-select-columns.component';
import {MenuComponent} from './menu/menu.component';
import {ReactiveFormsModule} from "@angular/forms";
import {IonicStorageModule} from '@ionic/storage';
import {HomePage} from './home/home';
import {RegisterForm} from './register/form/form-register';
import {RegisterModal} from './register/modal/modal-register';
import {AppGraphQLModule} from './graphql/graphql.module';
import {DateAdapter} from "@angular/material/core";
import * as moment from "moment/moment";
import {AppFormUtils, FormArrayHelper} from './form/form.utils';
import {AppTableUtils} from './table/table.utils';
import {IReferentialRef, Referential, ReferentialRef, referentialToString} from './services/model/referential.model';
import {Person} from './services/model/person.model';
import {Department} from './services/model/department.model';
import {StatusIds} from './services/model/model.enum';

import {environment} from '../../environments/environment';
import {
  Cloneable,
  Entity,
  EntityAsObjectOptions,
  entityToString,
  EntityUtils,
  PropertiesMap
} from './services/model/entity.model';
// import ngx-translate and the http loader
import {TranslateLoader, TranslateModule, TranslateService} from '@ngx-translate/core';
import {TranslateHttpLoader} from '@ngx-translate/http-loader';
import {HttpClient, HttpClientModule} from '@angular/common/http';
import {SelectPeerModal} from "./peer/select-peer.modal";
import {SettingsPage} from "./settings/settings.page";
import {LocalSettingsValidatorService} from "./services/validator/local-settings.validator";
import {LocalSettingsService} from "./services/local-settings.service";
import {AppEntityEditor} from "./form/editor.class";
import {EntitiesStorage} from "./services/entities-storage.service";
import {IonicModule} from "@ionic/angular";
import {CacheModule} from "ionic-cache";
import {AppPropertiesForm} from "./form/properties.form";
import {AppListForm} from "./form/list.form";

export {
  environment,
  AppForm,
  AppFormUtils,
  AppTable,
  AppTabEditor,
  EntitiesTableDataSource,
  AppEntityEditor,
  TableSelectColumnsComponent,
  BaseEntityService,
  AccountValidatorService,
  UserSettingsValidatorService,
  EntityMetadataComponent,
  FormButtonsBarComponent,
  RESERVED_START_COLUMNS,
  RESERVED_END_COLUMNS,
  Entity,
  Cloneable,
  EntityUtils,
  Referential,
  ReferentialRef,
  IReferentialRef,
  Department,
  Person,
  EntitiesService,
  LoadResult,
  toDateISOString,
  StatusIds,
  fromDateISOString,
  isNil,
  isNotNil,
  nullIfUndefined,
  entityToString,
  referentialToString,
  joinPropertiesPath,
  FormArrayHelper,
  AppTableUtils,
  EntityAsObjectOptions,
  PropertiesMap
};

export function HttpLoaderFactory(http: HttpClient) {
  if (environment.production) {
    // This is need to force a reload, after an app update
    return new TranslateHttpLoader(http, './assets/i18n/', `-${environment.version}.json`);
  }
  return new TranslateHttpLoader(http, './assets/i18n/', `.json`);
}

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    HttpClientModule,
    AppGraphQLModule,
    SharedModule,
    ReactiveFormsModule,
    IonicModule.forRoot(),
    CacheModule.forRoot(),
    IonicStorageModule.forRoot({
      name: 'sumaris', // default
      ...environment.storage
    }),
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [HttpClient]
      }
    })
  ],

  declarations: [
    HomePage,
    AboutModal,

    // Auth & Register
    AuthForm,
    AuthModal,
    RegisterForm,
    RegisterModal,
    RegisterConfirmPage,
    AccountPage,
    SettingsPage,

    // Network
    SelectPeerModal,

    // Components
    MenuComponent,
    TableSelectColumnsComponent,
    EntityMetadataComponent,
    FormButtonsBarComponent,
    AppPropertiesForm,
    AppListForm
  ],
  exports: [
    CommonModule,
    SharedModule,
    RouterModule,
    AppGraphQLModule,
    HomePage,
    AuthForm,
    AuthModal,
    TableSelectColumnsComponent,
    EntityMetadataComponent,
    FormButtonsBarComponent,
    MenuComponent,
    ReactiveFormsModule,
    TranslateModule,
    AboutModal,
    AppPropertiesForm,
    AppListForm
  ],
  providers: [
    LocalSettingsService,
    AccountValidatorService,
    UserSettingsValidatorService,
    LocalSettingsValidatorService,
    EntitiesStorage
  ]
})
export class CoreModule {

  constructor(
    translate: TranslateService,
    settings: LocalSettingsService,
    accountService: AccountService,
    dateAdapter: DateAdapter<any>) {

    console.info("[core] Starting module...");

    // this language will be used as a fallback when a translation isn't found in the current language
    translate.setDefaultLang(environment.defaultLocale);

    // When locale changes, apply to date adapter
    translate.onLangChange.subscribe(event => {
      if (event && event.lang) {

        // force 'en' as 'en_GB'
        if (event.lang === 'en') {
          event.lang = "en_GB";
        }

        // Config date adapter
        dateAdapter.setLocale(event.lang);

        // config moment lib
        try {
          moment.locale(event.lang);
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

    settings.onChange.subscribe(data => {
      if (data && data.locale && data.locale !== translate.currentLang) {
        translate.use(data.locale);
      }
    });

    accountService.onLogin.subscribe(account => {
      if (settings.settings.accountInheritance) {
        if (account.settings && account.settings.locale && account.settings.locale !== translate.currentLang) {
          translate.use(account.settings.locale);
        }
      }
    });
  }

}
