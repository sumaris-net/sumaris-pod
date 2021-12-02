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
import {IndividualMonitoringSubSamplesTable} from './sample/individualmonitoring/individual-monitoring-samples.table';
import {LeafletModule} from '@asymmetrik/ngx-leaflet';
import {SubBatchesModal} from './batch/modal/sub-batches.modal';
import {SubBatchForm} from './batch/form/sub-batch.form';
import {PhysicalGearModal} from './physicalgear/physical-gear.modal';
import {BatchForm} from './batch/form/batch.form';
import {BatchTreeComponent} from './batch/batch-tree.component';
import {BatchGroupForm} from './batch/form/batch-group.form';
import {BatchGroupModal} from './batch/modal/batch-group.modal';
import {SubBatchModal} from './batch/modal/sub-batch.modal';
import {AppReferentialModule} from '../referential/referential.module';
import {SampleForm} from './sample/sample.form';
import {SampleModal} from './sample/sample.modal';
import {SelectPhysicalGearModal} from './physicalgear/select-physical-gear.modal';
import {AppDataModule} from '../data/app-data.module';
import {OperationsMap} from './operation/map/operations.map';
import {SocialModule} from '@sumaris-net/ngx-components';
import {BatchModal} from './batch/modal/batch.modal';
import {TranslateModule} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {TripTrashModal} from './trip/trash/trip-trash.modal';
import {AppCoreModule} from '@app/core/core.module';
import {SelectOperationModal} from '@app/trip/operation/select-operation.modal';
import {SelectOperationByTripTable} from '@app/trip/operation/select-operation-by-trip.table';
import {TripOfflineModal} from '@app/trip/trip/offline/trip-offline.modal';
import {A11yModule} from '@angular/cdk/a11y';
import {VesselModule} from '@app/vessel/vessel.module';
import {SubSampleForm} from '@app/trip/sample/sub-sample.form';
import {SubSampleModal} from '@app/trip/sample/sub-sample.modal';

@NgModule({
  imports: [
    CommonModule,
    LeafletModule,
    TranslateModule.forChild(),

    // App module
    AppCoreModule,
    AppReferentialModule,
    AppDataModule,
    VesselModule,
    SocialModule,
    A11yModule
  ],
  declarations: [
    TripTable,
    TripForm,
    TripPage,
    TripTrashModal,
    TripOfflineModal,
    PhysicalGearTable,
    PhysicalGearForm,
    PhysicalGearModal,
    SelectPhysicalGearModal,
    SaleForm,
    OperationsTable,
    OperationForm,
    OperationPage,
    OperationsMap,
    SelectOperationModal,
    SelectOperationByTripTable,
    MeasurementsForm,
    CatchBatchForm,
    SamplesTable,
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
    SubSampleForm,
    SubSampleModal,
    IndividualMonitoringSubSamplesTable
  ],
  exports: [
    // Modules
    TranslateModule,
    SocialModule,

    // App modules
    AppCoreModule,
    AppDataModule,
    AppReferentialModule,

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
    SubSamplesTable,
    BatchGroupForm,
  ],
})
export class TripModule {

  constructor() {
    console.debug('[trip] Creating module...');
  }
}
