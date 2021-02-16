import {NgModule} from "@angular/core";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {CommentModal} from "./comment.modal";
import {CommentForm} from "./comment.form";
import {CoreModule} from "../../core/core.module";


@NgModule({
  imports: [
    CommonModule,
    CoreModule,
    TranslateModule.forChild()
  ],
  declarations: [
    CommentModal,
    CommentForm
  ],
  exports: [
    CommentModal,
    CommentForm
  ]
})
export class DataCommentModule { }
