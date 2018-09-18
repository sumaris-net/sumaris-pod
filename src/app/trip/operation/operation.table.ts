import { Component, OnInit, Input, OnDestroy } from "@angular/core";
import { Observable } from 'rxjs';
import { mergeMap } from "rxjs/operators";
import { ValidatorService } from "angular4-material-table";
import { AppTableDataSource, AppTable, AccountService } from "../../core/core.module";
import { OperationValidatorService } from "../services/operation.validator";
import { Referential, Operation, Trip, referentialToString } from "../services/trip.model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { Location } from '@angular/common';
import { ReferentialService } from "../../referential/referential.module";
import { OperationService, OperationFilter } from "../services/operation.service";
import { PositionValidatorService } from "../services/position.validator";


@Component({
  selector: 'table-operations',
  templateUrl: 'operation.table.html',
  styleUrls: ['operation.table.scss'],
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
    protected validatorService: OperationValidatorService,
    protected operationService: OperationService,
    protected referentialService: ReferentialService
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
      ['select',
        'metier',
        'startDateTime',
        'startPosition',
        'endDateTime',
        'endPosition',
        'comments',
        'actions'],
      new AppTableDataSource<Operation, OperationFilter>(Operation, operationService, validatorService)
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
    this.filter = this.filter || {};
    this.filter.tripId = tripId;
    this.dataSource.serviceOptions = this.dataSource.serviceOptions || {};
    this.dataSource.serviceOptions.tripId = tripId;
    if (tripId) {
      this.onRefresh.emit();
    }
  }

  public onOpenRowDetail(id: number): Promise<boolean> {
    return this.router.navigateByUrl('/operations/' + this.tripId + '/' + id);
  }

  public onAddRowDetail(): Promise<boolean> {
    return this.router.navigateByUrl('/operations/' + this.tripId + '/new');
  }

  referentialToString = referentialToString;
}

