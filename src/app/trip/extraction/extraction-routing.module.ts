import {RouterModule, Routes} from "@angular/router";
import {AuthGuardService} from "../../core/services/auth-guard.service";
import {NgModule} from "@angular/core";
import {ExtractionDataPage} from "./extraction-data.page";
import {AggregationTypePage} from "./aggregation-type.page";
import {ExtractionMapPage} from "./extraction-map.page";
import {SharedModule} from "../../shared/shared.module";

const routes: Routes = [
  {
    path: 'table',
    pathMatch: 'full',
    component: ExtractionDataPage,
    runGuardsAndResolvers: 'pathParamsChange',
    data: {
      profile: 'SUPERVISOR'
    }
  },
  {
    path: 'aggregation/:aggregationTypeId',
    component: AggregationTypePage,
    runGuardsAndResolvers: 'pathParamsChange',
    data: {
      profile: 'SUPERVISOR',
      pathIdParam: 'aggregationTypeId'
    }
  },
  {
    path: 'map',
    canActivate: [AuthGuardService],
    children: [
      {
        path: '',
        //pathMatch: 'full',
        component: ExtractionMapPage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'USER'
        }
      }
    ]
  },
];

@NgModule({
  imports: [
    SharedModule,
    RouterModule.forChild(routes)
  ],
  exports: [RouterModule]
})
export class ExtractionRoutingModule {
}
