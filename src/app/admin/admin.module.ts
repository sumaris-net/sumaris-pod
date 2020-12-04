import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule} from '../core/core.module';
import {UsersPage} from './users/list/users';
import {ReferentialModule} from "../referential/referential.module";
import {ConfigurationPage} from "./config/configuration.page";
import {SocialModule} from "../social/social.module";

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    SocialModule,
    ReferentialModule
  ],
  declarations: [
    UsersPage,
    ConfigurationPage
  ],
  exports: [
    UsersPage,
    ConfigurationPage
  ]
})
export class AdminModule {

  constructor() {
    console.debug('[admin] Creating module');
  }
}
