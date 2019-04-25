import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output
} from "@angular/core";
import {Observable} from 'rxjs';
import {map, mergeMap} from "rxjs/operators";
import {ValidatorService} from "angular4-material-table";
import {
  AccountService,
  AppTable,
  AppTableDataSource,
  isNotNil,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {OperationValidatorService} from "../services/operation.validator";
import {EntityUtils, Operation, ReferentialRef, referentialToString, Trip} from "../services/trip.model";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {ReferentialRefService} from "../../referential/referential.module";
import {OperationFilter, OperationService} from "../services/operation.service";
import {PositionValidatorService} from "../services/position.validator";
import {TranslateService} from "@ngx-translate/core";


@Component({
  selector: 'table-operations',
  templateUrl: 'operations.table.html',
  styleUrls: ['operations.table.scss'],
  providers: [
    { provide: ValidatorService, useClass: OperationValidatorService },
    { provide: ValidatorService, useClass: PositionValidatorService }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationTable extends AppTable<Operation, OperationFilter> implements OnInit, OnDestroy {

  @Input() latLongPattern: string;

  @Input() tripId: number;

  @Output()
  onOperationClick: EventEmitter<number> = new EventEmitter<number>();

  @Output()
  onNewOperationClick: EventEmitter<void> = new EventEmitter<void>();

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: OperationValidatorService,
    protected dataService: OperationService,
    protected referentialRefService: ReferentialRefService,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
      RESERVED_START_COLUMNS
        .concat(
          ['metier',
            'startDateTime',
            'startPosition',
            'endDateTime',
            'endPosition',
            'comments'])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Operation, OperationFilter>(Operation, dataService, validatorService,
        // DataSource options
        {
          prependNewElements: false,
          suppressErrors: false,
          serviceOptions: {
            saveOnlyDirtyRows: true
          }
        })
    );
    this.i18nColumnPrefix = 'TRIP.OPERATION.LIST.';
    this.autoLoad = false;
    this.latLongPattern = accountService.account.settings.latLongFormat || 'DDMM';
    this.pageSize = 1000; // Do not use paginator
  };


  ngOnInit() {
    super.ngOnInit();

    this.tripId && this.setTripId(this.tripId);

  }

  setTrip(data: Trip) {
    this.setTripId(data && data.id || undefined);
  }

  setTripId(id: number) {
    this.tripId = id;
    this.filter = this.filter || {};
    this.filter.tripId = id;
    this.dataSource.serviceOptions = this.dataSource.serviceOptions || {};
    this.dataSource.serviceOptions.tripId = id;
    if (isNotNil(id)) {
      this.onRefresh.emit();
    }
  }

  async deleteSelection(confirm?: boolean): Promise<void> {
    if (this.loading) return;

    if (!confirm) {
      const translations = this.translate.instant(['COMMON.YES', 'COMMON.NO', 'CONFIRM.DELETE', 'CONFIRM.ALERT_HEADER']);
      const alert = await this.alertCtrl.create({
        header: translations['CONFIRM.ALERT_HEADER'],
        message: translations['CONFIRM.DELETE'],
        buttons: [
          {
            text: translations['COMMON.NO'],
            role: 'cancel',
            cssClass: 'secondary',
            handler: () => { }
          },
          {
            text: translations['COMMON.YES'],
            handler: () => {
              confirm = true; // update upper value
            }
          }
        ]
      });
      await alert.present();
      await alert.onDidDismiss();
    }

    if (confirm) {
      await super.deleteSelection();
    }
  }

  protected async openEditRowDetail(id: number): Promise<boolean> {
    if (this.onOperationClick.observers.length) {
      this.onOperationClick.emit(id);
      return true;
    }

    return await this.router.navigateByUrl('/operations/' + this.tripId + '/' + id);
  }

  protected async openNewRowDetail(): Promise<boolean> {
    if (this.onNewOperationClick.observers.length) {
      this.onNewOperationClick.emit();
      return true;
    }
    return await this.router.navigateByUrl('/operations/' + this.tripId + '/new');
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

