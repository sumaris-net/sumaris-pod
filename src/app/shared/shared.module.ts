import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {SharedMaterialModule} from "./material/material.module";
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
  EntityService,
  EntityServiceLoadOptions,
  LoadResult,
  SuggestService,
  EntitiesService
} from "./services/entity-service.class";
import {
  changeCaseToUnderscore,
  sleep,
  fromDateISOString,
  isNil,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
  joinPropertiesPath,
  nullIfUndefined,
  propertyComparator,
  sort,
  startsWithUpperCase,
  toBoolean,
  toDateISOString,
  toFloat,
  toInt
} from "./functions";
import {filterNumberInput, InputElement, selectInputContent} from "./inputs";
import {
  fadeInAnimation,
  fadeInOutAnimation,
  slideInOutAnimation,
  slideUpDownAnimation
} from "./material/material.animations";
import {Color, ColorScale} from "./graph/graph-colors";
import {ColorPickerModule} from 'ngx-color-picker';
import {AppFormField} from "./form/field.component";
import {AudioProvider} from "./audio/audio";
import {CloseScrollStrategy, Overlay} from '@angular/cdk/overlay';
import {Hotkeys, SharedHotkeysModule} from "./hotkeys/shared-hotkeys.module";
import {FileService} from "./file/file.service";
import {BrowserModule, HAMMER_GESTURE_CONFIG, HammerModule} from "@angular/platform-browser";
import {AppGestureConfig} from "./gesture/gesture-config";
import {ModalToolbarComponent} from "./toolbar/modal-toolbar";
import {DragDropModule} from "@angular/cdk/drag-drop";
import {MAT_AUTOCOMPLETE_DEFAULT_OPTIONS, MAT_AUTOCOMPLETE_SCROLL_STRATEGY} from "@angular/material/autocomplete";
import {MAT_SELECT_SCROLL_STRATEGY} from "@angular/material/select";
import {SharedDirectivesModule} from "./directives/directives.module";
import {SharedPipesModule} from "./pipes/pipes.module";
import {AppLoadingSpinner} from "./form/loading-spinner";
import {SharedGestureModule} from "./gesture/gesture.module";
import {QuicklinkModule} from "ngx-quicklink";


export function scrollFactory(overlay: Overlay): () => CloseScrollStrategy {
  return () => overlay.scrollStrategies.close();
}

export {
  SuggestService, EntitiesService, LoadResult,
  EntityService, EntityServiceLoadOptions,
  isNil, isNilOrBlank, isNotNil, isNotNilOrBlank, isNotEmptyArray, nullIfUndefined, sleep,
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
    //HammerModule,
    IonicModule,
    ReactiveFormsModule,
    SharedMaterialModule,
    SharedDirectivesModule,
    SharedPipesModule,
    TranslateModule.forChild(),
    TextMaskModule,
    ColorPickerModule,
    SharedHotkeysModule,
    DragDropModule,
    QuicklinkModule // See https://web.dev/route-preloading-in-angular/
  ],
  declarations: [
    ToolbarComponent,
    ModalToolbarComponent,
    AppFormField,
    AppLoadingSpinner
  ],
  exports: [
    ReactiveFormsModule,
    IonicModule,
    SharedGestureModule,
    SharedMaterialModule,
    SharedDirectivesModule,
    SharedPipesModule,
    SharedHotkeysModule,
    ToolbarComponent,
    ModalToolbarComponent,
    TranslateModule,
    ColorPickerModule,
    AppFormField,
    AppLoadingSpinner,
    QuicklinkModule
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
