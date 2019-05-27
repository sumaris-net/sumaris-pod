import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from "@angular/core";
import {BehaviorSubject} from 'rxjs';
import {filter, first, startWith} from "rxjs/operators";
import {ValidatorService} from "angular4-material-table";
import {
  AccountService,
  AcquisitionLevelCodes,
  AppTable,
  AppTableDataSource,
  isNil,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {
  getPmfmName,
  ObservedLocation,
  ObservedVessel,
  personsToString,
  PmfmStrategy,
  referentialToString,
  Trip,
  vesselFeaturesToString
} from "../services/trip.model";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {ProgramService, ReferentialRefService} from "../../referential/referential.module";
import {FormBuilder} from "@angular/forms";
import {TranslateService} from '@ngx-translate/core';
import {MeasurementsValidatorService, SaleValidatorService} from "../services/trip.validators";
import {isNotNil, LoadResult} from "../../shared/shared.module";
import {ObservedVesselFilter, ObservedVesselService} from "../services/observed-vessel.service";

const PMFM_ID_REGEXP = /\d+/;
const OBSERVED_VESSEL_RESERVED_START_COLUMNS: string[] = ['vessel', 'dateTime', 'observers'];
const OBSERVED_VESSEL_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'table-observed-vessels',
  templateUrl: 'observed-vessels.table.html',
  styleUrls: ['observed-vessels.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: SaleValidatorService}
  ]
})
export class ObservedVesselsTable extends AppTable<ObservedVessel, ObservedVesselFilter> implements OnInit, OnDestroy {

  private _program: string;
  private _acquisitionLevel: string;
  private _dataSubject = new BehaviorSubject<LoadResult<ObservedVessel>>({data: []});
  private _onRefreshPmfms = new EventEmitter<any>();

  loading = false;
  loadingPmfms = true;
  pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  measurementValuesFormGroupConfig: { [key: string]: any };
  data: ObservedVessel[];
  excludesColumns = new Array<String>();

  set value(data: ObservedVessel[]) {
    if (this.data !== data) {
      this.data = data;
      if (!this.loading) this.onRefresh.emit();
    }
  }

  get value(): ObservedVessel[] {
    return this.data;
  }

  protected get dataSubject(): BehaviorSubject<LoadResult<ObservedVessel>> {
    return this._dataSubject;
  }

  @Input()
  set program(value: string) {
    if (this._program !== value && isNotNil(value)) {
      this._program = value;
      if (!this.loading) this._onRefreshPmfms.emit('set program');
    }
  }

  get program(): string {
    return this._program;
  }

  @Input()
  set acquisitionLevel(value: string) {
    if (this._acquisitionLevel !== value && isNotNil(value)) {
      this._acquisitionLevel = value;
      if (!this.loading) this._onRefreshPmfms.emit();
    }
  }

  get acquisitionLevel(): string {
    return this._acquisitionLevel;
  }

  @Input() showCommentsColumn = true;

  @Output()
  onObservedVesselClick: EventEmitter<number> = new EventEmitter<number>();

  @Output()
  onNewObservedVesselClick: EventEmitter<void> = new EventEmitter<void>();

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected dataService: ObservedVesselService,
    protected measurementsValidatorService: MeasurementsValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected alertCtrl: AlertController,
    protected programService: ProgramService,
    protected translate: TranslateService,
    protected formBuilder: FormBuilder
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
      RESERVED_START_COLUMNS.concat(OBSERVED_VESSEL_RESERVED_START_COLUMNS).concat(OBSERVED_VESSEL_RESERVED_END_COLUMNS).concat(RESERVED_END_COLUMNS));
    this.setDatasource(new AppTableDataSource<ObservedVessel, ObservedVesselFilter>(ObservedVessel, dataService, null,
      // DataSource options
      {
        prependNewElements: false,
        suppressErrors: false,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      }));
    this.i18nColumnPrefix = 'OBSERVED_VESSEL.TABLE.';
    this.autoLoad = false;
    this.pageSize = 1000; // Do not use paginator
  }

  async ngOnInit() {
    super.ngOnInit();

    if (!this.showCommentsColumn) this.excludesColumns.push('comments');

    this._onRefreshPmfms
      .pipe(startWith('ngOnInit'))
      .subscribe((event) => this.refreshPmfms(event));

    this.pmfms
      .pipe(
        filter(isNotNil),
        first()
      )
      .subscribe(pmfms => {
        this.measurementValuesFormGroupConfig = this.measurementsValidatorService.getFormGroupConfig(pmfms);
        let pmfmColumns = pmfms.map(p => p.pmfmId.toString());

        this.displayedColumns = RESERVED_START_COLUMNS
          .concat(OBSERVED_VESSEL_RESERVED_START_COLUMNS)
          .concat(pmfmColumns)
          .concat(OBSERVED_VESSEL_RESERVED_END_COLUMNS)
          .concat(RESERVED_END_COLUMNS)
          // Remove columns to hide
          .filter(column => !this.excludesColumns.includes(column));

        this.loading = false;

        if (this.data) this.onRefresh.emit();
      });

    // Set default acquisition Level
    if (isNil(this._acquisitionLevel)) {
      this.acquisitionLevel = AcquisitionLevelCodes.OBSERVED_VESSEL;
    }
  }

  setParent(data: ObservedLocation | Trip) {
    if (data) {
      if (data instanceof ObservedLocation) {
        this.setParentData({observedLocationId: data.id});
      } else if (data instanceof Trip) {
        this.setParentData({tripId: data.id});
      }
    }
  }

  setParentData(parent: any) {
    this.filter = Object.assign(this.filter || {}, parent);
    this.dataSource.serviceOptions = Object.assign(this.dataSource.serviceOptions || {}, parent);
    if (isNotNil(parent)) {
      this.onRefresh.emit();
    }
  }

  async deleteSelection(confirm?: boolean): Promise<void> {
    if (this.loading) {
      return;
    }

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


  /* -- protected methods -- */

  protected async openEditRowDetail(id: number): Promise<boolean> {
    if (this.onObservedVesselClick.observers.length) {
      this.onObservedVesselClick.emit(id);
      return true;
    }

    if (this.filter) {
      if (isNotNil(this.filter.observedLocationId)) {
        console.log("TODO: openEditRowDetail() ?")
        //return await this.router.navigateByUrl('/observations/' + this.filter.observedLocationId + '/' + id);
      }
    }
  }

  protected async openNewRowDetail(): Promise<boolean> {
    if (this.onNewObservedVesselClick.observers.length) {
      this.onNewObservedVesselClick.emit();
      return true;
    }

    if (this.filter) {
      if (isNotNil(this.filter.observedLocationId)) {
        console.log("TODO: openNewRowDetail() ?");
        //return await this.router.navigateByUrl('/observations/' + this.filter.observedLocationId + '/new');
      }
    }
  }

  protected getI18nColumnName(columnName: string): string {

    // Try to resolve PMFM column, using the cached pmfm list
    if (PMFM_ID_REGEXP.test(columnName)) {
      const pmfmId = parseInt(columnName);
      const pmfm = (this.pmfms.getValue() || []).find(p => p.pmfmId === pmfmId);
      if (pmfm) return pmfm.name;
    }

    return super.getI18nColumnName(columnName);
  }

  protected async refreshPmfms(event?: any): Promise<PmfmStrategy[]> {
    if (isNil(this._program) || isNil(this._acquisitionLevel)) {
      return undefined;
    }

    this.loading = true;
    this.loadingPmfms = true;

    // Load pmfms
    const pmfms = (await this.programService.loadProgramPmfms(
      this._program,
      {
        acquisitionLevel: this._acquisitionLevel
      })) || [];

    if (!pmfms.length && this.debug) {
      console.debug(`[observed-vessels-table] No pmfm found (program=${this.program}, acquisitionLevel=${this._acquisitionLevel}). Please fill program's strategies !`);
    }

    this.loadingPmfms = false;

    this.pmfms.next(pmfms);

    this.markForCheck();

    return pmfms;
  }

  referentialToString = referentialToString;
  getPmfmColumnHeader = getPmfmName;
  vesselFeaturesToString = vesselFeaturesToString;
  personsToString = personsToString;
}

