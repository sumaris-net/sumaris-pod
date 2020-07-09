import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule} from '../core/core.module';
import {PersonValidatorService} from './services/validator/person.validator';
import {UsersPage} from './users/list/users';
import {AdminRoutingModule} from "./admin-routing.module";
import {ReferentialModule} from "../referential/referential.module";
import {ConfigurationPage} from "./config/configuration.page";
import {SocialModule} from "../social/social.module";

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    SocialModule,
    ReferentialModule,
    AdminRoutingModule
  ],
  declarations: [
    UsersPage,
    ConfigurationPage
  ],
  exports: [
    UsersPage,
    ConfigurationPage
  ],
  providers: [
    // PersonService,
    PersonValidatorService
  ]
})
export class AdminModule {
}
