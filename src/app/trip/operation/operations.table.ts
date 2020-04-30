import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {
  AppTable,
  AppTableDataSource, environment,
  isNotNil,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  referentialToString
} from "../../core/core.module";
import {OperationValidatorService} from "../services/operation.validator";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {ReferentialRefService} from "../../referential/referential.module";
import {OperationFilter, OperationService} from "../services/operation.service";
import {TranslateService} from "@ngx-translate/core";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {Operation, Trip} from "../services/model/trip.model";


@Component({
  selector: 'app-operations-table',
  templateUrl: 'operations.table.html',
  styleUrls: ['operations.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: OperationValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationTable extends AppTable<Operation, OperationFilter> implements OnInit, OnDestroy {

  displayAttributes: {
    [key: string]: string[]
  };
  @Input() latLongPattern: string;

  @Input() tripId: number;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected validatorService: ValidatorService,
    protected dataService: OperationService,
    protected referentialRefService: ReferentialRefService,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat(
          platform.is('mobile') ?
            ['physicalGear',
              'targetSpecies',
              'startDateTime',
              'endDateTime']  :
          ['physicalGear',
            'targetSpecies',
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
          suppressErrors: environment.production,
          serviceOptions: {
            readOnly: true
          }
        })
    );
    this.i18nColumnPrefix = 'TRIP.OPERATION.LIST.';

    this.readOnly = false; // Allow deletion
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.saveBeforeSort = false;
    this.saveBeforeDelete = false;
    this.autoLoad = false; // waiting parent to be loaded

    this.pageSize = 1000; // Do not use paginator

    settings.ready().then(() => {
      this.latLongPattern = this.settings.latLongFormat;
    });
  }

  ngOnInit() {
    super.ngOnInit();

    this.displayAttributes = {
      gear: this.settings.getFieldDisplayAttributes('gear'),
      taxonGroup: this.settings.getFieldDisplayAttributes('taxonGroup'),
    };

    this.registerSubscription(
      this.settings.onChange.subscribe((settings) => {
        if (this.loading) return; // skip
        this.latLongPattern = settings.latLongFormat;

        this.displayAttributes = {
          gear: this.settings.getFieldDisplayAttributes('gear'),
          taxonGroup: this.settings.getFieldDisplayAttributes('taxonGroup'),
        };

        this.markForCheck();
      }));

    // Apply trip id, if already set
    if (isNotNil(this.tripId)) {
      this.setTripId(this.tripId);
    }
  }

  setTrip(data: Trip) {
    this.setTripId(data && data.id || undefined);
  }

  setTripId(id: number) {
    if (this.tripId !== id) {
      this.tripId = id;
      const filter = this.filter || {};
      filter.tripId = id;
      this.dataSource.serviceOptions = this.dataSource.serviceOptions || {};
      this.dataSource.serviceOptions.tripId = id;
      this.setFilter(filter, {emitEvent: isNotNil(id)});
    }
  }

  referentialToString = referentialToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

