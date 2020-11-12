import {NgModule} from "@angular/core";
import {CommonModule, KeyValuePipe} from "@angular/common";
import {TranslateModule} from "@ngx-translate/core";
import {IonicModule} from "@ionic/angular";
import {DateFormatPipe} from "./date-format.pipe";
import {DateDiffDurationPipe} from "./date-diff-duration.pipe";
import {DateFromNowPipe} from "./date-from-now.pipe";
import {LatitudeFormatPipe, LatLongFormatPipe, LongitudeFormatPipe} from "./latlong-format.pipe";
import {NumberFormatPipe} from "./number-format.pipe";
import {HighlightPipe} from "./highlight.pipe";
import {FileSizePipe} from "./file-size.pipe";
import {DurationPipe} from "./duration.pipe";
import {MathAbsPipe} from "./math-abs.pipe";
import {ArrayLengthPipe, EmptyArrayPipe, NotEmptyArrayPipe} from "./arrays.pipe";
import {MapGetPipe} from "./maps.pipe";


@NgModule({
  imports: [
    CommonModule,
    IonicModule,
    TranslateModule
  ],
  declarations: [
    DateFormatPipe,
    DateDiffDurationPipe,
    DurationPipe,
    DateFromNowPipe,
    LatLongFormatPipe,
    LatitudeFormatPipe,
    LongitudeFormatPipe,
    HighlightPipe,
    NumberFormatPipe,
    FileSizePipe,
    MathAbsPipe,
    NotEmptyArrayPipe,
    EmptyArrayPipe,
    ArrayLengthPipe,
    MapGetPipe
  ],
  exports: [
    DateFormatPipe,
    DateFromNowPipe,
    DateDiffDurationPipe,
    DurationPipe,
    LatLongFormatPipe,
    LatitudeFormatPipe,
    LongitudeFormatPipe,
    HighlightPipe,
    NumberFormatPipe,
    FileSizePipe,
    MathAbsPipe,
    NotEmptyArrayPipe,
    EmptyArrayPipe,
    ArrayLengthPipe,
    MapGetPipe
  ]
})
export class SharedPipesModule {

}
