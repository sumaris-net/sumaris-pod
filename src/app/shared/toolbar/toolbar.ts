import { Component, Input, Output, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { ProgressBarService } from '../../core/services/progress-bar.service';
import { BehaviorSubject } from "rxjs";
import { Router } from "@angular/router";
import { IonBackButton, IonRouterOutlet } from "@ionic/angular";

import { Location } from '@angular/common';

@Component({
  selector: 'app-toolbar',
  templateUrl: 'toolbar.html',
  styleUrls: ['./toolbar.scss'],
})
export class ToolbarComponent implements OnInit {

  @Input()
  title: string = '';

  @Input()
  color: string = 'primary';

  @Input()
  class: string = '';

  @Input()
  hasValidate: boolean = false;

  @Input()
  defaultBackHref: string | undefined;

  @Input()
  hasSearch: boolean = false;

  @Output()
  onValidate: EventEmitter<any> = new EventEmitter<any>();

  @Output()
  onBackClick: EventEmitter<any> = new EventEmitter<any>();

  progressBarMode: BehaviorSubject<string> = new BehaviorSubject('none');

  @ViewChild("backButton") backButton: IonBackButton;

  constructor(
    private progressBarService: ProgressBarService,
    protected router: Router,
    public routerOutlet: IonRouterOutlet
  ) {
    this.routerOutlet.activateEvents.subscribe(event => console.log(event));
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
  }

  enableSearchBar() {
    console.warn('[app-toolbar] TODO: implement enableSearchBar()');
  }

  doBackClick(event: MouseEvent) {

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
