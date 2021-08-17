import {Component, Input, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {PopoverController} from '@ionic/angular';
import {Subscription} from 'rxjs';
import {TranslateService} from '@ngx-translate/core';
import {CommentForm} from './comment.form';
import {AppFormUtils} from '@sumaris-net/ngx-components';

@Component({
  selector: 'app-comment-modal',
  templateUrl: './comment.modal.html'
})
export class CommentModal implements OnInit, OnDestroy {

  loading = false;
  subscription = new Subscription();

  @ViewChild('commentForm', {static: true}) commentForm: CommentForm;

  @Input() comment: string;
  @Input() title: string;
  @Input() editing: boolean;

  get disabled() {
    return this.commentForm.disabled;
  }

  get enabled() {
    return this.commentForm.enabled;
  }

  get valid() {
    return this.commentForm && this.commentForm.valid || false;
  }


  constructor(
    protected viewCtrl: PopoverController,
    protected translate: TranslateService
  ) {

  }

  ngOnInit(): void {
    this.enable();
    this.commentForm.setValue({comment: this.comment});
  }

  async onSave(event: any): Promise<any> {

    // Avoid multiple call
    if (this.disabled) return;

    await AppFormUtils.waitWhilePending(this.commentForm);

    if (this.commentForm.invalid) {
      AppFormUtils.logFormErrors(this.commentForm.form);
      return;
    }

    this.loading = true;

    try {
      const value = this.commentForm.value;
      this.disable();
      await this.viewCtrl.dismiss(value);
      this.commentForm.error = null;
    } catch (err) {
      this.commentForm.error = err && err.message || err;
      this.enable();
      this.loading = false;
    }
  }

  disable() {
    this.commentForm.disable();
  }

  enable() {
    if (this.editing)
      this.commentForm.enable();
  }

  cancel() {
    this.viewCtrl.dismiss();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

}
