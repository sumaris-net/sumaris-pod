import {RouterModule, Routes} from "@angular/router";
import {AuthGuardService} from "../core/services/auth-guard.service";
import {NgModule} from "@angular/core";
import {OutputExtractionPage} from "./output-extraction-page.component";
import {OutputMapPage} from "./output-map-page.component";

const routes: Routes = [
  {
    path: 'data',
    pathMatch: 'full',
    component: OutputExtractionPage,
    canActivate: [AuthGuardService],
    runGuardsAndResolvers: 'pathParamsChange',
    data: {
      profile: 'SUPERVISOR'
    }
  },
  {
    path: 'map',
    pathMatch: 'full',
    component: OutputMapPage,
    canActivate: [AuthGuardService],
    runGuardsAndResolvers: 'pathParamsChange',
    data: {
      profile: 'USER'
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class OutputRoutingModule {
}
