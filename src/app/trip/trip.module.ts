import {NgModule} from '@angular/core';
import {TripTable} from './trip/trips.table';
import {TripPage} from './trip/trip.page';
import {TripForm} from './trip/trip.form';
import {SaleForm} from './sale/sale.form';
import {OperationForm} from './operation/operation.form';
import {OperationPage} from './operation/operation.page';
import {MeasurementsForm} from './measurement/measurements.form.component';
import {CatchBatchForm} from './catch/catch.form';
import {PhysicalGearForm} from './physicalgear/physical-gear.form';
import {PhysicalGearTable} from './physicalgear/physical-gears.table';
import {OperationsTable} from './operation/operations.table';
import {SamplesTable} from './sample/samples.table';
import {SubSamplesTable} from './sample/sub-samples.table';
import {BatchGroupsTable} from './batch/table/batch-groups.table';
import {BatchesTable} from './batch/table/batches.table';
import {SubBatchesTable} from './batch/table/sub-batches.table';
import {BiologicalSamplingSamplesTable} from './sample/sampling/biological-sampling-samples.table';
import {IndividualMonitoringSubSamplesTable} from './sample/individualmonitoring/individual-monitoring-samples.table';
import {LeafletModule} from "@asymmetrik/ngx-leaflet";
import {SubBatchesModal} from "./batch/modal/sub-batches.modal";
import {SubBatchForm} from "./batch/form/sub-batch.form";
import {PhysicalGearModal} from "./physicalgear/physical-gear.modal";
import {BatchForm} from "./batch/form/batch.form";
import {BatchTreeComponent} from "./batch/batch-tree.component";
import {BatchGroupForm} from "./batch/form/batch-group.form";
import {BatchGroupModal} from "./batch/modal/batch-group.modal";
import {SubBatchModal} from "./batch/modal/sub-batch.modal";
import {ReferentialModule} from "../referential/referential.module";
import {SampleForm} from "./sample/sample.form";
import {SampleModal} from "./sample/sample.modal";
import {SelectPhysicalGearModal} from "./physicalgear/select-physical-gear.modal";
import {DataModule} from "../data/data.module";
import {OperationsMap} from "./operation/map/operations.map";
import {SocialModule} from "../social/social.module";
import {BatchModal} from "./batch/modal/batch.modal";
import {CoreModule} from "../core/core.module";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {TripTrashModal} from "./trip/trash/trip-trash.modal";
import {TableAddPmfmsComponent} from "./sample/table-add-pmfms.component";

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    ReferentialModule,
    DataModule,
    SocialModule,
    LeafletModule,
    TranslateModule.forChild()
  ],
  declarations: [
    TripTable,
    TripForm,
    TripPage,
    TripTrashModal,
    PhysicalGearTable,
    PhysicalGearForm,
    PhysicalGearModal,
    SelectPhysicalGearModal,
    SaleForm,
    OperationsTable,
    OperationForm,
    OperationPage,
    OperationsMap,
    MeasurementsForm,
    CatchBatchForm,
    SamplesTable,
    BiologicalSamplingSamplesTable,
    SubSamplesTable,
    BatchGroupsTable,
    BatchesTable,
    BatchForm,
    BatchModal,
    SubBatchesTable,
    SubBatchForm,
    SubBatchModal,
    SubBatchesModal,
    BatchGroupsTable,
    BatchGroupForm,
    BatchGroupModal,
    BatchTreeComponent,
    SamplesTable,
    SampleForm,
    SampleModal,
    SubSamplesTable,
    IndividualMonitoringSubSamplesTable,
    TableAddPmfmsComponent
  ],
  exports: [
    // Modules
    TranslateModule,
    CoreModule,
    DataModule,
    SocialModule,
    ReferentialModule,

    // Pipes

    // Components
    TripTable,
    TripPage,
    TripForm,
    PhysicalGearModal,
    SaleForm,
    PhysicalGearForm,
    PhysicalGearTable,
    OperationForm,
    OperationPage,
    OperationsTable,
    MeasurementsForm,
    BatchForm,
    BatchTreeComponent,
    SubBatchForm,
    SubBatchModal,
    SampleForm,
    SamplesTable,
    BiologicalSamplingSamplesTable,
    SubSamplesTable,
    TableAddPmfmsComponent
  ]
})
export class TripModule {

  constructor() {
    console.debug('[trip] Creating module...');
  }
}
