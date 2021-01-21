import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Inject,
  Injector,
  Input,
  OnDestroy,
  OnInit
} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {StrategyValidatorService} from "../services/validator/strategy.validator";
import {Strategy} from "../services/model/strategy.model";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {toBoolean} from "../../shared/functions";
import {DefaultStatusList} from "../../core/services/model/referential.model";
import {AppInMemoryTable} from "../../core/table/memory-table.class";
import {StrategyFilter, StrategyService} from "../services/strategy.service";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {Program} from "../services/model/program.model";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {environment} from "../../../environments/environment";


@Component({
  selector: 'app-strategy-table',
  templateUrl: 'strategies.table.html',
  styleUrls: ['strategies.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: StrategyValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategiesTable extends AppTable<Strategy, StrategyFilter> implements OnInit, OnDestroy {

  statusList = DefaultStatusList;
  statusById: any;

  @Input() canEdit = false;
  @Input() canDelete = false;

  constructor(
    route: ActivatedRoute,
    router: Router,
    platform: Platform,
    location: Location,
    modalCtrl: ModalController,
    localSettingsService: LocalSettingsService,
    injector: Injector,
    dataService: StrategyService,
    validatorService: ValidatorService,
    protected cd: ChangeDetectorRef,
  ) {
    super(route,
      router,
      platform,
      location,
      modalCtrl,
      localSettingsService,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'label',
          'name',
          'description',
          'status',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      new EntitiesTableDataSource(Strategy, dataService, environment, validatorService, {
        prependNewElements: false,
        suppressErrors: false,
        dataServiceOptions: {
          saveOnlyDirtyRows: false
        }
      }),
      null,
      injector);

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
  }

  setProgram(program: Program) {
    console.debug('[strategy-table] Setting program:', program);
    this.setFilter( {
      levelId: program && program.id
    });
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

