import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from './material/material.module';
import { AutofocusDirective } from './directives/autofocus.directive';
import { DateFormatPipe } from './pipes/date-format.pipe';
import { HighlightPipe } from './pipes/highlight.pipe';
import { ToolbarComponent } from './toolbar/toolbar';
import { IonicModule } from "ionic-angular";
import { MatDateTime } from './material/material.datetime';

import { TranslateModule } from "@ngx-translate/core";

@NgModule({
    imports: [
        CommonModule,
        MaterialModule,
        IonicModule,
        TranslateModule.forChild()
    ],
    declarations: [
        AutofocusDirective,
        ToolbarComponent,
        DateFormatPipe,
        HighlightPipe,
        MatDateTime
    ],
    exports: [
        AutofocusDirective,
        MaterialModule,
        ToolbarComponent,
        IonicModule,
        DateFormatPipe,
        HighlightPipe,
        MatDateTime,
        TranslateModule
    ],
    providers: [
        DateFormatPipe,
        HighlightPipe
    ]
})
export class SharedModule { }
