import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AuthGuardService } from "./services/auth-guard.service";
import { HomePage } from "./home/home";
import { RegisterConfirmPage } from './register/confirm/confirm';
import { AccountPage } from './account/account';
import { UsersPage } from '../admin/users/list/users';

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
    canActivate: [AuthGuardService]
  },

  {
    path: "**",
    redirectTo: '/'
  }
];

export { routes as CoreRoutes };

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class CoreRoutingModule { }
