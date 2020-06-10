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
import {isEmptyArray, toBoolean} from "../../shared/functions";
import {AccountService} from "../../core/services/account.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {DefaultStatusList} from "../../core/services/model";
import {AppInMemoryTable} from "../../core/table/memory-table.class";

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
export class StrategiesTable extends AppInMemoryTable<Strategy, StrategyFilter> implements OnInit, OnDestroy {

  statusList = DefaultStatusList;
  statusById: any;

  @Input() canEdit = false;
  @Input() canDelete = false;


  constructor(
    protected injector: Injector,
    protected memoryDataService: InMemoryTableDataService<Strategy, StrategyFilter>,
    protected validatorService: ValidatorService,
    protected cd: ChangeDetectorRef
  ) {
    super(injector,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'label',
          'name',
          'description',
          'status',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      Strategy,
      memoryDataService,
      validatorService,
      null,
      {});

    this.i18nColumnPrefix = 'REFERENTIAL.';
    this.autoLoad = false; // waiting parent to load

    this.confirmBeforeDelete = true;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.debug = !environment.production;
  }

  ngOnInit() {
    this.inlineEdition = toBoolean(this.inlineEdition, true);
    super.ngOnInit();

    console.log("TODO check inlineEdition:" + this.inlineEdition);
  }


  setValue(value: Strategy[]) {
    super.setValue(value);
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

