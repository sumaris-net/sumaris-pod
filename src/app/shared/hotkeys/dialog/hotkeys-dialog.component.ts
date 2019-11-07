import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';

@Component({
  templateUrl: './hotkeys-dialog.component.html',
  styleUrls: ['./hotkeys-dialog.component.scss']
})
export class HotkeysDialogComponent implements OnInit {
  hotkeys = Array.from(this.data);

  constructor(@Inject(MAT_DIALOG_DATA) protected data) { }

  ngOnInit() {
  }

}
