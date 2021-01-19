import {ModuleWithProviders, NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';
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
  IEntitiesService,
  isNil,
  isNotNil,
  joinPropertiesPath,
  LoadResult,
  nullIfUndefined,
  SharedModule,
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
import {IonicStorageModule} from '@ionic/storage';
import {HomePage} from './home/home';
import {RegisterForm} from './register/form/form-register';
import {RegisterModal} from './register/modal/modal-register';
import {AppGraphQLModule} from './graphql/graphql.module';
import {AppFormUtils, FormArrayHelper} from './form/form.utils';
import {AppTableUtils} from './table/table.utils';
import {IReferentialRef, Referential, ReferentialRef, referentialToString} from './services/model/referential.model';
import {Person} from './services/model/person.model';
import {Department} from './services/model/department.model';
import {StatusIds} from './services/model/model.enum';

import {environment} from '../../environments/environment';
import {Cloneable, Entity, EntityAsObjectOptions, entityToString, EntityUtils} from './services/model/entity.model';
// import ngx-translate and the http loader
import {HttpClientModule} from '@angular/common/http';
import {SelectPeerModal} from "./peer/select-peer.modal";
import {SettingsPage} from "./settings/settings.page";
import {AppEntityEditor} from "./form/editor.class";
import {CacheModule} from "ionic-cache";
import {AppPropertiesForm} from "./form/properties.form";
import {AppListForm} from "./form/list.form";
import {PlatformService} from "./services/platform.service";
import {IsNotOnFieldModePipe, IsOnFieldModePipe} from "./services/pipes/usage-mode.pipes";
import {PersonToStringPipe} from "./services/pipes/person-to-string.pipe";
import {AppInstallUpgradeCard} from "./install/install-upgrade-card.component";
import {AccountToStringPipe, IsLoginAccountPipe} from "./services/pipes/account.pipes";

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
  IEntitiesService,
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
  EntityAsObjectOptions
};


@NgModule({
  imports: [
    SharedModule,
    RouterModule,
    HttpClientModule,
    AppGraphQLModule,
    CacheModule,
    IonicStorageModule
  ],

  declarations: [
    // Pipes
    IsOnFieldModePipe,
    IsNotOnFieldModePipe,
    PersonToStringPipe,
    IsLoginAccountPipe,
    AccountToStringPipe,

    // Home and menu
    HomePage,
    MenuComponent,
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
    AppInstallUpgradeCard,

    // Other components
    TableSelectColumnsComponent,
    EntityMetadataComponent,
    FormButtonsBarComponent,
    AppPropertiesForm,
    AppListForm

  ],
  exports: [
    SharedModule,
    RouterModule,
    AppGraphQLModule,

    // Pipes
    IsOnFieldModePipe,
    IsNotOnFieldModePipe,
    PersonToStringPipe,
    IsLoginAccountPipe,
    AccountToStringPipe,

    // Components
    HomePage,
    AuthForm,
    AuthModal,
    TableSelectColumnsComponent,
    EntityMetadataComponent,
    FormButtonsBarComponent,
    MenuComponent,
    AboutModal,
    AppPropertiesForm,
    AppListForm,
    AppInstallUpgradeCard,
  ]
})
export class CoreModule {

  static forRoot(): ModuleWithProviders<CoreModule> {
    console.info("[core] Creating module (root)");
    return {
      ngModule: CoreModule,
      providers: [ PlatformService ]
    };
  }
}
