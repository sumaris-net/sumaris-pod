import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit
} from "@angular/core";
import {
  AppTable,
  EntitiesTableDataSource,
  environment, isNil, isNotNil, referentialToString,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  StatusIds
} from "../../core/core.module";
import {AlertController, ModalController} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {FormBuilder} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {PlatformService} from "../../core/services/platform.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {AccountService} from "../../core/services/account.service";
import {NetworkService} from "../../core/services/network.service";
import {VesselSnapshotService} from "../../referential/services/vessel-snapshot.service";
import {BehaviorSubject} from "rxjs";
import {filterNotNil} from "../../shared/observables";
import {isNotEmptyArray, toBoolean} from "../../shared/functions";
import {AggregatedLanding, VesselActivity} from "../services/model/aggregated-landing.model";
import {AggregatedLandingFilter, AggregatedLandingService} from "../services/aggregated-landing.service";
import {Moment} from "moment";
import {ObservedLocation} from "../services/model/observed-location.model";
import * as momentImported from "moment";
const moment = momentImported;
import {TableElement} from "@e-is/ngx-material-table";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {ProgramService} from "../../referential/services/program.service";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {add} from "ionicons/icons";
import {PacketModal} from "../packet/packet.modal";
import {AggregatedLandingModal} from "./aggregated-landing.modal";
import {VesselSnapshot} from "../../referential/services/model/vessel-snapshot.model";

@Component({
  selector: 'app-aggregated-landings-table',
  templateUrl: 'aggregated-landings.table.html',
  styleUrls: ['./aggregated-landings.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AggregatedLandingsTable extends AppTable<AggregatedLanding, AggregatedLandingFilter> implements OnInit, OnDestroy {

  canEdit: boolean;
  canDelete: boolean;
  isAdmin: boolean;
  filterIsEmpty = true;
  offline = false;
  mobile: boolean;

  private _onRefreshDates = new EventEmitter<any>();
  private _onRefreshPmfms = new EventEmitter<any>();
  private _program: string;
  private _acquisitionLevel: string;
  private _nbDays: number;
  private _startDate: Moment;
  $dates = new BehaviorSubject<Moment[]>(undefined);
  $pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);

  set nbDays(value: number) {
    if (value && value !== this._nbDays) {
      this._nbDays = value;
      this._onRefreshDates.emit();
    }
  }

  set startDate(value: Moment) {
    if (value && (!this._startDate || !value.isSame(this._startDate, "day"))) {
      this._startDate = moment(value).startOf("day");
      this._onRefreshDates.emit();
    }
  }

  set program(value: string) {
    if (this._program !== value && isNotNil(value)) {
      this._program = value;
      this._onRefreshPmfms.emit();
    }
  }

  get program(): string {
    return this._program;
  }

  @Input()
  set acquisitionLevel(value: string) {
    if (this._acquisitionLevel !== value && isNotNil(value)) {
      this._acquisitionLevel = value;
      this._onRefreshPmfms.emit();
    }
  }

  constructor(
    public network: NetworkService,
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: PlatformService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected accountService: AccountService,
    protected service: AggregatedLandingService,
    protected referentialRefService: ReferentialRefService,
    protected programService: ProgramService,
    protected vesselSnapshotService: VesselSnapshotService,
    protected formBuilder: FormBuilder,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected cd: ChangeDetectorRef
  ) {

    super(route, router, platform, location, modalCtrl, settings,
      ['vessel'],
      new EntitiesTableDataSource<AggregatedLanding, AggregatedLandingFilter>(AggregatedLanding, service, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      }),
      null,
      injector
    );
    this.i18nColumnPrefix = 'AGGREGATED_LANDING.TABLE.';

    this.readOnly = false; // Allow deletion
    this.inlineEdition = false;
    this.confirmBeforeDelete = true;
    this.saveBeforeSort = false;
    this.saveBeforeFilter = false;
    this.saveBeforeDelete = false;
    this.autoLoad = false;
    this.pageSize = -1; // Do not use paginator
    this.mobile = this.settings.mobile;

    // default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.LANDING;

    // FOR DEV ONLY ----
    this.debug = !environment.production;

  }

  ngOnInit() {
    super.ngOnInit();

    this.isAdmin = this.accountService.isAdmin();
    this.canEdit = this.isAdmin || this.accountService.isUser();
    this.canDelete = this.isAdmin;

    // Listen network
    this.offline = this.network.offline;

    this.registerSubscription(this._onRefreshDates.subscribe(() => this.refreshDates()));
    this.registerSubscription(this._onRefreshPmfms.subscribe(() => this.refreshPmfms()));

    this.registerSubscription(filterNotNil(this.$dates).subscribe(() => this.updateColumns()));
  }

  setParent(parent: ObservedLocation) {
    if (!parent) {
      this.setFilter({});
    } else {
      this.startDate = parent.startDateTime;
      this.setFilter({
        observedLocationId: parent.id,
        programLabel: this._program,
        locationId: parent.location.id,
        startDate: parent.startDateTime,
        endDate: parent.endDateTime || moment(parent.startDateTime).add(this._nbDays, "day")
      });
    }
  }

  setFilter(filter: AggregatedLandingFilter, opts?: { emitEvent: boolean }) {

    // Don't refilter if actual filter is equal
    if (AggregatedLandingFilter.equals(this.filter, filter))
      return;

    super.setFilter(filter, opts);
  }

  referentialToString = referentialToString;
  measurementValueToString = MeasurementValuesUtils.valueToString;

  getActivities(row: TableElement<AggregatedLanding>, date: Moment): VesselActivity[] {
    const activities = row.currentData?.vesselActivities.filter(activity => activity.date.isSame(date)) || [];
    return isNotEmptyArray(activities) ? activities : undefined;
  }

  /* -- protected methods -- */
  protected markForCheck() {
    this.cd.markForCheck();
  }

  private refreshDates() {
    if (isNil(this._startDate) || isNil(this._nbDays)) return;

    const dates: Moment[] = [];
    for (let d = 0; d < this._nbDays; d++) {
      dates[d] = moment(this._startDate).add(d, "day");
    }
    this.$dates.next(dates);
  }

  private updateColumns() {
    if (!this.$dates.getValue()) return;
    this.displayedColumns = this.getDisplayColumns();
    if (!this.loading) this.markForCheck();
  }

  protected getDisplayColumns(): string[] {

    const additionalColumns = [];
    if (this.mobile) {
      // add summary column
    } else {
      additionalColumns.push(...(this.$dates.getValue()?.map(date => date.valueOf().toString()) || []));
    }

    return RESERVED_START_COLUMNS
      .concat(this.columns)
      .concat(additionalColumns)
      .concat(RESERVED_END_COLUMNS);
  }

  private async refreshPmfms() {
    if (isNil(this._program) || isNil(this._acquisitionLevel)) return;

    // Load pmfms
    let pmfms = (await this.programService.loadProgramPmfms(
      this._program,
      {
        acquisitionLevel: this._acquisitionLevel
      })) || [];

    if (!pmfms.length && this.debug) {
      console.debug(`[aggregated-landings-table] No pmfm found (program=${this.program}, acquisitionLevel=${this._acquisitionLevel}). Please fill program's strategies !`);
    }

    this.$pmfms.next(pmfms);
  }

  clickRow(event: MouseEvent | undefined, row: TableElement<AggregatedLanding>): boolean {
    if (event && event.defaultPrevented || this.loading) return false;
    if (!this.mobile) return false;

    const today = moment().startOf("day");
    this.setLoading(true);
    this.openModal(event, row, today)
      .then(() => this.setLoading(false));

  }

  clickCell($event: MouseEvent, row: TableElement<AggregatedLanding>, date: Moment) {
    if ($event) $event.stopPropagation();
    if (this.debug)
      console.debug('clickCell', $event, row.currentData.vesselSnapshot.exteriorMarking + "|" + row.currentData.vesselActivities.length, date.toISOString());

    this.setLoading(true);
    this.openModal($event, row, date)
      .then(() => this.setLoading(false));
  }

  async openModal(event: MouseEvent|undefined, row: TableElement<AggregatedLanding>, date?: Moment) {
    this.onEditRow(event, row);
    const modal = await this.modalCtrl.create({
      component: AggregatedLandingModal,
      componentProps: {
        data: row.currentData.clone(),
        options: {
          dates: this.$dates.getValue(),
          initialDate: date,
          program: this._program,
          acquisitionLevel: this._acquisitionLevel
        }
      },
      backdropDismiss: false,
      // cssClass: 'modal-large'
    });

    await modal.present();
    const res = await modal.onDidDismiss();

    if (res && res.data) {

      if (res.data.aggregatedLanding) {
        console.debug('data to update:', res.data.aggregatedLanding);

        row.currentData.vesselActivities.splice(0, row.currentData.vesselActivities.length, ...res.data.aggregatedLanding.vesselActivities);
        // this.markAsDirty();
        this.confirmEditCreate();
        this.markForCheck();
      }

      if (toBoolean(res.data.saveOnDismiss, false)) {
        // call save
        await this.save();
      }

      if (res.data.tripToOpen) {
        // navigate to trip
        this.setLoading(true);
        this.markForCheck();

        try {
          await this.router.navigateByUrl(`/observations/${res.data.tripToOpen.observedLocationId}/trip/${res.data.tripToOpen.tripId}`);
        } finally {
          this.setLoading(false);
          this.markForCheck();
        }
      }
    }
  }

  async addAggregatedRow(vessel: VesselSnapshot) {
    const row = await this.addRowToTable();
    row.currentData.vesselSnapshot = vessel;
    this.markForCheck();
    // TODO scroll to row
    // this.scrollToRow(row);
  }

  async vesselIdsAlreadyPresent(): Promise<number[]> {
    const rows = await this.dataSource.getRows()
    return (rows || []).map(row => row.currentData.vesselSnapshot.id);
  }

  // private scrollToRow(row: TableElement<AggregatedLanding>) {
  //   if (!row) return;
  //   const rect = row._elementRef.nativeElement.getBoundingClientRect();
  //   if ((rect.y <= 0) || ((rect.y+rect.height) > this.table._elementRef.nativeElement.getBoundingClientRect().height))
  //   {
  //     row.element.nativeElement.scrollIntoView();
  //   }
  // }
}

