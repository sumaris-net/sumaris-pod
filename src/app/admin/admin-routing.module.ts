import {RouterModule, Routes} from "@angular/router";
import {UsersPage} from "./users/list/users";
import {AuthGuardService} from "../core/services/auth-guard.service";
import {NgModule} from "@angular/core";
import {ConfigurationPage} from "./config/configuration.page";
import {SharedRoutingModule} from "../shared/shared-routing.module";

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
    component: ConfigurationPage,
    canActivate: [AuthGuardService],
    data: {
      profile: 'ADMIN'
    }
  }
];

@NgModule({
  imports: [
    SharedRoutingModule,
    RouterModule.forChild(routes)
  ],
  exports: [RouterModule]
})
export class AdminRoutingModule { }
