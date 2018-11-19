import { Component, Input, Output, EventEmitter, OnInit, ViewChild } from '@angular/core';
import { ProgressBarService } from '../../core/services/progress-bar.service';
import { BehaviorSubject } from "rxjs";
import { IonBackButton } from "@ionic/angular";

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
  defaultBackHref: string | undefined;

  @Input()
  hasSearch: boolean = false;

  @Output()
  onValidate: EventEmitter<any> = new EventEmitter<any>();

  progressBarMode: BehaviorSubject<string> = new BehaviorSubject('none');

  @ViewChild('backButton') backButton: IonBackButton;


  constructor(
    private progressBarService: ProgressBarService
  ) {
  }

  ngOnInit() {
    this.hasValidate = this.hasValidate && this.onValidate.observers.length > 0;
    this.progressBarService.updateProgressBar$.subscribe((mode: string) => {
      if (mode != this.progressBarMode.getValue()) {
        setTimeout(() => {
          this.progressBarMode.next(mode);
        });
      }
    });
    console.log(this.backButton);
    //this.backButton.onClick
  }

  enableSearchBar() {
    console.warn('[app-toolbar] TODO: implement enableSearchBar()');
  }

}
