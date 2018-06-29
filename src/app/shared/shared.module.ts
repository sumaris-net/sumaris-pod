import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from './material/material.module';
import { ReactiveFormsModule } from "@angular/forms";
import { TranslateModule } from "@ngx-translate/core";
import { IonicModule } from "@ionic/angular";

import { AutofocusDirective } from './directives/autofocus.directive';
import { DateFormatPipe } from './pipes/date-format.pipe';
import { HighlightPipe } from './pipes/highlight.pipe';
import { ToolbarComponent } from './toolbar/toolbar';
import { MatDateTime } from './material/material.datetime';
@NgModule({
    imports: [
        CommonModule,
        MaterialModule,
        ReactiveFormsModule,
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
        MaterialModule,
        ReactiveFormsModule,
        IonicModule,
        AutofocusDirective,
        ToolbarComponent,
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
