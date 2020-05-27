import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";


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
    RouterModule.forChild(routes)
  ],
  exports: [
    RouterModule
  ]
})
export class SharedTestingModule {
}
