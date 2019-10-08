import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {OutputExtractionPage} from "./output-extraction-page.component";
import {OutputMapPage} from "./output-map-page.component";
import {OutputSelectTypeModal} from "./output-list-modal.component";
import {CoreModule} from "../core/core.module";
import {OutputRoutingModule} from "./output-routing.module";
import {LeafletModule} from "@asymmetrik/ngx-leaflet";

@NgModule({
  declarations: [
    OutputExtractionPage,
    OutputMapPage,
    OutputSelectTypeModal
  ],
  imports: [
    CommonModule,
    CoreModule,
    LeafletModule,
    OutputRoutingModule
  ],
  entryComponents: [
    OutputSelectTypeModal
  ],
  exports: [
    OutputExtractionPage,
    OutputMapPage
  ]
})
export class OutputModule {
  constructor() {
    console.debug('[output] Starting module ...');
  }
}
