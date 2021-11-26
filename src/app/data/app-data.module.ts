import { NgModule } from '@angular/core';
import { AppReferentialModule } from '../referential/referential.module';
import { EntityQualityFormComponent } from './quality/entity-quality-form.component';
import { CoreModule } from '@sumaris-net/ngx-components';
import { QualityFlagToColorPipe } from './services/pipes/quality-flag-to-color.pipe';
import { StrategySummaryCardComponent } from './strategy/strategy-summary-card.component';
import { DataCommentModule } from './comment/comment.module';
import { ImageModule } from '@app/image/image.module';

@NgModule({
  imports: [
    CoreModule,
    AppReferentialModule,
    ImageModule,

    // Sub modules
    DataCommentModule
  ],
  declarations: [
    // Pipes
    QualityFlagToColorPipe,

    // Components
    EntityQualityFormComponent,
    StrategySummaryCardComponent

  ],
  exports: [
    // Sub modules
    DataCommentModule,
    ImageModule,

    // Pipes
    QualityFlagToColorPipe,

    // Components
    EntityQualityFormComponent,
    StrategySummaryCardComponent
  ]
})
export class AppDataModule {

}
