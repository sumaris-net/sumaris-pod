import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {CommonModule} from "@angular/common";
import {TranslateModule} from "@ngx-translate/core";
import {CoreModule} from "../core/core.module";
import {BatchTreeTestPage} from "./batch/testing/batch-tree.test";
import {TripModule} from "./trip.module";
import {APP_CONFIG_OPTIONS} from "../core/services/config.service";
import {ConfigOptions} from "../core/services/config/core.config";
import {TripConfigOptions} from "./services/config/trip.config";
import {APP_TESTING_PAGES, TestingPage} from "../shared/material/testing/material.testing.page";


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
    CoreModule,
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
