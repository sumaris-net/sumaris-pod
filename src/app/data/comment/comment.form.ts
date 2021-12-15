import { AfterViewChecked, ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, Injector, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {FormBuilder, Validators} from "@angular/forms";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {AppForm}  from "@sumaris-net/ngx-components";

@Component({
  selector: 'app-comment-form',
  templateUrl: './comment.form.html',
  styleUrls: ['./comment.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CommentForm extends AppForm<{comment: string}> implements OnInit, AfterViewChecked, OnDestroy {

  @Input() showError = true;

  @ViewChild('textarea') textArea: ElementRef;

  private focused = false;

  constructor(
    injector: Injector,
    protected settings: LocalSettingsService,
    protected cd: ChangeDetectorRef,
    protected formBuilder: FormBuilder,
  ) {
    super(injector, formBuilder.group({comment: [null, Validators.maxLength(2000)]}));
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  ngAfterViewChecked(): void {
    if (this.textArea && !this.focused) {
      this.focused = true;
      setTimeout(() => this.textArea.nativeElement.focus(), 500);
    }
  }
}
