import {ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit} from "@angular/core";


export declare interface NumpadKeyDef {
  key: string | number;
  label?: string;
  matIcon?: string;
  icon?: string;
}

@Component({
  selector: 'app-numpad',
  template: `
    <ion-grid>
      Grid
    <ion-row *ngFor="let row of keymap">
      <ion-col size="{{12/columnCount}}" *ngFor="let keyDef of row">
        <ion-button expand="block"
                    (click)="onKeyPress($event, keyDef)">
          <ion-label *ngIf="keyDef.label">{{keyDef.label}}</ion-label>
          <mat-icon *ngIf="keyDef.matIcon">{{keyDef.matIcon}}</mat-icon>
          <ion-icon *ngIf="keyDef.icon" [name]="keyDef.icon"></ion-icon>
        </ion-button>
      </ion-col>
    </ion-row>
  </ion-grid>`,
  styleUrls: ['./material.numpad.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatNumpadComponent implements OnInit, OnDestroy {

  private debug = true;

  columnCount = 4;

  @Input()
  keymap: Array<NumpadKeyDef[]> = [
    [{key: 1}, {key: 2}, {key: 3}, {matIcon: 'backspace', key: 'back'}],
    [{key: 4}, {key: 5}, {key: 6}],
    //[[7, 7], [8, 8], [9, 9]],
    //[['.', '.'], [0, 0], ['']]
  ];

  constructor() {
    if (this.debug) console.debug('[numpad] Creating component')
  }

  ngOnInit(): void {
    this.columnCount = this.keymap.reduce((res, row) => Math.max(res, row.length), 0);

    if (this.debug) console.debug('[numpad] columnCount=' + this.columnCount);
  }

  ngOnDestroy(): void {
  }

  onKeyPress(event: UIEvent, keyDef: NumpadKeyDef) {
    if (this.debug) console.debug('[numpad] click', keyDef.key, event);
  }
}
