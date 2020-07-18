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
import {ProgressBarService, ProgressMode} from '../services/progress-bar.service';
import {Router} from "@angular/router";
import {IonBackButton, IonRouterOutlet, IonSearchbar} from "@ionic/angular";
import {isNotNil, isNotNilOrBlank, toBoolean} from "../functions";
import {debounceTime, distinctUntilChanged, startWith} from "rxjs/operators";
import {Observable} from "rxjs";

@Component({
  selector: 'app-toolbar',
  templateUrl: 'toolbar.html',
  styleUrls: ['./toolbar.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ToolbarComponent implements OnInit {

  private _validateTapCount = 0;
  private _defaultBackHref: string;
  private _backHref: string;

  @Input()
  title = '';

  @Input()
  color = 'primary';

  @Input()
  class = '';

  @Input() set backHref(value: string) {
    if (value !== this._backHref) {
      this._backHref = value;
      this.canGoBack = this.canGoBack || isNotNil(value);
      this.cd.markForCheck();
    }
  }

  get backHref(): string {
    return this._backHref;
  }

  @Input() set defaultBackHref(value: string) {
    if (value !== this._defaultBackHref) {
      this._defaultBackHref = value;
      this.canGoBack = this.canGoBack || isNotNil(value);
      this.cd.markForCheck();
    }
  }

  get defaultBackHref(): string {
    return this._defaultBackHref;
  }

  @Input()
  hasValidate = false;

  @Input()
  hasClose = false;

  @Input()
  hasSearch: boolean;

  @Input()
  canGoBack: boolean;

  @Output()
  onValidate = new EventEmitter<Event>();

  @Output()
  onClose = new EventEmitter<Event>();

  @Output()
  onValidateAndClose = new EventEmitter<Event>();


  @Output()
  onBackClick = new EventEmitter<Event>();

  @Output()
  onSearch = new EventEmitter<CustomEvent>();

  $progressBarMode: Observable<ProgressMode>;

  showSearchBar = false;

  @ViewChild('searchbar', {static: true}) searchbar: IonSearchbar;

  constructor(
    private progressBarService: ProgressBarService,
    private router: Router,
    private routerOutlet: IonRouterOutlet,
    private cd: ChangeDetectorRef
  ) {

    // Listen progress bar service mode
    this.$progressBarMode = this.progressBarService.onProgressChanged
      .pipe(
        startWith<ProgressMode, ProgressMode>('none' as ProgressMode),
        debounceTime(100), // wait 100ms, to group changes
        distinctUntilChanged((mode1, mode2) => mode1 == mode2)
      );
  }

  ngOnInit() {
    this.hasValidate = toBoolean(this.hasValidate, this.onValidate.observers.length > 0);
    this.canGoBack = toBoolean(this.canGoBack, this.routerOutlet.canGoBack()
      || isNotNilOrBlank(this._backHref)
      || isNotNilOrBlank(this._defaultBackHref));
    this.hasSearch = toBoolean(this.hasSearch, this.onSearch.observers.length > 0);
  }

  async toggleSearchBar() {
    this.showSearchBar = !this.showSearchBar;
    if (this.showSearchBar && this.searchbar) {
      setTimeout(async () => {
        await this.searchbar.setFocus();
      }, 300);
    }
  }

  doBackClick(event: Event) {

    this.onBackClick.emit(event);

    // Stop propagation, if need (can be cancelled by onBackClick observers)
    if (event.defaultPrevented) return;

    // Execute the back action
    this.goBack();
  }

  async goBack(): Promise<void> {
    if (this._backHref) {
      await this.router.navigateByUrl(this._backHref);
    }
    else if (this.routerOutlet.canGoBack()) {
      await this.routerOutlet.pop();
    }
    else if (this._defaultBackHref) {
      await this.router.navigateByUrl(this._defaultBackHref);
    }
    else {
      console.error("[toolbar] Cannot go back. Missing attribute 'defaultBackHref' or 'backHref'");
    }
  }

  doValidateTap(event: Event & { tapCount?: number; }) {
    //FIXME console.log("TODO doValidateTap", event);
    if (!this.onValidateAndClose.observers.length) {
      this.onValidate.emit(event);
    }

    // Distinguish simple and double tap
    else {
      this._validateTapCount = event.tapCount;
      setTimeout(() => {
        // Event is obsolete (a new tap event occur)
        if (event.tapCount < this._validateTapCount) {
          // Ignore event
        }

        // If event still the last tap event: process it
        else {
          if (this._validateTapCount === 1) {
            this.onValidate.emit(event);
          }
          else if (this._validateTapCount >= 2) {
            this.onValidateAndClose.emit(event);
          }

          // Reset tab count
          this._validateTapCount = 0;
        }
      }, 500);
    }
  }
}
