import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import {ProgressBarService} from '../services/progress-bar.service';
import {Router} from "@angular/router";
import {IonBackButton, IonRouterOutlet} from "@ionic/angular";
import {isNil, isNotNil} from "../functions";
import {debounceTime, distinctUntilChanged} from "rxjs/operators";

@Component({
  selector: 'app-toolbar',
  templateUrl: 'toolbar.html',
  styleUrls: ['./toolbar.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
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

  progressBarMode: 'none'|'query' = 'none';

  @ViewChild("backButton") backButton: IonBackButton;

  constructor(
    private progressBarService: ProgressBarService,
    protected router: Router,
    public routerOutlet: IonRouterOutlet,
    private cd: ChangeDetectorRef
  ) {
  }

  ngOnInit() {
    this.hasValidate = this.hasValidate && this.onValidate.observers.length > 0;
    this.progressBarService.onProgressChanged
      .pipe(
        debounceTime(100),
        distinctUntilChanged()
      )
      .subscribe((mode) => {
        this.progressBarMode = mode;
        this.cd.detectChanges();
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
