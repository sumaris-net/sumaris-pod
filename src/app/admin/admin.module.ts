import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule} from '../core/core.module';
import {PersonValidatorService} from './services/person.validator';
import {UsersPage} from './users/list/users';
import {AdminRoutingModule} from "./admin-routing.module";
import {SoftwarePage} from "../referential/software/software.page";
import {ReferentialModule} from "../referential/referential.module";

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    ReferentialModule,
    AdminRoutingModule
  ],
  declarations: [
    UsersPage
  ],
  exports: [
    UsersPage
  ],
  entryComponents: [
    SoftwarePage
  ],
  providers: [
    PersonValidatorService
  ]
})
export class AdminModule {
  constructor() {
    console.debug('[admin] Starting module ...');
  }
}
