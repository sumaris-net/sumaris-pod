import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule} from '../core/core.module';
import {TripTable} from './trip/trips.table';
import {TripPage} from './trip/trip.page';
import {TripForm} from './trip/trip.form';
import {SaleForm} from './sale/sale.form';
import {OperationForm} from './operation/operation.form';
import {OperationPage} from './operation/operation.page';
import {MeasurementsForm} from './measurement/measurements.form.component';
import {MeasurementQVFormField} from './measurement/measurement-qv.form-field.component';
import {MeasurementFormField} from './measurement/measurement.form-field.component';
import {CatchBatchForm} from './catch/catch.form';
import {PhysicalGearForm} from './physicalgear/physicalgear.form';
import {PhysicalGearTable} from './physicalgear/physicalgears.table';
import {OperationTable} from './operation/operations.table';
import {SamplesTable} from './sample/samples.table';
import {SubSamplesTable} from './sample/sub-samples.table';
import {BatchGroupsTable} from './batch/batch-groups.table';
import {BatchesTable} from './batch/batches.table';
import {SubBatchesTable} from './batch/sub-batches.table';
import {IndividualMonitoringSubSamplesTable} from './sample/individualmonitoring/individual-monitoring-samples.table';
import {MeasurementValuesForm} from './measurement/measurement-values.form.class';
import {EntityQualityFormComponent} from "./quality/entity-quality-form.component";

import {TripService} from './services/trip.service';
import {LandingService} from './services/landing.service';
import {OperationService} from './services/operation.service';

import {
  BatchGroupValidatorService,
  BatchValidatorService,
  MeasurementsValidatorService,
  OperationValidatorService,
  PhysicalGearValidatorService,
  PositionValidatorService,
  SaleValidatorService,
  SampleValidatorService,
  SubBatchValidatorService,
  SubSampleValidatorService,
  TripValidatorService
} from './services/trip.validators';
import {ObservedLocationForm} from "./observedlocation/observed-location.form";
import {ObservedLocationPage} from "./observedlocation/observed-location.page";
import {ObservedLocationsPage} from "./observedlocation/observed-locations.page";
import {ObservedLocationService} from "./services/observed-location.service";
import {ObservedLocationValidatorService} from "./services/observed-location.validator";
import {LandingsTable} from "./landing/landings.table";
import {SaleService} from "./services/sale.service";
import {LeafletModule} from "@asymmetrik/ngx-leaflet";
import {LandingValidatorService} from "./services/landing.validator";
import {LandingPage} from "./landing/landing.page";
import {LandingForm} from "./landing/landing.form";
import {SelectLandingsModal} from "./landing/select-landings.modal";
import {AuctionControlPage} from "./auctioncontrol/auction-control.page";
import {SubBatchesModal} from "./batch/sub-batches.modal";
import {SubBatchForm} from "./batch/sub-batch.form";
import {PhysicalGearModal} from "./physicalgear/physicalgear.modal";
import {BatchModal} from "./batch/batch.modal";
import {BatchForm} from "./batch/batch.form";
import {BatchGroupPage} from "./batch/batch-group.page";
import {BatchGroupForm} from "./batch/batch-group.form";
import {BatchGroupModal} from "./batch/batch-group.modal";
import {SubBatchModal} from "./batch/sub-batch.modal";
import {FullscreenOverlayContainer, OverlayContainer} from "@angular/cdk/overlay";
import {AggregationTypeValidatorService} from "./services/validator/aggregation-type.validator";
import {ReferentialModule} from "../referential/referential.module";
import {SampleForm} from "./sample/sample.form";
import {SampleModal} from "./sample/sample.modal";
import {SelectVesselsModal} from "./observedlocation/vessels/select-vessel.modal";
import {LandedTripPage} from "./landedtrip/landed-trip.page";
import {OperationGroupTable} from "./operationgroup/operation-groups.table";
import {OperationGroupValidatorService} from "./services/validator/operation-group.validator";
import {ProductsTable} from "./product/products.table";
import {ProductValidatorService} from "./services/validator/product.validator";
import {LandedSaleForm} from "./sale/landed-sale.form";
import {PacketsTable} from "./packet/packets.table";
import {PacketValidatorService} from "./services/validator/packet.validator";
import {PacketForm} from "./packet/packet.form";
import {PacketModal} from "./packet/packet.modal";
import {SelectPhysicalGearModal} from "./physicalgear/select-physicalgear.modal";
import {PhysicalGearService} from "./services/physicalgear.service";
import {ProductSaleForm} from "./sale/product-sale.form";
import {PacketCompositionValidatorService} from "./services/validator/packet-composition.validator";
import {ProductSaleModal} from "./sale/product-sale.modal";
import {PacketSaleModal} from "./sale/packet-sale.modal";
import {PacketSaleForm} from "./sale/packet-sale.form";

export { TripTable, TripPage, MeasurementValuesForm, SaleForm, MeasurementsForm, EntityQualityFormComponent };

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    LeafletModule,
    ReferentialModule
  ],
  declarations: [
    TripTable,
    TripPage,
    TripForm,
    PhysicalGearModal,
    SelectPhysicalGearModal,
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
    LandingsTable,
    LandingPage,
    LandingForm,
    SelectLandingsModal,
    SamplesTable,
    SubSamplesTable,
    BatchGroupsTable,
    BatchesTable,
    SubBatchesTable,
    SubBatchForm,
    SubBatchesModal,
    BatchForm,
    BatchModal,
    BatchGroupModal,
    BatchGroupPage,
    BatchGroupForm,
    SubBatchModal,
    SampleForm,
    SampleModal,
    IndividualMonitoringSubSamplesTable,
    AuctionControlPage,
    EntityQualityFormComponent,
    SelectVesselsModal,
    LandedTripPage,
    OperationGroupTable,
    ProductsTable,
    ProductSaleForm,
    ProductSaleModal,
    LandedSaleForm,
    PacketsTable,
    PacketForm,
    PacketModal,
    PacketSaleForm,
    PacketSaleModal
  ],
  exports: [
    TripTable,
    TripPage,
    TripForm,
    PhysicalGearModal,
    SaleForm,
    PhysicalGearForm,
    PhysicalGearTable,
    OperationForm,
    OperationPage,
    OperationTable,
    MeasurementsForm,
    MeasurementQVFormField,
    EntityQualityFormComponent,
    LandingsTable,
    LandingForm,
    LandingPage,
    SelectLandingsModal,
    SelectVesselsModal,
    AuctionControlPage,
    BatchForm,
    BatchGroupPage,
    SubBatchForm,
    SubBatchModal,
    SampleForm
  ],
  entryComponents: [
    TripTable,
    TripPage,
    PhysicalGearModal,
    SelectPhysicalGearModal,
    PhysicalGearTable,
    OperationTable,
    OperationPage,
    ObservedLocationPage,
    ObservedLocationsPage,
    LandingPage,
    SelectLandingsModal,
    AuctionControlPage,
    SubBatchesModal,
    BatchModal,
    BatchGroupModal,
    BatchGroupPage,
    SubBatchModal,
    SampleModal,
    SelectVesselsModal,
    PacketModal,
    ProductSaleModal,
    PacketSaleModal
  ],
  providers: [
    TripService,
    TripValidatorService,
    PhysicalGearValidatorService,
    OperationService,
    OperationValidatorService,
    OperationGroupValidatorService,
    ObservedLocationService,
    ObservedLocationValidatorService,
    LandingService,
    LandingValidatorService,
    SaleService,
    SaleValidatorService,
    PositionValidatorService,
    MeasurementsValidatorService,
    BatchValidatorService,
    SubBatchValidatorService,
    BatchGroupValidatorService,
    SampleValidatorService,
    SubSampleValidatorService,
    {provide: OverlayContainer, useClass: FullscreenOverlayContainer},
    AggregationTypeValidatorService,
    ProductValidatorService,
    PacketValidatorService,
    PacketCompositionValidatorService,
    PhysicalGearService
  ]
})
export class TripModule {

}
