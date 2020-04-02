import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {MaterialModule} from "./material/material.module";
import {ReactiveFormsModule} from "@angular/forms";
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import {IonicModule} from "@ionic/angular";
import {AutofocusDirective} from "./directives/autofocus.directive";
import {DateFormatPipe} from "./pipes/date-format.pipe";
import {DateDiffDurationPipe} from "./pipes/date-diff-duration.pipe";
import {DateFromNowPipe} from "./pipes/date-from-now.pipe";
import {LatLongFormatPipe} from "./pipes/latlong-format.pipe";
import {NumberFormatPipe} from "./pipes/number-format.pipe";
import {HighlightPipe} from "./pipes/highlight.pipe";
import {ToolbarComponent} from "./toolbar/toolbar";
import {MatDate} from "./material/material.date";
import {MatDateTime} from "./material/material.datetime";
import {MatLatLong} from "./material/material.latlong";
import {MatBooleanField} from "./material/material.boolean";
import {MatAutocompleteField} from "./material/material.autocomplete";
import {TextMaskModule} from "angular2-text-mask";
import {MAT_AUTOCOMPLETE_SCROLL_STRATEGY, MatPaginatorIntl} from "@angular/material";
import {MatPaginatorI18n} from "./material/material.paginator-i18n";
import {ProgressBarService} from "./services/progress-bar.service";
import {HTTP_INTERCEPTORS} from "@angular/common/http";
import {ProgressInterceptor} from "./interceptors/progess.interceptor";
import {
  DataService,
  EditorDataService,
  EditorDataServiceLoadOptions,
  LoadResult,
  SuggestionDataService,
  TableDataService
} from "./services/data-service.class";
import {
  changeCaseToUnderscore,
  filterNumberInput,
  fromDateISOString,
  isNil,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
  delay,
  joinPropertiesPath,
  nullIfUndefined,
  propertyComparator,
  selectInputContent,
  sort,
  startsWithUpperCase,
  toBoolean,
  toDateISOString,
  toFloat,
  toInt
} from "./functions";
import {fadeInAnimation, fadeInOutAnimation, slideInOutAnimation, slideUpDownAnimation} from "./material/material.animations";
import {InputElement} from "./material/focusable";
import {Color, ColorScale} from "./graph/graph-colors";
import {ColorPickerModule} from 'ngx-color-picker';
import {AppFormField} from "./form/field.component";
import {NumpadComponent} from "./numpad/numpad";
import {AudioProvider} from "./audio/audio";
import {CloseScrollStrategy, Overlay} from '@angular/cdk/overlay';
import {Hotkeys, HotkeysModule} from "./hotkeys/hotkeys.module";
//import {FileTransfer} from "@ionic-native/file-transfer/ngx";
//import {FileChooser} from "@ionic-native/file-chooser/ngx";
//import {File} from "@ionic-native/file/ngx";
import {FileService} from "./file/file.service";
import {HAMMER_GESTURE_CONFIG} from "@angular/platform-browser";
import {AppGestureConfig} from "./gesture/gesture-config";
import {FileSizePipe} from "./pipes/file-size.pipe";
import {MatDuration} from "./material/material.duration";
import {DurationPipe} from "./pipes/duration.pipe";
import {DurationPickerModule} from "ngx-duration-picker";


export function scrollFactory(overlay: Overlay): () => CloseScrollStrategy {
  return () => overlay.scrollStrategies.close();
}

export {
  DataService, SuggestionDataService, TableDataService, LoadResult,
  EditorDataService, EditorDataServiceLoadOptions,
  isNil, isNilOrBlank, isNotNil, isNotNilOrBlank, isNotEmptyArray, nullIfUndefined, delay,
  toBoolean, toFloat, toInt,
  toDateISOString, fromDateISOString, filterNumberInput,
  startsWithUpperCase,
  propertyComparator, joinPropertiesPath, sort, selectInputContent,
  fadeInAnimation, fadeInOutAnimation, slideInOutAnimation, slideUpDownAnimation, changeCaseToUnderscore,
  DateFormatPipe, DateFromNowPipe,
  ToolbarComponent,
  Color, ColorScale, InputElement,
  Hotkeys,
};

@NgModule({
  imports: [
    CommonModule,
    MaterialModule,
    ReactiveFormsModule,
    TextMaskModule,
    IonicModule,
    TranslateModule.forChild(),
    ColorPickerModule,
    HotkeysModule
  ],
  declarations: [
    AutofocusDirective,
    ToolbarComponent,
    NumpadComponent,
    DateFormatPipe,
    DateDiffDurationPipe,
    DurationPipe,
    DateFromNowPipe,
    LatLongFormatPipe,
    HighlightPipe,
    NumberFormatPipe,
    FileSizePipe,
    MatDate,
    MatDateTime,
    MatDuration,
    MatLatLong,
    MatBooleanField,
    MatAutocompleteField,
    AppFormField
  ],
  exports: [
    MaterialModule,
    ReactiveFormsModule,
    IonicModule,
    HotkeysModule,
    AutofocusDirective,
    ToolbarComponent,
    NumpadComponent,
    DateFormatPipe,
    DateFromNowPipe,
    DateDiffDurationPipe,
    DurationPipe,
    LatLongFormatPipe,
    HighlightPipe,
    NumberFormatPipe,
    FileSizePipe,
    TextMaskModule,
    TranslateModule,
    MatDate,
    MatDateTime,
    MatDuration,
    MatLatLong,
    MatBooleanField,
    MatAutocompleteField,
    ColorPickerModule,
    AppFormField
  ],
  providers: [
    DateFormatPipe,
    DateFromNowPipe,
    DateDiffDurationPipe,
    LatLongFormatPipe,
    HighlightPipe,
    NumberFormatPipe,
    ProgressBarService,
    AudioProvider,
    //File,
    //FileTransfer,
    //FileChooser,
    FileService,
    {provide: HTTP_INTERCEPTORS, useClass: ProgressInterceptor, multi: true, deps: [ProgressBarService]},
    {
      provide: MatPaginatorIntl,
      useFactory: (translate) => {
        const service = new MatPaginatorI18n();
        service.injectTranslateService(translate);
        return service;
      },
      deps: [TranslateService]
    },
    // Configure hammer gesture
    {
      provide: HAMMER_GESTURE_CONFIG,
      useClass: AppGestureConfig
    },
    // FIXME: try to force a custom overlay for autocomplete, because of there is a bug when using inside an ionic modal
    //{ provide: Overlay, useClass: Overlay},
    { provide: MAT_AUTOCOMPLETE_SCROLL_STRATEGY, useFactory: scrollFactory, deps: [Overlay] },
  ]
})
export class SharedModule {
}
