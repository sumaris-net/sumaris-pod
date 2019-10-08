import {RouterModule, Routes} from "@angular/router";
import {UsersPage} from "./users/list/users";
import {AuthGuardService} from "../core/services/auth-guard.service";
import {SoftwarePage} from "../referential/software/software.page";
import {NgModule} from "@angular/core";

const routes: Routes = [
  {
    path: 'users',
    pathMatch: 'full',
    component: UsersPage,
    canActivate: [AuthGuardService],
    data: {
      profile: 'ADMIN'
    }
  },
  {
    path: 'config',
    pathMatch: 'full',
    component: SoftwarePage,
    canActivate: [AuthGuardService],
    data: {
      profile: 'ADMIN'
    }
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
