import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { CoreRoutes } from './core/core-routing.module';
import { ReferentialRoutes } from './referential/referential-routing.module';
import { AdminRoutes } from './admin/admin-routing.module';
import { TripRoutes } from './trip/trip-routing.module';
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

const routes2: Routes =
  CoreRoutes
    .concat(AdminRoutes)
    .concat(ReferentialRoutes)
    .concat(TripRoutes);

//console.log(routes);

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
          { path: '', component: ReferentialsPage },
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
      { path: '', component: TripsPage },
      {
        path: ':id', component: TripPage
      }
    ]
  },

  {
    path: "**",
    redirectTo: '/'
  },
];
@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
