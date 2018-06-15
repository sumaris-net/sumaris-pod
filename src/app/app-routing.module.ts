import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AuthGuard } from "../services/auth-guard";
import { HomePage } from "../pages/home/home";
import { TripPage } from "../pages/trip/trip";
import { TripsPage } from "../pages/trip/list/trips";
import { UsersPage } from '../pages/users/users';
import { RegisterConfirmPage } from '../pages/register/confirm/confirm';
import { AccountPage } from '../pages/account/account';
import { VesselsPage } from '../pages/vessel/list/vessels';
import { VesselPage } from '../pages/vessel/vessel';
import { ReferentialsPage } from '../pages/referential/list/referentials';



const routes: Routes = [
  {
    path: '',
    component: HomePage
  },
  {
    path: 'home/:action',
    component: HomePage
  },

  // Register
  {
    path: 'confirm/:email/:code',
    component: RegisterConfirmPage
  },

  // Account
  {
    path: 'account',
    component: AccountPage,
    canActivate: [AuthGuard]
  },


  // --- Data ---

  // Trips
  {
    path: 'trips',
    canActivate: [AuthGuard],
    children: [
      { path: '', component: TripsPage },
      { path: ':id', component: TripPage }
    ]
  },

  // --- Administration ---

  // Users
  {
    path: 'users',
    component: UsersPage,
    canActivate: [AuthGuard]
  },

  // Vessels
  {
    path: 'vessels',
    canActivate: [AuthGuard],
    children: [
      { path: '', component: VesselsPage },
      { path: ':id', component: VesselPage }
    ]
  },

  // Entities
  {
    path: 'referentials',
    canActivate: [AuthGuard],
    children: [
      { path: '', component: ReferentialsPage },
      { path: ':entityName', component: ReferentialsPage }
    ]
  },

  {
    path: "**",
    redirectTo: '/'
  }
];


@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
