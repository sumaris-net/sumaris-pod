import {RouterModule, Routes} from "@angular/router";
import {NgModule} from "@angular/core";
import {FormTestPage} from "./form/form.test";

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'form'
  },
  {
    path: 'form',
    pathMatch: 'full',
    component: FormTestPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [
    RouterModule
  ]
})
export class SharedTestRoutingModule { }
