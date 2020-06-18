import {Component, OnInit} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {SharedValidators} from "../../../validator/validators";
import * as moment from "moment";
import {BehaviorSubject} from "rxjs";
import {Moment} from "moment";
import {MatSwipeFieldConfig} from "../material.swipe";
import {DateFormatPipe} from "../../../pipes/date-format.pipe";

@Component({
  selector: 'app-swipe-test',
  templateUrl: './swipe.test.html'
})
export class SwipeTestPage implements OnInit {

  form: FormGroup;

  swipeDateOption: MatSwipeFieldConfig;

  $dates = new BehaviorSubject<Moment[]>(undefined);
  private _today = moment().startOf("day")

  constructor(
    protected formBuilder: FormBuilder,
    protected dateFormatPipe: DateFormatPipe
  ) {
    this.form = formBuilder.group({
      empty: [null, Validators.required],
      date: [null, Validators.compose([Validators.required, SharedValidators.validDate])],
      disabled: [null, Validators.required]
    });

    this.form.get('disabled').disable();

    this.swipeDateOption = {
      displayWith: (obj) => dateFormatPipe.transform(obj, {pattern: 'dddd L'}).toString()
    };
  }

  ngOnInit() {

    const dates: Moment[] = [];
    for (let d = 0; d < 7; d++) {
      dates[d] = moment(this._today).add(d-3, "day");
    }
    this.$dates.next(dates);


    this.loadData();
    //setTimeout(() => this.loadData(), 1500);

  }

  // Load the form with data
  async loadData() {
    const data = {
      empty: null,
      date: this._today,
      disabled: "disabled"
    };

    this.form.patchValue(data);
  }

  doSubmit(event) {
    console.debug("Validate form: ", this.form.value);
  }

  /* -- protected methods -- */


}

