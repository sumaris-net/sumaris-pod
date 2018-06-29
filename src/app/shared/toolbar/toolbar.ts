import { Component, Input, Output, EventEmitter, OnInit, ViewChild } from '@angular/core';

@Component({
  selector: 'app-toolbar',
  templateUrl: 'toolbar.html'
})
export class ToolbarComponent implements OnInit {

  @Input()
  title: string = '';

  @Input()
  color: string = '';

  @Input()
  class: string = '';

  @Input()
  hasValidate: boolean = true;

  @Input()
  hasSearch: boolean = true;

  @Output()
  onValidate: EventEmitter<any> = new EventEmitter<any>();

  constructor(
  ) {
  }

  ionViewDidLoad() {
    console.debug('[toolbar] page loaded');
  }

  ngOnInit() {
    this.hasValidate = this.hasValidate && this.onValidate.observers.length > 0;

  }

  enableSearchBar() {
    console.log('TODO: add toolbar');
  }

}
