import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {IonicModule} from "@ionic/angular";
import {MatNumpadComponent} from "./material.numpad";
import {MatButtonModule} from "@angular/material/button";
import {MatIconModule} from "@angular/material/icon";

@NgModule({
  imports: [
    CommonModule,
    IonicModule,
    MatButtonModule,
    MatIconModule
  ],
  exports: [
    MatNumpadComponent
  ],
  declarations: [
    MatNumpadComponent
  ]
})
export class SharedMatNumpadModule {
}

