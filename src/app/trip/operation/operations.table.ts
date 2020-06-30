import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {
  AppTable,
  AppTableDataSource,
  environment,
  isNotNil,
  referentialToString,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {OperationValidatorService} from "../services/validator/operation.validator";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {OperationFilter, OperationService, OperationServiceOptions} from "../services/operation.service";
import {TranslateService} from "@ngx-translate/core";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {Operation, Trip} from "../services/model/trip.model";
import {LatLongPattern} from "../../shared/material/latlong/latlong.utils";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {toBoolean} from "../../shared/functions";
import {BatchModal} from "../batch/batch.modal";
import {OperationsMap} from "./map/operations.map";


@Component({
  selector: 'app-operations-table',
  templateUrl: 'operations.table.html',
  styleUrls: ['operations.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: OperationValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OperationsTable extends AppTable<Operation, OperationFilter> implements OnInit, OnDestroy {

  displayAttributes: {
    [key: string]: string[]
  };

  @Input() latLongPattern: LatLongPattern;

  @Input() tripId: number;

  @Input() showMap: boolean; // false by default

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
      new AppTableDataSource<Operation, OperationFilter, OperationServiceOptions>(Operation, dataService,
        null,
        // DataSource options
        {
          prependNewElements: false,
          suppressErrors: environment.production,
          dataServiceOptions: {
            readOnly: true,
            withBatchTree: false,
            withSamples: false
          }
        })
    );
    this.i18nColumnPrefix = 'TRIP.OPERATION.LIST.';

    this.readOnly = false; // Allow deletion
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.saveBeforeSort = false;
    this.saveBeforeFilter = false;
    this.saveBeforeDelete = false;
    this.autoLoad = false; // waiting parent to be loaded

    this.pageSize = 1000; // Do not use paginator

    settings.ready().then(() => {
      if (this.settings.settings.accountInheritance) {

      }
      else {
        this.latLongPattern = this.settings.latLongFormat;
      }
    });
  }

  ngOnInit() {
    super.ngOnInit();

    this.showMap = toBoolean(this.showMap, false);

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

  async openMapModal(event: UIEvent) {
    const operations = (await this.dataSource.getRows())
      .map(row => row.currentData);

    const modal = await this.modalCtrl.create({
      component: OperationsMap,
      componentProps: {
        operations
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const res = await modal.onDidDismiss();
    console.log(res)
  }

  referentialToString = referentialToString;

  /* -- protected methods -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

