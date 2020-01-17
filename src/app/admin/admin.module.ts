import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule} from '../core/core.module';
import {PersonValidatorService} from './services/person.validator';
import {UsersPage} from './users/list/users';
import {AdminRoutingModule} from "./admin-routing.module";
import {SoftwarePage} from "../referential/software/software.page";
import {ReferentialModule} from "../referential/referential.module";
import {ConfigurationPage} from "./config/configuration.page";

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
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
  entryComponents: [
    ConfigurationPage
  ],
  providers: [
    // PersonService,
    PersonValidatorService
  ]
})
export class AdminModule {
}
