import { Component, EventEmitter, OnInit, Output, Input, ViewChild, OnDestroy } from "@angular/core";
import { MatPaginator, MatSort } from "@angular/material";
import { merge } from "rxjs/observable/merge";
import { Observable } from 'rxjs';
import { startWith, switchMap, mergeMap } from "rxjs/operators";
import { ValidatorService, TableElement } from "angular4-material-table";
import { AppTableDataSource, AppTable, TableSelectColumnsComponent, AccountService } from "../../../core/core.module";
import { OperationValidatorService } from "../validator/validators";
import { SelectionModel } from "@angular/cdk/collections";
import { Referential, Operation, Trip, referentialToString } from "../../services/model";
import { Subscription } from "rxjs-compat";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { PopoverController } from '@ionic/angular';
import { FormGroup, Validators, FormBuilder } from "@angular/forms";
import { ReferentialService } from "../../../referential/referential.module";
import { MatButtonToggle } from "@angular/material";
import { OperationService, OperationFilter } from "../../services/operation-service";
import { PositionValidatorService } from "../../position/validator/validators";


@Component({
  selector: 'table-operations',
  templateUrl: 'table-operations.html',
  styleUrls: ['table-operations.scss'],
  providers: [
    { provide: ValidatorService, useClass: OperationValidatorService },
    { provide: ValidatorService, useClass: PositionValidatorService }
  ],
})
export class OperationTable extends AppTable<Operation, OperationFilter> implements OnInit, OnDestroy {


  metiers: Observable<Referential[]>;

  @Input() latLongPattern: string;

  @Input() tripId: number;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected operationValidatorService: OperationValidatorService,
    protected operationService: OperationService,
    protected referentialService: ReferentialService
  ) {
    super(route, router, platform, location, modalCtrl, accountService, operationValidatorService,
      new AppTableDataSource<Operation, OperationFilter>(Operation, operationService, operationValidatorService),
      ['select', 'id',
        'metier',
        'startDateTime',
        'startPosition',
        'endDateTime',
        'endPosition',
        'comments',
        'actions'],
      {} // filter
    );
    this.i18nColumnPrefix = 'TRIP.OPERATION.LIST.';
    this.autoLoad = false;
    this.latLongPattern = accountService.account.settings.latLongFormat || 'DDMM';
    //this.inlineEdition = true; // TODO: remove this line !
  };


  ngOnInit() {

    super.ngOnInit();

    this.tripId && this.setTripId(this.tripId);

    // Combo: métiers
    this.metiers = Observable.of("") // TODO: change this to get user input
      .pipe(
        mergeMap(value => {
          if (!value) return Observable.empty();
          if (typeof value != "string" || value.length < 2) return Observable.of([]);
          return this.referentialService.loadAll(0, 10, undefined, undefined,
            {
              searchText: value as string
            },
            { entityName: 'Metier' });
        }));

  }

  setTrip(data: Trip) {
    this.setTripId(data.id);
  }

  setTripId(tripId: number) {
    this.tripId = tripId;
    this.filter.tripId = tripId;
    this.dataSource.serviceOptions = this.dataSource.serviceOptions || {};
    this.dataSource.serviceOptions.tripId = tripId;
    if (tripId) {
      this.onRefresh.emit();
    }
  }

  markAsPristine() {
    this.dirty = false;
  }

  public onOpenRowDetail(id: number): Promise<boolean> {
    return this.router.navigateByUrl('/operations/' + id);
  }

  public onAddRowDetail(): Promise<boolean> {
    return this.router.navigateByUrl('/operations/new');
  }

  referentialToString = referentialToString;
}

