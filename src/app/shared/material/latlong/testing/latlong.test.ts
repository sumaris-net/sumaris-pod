import {Component, OnInit} from "@angular/core";
import {FormBuilder, FormGroup} from "@angular/forms";
import {SharedValidators} from "../../../validator/validators";


@Component({
  selector: 'app-latlong-test',
  templateUrl: './latlong.test.html'
})
export class LatLongTestPage implements OnInit {


  form: FormGroup;

  constructor(
    protected formBuilder: FormBuilder
  ) {
    this.form = formBuilder.group({
      latitude: [null, SharedValidators.latitude],
      longitude: [null, SharedValidators.longitude]
    });

  }

  ngOnInit() {


    setTimeout(() => this.loadData(), 250);
  }

  // Load the form with data
  async loadData() {
    const data = {
      latitude: 50.1,
      longitude: -2
    };

    this.form.setValue(data);
  }

  doSubmit(event) {
    console.debug("Validate form: ", this.form.value);
  }

  stringify = JSON.stringify;

  /* -- protected methods -- */


}

