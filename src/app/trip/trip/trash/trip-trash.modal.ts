import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnDestroy, OnInit} from "@angular/core";
import {AlertController, ModalController} from "@ionic/angular";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../../core/table/table.class";
import {Trip} from "../../services/model/trip.model";
import {ActivatedRoute, Router} from "@angular/router";
import {PlatformService} from "../../../core/services/platform.service";
import {Location} from "@angular/common";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {AccountService} from "../../../core/services/account.service";
import {TripFilter, TripService} from "../../services/trip.service";
import {UserEventService} from "../../../social/services/user-event.service";
import {PersonService} from "../../../admin/services/person.service";
import {ReferentialRefService} from "../../../referential/services/referential-ref.service";
import {VesselSnapshotService} from "../../../referential/services/vessel-snapshot.service";
import {FormBuilder} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {EntitiesTableDataSource} from "../../../core/table/entities-table-datasource.class";
import {environment} from "../../../../environments/environment";
import {personsToString} from "../../../core/services/model/person.model";
import {TableElement} from "@e-is/ngx-material-table";
import {ReferentialRef, referentialToString} from "../../../core/services/model/referential.model";

@Component({
  selector: 'app-trip-trash-modal',
  templateUrl: './trip-trash.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TripTrashModal extends AppTable<Trip> implements OnInit, OnDestroy {


  isAdmin: boolean;
  displayedAttributes: {
    [key: string]: string[]
  };

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
    protected userEventService: UserEventService,
    protected personService: PersonService,
    protected referentialRefService: ReferentialRefService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected formBuilder: FormBuilder,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef,
    protected viewCtrl: ModalController
  ) {

    super(route, router, platform, location, modalCtrl, settings,
      RESERVED_START_COLUMNS
        .concat([
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
          saveOnlyDirtyRows: true
        }
      }),
      {
        //synchronizationStatus: SynchronizationStatusEnum.DELETED
      },
      injector
    );
    this.i18nColumnPrefix = 'TRIP.TABLE.';

    this.readOnly = true;
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.saveBeforeSort = false;
    this.saveBeforeFilter = false;
    this.saveBeforeDelete = false;
    this.autoLoad = true;
    this.sortBy = 'departureDateTime';
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

    // Load trips
    /*setTimeout(() => {
      this.onRefresh.next("modal");
      this.markForCheck();
    }, 200);*/

  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  restoreTrips(event: UIEvent, rows: TableElement<Trip>[]) {

  }

  async close(event?: any) {
    await this.viewCtrl.dismiss();
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  personsToString = personsToString;
  referentialToString = referentialToString;

  programToString(item: ReferentialRef) {
    return item && item.label || undefined;
  }
  /* -- protected method -- */

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
