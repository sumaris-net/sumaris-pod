import {Component, OnDestroy, OnInit} from "@angular/core";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {ValidatorService} from "angular4-material-table";
import {AppTable, AppTableDataSource, isNotNil} from "../../core/core.module";
import {ReferentialValidatorService} from "../services/referential.validator";
import {ReferentialFilter, ReferentialService} from "../services/referential.service";
import {Referential, ReferentialRef, StatusIds} from "../services/model";
import {ModalController, Platform} from "@ionic/angular";
import {ActivatedRoute, Router} from "@angular/router";
import {AccountService} from '../../core/services/account.service';
import {Location} from '@angular/common';
import {FormBuilder, FormGroup} from "@angular/forms";
import {TranslateService} from "@ngx-translate/core";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";


const DEFAULT_ENTITY_NAME = "Location";

@Component({
  selector: 'page-referentials',
  templateUrl: 'referentials.html',
  styleUrls: ['referentials.scss'],
  providers: [
    { provide: ValidatorService, useClass: ReferentialValidatorService }
  ],
})
export class ReferentialsPage extends AppTable<Referential, ReferentialFilter> implements OnInit, OnDestroy {

  protected entityName: string;

  showLevelColumn = true;
  filterForm: FormGroup;
  entities: Observable<{ id: string, label: string, level?: string, levelLabel?: string }[]>;
  levels: Observable<ReferentialRef[]>;
  statusList: any[] = [
    {
      id: StatusIds.ENABLE,
      icon: 'checkmark',
      label: 'REFERENTIAL.STATUS_ENABLE'
    },
    {
      id: StatusIds.DISABLE,
      icon: 'close',
      label: 'REFERENTIAL.STATUS_DISABLE'
    },
    {
      id: StatusIds.TEMPORARY,
      icon: 'warning',
      label: 'REFERENTIAL.STATUS_TEMPORARY'
    }
  ];
  statusById; any;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected platform: Platform,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected validatorService: ReferentialValidatorService,
    protected referentialService: ReferentialService,
    protected formBuilder: FormBuilder,
    protected translate: TranslateService
  ) {
    super(route, router, platform, location, modalCtrl, accountService,
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
        suppressErrors: false,
        serviceOptions: {
          saveOnlyDirtyRows: true
        }
      })
    );

    this.allowRowDetail = false;

    // Allow inline edition only if admin
    this.inlineEdition = accountService.isAdmin();

    this.i18nColumnPrefix = 'REFERENTIAL.';

    this.filterForm = formBuilder.group({
      'entityName': [''],
      'searchText': [null],
      'levelId': [null],
      'statusId': [null]
    });

    this.route.params.subscribe(res => {
      const entityName = res["entityName"];
      if (!entityName) {
        this.router.navigate([DEFAULT_ENTITY_NAME], {
          relativeTo: this.route,
          skipLocationChange: false
        });
      }
      else {
        this.route.queryParams.first().subscribe(queryParams => {
          this.filterForm.setValue({
            entityName: entityName,
            searchText: queryParams["q"] || null,
            levelId: isNotNil(queryParams["level"]) ? parseInt(queryParams["level"]) : null,
            statusId: isNotNil(queryParams["status"]) ? parseInt(queryParams["status"]) : null
          });
        });
      }
    });

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);
    this.autoLoad = false;

    // FOR DEV ONLY
    this.debug = true;
  };

  ngOnInit() {

    super.ngOnInit();

    // Combo entities
    this.entities = this.referentialService.loadTypes()
      .first()
      .pipe(
        map((types) => types.map(type => {
          return {
            id: type.id,
            label: ('REFERENTIAL.ENTITY.' + type.id.replace(/([a-z])([A-Z])/g, "$1_$2").toUpperCase()),
            level: type.level,
            levelLabel: type.level && ('REFERENTIAL.ENTITY.' + type.level.replace(/([a-z])([A-Z])/g, "$1_$2").toUpperCase())
          };
        }))
      );

    // Update filter when changes
    this.filterForm.valueChanges.subscribe(() => {
      this.filter = this.filterForm.value;
    });
    this.onRefresh.subscribe(() => {
      this.filterForm.markAsUntouched();
      this.filterForm.markAsPristine();
    });

    // Only if entityName has been select:  load data
    this.filter = this.filterForm.value;
    this.entityName = this.filter.entityName;
    if (this.entityName) {
      // Load levels, then refresh
      this.loadLevels(this.entityName)
        .then(() => this.onRefresh.emit())
    }
  }

  async onEntityNameChange(entityName: string): Promise<any> {
    // No change: skip
    if (this.entityName === entityName) return;

    this.entityName = entityName || this.filterForm.controls['entityName'].value;

    // Load levels
    await this.loadLevels(entityName);

    console.info(`[referential] Loading ${entityName}...`);
    this.onRefresh.emit();

    this.router.navigate(['..', entityName], {
      relativeTo: this.route,
      skipLocationChange: false
    });
  }

  addRow(): boolean {
    // Create new row
    const result = super.addRow();
    if (!result) return result;

    const row = this.dataSource.getRow(-1);
    row.validator.controls['entityName'].setValue(this.entityName);
    return true;
  }

  async loadLevels(entityName: string): Promise<Referential[]> {
    const res = await this.referentialService.loadLevels(entityName, {
      fetchPolicy: 'network-only'
    });
    this.levels = Observable.of(res);
    this.showLevelColumn = res && res.length > 0;
    return res
  }
}

