import {ModuleWithProviders, NgModule} from "@angular/core";
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
  EntitiesService,
  EntityService,
  EntityServiceLoadOptions,
  LoadResult,
  SuggestService
} from "./services/entity-service.class";
import {
  changeCaseToUnderscore,
  fromDateISOString,
  isNil,
  isNilOrBlank,
  isNotEmptyArray,
  isNotNil,
  isNotNilOrBlank,
  joinPropertiesPath,
  nullIfUndefined,
  propertyComparator,
  sleep,
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
import {CloseScrollStrategy, FullscreenOverlayContainer, Overlay, OverlayContainer} from '@angular/cdk/overlay';
import {Hotkeys, SharedHotkeysModule} from "./hotkeys/shared-hotkeys.module";
import {FileService} from "./file/file.service";
import {ModalToolbarComponent} from "./toolbar/modal-toolbar";
import {DragDropModule} from "@angular/cdk/drag-drop";
import {MAT_AUTOCOMPLETE_DEFAULT_OPTIONS, MAT_AUTOCOMPLETE_SCROLL_STRATEGY} from "@angular/material/autocomplete";
import {MAT_SELECT_SCROLL_STRATEGY} from "@angular/material/select";
import {SharedDirectivesModule} from "./directives/directives.module";
import {SharedPipesModule} from "./pipes/pipes.module";
import {AppLoadingSpinner} from "./form/loading-spinner";
import {QuicklinkModule} from "ngx-quicklink";
import {DateDiffDurationPipe} from "./pipes/date-diff-duration.pipe";
import {LatitudeFormatPipe, LatLongFormatPipe, LongitudeFormatPipe} from "./pipes/latlong-format.pipe";
import {HighlightPipe} from "./pipes/highlight.pipe";
import {NumberFormatPipe} from "./pipes/number-format.pipe";


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
    IonicModule,
    ReactiveFormsModule,
    TranslateModule,
    ColorPickerModule,
    TextMaskModule,
    DragDropModule,
    QuicklinkModule, // See https://web.dev/route-preloading-in-angular/

    // Sub modules
    SharedMaterialModule,
    SharedDirectivesModule,
    SharedPipesModule,
    SharedHotkeysModule
  ],
  declarations: [
    ToolbarComponent,
    ModalToolbarComponent,
    AppFormField,
    AppLoadingSpinner
  ],
  exports: [
    CommonModule,
    IonicModule,
    TranslateModule,
    ReactiveFormsModule,
    ColorPickerModule,
    TextMaskModule,
    DragDropModule,
    QuicklinkModule,

    // Sub-modules
    SharedMaterialModule,
    SharedDirectivesModule,
    SharedPipesModule,
    SharedHotkeysModule,

    // Components
    ToolbarComponent,
    ModalToolbarComponent,
    AppFormField,
    AppLoadingSpinner
  ]
})
export class SharedModule {

  static forRoot(): ModuleWithProviders<SharedModule> {
    console.debug('[shared] Creating module (root)');

    return {
      ngModule: SharedModule,
      providers: [
        ProgressBarService,
        AudioProvider,
        FileService,

        // Export Pipes as providers
        DateFormatPipe,
        DateFromNowPipe,
        DateDiffDurationPipe,
        LatLongFormatPipe,
        LatitudeFormatPipe,
        LongitudeFormatPipe,
        HighlightPipe,
        NumberFormatPipe,

        {provide: OverlayContainer, useClass: FullscreenOverlayContainer},
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
    };
  }
}
