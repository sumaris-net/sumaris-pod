import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Injector, Input} from "@angular/core";
import {Strategy} from "../services/model/strategy.model";
import {DefaultStatusList, Referential} from "../../core/services/model/referential.model";
import {StrategyService} from "../services/strategy.service";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {ENVIRONMENT} from "../../../environments/environment.class";
import {fromDateISOString} from "../../shared/dates";
import {Program} from "../services/model/program.model";
import {firstArrayValue, isEmptyArray, isNotNil} from "../../shared/functions";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {PmfmStrategy} from "../services/model/pmfm-strategy.model";
import {PmfmUtils} from "../services/model/pmfm.model";
import {LocationLevelIds, ParameterLabel, ParameterLabelGroups, TaxonomicLevelIds} from "../services/model/model.enum";
import {PredefinedColors} from "@ionic/core";
import {ReferentialFilter} from "../services/referential.service";
import {DenormalizedStrategy, DenormalizedStrategyService, StrategyEffort} from "./denormalized-strategy.service";
import {ReferentialRefService} from "../services/referential-ref.service";
import {StatusIds} from "../../core/services/model/model.enum";
import {ProgramProperties} from "../services/config/program.config";



@Component({
  selector: 'app-simple-strategies-table',
  templateUrl: 'simple-strategies.table.html',
  styleUrls: ['simple-strategies.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleStrategiesTable extends AppTable<DenormalizedStrategy, ReferentialFilter> {

  private _program: Program;

  statusList = DefaultStatusList;
  statusById: any;
  quarters = [1, 2, 3, 4];

  @Input() canEdit = false;
  @Input() canDelete = false;

  @Input() set program(program: Program) {
   this.setProgram(program);
  }

  get program(): Program {
    return this._program;
  }

  constructor(
    route: ActivatedRoute,
    router: Router,
    platform: Platform,
    location: Location,
    modalCtrl: ModalController,
    localSettingsService: LocalSettingsService,
    injector: Injector,
    dataService: DenormalizedStrategyService,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef,
    @Inject(ENVIRONMENT) protected environment
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
          'analyticReference',
          'recorderDepartments',
          'locations',
          'taxonNames',
          'comments',
          'parameters',
          'effortQ1',
          'effortQ2',
          'effortQ3',
          'effortQ4'])
        .concat(RESERVED_END_COLUMNS),
      new EntitiesTableDataSource(DenormalizedStrategy, dataService, environment, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        dataServiceOptions: {
          readOnly: true,
        }
      }),
      null,
      injector);

    this.i18nColumnPrefix = 'PROGRAM.STRATEGY.TABLE.'; // Override by program
    this.autoLoad = true; // waiting parent to load

    this.confirmBeforeDelete = true;
    this.inlineEdition = false;

    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.registerAutocompleteField('department', {
      service: this.referentialRefService,
      filter: <ReferentialFilter>{
        entityName: 'Department',
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }
    });

    this.registerAutocompleteField('location', {
      service: this.referentialRefService,
      filter: <ReferentialFilter>{
        entityName: 'Location',
        // TODO BLA: rendre ceci paramètrable par program properties
        levelIds: [LocationLevelIds.ICES_DIVISION],
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }
    });

    this.registerAutocompleteField('taxonName', {
      service: this.referentialRefService,
      filter: <ReferentialFilter>{
        entityName: 'TaxonName',
        levelIds: [TaxonomicLevelIds.SPECIES, TaxonomicLevelIds.SUBSPECIES],
        statusIds: [StatusIds.ENABLE, StatusIds.TEMPORARY]
      }
    });
  }

  protected setProgram(program: Program) {
    if (program && isNotNil(program.id) && this._program !== program) {
      console.debug('[strategy-table] Setting program:', program);

      this._program = program;

      this.i18nColumnPrefix = 'PROGRAM.STRATEGY.TABLE.';

      // Add a i18n suffix (e.g. in Biological sampling program)
      const i18nSuffix = program.getProperty(ProgramProperties.PROGRAM_STRATEGY_I18N_SUFFIX);
      this.i18nColumnPrefix += i18nSuffix !== 'legacy' && i18nSuffix || '';

      this.setFilter( {
        ...this.filter,
        levelId: program.id
      });
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

