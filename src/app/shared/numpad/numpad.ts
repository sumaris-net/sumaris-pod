import {ChangeDetectionStrategy, Component, Input, OnDestroy, OnInit} from "@angular/core";


@Component({
  selector: 'app-numpad',
  templateUrl: 'numpad.html',
  styleUrls: ['./numpad.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NumpadComponent implements OnInit, OnDestroy {

  @Input()
  keymap: Array<any[][]>;

  constructor() {
    this.keymap = [
      [[1, 1], [2, 2], [3, 3], ['<ion-icon name="pin"></ion-icon>', 'back']],
      [[4, 4], [5, 5], [6, 6]],
      [[7, 7], [8, 8], [9, 9]],
      [['.', '.'], [0, 0], ['']]
    ]
    ;
  }

  ngOnInit(): void {
  }

  ngOnDestroy(): void {
  }

  onClick(event: UIEvent, keyChar: string|number) {
    console.log(keyChar);
  }
}
