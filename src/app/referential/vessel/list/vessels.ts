import { Component, EventEmitter, OnInit, Output, ViewChild, OnDestroy } from "@angular/core";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource, AppTable, AppFormUtils } from "../../../core/core.module";
import { VesselValidatorService } from "../../services/vessel.validator";
import { VesselService, VesselFilter } from "../../services/vessel-service";
import { VesselModal } from "../modal/modal-vessel";
import { VesselFeatures, Referential, toDateISOString, fromDateISOString, referentialToString, ReferentialRef } from "../../services/model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { AccountService } from "../../../core/services/account.service";
import { Location } from '@angular/common';
import { Observable } from 'rxjs';
import { FormGroup, FormBuilder } from "@angular/forms";

@Component({
  selector: 'page-vessels',
  templateUrl: 'vessels.html',
  styleUrls: ['./vessels.scss'],
  providers: [
    { provide: ValidatorService, useClass: VesselValidatorService }
  ],
})
export class VesselsPage extends AppTable<VesselFeatures, VesselFilter> implements OnInit {

  filterForm: FormGroup;
  locations: Observable<ReferentialRef[]>;

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
    super(route, router, platform, location, modalCtrl, accountService,
      ['select',
        'id',
        'exteriorMarking',
        'startDate',
        'name',
        'basePortLocation',
        'comments',
        'actions'
      ],
      new AppTableDataSource<VesselFeatures, VesselFilter>(VesselFeatures, vesselService, vesselValidatorService),
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

  async openNewRowDetail(): Promise<any> {
    if (this.loading) return Promise.resolve();

    const modal = await this.modalCtrl.create({ component: VesselModal });
    // if new vessel added, refresh the table
    modal.onDidDismiss().then(res => {
      if (res) this.onRefresh.emit();
    });
    return modal.present();
  }

  protected async openRow(id: number, row?: TableElement<VesselFeatures>): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    return await this.router.navigateByUrl('/referential/vessels/' + id);
  }

  referentialToString = referentialToString;
}

