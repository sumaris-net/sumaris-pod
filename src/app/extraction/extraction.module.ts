import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule}  from "@sumaris-net/ngx-components";
import {AppExtractionRoutingModule} from "./extraction-routing.module";
import {ExtractionTablePage} from "./table/extraction-table.page";
import {ProductPage} from "./product/page/product.page";
import {ExtractionMapPage} from "./map/extraction-map.page";
import {ExtractionCriteriaValidatorService} from "./services/validator/extraction-criterion.validator";
import {SelectProductModal} from "./product/modal/select-product.modal";
import {ExtractionCriteriaForm} from "./form/extraction-criteria.form";
import {ProductForm} from "./product/form/product.form";
import {AppReferentialModule} from "../referential/app-referential.module";
import {LeafletModule} from "@asymmetrik/ngx-leaflet";
import {MarkdownModule} from "ngx-markdown";
import {ExtractionHelpModal} from "./help/help.modal";
import {TranslateModule} from "@ngx-translate/core";
import {ChartsModule} from "ng2-charts";
import {AppCoreModule} from '@app/core/core.module';

@NgModule({
  imports: [
    CommonModule,
    LeafletModule,
    TranslateModule.forChild(),
    MarkdownModule.forChild(),
    ChartsModule,

    AppCoreModule,
    AppReferentialModule
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
export class AppExtractionModule {

  constructor() {
    console.debug('[extraction] Creating module');
  }
}
