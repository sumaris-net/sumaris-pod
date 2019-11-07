import {Inject, Injectable} from '@angular/core';
import {EventManager} from '@angular/platform-browser';
import {Observable} from 'rxjs';
import {DOCUMENT} from "@angular/common";
import {MatDialog} from '@angular/material';
import {HotkeysDialogComponent} from './dialog/hotkeys-dialog.component';
import {isNotNilOrBlank} from "../functions";

type Options = {
  element: any;
  description: string | undefined;
  keys: string;
  preventDefault: boolean;
};

@Injectable({
  providedIn: 'root'
})
export class Hotkeys {

  private _hotkeys = new Map();

  private _defaults: Partial<Options> = {
    element: this.document,
    preventDefault: true
  };

  constructor(private eventManager: EventManager,
              private dialog: MatDialog,
              @Inject(DOCUMENT) private document: Document) {
    console.debug("[hotkeys] Starting hotkeys service... Press Shift+? to get help modal.");

    this.addShortcut({ keys: 'shift.?' })
      .subscribe(() => this.openHelpModal());

  }

  addShortcut(options: Partial<Options>): Observable<UIEvent> {

    const merged = { ...this._defaults, ...options };
    const event = `keydown.${merged.keys}`;


    if (isNotNilOrBlank(merged.description)) {
      console.debug(`[hotkeys] Add shortcut {${options.keys}}: ${merged.description}`);
      this._hotkeys.set(merged.keys, merged.description);
    }
    else {
      console.debug(`[hotkeys] Add shortcut {${options.keys}}`);
    }

    return new Observable(observer => {
      const handler = (e: UIEvent) => {
        if (e instanceof KeyboardEvent && e.repeat) return; // skip when repeated
        if (merged.preventDefault) e.preventDefault();
        console.debug(`[hotkeys] Shortcut {${options.keys}} detected...`);
        observer.next(e);
      };

      const dispose = this.eventManager.addEventListener(merged.element, event, handler);

      return () => {
        dispose();
        this._hotkeys.delete(merged.keys);
      };
    });
  }

  openHelpModal() {
    this.dialog.open(HotkeysDialogComponent, {
      width: '500px',
      data: this._hotkeys
    });
  }

}
