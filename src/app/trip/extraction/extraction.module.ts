import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {CoreModule} from "../../core/core.module";
import {ExtractionRoutingModule} from "./extraction-routing.module";
import {ExtractionDataPage} from "./extraction-data.page";
import {AggregationTypePage} from "./aggregation-type.page";
import {ExtractionMapPage} from "./extraction-map.page";
import {ExtractionCriteriaValidatorService} from "../services/validator/extraction-criterion.validator";
import {AggregationTypeSelectModal} from "./aggregation-type-select.modal";
import {ExtractionCriteriaForm} from "./extraction-criteria.form";
import {AggregationTypeForm} from "./aggregation-type.form";
import {ReferentialModule} from "../../referential/referential.module";
import {LeafletModule} from "@asymmetrik/ngx-leaflet";

@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    ReferentialModule,
    ExtractionRoutingModule,
    LeafletModule
  ],
  declarations: [
    AggregationTypePage,
    AggregationTypeForm,
    AggregationTypeSelectModal,
    ExtractionDataPage,
    ExtractionMapPage,
    ExtractionCriteriaForm
  ],
  providers: [
    ExtractionCriteriaValidatorService
  ],
  exports: [
    AggregationTypePage
  ]
})
export class ExtractionModule { }
