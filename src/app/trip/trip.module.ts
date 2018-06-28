import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { TripRoutingModule } from './trip-routing.module';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { TripsPage } from './list/trips';
import { TripPage } from './page/page-trip';
import { SaleForm } from './sale/form/form-sale';
import { TripValidatorService } from './validator/validators';
import { SaleValidatorService } from './sale/validator/validators';
import { PositionValidatorService } from './position/validator/validators';
import { TripForm } from './form/form-trip';
import { OperationTable } from './operation/table/table-operations';
import { TripService } from './services/trip-service';
import { OperationService } from './services/operation-service';
import { OperationValidatorService } from './operation/validator/validators';
import { TripModal } from './modal/modal-trip';
import { IonicModule } from 'ionic-angular';

@NgModule({
    imports: [
        CommonModule,
        CoreModule
    ],

    declarations: [
        TripsPage,
        TripPage,
        TripForm,
        TripModal,
        SaleForm,
        OperationTable
    ],
    exports: [
        TripsPage,
        TripPage,
        TripForm,
        TripModal,
        SaleForm,
        OperationTable
    ],
    entryComponents: [
        TripsPage,
        TripPage,
        TripModal,
        OperationTable
    ],
    providers: [
        TripService,
        TripValidatorService,
        SaleValidatorService,
        OperationService,
        OperationValidatorService,
        PositionValidatorService
    ]
})
export class TripModule {
    constructor() {
        console.info("[trip] Starting module...");
    }
}