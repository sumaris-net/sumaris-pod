import {NgModule} from "@angular/core";
import {SharedModule} from "../shared.module";
import {RouterModule, Routes} from "@angular/router";
import {AutocompleteTestPage} from "./autocomplete/testing/autocomplete.test";


const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'autocomplete'
  },
  {
    path: 'autocomplete',
    pathMatch: 'full',
    component: AutocompleteTestPage
  }
];

@NgModule({
  imports: [
    SharedModule,
    RouterModule.forChild(routes)
  ],
  declarations: [
    AutocompleteTestPage
  ],
  exports: [
    RouterModule,
    AutocompleteTestPage
  ]
})
export class MaterialTestingModule {
}
