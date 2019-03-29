import { Component, Input, Output, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { ProgressBarService } from '../services/progress-bar.service';
import { BehaviorSubject } from "rxjs";
import { Router } from "@angular/router";
import { IonBackButton, IonRouterOutlet } from "@ionic/angular";
import {isNil, isNotNil} from "../functions";

@Component({
  selector: 'app-toolbar',
  templateUrl: 'toolbar.html',
  styleUrls: ['./toolbar.scss'],
})
export class ToolbarComponent implements OnInit {

  @Input()
  title: String = '';

  @Input()
  color: String = 'primary';

  @Input()
  class: String = '';

  @Input()
  hasValidate: Boolean = false;

  @Input()
  defaultBackHref: string | undefined;

  @Input()
  hasSearch: boolean = false;

  @Input()
  canGoBack: boolean;

  @Output()
  onValidate: EventEmitter<Event> = new EventEmitter<Event>();

  @Output()
  onBackClick: EventEmitter<Event> = new EventEmitter<Event>();

  progressBarMode: BehaviorSubject<string> = new BehaviorSubject('none');

  @ViewChild("backButton") backButton: IonBackButton;

  constructor(
    private progressBarService: ProgressBarService,
    protected router: Router,
    public routerOutlet: IonRouterOutlet
  ) {
  }

  ngOnInit() {
    this.hasValidate = this.hasValidate && this.onValidate.observers.length > 0;
    this.progressBarService.updateProgressBar$.subscribe((mode: string) => {
      if (mode != this.progressBarMode.getValue()) {
        setTimeout(() => {
          this.progressBarMode.next(mode);
        });
      }
    });
    if (isNil(this.canGoBack)) {
      this.canGoBack = this.routerOutlet.canGoBack() || isNotNil(this.defaultBackHref);
    }
  }

  enableSearchBar() {
    console.warn('[app-toolbar] TODO: implement enableSearchBar()');
  }

  doBackClick(event: Event) {

    this.onBackClick.emit(event);

    // Stop propagation, if need
    if (event.defaultPrevented) return;

    this.goBack();
  }

  goBack() {
    if (this.routerOutlet.canGoBack()) {
      this.routerOutlet.pop();
    }
    else {
      this.router.navigateByUrl(this.defaultBackHref);
    }
  }

}
