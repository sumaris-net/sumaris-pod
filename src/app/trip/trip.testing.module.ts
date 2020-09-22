import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {CommonModule} from "@angular/common";
import {CoreModule} from "../core/core.module";
import {BatchTreeTestPage} from "./batch/testing/batch-tree.test";
import {TripModule} from "./trip.module";
import {SharedModule} from "../shared/shared.module";
import {TranslateModule} from "@ngx-translate/core";

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
