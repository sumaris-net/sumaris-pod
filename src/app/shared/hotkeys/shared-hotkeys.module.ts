import {NgModule} from "@angular/core";
import {CommonModule} from "@angular/common";
import {TranslateModule} from "@ngx-translate/core";
import {Hotkeys} from "./hotkeys.service";
import {HotkeysDialogComponent} from "./dialog/hotkeys-dialog.component";
import {MatDialogModule} from "@angular/material/dialog";
import {MatCommonModule} from "@angular/material/core";

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
export class SharedHotkeysModule {
}
