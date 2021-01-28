import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
// import fade in animation
import {merge, Subscription} from "rxjs";
import {Router} from "@angular/router";
import {environment} from "../../../environments/environment";
import {AppRootDataEditor} from "../form/root-data-editor.class";
import {isNil} from "../../shared/functions";
import {fadeInAnimation} from "../../shared/material/material.animations";
import {Strategy} from "../../referential/services/model/strategy.model";

@Component({
  selector: 'app-strategy-summary-card',
  templateUrl: './strategy-summary-card.component.html',
  styleUrls: ['./strategy-summary-card.component.scss'],
  animations: [fadeInAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategySummaryCardComponent<T extends Strategy<T> = Strategy<any>> implements OnInit, OnDestroy {

  private _debug = false;
  private _subscription = new Subscription();

  data: T;
  loading = true;

  @Input() title: string;

  @Input("value")
  set value(value: T) {
    this.data = value;
    this.updateView();
  }
  get value(): T {
    return this.data;
  }

  @Input() editor: AppRootDataEditor<any, any>;

  constructor (
    protected router: Router,
    protected cd: ChangeDetectorRef
  ) {

    this._debug = !environment.production;
  }

  ngOnInit(): void {

    // Check editor exists
    if (!this.editor) throw new Error("Missing mandatory 'editor' input!");

    // Subscribe to refresh events
    this._subscription
        .add(
            merge(
                this.editor.onUpdateView
            )
            .subscribe(() => this.updateView(this.editor.data))
        );
  }

  ngOnDestroy(): void {
    this._subscription.unsubscribe();
  }

  /* -- protected method -- */

  protected updateView(data?: T) {

    this.data = data || this.data || (this.editor && this.editor.strategy as T);

    this.loading = isNil(data) || isNil(data.id);
    this.markForCheck();
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
