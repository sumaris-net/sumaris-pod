import {NgModule} from "@angular/core";
import {RouterModule, Routes} from "@angular/router";
import {AutocompleteTestPage} from "../autocomplete/testing/autocomplete.test";
import {LatLongTestPage} from "../latlong/testing/latlong.test";
import {MaterialTestingPage} from "./material.testing.page";
import {MaterialModule} from "../material.module";
import {IonicModule} from "@ionic/angular";
import {CommonModule} from "@angular/common";
import {ReactiveFormsModule} from "@angular/forms";


const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    component: MaterialTestingPage
  },
  {
    path: 'autocomplete',
    pathMatch: 'full',
    component: AutocompleteTestPage
  },
  {
    path: 'latlong',
    pathMatch: 'full',
    component: LatLongTestPage
  }
];

@NgModule({
  imports: [
    CommonModule,
    IonicModule,
    ReactiveFormsModule,
    MaterialModule,
    RouterModule.forChild(routes)
  ],
  declarations: [
    MaterialTestingPage,
    AutocompleteTestPage,
    LatLongTestPage
  ],
  exports: [
    RouterModule,
    MaterialTestingPage,
    AutocompleteTestPage,
    LatLongTestPage
  ]
})
export class MaterialTestingModule {
}
