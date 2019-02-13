import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { PersonService } from './services/person.service';
import { PersonValidatorService } from './services/person.validator';
import { UsersPage } from './users/list/users';
import { PodConfigPage } from './podconfig/podconfig';
 import { CarouselComponent } from './podconfig/carousel/carousel.component';
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
        PodConfigPage,
        CarouselComponent
    ],
    exports: [
        UsersPage, 
        PodConfigPage
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
