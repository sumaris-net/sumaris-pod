import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {MaterialModule} from "./material/material.module";
import {MaterialTestingModule} from "./material/testing/material.testing.module";


const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'material'
  },
  {
    path: 'material',
    loadChildren: () => import('./material/testing/material.testing.module').then(m => m.MaterialTestingModule)
  }
];

@NgModule({
  imports: [
    MaterialModule,
    RouterModule.forChild(routes),
    MaterialTestingModule
  ],
  exports: [
    RouterModule,
    MaterialTestingModule
  ]
})
export class SharedTestingModule {
}
