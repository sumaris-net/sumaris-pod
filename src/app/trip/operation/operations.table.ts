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
import {Operation, referentialToString, Trip} from "../services/trip.model";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {ReferentialRefService} from "../../referential/referential.module";
import {OperationFilter, OperationService} from "../services/operation.service";
import {PositionValidatorService} from "../services/position.validator";
import {TranslateService} from "@ngx-translate/core";
import {LocalSettingsService} from "../../core/services/local-settings.service";


@Component({
  selector: 'app-operation-table',
  templateUrl: 'operations.table.html',
  styleUrls: ['operations.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: OperationValidatorService},
    {provide: ValidatorService, useClass: PositionValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationTable extends AppTable<Operation, OperationFilter> implements OnInit, OnDestroy {

  @Input() latLongPattern: string;

  @Input() tripId: number;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settingsService: LocalSettingsService,
    protected validatorService: OperationValidatorService,
    protected dataService: OperationService,
    protected referentialRefService: ReferentialRefService,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, settingsService,
      RESERVED_START_COLUMNS
        .concat(
          ['metier',
            'startDateTime',
            'startPosition',
            'endDateTime',
            'endPosition',
            'comments'])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Operation, OperationFilter>(Operation, dataService, null,
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
    this.inlineEdition = false;
    this.latLongPattern = settingsService.settings.latLongFormat;
    this.pageSize = 1000; // Do not use paginator
  };


  ngOnInit() {
    super.ngOnInit();

    this.tripId && this.setTripId(this.tripId);

    this.registerSubscription(
      this.settingsService.onChange.subscribe((settings) => {
        if (this.loading) return; // skip
        this.latLongPattern = settings.latLongFormat;
        this.markForCheck();
      }));
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
            handler: () => {
            }
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

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

