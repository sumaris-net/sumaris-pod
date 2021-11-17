import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {CommonModule} from "@angular/common";
import {CoreModule}  from "@sumaris-net/ngx-components";
import {BatchTreeTestPage} from "./batch/testing/batch-tree.test";
import {TripModule} from "./trip.module";
import {SharedModule} from "@sumaris-net/ngx-components";
import {TranslateModule} from "@ngx-translate/core";
import {TestingPage} from "@sumaris-net/ngx-components";
import { BatchGroupFormTestPage } from '@app/trip/batch/form/testing/batch-group.form.test';
import { MatCheckboxModule } from '@angular/material/checkbox';

export const TRIP_TESTING_PAGES = [
  <TestingPage>{label: 'Batch tree', page: '/testing/trip/batchTree'},
  <TestingPage>{label: 'Batch group form', page: '/testing/trip/batchGroupForm'}
];

const routes: Routes = [
  {
    path: 'batchTree',
    pathMatch: 'full',
    component: BatchTreeTestPage
  },
  {
    path: 'batchGroupForm',
    pathMatch: 'full',
    component: BatchGroupFormTestPage
  }
];

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    CoreModule,
    TranslateModule.forChild(),
    RouterModule.forChild(routes),
    TripModule,
    MatCheckboxModule,
  ],
  declarations: [
    BatchGroupFormTestPage,
    BatchTreeTestPage
  ],
  exports: [
    BatchGroupFormTestPage,
    BatchTreeTestPage
  ]
})
export class TripTestingModule {

}
