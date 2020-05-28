import {NgModule} from "@angular/core";
import {RouterModule} from "@angular/router";
import {CommonModule} from "@angular/common";
import {MaterialTestingModule} from "./material/testing/material.testing.module";


// const routes: Routes = [
//   {
//     path: '',
//     pathMatch: 'full',
//     redirectTo: 'material'
//   },
//   {
//     path: 'material',
//     loadChildren: () => import('./material/testing/material.testing.module').then(m => m.MaterialTestingModule)
//   }
// ];

@NgModule({
  imports: [
    CommonModule,
    //RouterModule.forChild(routes),
    MaterialTestingModule
  ],
  exports: [
    RouterModule,
    MaterialTestingModule
  ]
})
export class SharedTestingModule {
}
