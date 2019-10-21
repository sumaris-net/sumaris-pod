import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import {ExtractionTablePage} from "./extraction-table-page.component";
import {ExtractionMapPage} from "./extraction-map-page.component";
import {ExtractionSelectTypeModal} from "./extraction-list-modal.component";
import {CoreModule} from "../core/core.module";
import {OutputRoutingModule} from "./output-routing.module";
import {LeafletModule} from "@asymmetrik/ngx-leaflet";

@NgModule({
  declarations: [
    ExtractionTablePage,
    ExtractionMapPage,
    ExtractionSelectTypeModal
  ],
  imports: [
    CommonModule,
    CoreModule,
    LeafletModule,
    OutputRoutingModule
  ],
  entryComponents: [
    ExtractionSelectTypeModal
  ],
  exports: [
    ExtractionTablePage,
    ExtractionMapPage
  ]
})
export class OutputModule {
  constructor() {
    console.debug('[output] Starting module ...');
  }
}
