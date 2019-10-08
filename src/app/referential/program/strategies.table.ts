import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {
  AppTable,
  AppTableDataSource,
  environment, isNotNil, referentialToString,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS, StatusIds
} from "../../core/core.module";
import {StrategyValidatorService} from "../services/validator/strategy.validator";
import {Strategy} from "../services/model";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {isEmptyArray} from "../../shared/functions";
import {AccountService} from "../../core/services/account.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";

export declare interface StrategyFilter {
}

@Component({
  selector: 'app-strategy-table',
  templateUrl: 'strategies.table.html',
  styleUrls: ['strategies.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: StrategyValidatorService},
    {
      provide: InMemoryTableDataService,
      useFactory: () => new InMemoryTableDataService<Strategy, StrategyFilter>(Strategy, {})
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategiesTable extends AppTable<Strategy, StrategyFilter> implements OnInit, OnDestroy {

  statusList: any[] = [
    {
      id: StatusIds.ENABLE,
      icon: 'checkmark',
      label: 'REFERENTIAL.STATUS_ENUM.ENABLE'
    },
    {
      id: StatusIds.DISABLE,
      icon: 'close',
      label: 'REFERENTIAL.STATUS_ENUM.DISABLE'
    },
    {
      id: StatusIds.TEMPORARY,
      icon: 'warning',
      label: 'REFERENTIAL.STATUS_ENUM.TEMPORARY'
    }
  ];
  statusById: any;

  @Input() canEdit = false;
  @Input() canDelete = false;

  set value(data: Strategy[]) {
    const firstCall = isEmptyArray(this.memoryDataService.value);
    this.memoryDataService.value = data;
    if (firstCall) {
      this.onRefresh.emit();
    }
  }

  get value(): Strategy[] {
    return this.memoryDataService.value;
  }

  get dirty(): boolean {
    return this._dirty && this.memoryDataService.dirty;
  }

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected validatorService: ValidatorService,
    protected memoryDataService: InMemoryTableDataService<Strategy, StrategyFilter>,
    protected cd: ChangeDetectorRef,
    protected injector: Injector
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'label',
          'name',
          'description',
          'status',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Strategy, StrategyFilter>(Strategy, memoryDataService, validatorService),
      {},
      injector);

    this.i18nColumnPrefix = 'REFERENTIAL.';
    this.autoLoad = false; // waiting parent to load
    this.inlineEdition = true;
    this.confirmBeforeDelete = true;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();
  }

  async deleteSelection(confirm?: boolean): Promise<void> {

    await super.deleteSelection(confirm);

    if (confirm) {
      this._dirty = true;
    }
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

