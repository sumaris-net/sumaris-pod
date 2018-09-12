import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { TripsPage } from './trips.page';
import { TripPage } from './trip.page';
import { TripForm } from './trip.form';
import { SaleForm } from './sale/sale.form';
import { OperationForm } from './operation/operation.form';
import { OperationPage } from './operation/operation.page';
import { MeasurementsForm } from './measurement/measurements.form';
import { MeasurementQVFormField } from './measurement/measurement-qv.form-field';
import { MeasurementFormField } from './measurement/measurement.form-field';
import { CatchForm } from './catch/catch.form';
import { PhysicalGearForm } from './physicalgear/physicalgear.form';
import { PhysicalGearTable } from './physicalgear/physicalgears.table';
import { OperationTable } from './operation/operations.table';
import { TripModal } from './trip.modal';

import { TripService } from './services/trip.service';
import { OperationService } from './services/operation.service';

import { TripValidatorService } from './services/trip.validator';
import { SaleValidatorService } from './services/sale.validator';
import { PhysicalGearValidatorService } from './services/physicalgear.validator';
import { OperationValidatorService } from './services/operation.validator';
import { PositionValidatorService } from './services/position.validator';
import { MeasurementsValidatorService } from './services/measurement.validator';
import { CatchValidatorService } from './services/catch.validator';

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
        PhysicalGearTable,
        OperationForm,
        OperationPage,
        OperationTable,
        MeasurementsForm,
        MeasurementQVFormField,
        MeasurementFormField,
        CatchForm
    ],
    exports: [
        TripsPage,
        TripPage,
        TripForm,
        TripModal,
        SaleForm,
        PhysicalGearForm,
        PhysicalGearTable,
        OperationForm,
        OperationPage,
        OperationTable,
        MeasurementsForm,
        MeasurementQVFormField
    ],
    entryComponents: [
        TripsPage,
        TripPage,
        TripModal,
        PhysicalGearTable,
        OperationTable,
        OperationPage,
        MeasurementsForm,
        MeasurementQVFormField,
        MeasurementFormField
    ],
    providers: [
        TripService,
        TripValidatorService,
        SaleValidatorService,
        PhysicalGearValidatorService,
        OperationService,
        OperationValidatorService,
        PositionValidatorService,
        MeasurementsValidatorService,
        CatchValidatorService
    ]
})
export class TripModule {
}
