import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild} from '@angular/core';
import {ProgressBarService, ProgressMode} from '../services/progress-bar.service';
import {Router} from "@angular/router";
import {IonBackButton, IonRouterOutlet, IonSearchbar} from "@ionic/angular";
import {isNotNil, toBoolean} from "../functions";
import {debounceTime, distinctUntilChanged, startWith} from "rxjs/operators";
import {Observable, Subscription} from "rxjs";
import {Hotkeys} from "../hotkeys/hotkeys.service";
import {element} from "protractor";

@Component({
  selector: 'app-modal-toolbar',
  templateUrl: 'modal-toolbar.html',
  styleUrls: ['./modal-toolbar.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ModalToolbarComponent implements OnInit, OnDestroy {

  private _subscription = new Subscription();

  @Input()
  modalName = 'modal-toolbar';

  @Input()
  title = '';

  @Input()
  color = 'primary';

  @Input()
  showSpinner = false;

  @Input()
  canValidate = true;

  @Output()
  cancel = new EventEmitter<UIEvent>();

  @Output()
  validate = new EventEmitter<UIEvent>();

  constructor(private hotkeys: Hotkeys) {}

  ngOnInit() {

    // Escape
    this._subscription.add(
      this.hotkeys.addShortcut({keys: 'escape', description: 'COMMON.BTN_CLOSE', elementName: this.modalName})
        .subscribe((event) => this.cancel.emit(event)));

  }

  ngOnDestroy(): void {
    this._subscription.unsubscribe();
  }
}
