import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { PersonService } from './services/person.service';
import { PersonValidatorService } from './services/person.validator';
import { UsersPage } from './users/list/users';

@NgModule({
    imports: [
        CommonModule,
        CoreModule
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
}
