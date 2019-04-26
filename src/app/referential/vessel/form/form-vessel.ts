import {Component, OnInit} from '@angular/core';
import {VesselValidatorService} from "../../services/vessel.validator";
import {FormGroup} from "@angular/forms";
import {EntityUtils, LocationLevelIds, ReferentialRef, referentialToString, VesselFeatures} from "../../services/model";
import {Platform} from '@ionic/angular';
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {Observable} from 'rxjs';
import {debounceTime, mergeMap, switchMap} from 'rxjs/operators';
import {AppForm, AppFormUtils} from '../../../core/core.module';
import {ReferentialRefService} from '../../services/referential-ref.service';


@Component({
  selector: 'form-vessel',
  templateUrl: './form-vessel.html',
  styleUrls: ['./form-vessel.scss']
})
export class VesselForm extends AppForm<VesselFeatures> implements OnInit {

  form: FormGroup;
  data: VesselFeatures;
  locations: Observable<ReferentialRef[]>;


  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: Platform,
    protected vesselValidatorService: VesselValidatorService,
    protected referentialRefService: ReferentialRefService
  ) {

    super(dateAdapter, platform, vesselValidatorService.getFormGroup());
  }

  ngOnInit() {
    this.locations = this.form.controls['basePortLocation']
      .valueChanges
      .pipe(
        debounceTime(250),
        switchMap(value => this.referentialRefService.suggest(value, {
            entityName: 'Location',
            levelId: LocationLevelIds.PORT
          }
        ))
      )
    ;
  }

  referentialToString = referentialToString;
  filterNumberInput = AppFormUtils.filterNumberInput;

}
