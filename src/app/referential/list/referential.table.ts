import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Injector, Input} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {DefaultStatusList, Referential} from "../../core/services/model/referential.model";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService} from "../../core/services/account.service";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {ReferentialValidatorService} from "../services/validator/referential.validator";
import {ReferentialFilter} from "../services/referential.service";
import {AppInMemoryTable} from "../../core/table/memory-table.class";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {EnvironmentService} from "../../../environments/environment.class";


@Component({
  selector: 'app-referential-table',
  templateUrl: 'referential.table.html',
  styleUrls: ['referential.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: ReferentialValidatorService},
    {
      provide: InMemoryEntitiesService,
      useFactory: () => {
        return new InMemoryEntitiesService<Referential, ReferentialFilter>(Referential);
      }
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

  @Input() set showUpdateDateColumn(value: boolean) {
    this.setShowColumn('updateDate', value);
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
    protected memoryDataService: InMemoryEntitiesService<Referential, ReferentialFilter>,
    protected cd: ChangeDetectorRef,
    protected injector: Injector,
    @Inject(EnvironmentService) protected environment
  ) {
    super(injector,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'label',
          'name',
          'description',
          'status',
          'updateDate',
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
    this.showUpdateDateColumn = false;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.debug = !environment.production;
  }

  protected onRowCreated(row: TableElement<Referential>) {
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

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

