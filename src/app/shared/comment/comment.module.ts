import {NgModule} from "@angular/core";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {CommentModal} from "./comment.modal";
import {CommentForm} from "./comment.form";


@NgModule({
  imports: [
    CommonModule,
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
export class SharedCommentModule { }
