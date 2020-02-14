import {Component, Injector, OnDestroy, OnInit} from "@angular/core";
import {BehaviorSubject, Observable, of} from "rxjs";
import {filter, first, map} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {AppTable, AppTableDataSource, environment, isNil, isNotNil} from "../../core/core.module";
import {ReferentialValidatorService} from "../services/referential.validator";
import {ReferentialFilter, ReferentialService} from "../services/referential.service";
import {Referential, ReferentialRef} from "../services/model";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {AccountService} from '../../core/services/account.service';
import {Location} from '@angular/common';
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {sort, DefaultStatusList} from "../../core/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {isNotNilOrBlank} from "../../shared/functions";


@Component({
  selector: 'page-referentials',
  templateUrl: 'referentials.html',
  styleUrls: ['referentials.scss'],
  providers: [
    {provide: ValidatorService, useExisting: ReferentialValidatorService}
  ],
})
export class ReferentialsPage extends AppTable<Referential, ReferentialFilter> implements OnInit, OnDestroy {

  static DEFAULT_ENTITY_NAME = "Program";

  protected entityName: string;

  canEdit = false;
  showLevelColumn = true;
  filterForm: FormGroup;
  $entity = new BehaviorSubject<{ id: string, label: string, level?: string, levelLabel?: string }>(undefined);
  $entities = new BehaviorSubject<{ id: string, label: string, level?: string, levelLabel?: string }[]>(undefined);
  levels: Observable<ReferentialRef[]>;
  statusList = DefaultStatusList;
  statusById: any;

  canOpenDetail = false;
  detailsPath = {
    'Program': '/referential/program/:id',
    'Software': '/referential/software/:id?label=:label'
  };

  constructor(
    protected injector: Injector,
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected settings: LocalSettingsService,
    protected validatorService: ReferentialValidatorService,
    protected referentialService: ReferentialService,
    protected formBuilder: FormBuilder,
    protected translate: TranslateService
  ) {
    super(route, router, platform, location, modalCtrl, settings,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'label',
          'name',
          'level',
          'status',
          'comments'])
        .concat(RESERVED_END_COLUMNS),
      new AppTableDataSource<Referential, ReferentialFilter>(Referential, referentialService, validatorService, {
        prependNewElements: false,
        suppressErrors: environment.production,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      }),
      null,
      injector
    );

    this.allowRowDetail = false;
    this.confirmBeforeDelete = true;

    // Allow inline edition only if admin
    this.inlineEdition = accountService.isAdmin();
    this.canEdit = accountService.isAdmin();

    this.i18nColumnPrefix = 'REFERENTIAL.';

    this.filterForm = formBuilder.group({
      'entityName': [null],
      'searchText': [null],
      'levelId': [null],
      'statusId': [null]
    });

    // Listen route parameters
    this.registerSubscription(
      this.route.queryParams
        .pipe(first()) // Do not refresh after the first page load (e.g. when location query path changed)
        .subscribe(({entity, q, level, status}) => {
          if (!entity) {
            this.setEntityName(ReferentialsPage.DEFAULT_ENTITY_NAME);
          } else {
            this.filterForm.setValue({
              entityName: entity,
              searchText: q || null,
              levelId: isNotNil(level) ? +level : null,
              statusId: isNotNil(status) ? +status : null
            });
            this.setEntityName(entity, {skipLocationChange: true});
          }
        }));

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);
    this.autoLoad = false;

    // FOR DEV ONLY
    this.debug = true;
  };

  ngOnInit() {

    super.ngOnInit();

    // Load entities
    this.registerSubscription(
      this.referentialService.loadTypes()
        .pipe(
          map(types => types.map(type => {
            return {
              id: type.id,
              label: this.getI18nEntityName(type.id),
              level: type.level,
              levelLabel: this.getI18nEntityName(type.level)
            };
          })),
          map(types => sort(types, 'label'))
        )
        .subscribe(types => this.$entities.next(types))
    );

    // Update filter when changes
    this.filterForm.valueChanges.subscribe(() => {
      this.setFilter(this.filterForm.value, {emitEvent: false});
    });
    this.onRefresh.subscribe(() => {
      this.filterForm.markAsUntouched();
      this.filterForm.markAsPristine();
    });

    // // Only if entityName has been select:  load data
    // this.filter = this.filterForm.value;
    // this.entityName = this.filter.entityName;
    // if (this.entityName) {
    //   this.setEntityName(this.entityName, {skipLocationChange: true});
    //   // Load levels, then refresh
    //   this.loadLevels(this.entityName)
    //     .then(() => this.onRefresh.emit());
    // }
  }

  async setEntityName(entityName: string, opts?: { emitEvent?: boolean; skipLocationChange?: boolean }) {
    opts = opts || {emitEvent: true, skipLocationChange: false};
    // No change: skip
    if (this.entityName === entityName) return;

    this.canOpenDetail = false;

    // Wait end of entity loading
    const entities = this.$entities.getValue();
    if (isNil(entities) || !entities.length) {
      console.debug("[referential] Waiting entities to be loaded...");
      return this.$entities.pipe(filter(isNotNil), first())
      // Loop
        .subscribe((entities) => {
          this.setEntityName(entityName);
        });
    }

    const entity = entities.find(e => e.id === entityName);
    if (!entity) {
      throw new Error(`[referential] Entity {${entityName}} not found !`);
    }

    this.entityName = entityName;
    this.$entity.next(entity);
    this.filterForm.get('entityName').setValue(entityName);
    this.setFilter(this.filterForm.value, {emitEvent: false});

    // Load levels
    await this.loadLevels(entityName);

    this.canOpenDetail = !!this.detailsPath[entityName];
    this.inlineEdition = !this.canOpenDetail;

    if (opts.emitEvent !== false) {
      console.info(`[referential] Loading ${entityName}...`);
      this.onRefresh.emit();
    }

    if (opts.skipLocationChange === false) {
      this.router.navigate(['.'], {
        relativeTo: this.route,
        skipLocationChange: false,
        queryParams: {
          entity: entityName
        }
      });
    }
  }

  async onEntityNameChange(entityName: string): Promise<any> {
    // No change: skip
    if (this.entityName === entityName) return;
    this.setEntityName(entityName);
  }

  addRow(event?: any): boolean {
    // Create new row
    const result = super.addRow(event);
    if (!result) return result;

    const row = this.dataSource.getRow(-1);
    row.validator.controls['entityName'].setValue(this.entityName);
    return true;
  }

  async loadLevels(entityName: string): Promise<Referential[]> {
    const res = await this.referentialService.loadLevels(entityName, {
      fetchPolicy: 'network-only'
    });

    this.levels = of(res);
    this.showLevelColumn = res && res.length > 0;

    return res;
  }

  getI18nEntityName(entityName: string, self?: ReferentialsPage): string {
    self = self || this;

    if (isNil(entityName)) return undefined;

    const tableName = entityName.replace(/([a-z])([A-Z])/g, "$1_$2").toUpperCase();
    const key = `REFERENTIAL.ENTITY.${tableName}`;
    let message = self.translate.instant(key);

    if (message !== key) return message;
    // No I18n translation: continue

    // Use tableName, but replace underscore with space
    message = tableName.replace(/[_-]+/g, " ").toUpperCase() || '';
    // First letter as upper case
    if (message.length > 1) {
      return message.substring(0, 1) + message.substring(1).toLowerCase();
    }
    return message;
  }

  async openRow(id: number, row: TableElement<Referential>): Promise<boolean> {
    const path = this.detailsPath[this.entityName];

    if (isNotNilOrBlank(path)) {
      await this.router.navigateByUrl(
        path
          // Replace the id in the path
          .replace(':id', isNotNil(row.currentData.id) ? row.currentData.id.toString() : '')
          // Replace the label in the path
          .replace(':label', row.currentData.label || '')
      );
      return true;
    }

    return super.openRow(id, row);
  }

  protected async openNewRowDetail(): Promise<boolean> {
    const path = this.detailsPath[this.entityName];

    if (path) {
      await this.router.navigateByUrl(path
        .replace(':id', "new")
        .replace(':label', ""));
      return true;
    }

    return super.openNewRowDetail();
  }
}

