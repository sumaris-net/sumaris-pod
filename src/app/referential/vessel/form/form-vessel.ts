import { Component, OnInit } from '@angular/core';
import { VesselValidatorService } from "../../services/vessel.validator";
import { FormGroup } from "@angular/forms";
import { VesselFeatures, LocationLevelIds, referentialToString, EntityUtils, ReferentialRef } from "../../services/model";
import { Platform } from '@ionic/angular';
import { Moment } from 'moment/moment';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { VesselService } from '../../services/vessel-service';
import { AppForm } from '../../../core/core.module';
import { ReferentialRefService } from '../../services/referential-ref.service';


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
    protected vesselService: VesselService,
    protected referentialRefService: ReferentialRefService
  ) {

    super(dateAdapter, platform, vesselValidatorService.getFormGroup());
  }

  ngOnInit() {
    this.locations = this.form.controls['basePortLocation']
      .valueChanges
      .pipe(
        mergeMap(value => {
          if (EntityUtils.isNotEmpty(value)) return Observable.of([value]);
          value = (typeof value == "string") && value || undefined;
          return this.referentialRefService.loadAll(0, 50, undefined, undefined,
            {
              entityName: 'Location',
              levelId: LocationLevelIds.PORT,
              searchText: value as string
            }
          );
        }));
  }

  referentialToString = referentialToString;

}
