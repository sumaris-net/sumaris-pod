import {NgModule} from "@angular/core";
import {ReferentialModule} from "../referential/referential.module";
import {EntityQualityFormComponent} from "./quality/entity-quality-form.component";
import {CoreModule} from "../core/core.module";
import {QualityFlagToColorPipe} from "./services/pipes/quality-flag-to-color.pipe";
import {StrategySummaryCardComponent} from "./strategy/strategy-summary-card.component";

@NgModule({
  imports: [
    CoreModule,
    ReferentialModule
  ],
  declarations: [
    // Pipes
    QualityFlagToColorPipe,

    // Components
    EntityQualityFormComponent,
    StrategySummaryCardComponent
  ],
  exports: [

    // Pipes
    QualityFlagToColorPipe,

    // Components
    EntityQualityFormComponent,
    StrategySummaryCardComponent
  ]
})
export class DataModule {

}
