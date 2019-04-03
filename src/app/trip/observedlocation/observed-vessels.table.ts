import {Component, EventEmitter, Input, OnDestroy, OnInit, Output} from "@angular/core";
import {BehaviorSubject} from 'rxjs';
import {startWith} from "rxjs/operators";
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
  personsToString,
  PmfmStrategy,
  referentialToString,
  Sale,
  Trip,
  vesselFeaturesToString
} from "../services/trip.model";
import {AlertController, ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {Location} from '@angular/common';
import {ProgramService, ReferentialRefService} from "../../referential/referential.module";
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from '@ngx-translate/core';
import {environment} from '../../../environments/environment';
import {MeasurementsValidatorService, SaleValidatorService} from "../services/trip.validators";
import {isNotNil, LoadResult} from "../../shared/shared.module";
import {ObservedLocation} from "../services/observed-location.model";
import {SaleFilter, SaleService} from "../services/sale.service";

const PMFM_ID_REGEXP = /\d+/;
const SALE_RESERVED_START_COLUMNS: string[] = ['vessel', 'startDateTime', 'observers', 'pmfm_2', 'pmfm_3'];
const SALE_RESERVED_END_COLUMNS: string[] = ['comments'];

@Component({
  selector: 'table-observed-vessels',
  templateUrl: 'observed-vessels.table.html',
  styleUrls: ['observed-vessels.table.scss'],
  providers: [
    { provide: ValidatorService, useClass: SaleValidatorService }
  ]
})
export class ObservedVesselsTable extends AppTable<Sale, SaleFilter> implements OnInit, OnDestroy, ValidatorService {

  private _program: string = environment.defaultProgram;
  private _acquisitionLevel: string;
  private _dataSubject = new BehaviorSubject<LoadResult<Sale>>({data: []});
  private _onRefreshPmfms = new EventEmitter<any>();

  loading = true;
  loadingPmfms = true;
  pmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  measurementValuesFormGroupConfig: { [key: string]: any };
  data: Sale[];

  set value(data: Sale[]) {
    if (this.data !== data) {
      this.data = data;
      if (!this.loading) this.onRefresh.emit();
    }
  }

  get value(): Sale[] {
    return this.data;
  }

  protected get dataSubject(): BehaviorSubject<LoadResult<Sale>> {
    return this._dataSubject;
  }

  @Input()
  set program(value: string) {
    if (this._program === value) return; // Skip if same
    this._program = value;
    if (!this.loading) {
      this._onRefreshPmfms.emit('set program');
    }
  }

  get program(): string {
    return this._program;
  }

  @Input()
  set acquisitionLevel(value: string) {
    if (this._acquisitionLevel !== value) {
      this._acquisitionLevel = value;
      if (!this.loading) this.onRefresh.emit();
    }
  }

  get acquisitionLevel(): string {
    return this._acquisitionLevel;
  }

  @Input() showCommentsColumn: boolean = true;

  @Output()
  onSaleClick: EventEmitter<number> = new EventEmitter<number>();

  @Output()
  onNewSaleClick: EventEmitter<void> = new EventEmitter<void>();

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: SaleValidatorService,
    protected dataService: SaleService,
    protected measurementsValidatorService: MeasurementsValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected alertCtrl: AlertController,
    protected programService: ProgramService,
    protected translate: TranslateService,
    protected formBuilder: FormBuilder
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
      RESERVED_START_COLUMNS.concat(SALE_RESERVED_START_COLUMNS).concat(SALE_RESERVED_END_COLUMNS).concat(RESERVED_END_COLUMNS));
    this.setDatasource(new AppTableDataSource<Sale, SaleFilter>(Sale, dataService, this,
      // DataSource options
      {
        prependNewElements: false,
        suppressErrors: false,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      }));
    this.i18nColumnPrefix = 'SALE.TABLE.';
    this.autoLoad = false;
    this.pageSize = 1000; // Do not use paginator
  };

  async ngOnInit() {
    super.ngOnInit();

    let excludesColumns:String[] = new Array<String>();
    if (!this.showCommentsColumn) excludesColumns.push('comments');

    this._onRefreshPmfms
      .pipe(
        startWith('ngOnInit')
      )
      .subscribe((event) => this.refreshPmfms(event));

    this.pmfms
      .filter(pmfms => pmfms && pmfms.length > 0)
      .first()
      .subscribe(pmfms => {
        this.measurementValuesFormGroupConfig = this.measurementsValidatorService.getFormGroupConfig(pmfms);
        let pmfmColumns = pmfms.map(p => p.pmfmId.toString());

        this.displayedColumns = RESERVED_START_COLUMNS
          .concat(SALE_RESERVED_START_COLUMNS)
          .concat(pmfmColumns)
          .concat(SALE_RESERVED_END_COLUMNS)
          .concat(RESERVED_END_COLUMNS)
          // Remove columns to hide
          .filter(column => !excludesColumns.includes(column));

        this.loading = false;

        if (this.data) this.onRefresh.emit();
      });

    // Set default acquisition Level
    if (isNil(this._acquisitionLevel)) {
      this.acquisitionLevel = AcquisitionLevelCodes.SALE;
    }
  }

  setParent(data: ObservedLocation|Trip) {
    if (data) {
      if (data instanceof ObservedLocation) {
        this.setParentData({observedLocationId: data.id});
      }
      else if (data instanceof Trip) {
        this.setParentData({tripId: data.id});
      }
    }
  }

  setParentData(parent: any) {
    this.filter = Object.assign( this.filter || {}, parent);
    this.dataSource.serviceOptions = Object.assign( this.dataSource.serviceOptions || {}, parent);
    if (isNotNil(parent)) {
      this.onRefresh.emit();
    }
  }

  getRowValidator(): FormGroup {
    const formGroup = this.validatorService.getRowValidator();
    if (this.measurementValuesFormGroupConfig) {
      formGroup.addControl('measurementValues', this.formBuilder.group(this.measurementValuesFormGroupConfig));
    }
    return formGroup;
  }

  async deleteSelection(confirm?: boolean): Promise<void> {
    if (this.loading) { return; }

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


  /* -- protected methods -- */

  protected async openEditRowDetail(id: number): Promise<boolean> {
    if (this.onSaleClick.observers.length) {
      this.onSaleClick.emit(id);
      return true;
    }

    if (this.filter) {
      if (isNotNil(this.filter.observedLocationId)) {
        return await this.router.navigateByUrl('/observations/' + this.filter.observedLocationId + '/' + id);
      } else if (isNotNil(this.filter.tripId)) {
        return await this.router.navigateByUrl('/trips/' + this.filter.tripId + '/sales/' + id); // FIXME
      }
    }
  }

  protected async openNewRowDetail(): Promise<boolean> {
    if (this.onNewSaleClick.observers.length) {
      this.onNewSaleClick.emit();
      return true;
    }

    if (this.filter) {
      if (isNotNil(this.filter.observedLocationId)) {
        return await this.router.navigateByUrl('/observations/' + this.filter.observedLocationId + '/new');
      } else if (isNotNil(this.filter.tripId)) {
        return await this.router.navigateByUrl('/trips/' + this.filter.tripId + '/sales/new'); // FIXME
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
    const candLoadPmfms = isNotNil(this._program) && isNotNil(this._acquisitionLevel);
    if (!candLoadPmfms) {
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
      console.debug(`[sale-table] No pmfm found (program=${this.program}, acquisitionLevel=${this._acquisitionLevel}). Please fill program's strategies !`);
    }

    this.loadingPmfms = false;

    this.pmfms.next(pmfms);

    return pmfms;
  }

  referentialToString = referentialToString;
  getPmfmColumnHeader = getPmfmName;
  vesselFeaturesToString = vesselFeaturesToString;
  personsToString = personsToString;
}

