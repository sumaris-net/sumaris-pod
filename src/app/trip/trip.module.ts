import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { TripsPage } from './list/trips';
import { TripPage } from './page/page-trip';
import { TripForm } from './form/form-trip';
import { SaleForm } from './sale/form/form-sale';
import { OperationForm } from './operation/form/form-operation';
import { OperationPage } from './operation/page/page-operation';
import { MeasurementList } from './measurement/list/list-measurements';
import { MeasurementsValidatorService } from './measurement/validator/validators';
import { MatQualitativeValueField } from './measurement/form/material.qv-field';

import { PhysicalGearForm } from './physicalGear/form/form-physical-gear';
import { TripValidatorService } from './validator/validators';
import { SaleValidatorService } from './sale/validator/validators';
import { PositionValidatorService } from './position/validator/validators';

import { OperationTable } from './operation/table/table-operations';
import { TripService } from './services/trip-service';
import { OperationService } from './services/operation-service';
import { OperationValidatorService } from './operation/validator/validators';
import { TripModal } from './modal/modal-trip';
import { IonicModule } from '@ionic/angular';
import { PhysicalGearValidatorService } from './physicalGear/validator/validators';

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
        PhysicalGearForm,
        OperationForm,
        OperationPage,
        OperationTable,
        MeasurementList,
        MatQualitativeValueField
    ],
    exports: [
        TripsPage,
        TripPage,
        TripForm,
        TripModal,
        SaleForm,
        PhysicalGearForm,
        OperationForm,
        OperationPage,
        OperationTable,
        MeasurementList,
        MatQualitativeValueField
    ],
    entryComponents: [
        TripsPage,
        TripPage,
        TripModal,
        OperationTable,
        OperationPage,
        MeasurementList,
        MatQualitativeValueField
    ],
    providers: [
        TripService,
        TripValidatorService,
        SaleValidatorService,
        PhysicalGearValidatorService,
        OperationService,
        OperationValidatorService,
        PositionValidatorService,
        MeasurementsValidatorService
    ]
})
export class TripModule {
}
