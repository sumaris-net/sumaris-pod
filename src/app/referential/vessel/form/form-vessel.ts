import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {VesselValidatorService} from "../../services/vessel.validator";
import {LocationLevelIds, ReferentialRef, referentialToString, VesselFeatures} from "../../services/model";
import {Moment} from 'moment/moment';
import {DateAdapter} from "@angular/material";
import {Observable} from 'rxjs';
import {debounceTime, switchMap} from 'rxjs/operators';
import {AppForm, AppFormUtils, LocalSettingsService} from '../../../core/core.module';
import {ReferentialRefService} from '../../services/referential-ref.service';


@Component({
  selector: 'form-vessel',
  templateUrl: './form-vessel.html',
  styleUrls: ['./form-vessel.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VesselForm extends AppForm<VesselFeatures> implements OnInit {

  data: VesselFeatures;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected vesselValidatorService: VesselValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef,
    protected settings: LocalSettingsService
  ) {

    super(dateAdapter, vesselValidatorService.getFormGroup(), settings);
  }

  ngOnInit() {
    super.ngOnInit();

    // Combo location
    this.registerAutocompleteConfig('location', {
      service: this.referentialRefService,
      filter: {
        entityName: 'Location',
        levelId: LocationLevelIds.PORT
      }
    });

    this.form.reset();
  }

  referentialToString = referentialToString;
  filterNumberInput = AppFormUtils.filterNumberInput;


  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
