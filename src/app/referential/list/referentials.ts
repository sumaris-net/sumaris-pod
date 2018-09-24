import { Component, OnInit, OnDestroy } from "@angular/core";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { ValidatorService } from "angular4-material-table";
import { AppTableDataSource, AppTable } from "../../core/core.module";
import { ReferentialValidatorService } from "../validator/validators";
import { ReferentialService, ReferentialFilter } from "../services/referential-service";
import { Referential, StatusIds } from "../services/model";
import { ModalController, Platform } from "@ionic/angular";
import { Router, ActivatedRoute } from "@angular/router";
import { VesselService } from '../services/vessel-service';
import { AccountService } from '../../core/services/account.service';
import { Location } from '@angular/common';
import { FormGroup, FormBuilder } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { RESERVED_START_COLUMNS, RESERVED_END_COLUMNS } from "../../core/table/table.class";

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

  entityNameForm: FormGroup;
  filterForm: FormGroup;
  entities: Observable<{ id: string, label: string, level?: string, levelLabel?: string }[]>;
  levels: Observable<Referential[]>;
  statusList: any[] = [
    {
      id: StatusIds.ENABLE,
      icon: 'checkmark',
      label: 'REFERENTIAL.STATUS_ENABLE'
    },
    {
      id: StatusIds.DISABLE,
      icon: 'warning',
      label: 'REFERENTIAL.STATUS_DISABLE'
    },
    {
      id: StatusIds.TEMPORARY,
      icon: 'code-working',
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
    protected vesselService: VesselService,
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
        serviceOptions: {
          full: true,
          saveOnlyDirtyRows: true,
          entityName: null // will be set from route parameters
        }
      })
    );
    //this.autoLoad = false;
    this.inlineEdition = true; // always inline edition
    this.i18nColumnPrefix = 'REFERENTIAL.';

    this.entityNameForm = formBuilder.group({
      'entityName': ['']
    });
    this.filterForm = formBuilder.group({
      'searchText': [null]
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
        this.dataSource.serviceOptions = this.dataSource.serviceOptions || { full: true };
        this.dataSource.serviceOptions.entityName = entityName;
        this.entityNameForm.setValue({ entityName: entityName });
      }
    });

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);
    this.autoLoad = false;
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
    if (this.dataSource.serviceOptions && this.dataSource.serviceOptions.entityName) {
      console.info("[referential] Loading " + this.dataSource.serviceOptions.entityName + "...");
      // Load levels
      this.levels = this.referentialService.loadLevels(this.dataSource.serviceOptions.entityName)
      // then load list
      this.onRefresh.emit();
    }
  }

  onEntityNameChange(entityName: string) {
    // No change: skip
    if (this.dataSource.serviceOptions.entityName === entityName) return;

    entityName = entityName || this.dataSource.serviceOptions.entityName;

    console.info("[referential] Loading " + entityName + "...");

    // Set the service options
    this.dataSource.serviceOptions = this.dataSource.serviceOptions || { full: true };
    this.dataSource.serviceOptions.entityName = entityName;
    this.onRefresh.emit();
    this.levels = this.referentialService.loadLevels(entityName);

    this.router.navigate([entityName], {
      relativeTo: this.route.parent,
      skipLocationChange: false
    });
  }

  debugRow(row: any): string {
    //console.log(row);
    console.log(row.currentData);
    return row.currentData.name;
  }
}

