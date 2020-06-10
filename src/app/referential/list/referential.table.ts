import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {
  AppTable,
  AppTableDataSource,
  environment,
  referentialToString,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS, StatusIds
} from "../../core/core.module";
import {PmfmStrategy, Referential} from "../services/model";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {isEmptyArray} from "../../shared/functions";
import {AccountService} from "../../core/services/account.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {DefaultStatusList} from "../../core/services/model";
import {ReferentialValidatorService} from "../services/referential.validator";
import {ReferentialFilter} from "../services/referential.service";
import {AppInMemoryTable} from "../../core/table/memory-table.class";
import {PmfmStrategyFilter} from "../strategy/pmfm-strategies.table";


@Component({
  selector: 'app-referential-table',
  templateUrl: 'referential.table.html',
  styleUrls: ['referential.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: ReferentialValidatorService},
    {
      provide: InMemoryTableDataService,
      useFactory: () => new InMemoryTableDataService<Referential, ReferentialFilter>(Referential, {})
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ReferentialTable extends AppInMemoryTable<Referential, ReferentialFilter> {

  statusList = DefaultStatusList;
  statusById: any;

  @Input() set entityName(entityName: string) {
    this.setFilter({
      ...this.filter,
      entityName
    });
  }

  get entityName(): string {
    return this.filter.entityName;
  }

  @Input() canEdit = false;
  @Input() canDelete = false;


  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected validatorService: ValidatorService,
    protected memoryDataService: InMemoryTableDataService<Referential, ReferentialFilter>,
    protected cd: ChangeDetectorRef,
    protected injector: Injector
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
      Referential,
      memoryDataService,
      validatorService,
      {
        onRowCreated: (row) => this.onRowCreated(row),
        prependNewElements: false,
        suppressErrors: true
      },
      {
        entityName: 'Program'
      });

    this.i18nColumnPrefix = 'REFERENTIAL.';
    this.autoLoad = false; // waiting parent to load
    this.inlineEdition = true;
    this.confirmBeforeDelete = true;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.debug = !environment.production;
  }

  async deleteSelection(confirm?: boolean): Promise<void> {

    await super.deleteSelection(confirm);

    if (confirm) {
      this._dirty = true;
    }
  }

  protected onRowCreated(row) {
    const defaultValues = {
      entityName: this.entityName
    };
    if (row.validator) {
      row.validator.patchValue(defaultValues);
    }
    else {
      Object.assign(row.currentData, defaultValues);
    }
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

