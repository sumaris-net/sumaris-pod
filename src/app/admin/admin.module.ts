import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { AdminRoutingModule } from './admin-routing.module';
import { SharedModule } from '../shared/shared.module';
import { PersonService } from './services/person-service';
import { PersonValidatorService } from './users/validator/validators';
import { UsersPage } from './users/list/users';

import { HttpClient } from "@angular/common/http";
import { TranslateHttpLoader } from "@ngx-translate/http-loader";
import { TranslateModule, TranslateLoader } from "@ngx-translate/core";

@NgModule({
    imports: [
        CommonModule,
        CoreModule
        //AdminRoutingModule,
    ],
    declarations: [
        UsersPage
    ],
    exports: [
        UsersPage
    ],
    entryComponents: [
    ],
    providers: [
        PersonService,
        PersonValidatorService
    ]
})
export class AdminModule {
    constructor() {
        console.info("[admin] Starting module...");
    }
}