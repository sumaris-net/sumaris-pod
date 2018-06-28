import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AuthGuardService } from "../core/services/auth-guard.service";

import { VesselsPage } from './vessel/list/vessels';
import { VesselPage } from './vessel/page/page-vessel';

import { ReferentialsPage } from './list/referentials';

const routes: Routes = [

  // Referential module
  {
    path: 'referential',
    canActivate: [AuthGuardService],
    children: [
      { path: 'vessels', component: VesselsPage },
      {
        path: 'list',
        children: [
          { path: '', component: ReferentialsPage },
          { path: ':entityName', component: ReferentialsPage }
        ]
      }
    ]
  },

  {
    path: 'vessels',
    canActivate: [AuthGuardService],
    component: VesselsPage
  }

];

export { routes as ReferentialRoutes };

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ReferentialRoutingModule { }
