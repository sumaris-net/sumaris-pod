import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { TripsPage } from './trip/trips.page';
import { TripPage } from './trip/trip.page';
import { TripForm } from './trip/trip.form';
import { SaleForm } from './sale/sale.form';
import { OperationForm } from './operation/operation.form';
import { OperationPage } from './operation/operation.page';
import { MeasurementsForm } from './measurement/measurements.form.component';
import { MeasurementQVFormField } from './measurement/measurement-qv.form-field.component';
import { MeasurementFormField } from './measurement/measurement.form-field.component';
import { CatchBatchForm } from './catch/catch.form';
import { PhysicalGearForm } from './physicalgear/physicalgear.form';
import { PhysicalGearTable } from './physicalgear/physicalgears.table';
import { OperationTable } from './operation/operations.table';
import { TripModal } from './trip/trip.modal';
import { SamplesTable } from './sample/samples.table';
import { SubSamplesTable } from './sample/sub-samples.table';
import { BatchGroupsTable } from './batch/batch-groups.table';
import { BatchesTable } from './batch/batches.table';
import { SubBatchesTable } from './batch/sub-batches.table';
import { IndividualMonitoringTable } from './sample/individualmonitoring/sample-individual-monitoring.table';
import { MeasurementValuesForm } from './measurement/measurement-values.form.class';
import { EntityQualityFormComponent} from "./quality/entity-quality-form.component";

import {TripService, OperationService, ExtractionService, ObservedVesselService} from './services/trip.services';

import {
    TripValidatorService, SaleValidatorService, PhysicalGearValidatorService, OperationValidatorService, PositionValidatorService,
    MeasurementsValidatorService, BatchValidatorService, BatchGroupsValidatorService, SampleValidatorService,
    SubSampleValidatorService, SubBatchValidatorService
} from './services/trip.validators';
import {ExtractionTablePage} from "./extraction/extraction-table-page.component";
import {ObservedLocationForm} from "./observedlocation/observed-location.form";
import {ObservedLocationPage} from "./observedlocation/observed-location.page";
import {ObservedLocationsPage} from "./observedlocation/observed-locations.page";
import {ObservedLocationService} from "./services/observed-location.service";
import {ObservedLocationValidatorService} from "./services/observed-location.validator";
import {ObservedVesselsTable} from "./observedlocation/observed-vessels.table";
import {SaleService} from "./services/sale.service";
import {ExtractionMapPage} from "./extraction/extraction-map-page.component";
import {LeafletModule} from "@asymmetrik/ngx-leaflet";

export { TripsPage, TripPage, MeasurementValuesForm, SaleForm, MeasurementsForm, EntityQualityFormComponent };

@NgModule({
    imports: [
      CommonModule,
      CoreModule,
      LeafletModule
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
      ObservedLocationForm,
      ObservedLocationPage,
      ObservedLocationsPage,
      MeasurementsForm,
      MeasurementQVFormField,
      MeasurementFormField,
      CatchBatchForm,
      ObservedVesselsTable,
      SamplesTable,
      SubSamplesTable,
      BatchGroupsTable,
      BatchesTable,
      SubBatchesTable,
      IndividualMonitoringTable,
      EntityQualityFormComponent,
      ExtractionTablePage,
      ExtractionMapPage
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
      MeasurementQVFormField,
      ExtractionTablePage,
      ExtractionMapPage,
      EntityQualityFormComponent
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
      MeasurementFormField,
      ExtractionTablePage,
      ExtractionMapPage,
      ObservedLocationPage,
      ObservedLocationsPage
    ],
    providers: [
      TripService,
      TripValidatorService,
      PhysicalGearValidatorService,
      OperationService,
      OperationValidatorService,
      ObservedLocationService,
      ObservedLocationValidatorService,
      ObservedVesselService,
      SaleService,
      SaleValidatorService,
      PositionValidatorService,
      MeasurementsValidatorService,
      BatchValidatorService,
      SubBatchValidatorService,
      BatchGroupsValidatorService,
      SampleValidatorService,
      SubSampleValidatorService,
      ExtractionService
    ]
})
export class TripModule {
}
