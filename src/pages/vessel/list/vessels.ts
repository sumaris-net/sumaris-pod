import { Component, EventEmitter, OnInit, Output, ViewChild, OnDestroy } from "@angular/core";
import { merge } from "rxjs/observable/merge";
import { startWith, switchMap } from "rxjs/operators";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource } from "../../../app/material/material.table";
import { VesselValidatorService } from "../validator/validators";
import { VesselService, VesselFilter } from "../../../services/vessel-service";
import { SelectionModel } from "@angular/cdk/collections";
import { VesselModal } from "../modal/modal-vessel";
import { VesselFeatures, Referential, toDateISOString, fromDateISOString } from "../../../services/model";
import { Subscription } from "rxjs";
import { ModalController, Platform } from "ionic-angular";
import { Router, ActivatedRoute } from "@angular/router";
import { AppTable } from "../../../app/table/table";
import { AccountService } from "../../../services/account-service";
import { Location } from '@angular/common';
import { Observable } from 'rxjs';
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { MatButtonToggleGroup } from "@angular/material";

@Component({
  selector: 'page-vessels',
  templateUrl: 'vessels.html',
  providers: [
    { provide: ValidatorService, useClass: VesselValidatorService }
  ],
})
export class VesselsPage extends AppTable<VesselFeatures, VesselFilter> implements OnInit {

  filterForm: FormGroup;
  locations: Observable<Referential[]>;

  @ViewChild(MatButtonToggleGroup) clickModeGroup: MatButtonToggleGroup;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected vesselValidatorService: VesselValidatorService,
    protected vesselService: VesselService,
    private formBuilder: FormBuilder
  ) {
    super(route, router, platform, location, modalCtrl, accountService, vesselValidatorService,
      new AppTableDataSource<VesselFeatures, VesselFilter>(VesselFeatures, vesselService, vesselValidatorService),
      ['select',
        'id',
        'exteriorMarking',
        'startDate',
        'name',
        'basePortLocation',
        'comments'],
      {
        date: null,
        searchText: null
      }
    );
    this.i18nColumnPrefix = 'VESSEL.';
    this.filterForm = formBuilder.group({
      'date': [null],
      'searchText': [null]
    });
  };

  ngOnInit() {

    super.ngOnInit();

    this.clickModeGroup.valueChange.subscribe((value) => {
      this.inlineEdition = (value === "edit");
    });

    // TODO fill locations

    // Update filter when changes
    this.filterForm.valueChanges.subscribe(() => {
      this.filter = this.filterForm.value;
    });

    this.onRefresh.subscribe(() => {
      this.filterForm.markAsUntouched();
      this.filterForm.markAsPristine();
    });


  }

  addRowModal(): Promise<any> {
    if (this.loading) return Promise.resolve();

    let modal = this.modalCtrl.create(VesselModal);
    modal.onDidDismiss(res => {
      // if new vessel added, refresh the table
      if (res) this.onRefresh.emit();
    });
    return modal.present();
  }
}

