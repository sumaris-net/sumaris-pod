import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {AlertController, ModalController} from "@ionic/angular";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../../core/table/table.class";
import {Trip} from "../../services/model/trip.model";
import {ActivatedRoute, Router} from "@angular/router";
import {PlatformService} from "../../../core/services/platform.service";
import {Location} from "@angular/common";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {AccountService} from "../../../core/services/account.service";
import {TripFilter, TripService} from "../../services/trip.service";
import {FormBuilder} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {EntitiesTableDataSource} from "../../../core/table/entities-table-datasource.class";
import {environment} from "../../../../environments/environment";
import {TableElement} from "@e-is/ngx-material-table";
import {SynchronizationStatus, SynchronizationStatusEnum} from "../../../data/services/model/root-data-entity.model";
import {isEmptyArray, isNotNil} from "../../../shared/functions";
import {OperationService} from "../../services/operation.service";
import {EntitiesStorage} from "../../../core/services/entities-storage.service";

@Component({
  selector: 'app-trip-trash-modal',
  templateUrl: './trip-trash.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripTrashModal extends AppTable<Trip, TripFilter> implements OnInit, OnDestroy {


  isAdmin: boolean;
  displayedAttributes: {
    [key: string]: string[]
  };

  @Input() synchronizationStatus: SynchronizationStatus;

  constructor(
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected accountService: AccountService,
    protected service: TripService,
    protected entities: EntitiesStorage,
    protected operationService: OperationService,
    protected formBuilder: FormBuilder,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef,
    protected viewCtrl: ModalController
  ) {

    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat([
          'updateDate',
          'program',
          'vessel',
          'departureLocation',
          'departureDateTime',
          'returnDateTime',
          'observers',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      new EntitiesTableDataSource<Trip, TripFilter>(Trip, service, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        dataServiceOptions: {
          saveOnlyDirtyRows: true,
          trash: true
        }
      }),
      {},
      injector
    );
    this.i18nColumnPrefix = 'TRIP.TABLE.';

    this.readOnly = true;
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.saveBeforeSort = false;
    this.saveBeforeFilter = false;
    this.saveBeforeDelete = false;
    this.autoLoad = false;
    this.sortBy = 'updateDate';
    this.sortDirection = 'desc';

    // FOR DEV ONLY ----
    this.debug = !environment.production;

  }

  ngOnInit() {
    super.ngOnInit();
    this.isAdmin = this.accountService.isAdmin();

    this.displayedAttributes = {
      vesselSnapshot: this.settings.getFieldDisplayAttributes('vesselSnapshot'),
      location: this.settings.getFieldDisplayAttributes('location')
    };

    this.setFilter(
      {
        ...this.filter,
        synchronizationStatus: this.synchronizationStatus
      },
      {emitEvent: true}
    );

  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  async closeAndRestore(event: UIEvent, rows: TableElement<Trip>[]) {

    await this.restore(event, rows)

    return this.close();
  }

  async restore(event: UIEvent, rows: TableElement<Trip>[]) {
    if (this.loading) return; // Skip

    if (event) {
      event.stopPropagation();
      event.preventDefault();
    }

    this.markAsLoading();

    try {
      const entities = (rows || []).map(row => row.currentData).filter(isNotNil);
      if (isEmptyArray(entities)) return; // Skip

      // Set the newt synchronization status (DIRTY or null)
      const synchronizationStatus = (!this.synchronizationStatus || this.synchronizationStatus !== 'SYNC') ?
        SynchronizationStatusEnum.DIRTY : null;
      entities.forEach(source => {
        source.synchronizationStatus = synchronizationStatus;
      });

      // Execute restore
      await this.service.restoreFromTrash(entities);

      this.selection.deselect(...rows);

      // Success toast
      setTimeout(() => {
        this.showToast({
          type: "info",
          message: rows.length === 1 ?
            'TRIP.TRASH.INFO.ONE_TRIP_RESTORED' :
            'TRIP.TRASH.INFO.MANY_TRIPS_RESTORED' });
      }, 200);

    }
    catch(err) {
      console.error(err && err.message || err, err);
      this.error = err && err.message || err;
    }
    finally {
      this.markAsLoaded();
    }
  }

  clickRow(event: MouseEvent|undefined, row: TableElement<Trip>): boolean {
    if (event && event.defaultPrevented) return; // Skip

    if (this.selection.isEmpty()) {
      this.selection.select(row);
    }
    else if (!this.selection.isSelected(row)) {
      if (!event.ctrlKey) {
        this.selection.clear();
      }
      this.selection.select(row);
    }
    else {
      this.selection.toggle(row);
    }
    return true;
  }

  async close(event?: any) {
    await this.viewCtrl.dismiss();
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  async cleanLocalTrash(event?: UIEvent, confirm?: boolean) {

    if (!confirm) {
      confirm = await this.askDeleteConfirmation(event);
      if (!confirm) return; // skip
    }

    console.debug('[trip-trash] Cleaning the trash...');
    await this.entities.clearTrash(Trip.TYPENAME);

    await this.close();

    // Success toast
    setTimeout(() => {
      this.showToast({
        message: 'TRIP.TRASH.INFO.LOCAL_TRASH_CLEANED' });
    }, 200);

  }

  /* -- protected method -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
