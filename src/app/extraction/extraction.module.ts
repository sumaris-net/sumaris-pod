import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule} from "../core/core.module";
import {ExtractionRoutingModule} from "./extraction-routing.module";
import {ExtractionTablePage} from "./table/extraction-table.page";
import {ProductPage} from "./product/page/product.page";
import {ExtractionMapPage} from "./map/extraction-map.page";
import {ExtractionCriteriaValidatorService} from "./services/validator/extraction-criterion.validator";
import {SelectProductModal} from "./product/modal/select-product.modal";
import {ExtractionCriteriaForm} from "./form/extraction-criteria.form";
import {ProductForm} from "./product/form/product.form";
import {ReferentialModule} from "../referential/referential.module";
import {LeafletModule} from "@asymmetrik/ngx-leaflet";
import {MarkdownModule} from "ngx-markdown";
import {ExtractionHelpModal} from "./help/help.modal";
import {TranslateModule} from "@ngx-translate/core";
import {ChartsModule} from "ng2-charts";

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    ReferentialModule,
    ExtractionRoutingModule,
    LeafletModule,
    TranslateModule.forChild(),
    MarkdownModule.forChild(),
    ChartsModule
  ],
  declarations: [
    ProductPage,
    ProductForm,
    SelectProductModal,
    ExtractionTablePage,
    ExtractionMapPage,
    ExtractionCriteriaForm,
    ExtractionHelpModal
  ],
  providers: [
    ExtractionCriteriaValidatorService
  ],
  exports: [
    ProductPage
  ]
})
export class ExtractionModule {

  constructor() {
    console.debug('[extraction] Creating module');
  }
}
