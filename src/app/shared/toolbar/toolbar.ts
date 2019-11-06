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
import {isNotNil, toBoolean} from "../functions";
import {debounceTime, distinctUntilChanged, startWith} from "rxjs/operators";
import {Observable} from "rxjs";
import {ConnectionType} from "../../core/services/network.service";

@Component({
  selector: 'app-toolbar',
  templateUrl: 'toolbar.html',
  styleUrls: ['./toolbar.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ToolbarComponent implements OnInit {

  @Input()
  title = '';

  @Input()
  color = 'primary';

  @Input()
  class = '';

  @Input()
  defaultBackHref: string;

  @Input()
  hasValidate = false;

  @Input()
  hasSearch: boolean;

  @Input()
  canGoBack: boolean;

  @Output()
  onValidate = new EventEmitter<Event>();

  @Output()
  onBackClick = new EventEmitter<Event>();

  @Output()
  onSearch = new EventEmitter<CustomEvent>();

  $progressBarMode: Observable<ProgressMode>;

  showSearchBar = false;

  @ViewChild("backButton", { static: false }) backButton: IonBackButton;

  @ViewChild('searchbar', {static: true}) searchbar: IonSearchbar;

  constructor(
    private progressBarService: ProgressBarService,
    private router: Router,
    private routerOutlet: IonRouterOutlet
  ) {

    // Listen progress bar service mode
    this.$progressBarMode = this.progressBarService.onProgressChanged
      .pipe(
        startWith('none' as ProgressMode),
        debounceTime(100), // wait 100ms, to group changes
        distinctUntilChanged((mode1, mode2) => mode1 == mode2)
      );
  }

  ngOnInit() {
    this.hasValidate = toBoolean(this.hasValidate, this.onValidate.observers.length > 0);
    this.canGoBack = toBoolean(this.canGoBack, this.routerOutlet.canGoBack() || isNotNil(this.defaultBackHref));
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
    if (this.routerOutlet.canGoBack()) {
      await this.routerOutlet.pop();
    }
    else {
      await this.router.navigateByUrl(this.defaultBackHref);
    }
  }

}
