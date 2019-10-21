import {RouterModule, Routes} from "@angular/router";
import {AuthGuardService} from "../core/services/auth-guard.service";
import {NgModule} from "@angular/core";
import {ExtractionTablePage} from "./extraction-table-page.component";
import {ExtractionMapPage} from "./extraction-map-page.component";

const routes: Routes = [
  {
    path: '',
    canActivate: [AuthGuardService],
    children: [
      {
        path: 'data',
        pathMatch: 'full',
        component: ExtractionTablePage,
        runGuardsAndResolvers: 'pathParamsChange',
        data: {
          profile: 'SUPERVISOR'
        }
      }
    ]
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
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class OutputRoutingModule { }
