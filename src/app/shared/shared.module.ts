import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {MaterialModule} from "./material/material.module";
import {ReactiveFormsModule} from "@angular/forms";
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import {IonicModule} from "@ionic/angular";
import {DateFormatPipe} from "./pipes/date-format.pipe";
import {DateFromNowPipe} from "./pipes/date-from-now.pipe";
import {ToolbarComponent} from "./toolbar/toolbar";
import {TextMaskModule} from "angular2-text-mask";
import {MatPaginatorIntl} from "@angular/material/paginator";
import {MatPaginatorI18n} from "./material/paginator/material.paginator-i18n";
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
  delay,
  filterNumberInput,
  fromDateISOString,
  isNil,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
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
import {
  fadeInAnimation,
  fadeInOutAnimation,
  slideInOutAnimation,
  slideUpDownAnimation
} from "./material/material.animations";
import {InputElement} from "./material/focusable";
import {Color, ColorScale} from "./graph/graph-colors";
import {ColorPickerModule} from 'ngx-color-picker';
import {AppFormField} from "./form/field.component";
import {NumpadComponent} from "./numpad/numpad";
import {AudioProvider} from "./audio/audio";
import {CloseScrollStrategy, Overlay} from '@angular/cdk/overlay';
import {Hotkeys, SharedHotkeysModule} from "./hotkeys/shared-hotkeys.module";
import {FileService} from "./file/file.service";
import {HAMMER_GESTURE_CONFIG} from "@angular/platform-browser";
import {AppGestureConfig} from "./gesture/gesture-config";
import {ModalToolbarComponent} from "./toolbar/modal-toolbar";
import {DragDropModule} from "@angular/cdk/drag-drop";
import {MAT_AUTOCOMPLETE_DEFAULT_OPTIONS, MAT_AUTOCOMPLETE_SCROLL_STRATEGY} from "@angular/material/autocomplete";
import {MAT_SELECT_SCROLL_STRATEGY} from "@angular/material/select";
import {SharedDirectivesModule} from "./directives/directives.module";
import {SharedPipesModule} from "./pipes/pipes.module";


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
    IonicModule,
    ReactiveFormsModule,
    MaterialModule,
    SharedDirectivesModule,
    SharedPipesModule,
    TranslateModule.forChild(),
    TextMaskModule,
    ColorPickerModule,
    SharedHotkeysModule,
    DragDropModule
  ],
  declarations: [
    ToolbarComponent,
    ModalToolbarComponent,
    NumpadComponent,
    AppFormField
  ],
  exports: [
    ReactiveFormsModule,
    IonicModule,
    MaterialModule,
    SharedDirectivesModule,
    SharedPipesModule,
    SharedHotkeysModule,
    ToolbarComponent,
    ModalToolbarComponent,
    NumpadComponent,
    TranslateModule,
    ColorPickerModule,
    AppFormField
  ],
  providers: [
    ProgressBarService,
    AudioProvider,
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
    {provide: HAMMER_GESTURE_CONFIG, useClass: AppGestureConfig},
    // FIXME: try to force a custom overlay for autocomplete, because of there is a bug when using inside an ionic modal
    //{ provide: Overlay, useClass: Overlay},
    { provide: MAT_AUTOCOMPLETE_SCROLL_STRATEGY, useFactory: scrollFactory, deps: [Overlay] },
    { provide: MAT_SELECT_SCROLL_STRATEGY, useFactory: scrollFactory, deps: [Overlay] },
    { provide: MAT_AUTOCOMPLETE_DEFAULT_OPTIONS, useValue: {
        autoActiveFirstOption: true
      }
    }
  ]
})
export class SharedModule {
}
