import {NgModule} from "@angular/core";
import {MatCommonModule} from "@angular/material/core";
import {MatFormFieldModule} from "@angular/material/form-field";
import {MatInputModule} from "@angular/material/input";
import {MatIconModule} from "@angular/material/icon";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import {SharedPipesModule} from "../../pipes/pipes.module";
import {ReactiveFormsModule} from "@angular/forms";
import {MatDate} from "./material.date";
import {MatDateTime} from "./material.datetime";
import {TextMaskModule} from "angular2-text-mask";
import {NgxMaterialTimepickerModule} from "ngx-material-timepicker";
import {MatDatepickerModule} from "@angular/material/datepicker";
import {MatButtonModule} from "@angular/material/button";

@NgModule({
  imports: [
    CommonModule,
    IonicModule,
    ReactiveFormsModule,
    SharedPipesModule,
    MatCommonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatDatepickerModule,
    TextMaskModule,
    NgxMaterialTimepickerModule,
    TranslateModule.forChild()
  ],
  exports: [
    MatIconModule,
    MatDate,
    MatDateTime
  ],
  declarations: [
    MatDate,
    MatDateTime
  ]
})
export class SharedMatDateTimeModule {
}

