import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule} from "../core/core.module";
import {ExtractionRoutingModule} from "./extraction-routing.module";
import {ExtractionTablePage} from "./table/extraction-table.page";
import {AggregationTypePage} from "./type/page/aggregation-type.page";
import {ExtractionMapPage} from "./map/extraction-map.page";
import {ExtractionCriteriaValidatorService} from "./services/validator/extraction-criterion.validator";
import {AggregationTypeSelectModal} from "./type/modal/aggregation-type-select.modal";
import {ExtractionCriteriaForm} from "./form/extraction-criteria.form";
import {AggregationTypeForm} from "./type/form/aggregation-type.form";
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
    AggregationTypePage,
    AggregationTypeForm,
    AggregationTypeSelectModal,
    ExtractionTablePage,
    ExtractionMapPage,
    ExtractionCriteriaForm,
    ExtractionHelpModal
  ],
  providers: [
    ExtractionCriteriaValidatorService
  ],
  exports: [
    AggregationTypePage
  ]
})
export class ExtractionModule {

  constructor() {
    console.debug('[extraction] Creating module');
  }
}
