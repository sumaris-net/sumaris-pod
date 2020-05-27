import {Component, OnInit} from "@angular/core";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";


@Component({
  selector: 'app-numpad-test',
  templateUrl: './numpad.test.html'
})
export class NumpadTestPage implements OnInit {


  form: FormGroup;

  constructor(
    protected formBuilder: FormBuilder
  ) {
    this.form = formBuilder.group({
      empty: [null, Validators.required],
      fill: [null, Validators.required],
      disable: [null, Validators.required],
    });

    this.form.get('disable').disable();
  }

  ngOnInit() {


    this.loadData();
    //setTimeout(() => this.loadData(), 1500);

  }

  // Load the form with data
  async loadData() {
    const data = {
      empty: null,
      fill: 99.99,
      disable: -111.11
    };

    this.form.setValue(data);
  }

  doSubmit(event) {
    console.debug("Validate form: ", this.form.value);
  }

  /* -- protected methods -- */


}

