import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AuthGuardService } from "../core/services/auth-guard.service";

import { TripPage } from "./page/page-trip";
import { TripsPage } from "./list/trips";

const routes: Routes = [
  {
    path: '',
    redirectTo: 'list',
    pathMatch: 'full'
  },
  {
    path: 'list',
    canActivate: [AuthGuardService],
    children: [
      { path: '', component: TripsPage },
      {
        path: ':id', component: TripPage
      }
    ]
  }
];

export { routes as TripRoutes };

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class TripRoutingModule { }
