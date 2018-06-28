import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { VesselValidatorService } from "../validator/validators";
import { FormGroup } from "@angular/forms";
import { VesselFeatures, Referential, LocationLevelIds, referentialToString } from "../../services/model";
import { Platform } from 'ionic-angular';
import { Moment } from 'moment/moment';
import { DATE_ISO_PATTERN } from '../../constants';
import { DateAdapter } from "@angular/material";
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { VesselService } from '../../services/vessel-service';
import { AppForm } from '../../../core/core.module';
import { ReferentialService } from '../../services/referential-service';


@Component({
  selector: 'form-vessel',
  templateUrl: './form-vessel.html'
})
export class VesselForm extends AppForm<VesselFeatures> implements OnInit {

  form: FormGroup;
  data: VesselFeatures;
  locations: Observable<Referential[]>;


  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected platform: Platform,
    protected vesselValidatorService: VesselValidatorService,
    protected vesselService: VesselService,
    protected referentialService: ReferentialService
  ) {

    super(dateAdapter, platform, vesselValidatorService.getFormGroup());
  }

  ngOnInit() {
    this.form = this.vesselValidatorService.getFormGroup();
    this.locations = this.form.controls['basePortLocation']
      .valueChanges
      .pipe(
        mergeMap(value => {
          if (!value) return Observable.empty();
          if (typeof value == "object") return Observable.of([value]);
          if (typeof value != "string" || value.length < 2) return Observable.of([]);
          return this.referentialService.loadAll(0, 50, undefined, undefined,
            {
              levelId: LocationLevelIds.PORT,
              searchText: value as string
            },
            { entityName: 'Location' }
          );
        }));
  }

  referentialToString = referentialToString;

}
