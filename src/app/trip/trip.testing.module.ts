import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {CommonModule} from "@angular/common";
import {CoreModule}  from "@sumaris-net/ngx-components";
import {BatchTreeTestPage} from "./batch/testing/batch-tree.test";
import {TripModule} from "./trip.module";
import {SharedModule} from "@sumaris-net/ngx-components";
import {TranslateModule} from "@ngx-translate/core";
import {TestingPage} from "@sumaris-net/ngx-components";

export const TRIP_TESTING_PAGES = [
  <TestingPage>{label: 'Batch tree', page: '/testing/trip/batchTree'}
];

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'batchTree'
  },
  {
    path: 'batchTree',
    pathMatch: 'full',
    component: BatchTreeTestPage
  }
];

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    CoreModule,
    TranslateModule.forChild(),
    RouterModule.forChild(routes),
    TripModule
  ],
  declarations: [
    BatchTreeTestPage
  ],
  exports: [
    BatchTreeTestPage
  ]
})
export class TripTestingModule {

}
