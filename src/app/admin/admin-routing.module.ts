import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AuthGuardService } from "../core/services/auth-guard.service";

import { UsersPage } from './users/list/users';

const routes: Routes = [

  // Users
  {
    path: 'users',
    component: UsersPage,
    canActivate: [AuthGuardService]
  }
];

export { routes as AdminRoutes }

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
