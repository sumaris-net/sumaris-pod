import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import * as moment from "moment/moment";
import {Platform} from "@ionic/angular";
import {debounceTime} from "rxjs/operators";

@Component({
  selector: 'app-data-time-test',
  templateUrl: './mat-date-time.test.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DateTimeTestPage implements OnInit {


  showDebugPanel = false;
  debugPanelContent = '';

  form: FormGroup;

  constructor(
    protected platform: Platform,
    protected formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef
  ) {
    this.form = formBuilder.group({
      empty: [null, Validators.required],
      enable: [null, Validators.required],
      disable: [null, Validators.required]
    });

    const disableControl = this.form.get('disable');
    disableControl.disable();

    // Copy the 'enable' value, into the disable control
    this.form.get('enable').valueChanges.subscribe(value => disableControl.setValue(value));

    const mobile = platform.is('mobile');
    if (mobile) {
      this.showDebugPanel = true;

    }

  }

  ngOnInit() {

    setTimeout(() => this.loadData(), 250);


  }

  // Load the form with data
  async loadData() {
    const now = moment();
    const data = {
      empty: null,
      enable: now,
      disable: now,
    };

    this.form.setValue(data);

    this.logDebug("[test-page] Data loaded: " + JSON.stringify(data));

    this.form.get('empty').valueChanges
      .pipe(debounceTime(300))
      .subscribe(value => this.logDebug("[test-page] Value n°1: " + JSON.stringify(value)));

    this.form.get('enable').valueChanges
      .pipe(debounceTime(300))
      .subscribe(value => this.logDebug("[test-page] Value n°2: " + JSON.stringify(value)));
  }

  doSubmit(event) {
    this.logDebug("[test-page] Form content: " + JSON.stringify(this.form.value));
  }

  logDebug(message: string) {
    console.debug(message);

    if (this.showDebugPanel) {
      this.debugPanelContent += message + '<br/>';
      this.cd.markForCheck();
    }
  }

  stringify = JSON.stringify;

  /* -- protected methods -- */


}

