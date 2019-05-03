import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {VesselValidatorService} from "../../services/vessel.validator";
import {FormGroup} from "@angular/forms";
import {LocationLevelIds, ReferentialRef, referentialToString, VesselFeatures} from "../../services/model";
import {Platform} from '@ionic/angular';
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {Observable} from 'rxjs';
import {debounceTime, switchMap} from 'rxjs/operators';
import {AppForm, AppFormUtils} from '../../../core/core.module';
import {ReferentialRefService} from '../../services/referential-ref.service';


@Component({
  selector: 'form-vessel',
  templateUrl: './form-vessel.html',
  styleUrls: ['./form-vessel.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselForm extends AppForm<VesselFeatures> implements OnInit {

  form: FormGroup;
  data: VesselFeatures;
  locations: Observable<ReferentialRef[]>;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: Platform,
    protected vesselValidatorService: VesselValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef
  ) {

    super(dateAdapter, platform, vesselValidatorService.getFormGroup());
  }

  ngOnInit() {
    super.ngOnInit();

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

    this.form.reset();
  }

  referentialToString = referentialToString;
  filterNumberInput = AppFormUtils.filterNumberInput;


  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
