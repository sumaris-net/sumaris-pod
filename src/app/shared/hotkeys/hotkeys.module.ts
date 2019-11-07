import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {MaterialModule} from "../material/material.module";
import {TranslateModule} from "@ngx-translate/core";
import {Hotkeys} from "./hotkeys.service";
import {HotkeysDialogComponent} from "./dialog/hotkeys-dialog.component";
import {MatCommonModule, MatDialogModule} from "@angular/material";

export {
  Hotkeys
};

@NgModule({
  imports: [
    CommonModule,
    MatCommonModule,
    MatDialogModule,
    TranslateModule.forChild()
  ],
  exports: [
    HotkeysDialogComponent
  ],
  entryComponents: [
    HotkeysDialogComponent
  ],
  declarations: [
    HotkeysDialogComponent
  ],
  providers: [
    Hotkeys
  ]
})
export class HotkeysModule {
}
