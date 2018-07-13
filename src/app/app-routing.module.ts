import { NgModule } from '@angular/core';
import { Routes, RouterModule, ExtraOptions } from '@angular/router';
import { HomePage } from './core/home/home';
import { RegisterConfirmPage } from './core/register/confirm/confirm';
import { AccountPage } from './core/account/account';
import { AuthGuardService } from './core/core.module';
import { UsersPage } from './admin/users/list/users';
import { VesselsPage } from './referential/vessel/list/vessels';
import { VesselPage } from './referential/vessel/page/page-vessel';
import { ReferentialsPage } from './referential/list/referentials';
import { TripsPage } from './trip/list/trips';
import { TripPage } from './trip/page/page-trip';

import { environment } from '../environments/environment';
import { OperationPage } from './trip/operation/page/page-operation';

const routeOptions: ExtraOptions = {
  enableTracing: false,
  //enableTracing: !environment.production,
  useHash: false
};

const routes: Routes = [
  // Core path
  {
    path: '',
    component: HomePage
  },

  {
    path: 'home/:action',
    component: HomePage
  },
  {
    path: 'confirm/:email/:code',
    component: RegisterConfirmPage
  },
  {
    path: 'account',
    component: AccountPage,
    canActivate: [AuthGuardService]
  },

  // Admin
  {
    path: 'admin/users',
    component: UsersPage,
    canActivate: [AuthGuardService]
  },

  // Referential pâth
  {
    path: 'referential',
    canActivate: [AuthGuardService],
    children: [
      {
        path: 'vessels',
        children: [
          { path: '', component: VesselsPage },
          { path: ':id', component: VesselPage }
        ]
      },
      {
        path: 'list',
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: '/referential/list/Location'
          },
          { path: ':entityName', component: ReferentialsPage }
        ]
      }
    ]
  },

  // Trip path
  {
    path: 'trips',
    canActivate: [AuthGuardService],
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: TripsPage
      },
      {
        path: ':tripId',
        pathMatch: 'full',
        component: TripPage,
        runGuardsAndResolvers: 'paramsOrQueryParamsChange'
      }
    ]
  },

  {
    path: 'operations',
    canActivate: [AuthGuardService],
    children: [
      {
        path: ':opeId',
        component: OperationPage,
        runGuardsAndResolvers: 'paramsOrQueryParamsChange'
      }
    ]
  },

  {
    path: "**",
    redirectTo: '/'
  },
];
@NgModule({
  imports: [
    RouterModule.forRoot(routes, routeOptions)
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule { }
