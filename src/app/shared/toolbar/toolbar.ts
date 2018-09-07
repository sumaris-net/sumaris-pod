import { Component, Input, Output, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { ProgressBarService } from '../../core/services/progress-bar.service';

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
  hasSearch: boolean = false;

  @Output()
  onValidate: EventEmitter<any> = new EventEmitter<any>();

  progressBarMode: string;

  constructor(
    private progressBarService: ProgressBarService
  ) {
  }

  ngOnInit() {
    this.hasValidate = this.hasValidate && this.onValidate.observers.length > 0;
    this.progressBarService.updateProgressBar$.subscribe((mode: string) => {
      this.progressBarMode = mode;
    });
  }

  enableSearchBar() {
    console.log('TODO: add search toolbar');
  }

}
