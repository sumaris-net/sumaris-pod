import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { PersonService } from './services/person.service';
import { PersonValidatorService } from './services/person.validator';
import { UsersPage } from './users/list/users';
import { ConfigPage } from './config/config.component';
 import { CarouselComponent } from './config/carousel/carousel.component';
export {
    PersonService, PersonValidatorService, UsersPage
}

@NgModule({
    imports: [
        CommonModule,
        CoreModule
    ],
    declarations: [
        UsersPage,
      ConfigPage,
        CarouselComponent
    ],
    exports: [
        UsersPage,
      ConfigPage
    ],
    entryComponents: [
    ],
    providers: [
        PersonService,
        PersonValidatorService,
        CarouselComponent
    ]
})
export class AdminModule {
}
