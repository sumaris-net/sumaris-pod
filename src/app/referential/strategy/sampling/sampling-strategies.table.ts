import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input} from "@angular/core";
import {DefaultStatusList}  from "@sumaris-net/ngx-components";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS}  from "@sumaris-net/ngx-components";
import {Program} from "../../services/model/program.model";
import {isNotEmptyArray, isNotNil} from "@sumaris-net/ngx-components";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {LocalSettingsService}  from "@sumaris-net/ngx-components";
import {EntitiesTableDataSource}  from "@sumaris-net/ngx-components";
import {LocationLevelIds, TaxonomicLevelIds} from "../../services/model/model.enum";
import {ReferentialFilter} from "../../services/filter/referential.filter";
import {ReferentialRefService} from "../../services/referential-ref.service";
import {StatusIds}  from "@sumaris-net/ngx-components";
import {ProgramProperties} from "../../services/config/program.config";
import {environment} from "../../../../environments/environment";
import {SamplingStrategy} from "../../services/model/sampling-strategy.model";
import {SamplingStrategyService} from "../../services/sampling-strategy.service";
import {StrategyFilter} from "../../services/strategy.service";


@Component({
  selector: 'app-sampling-strategies-table',
  templateUrl: 'sampling-strategies.table.html',
  styleUrls: ['sampling-strategies.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
/**
 *
 */
export class SamplingStrategiesTable extends AppTable<SamplingStrategy, StrategyFilter> {

  private _program: Program;
  errorDetails: any;

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
    dataService: SamplingStrategyService,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef
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
      new EntitiesTableDataSource(SamplingStrategy, dataService, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        dataServiceOptions: {
          readOnly: true,
        }
      }),
      null,
      injector);

    this.i18nColumnPrefix = 'PROGRAM.STRATEGY.TABLE.'; // Can be overwrite by a program property - see setProgram()
    this.autoLoad = false; // waiting parent to load

    this.confirmBeforeDelete = true;
    this.inlineEdition = false;

    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();


    // Remove error after changed selection
    this.selection.changed.subscribe(() => {
      this.error = null;
    });

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

    // Load data, if program already set
    if (this._program && !this.autoLoad) {
      this.onRefresh.emit();
    }
  }

  protected setProgram(program: Program) {
    if (program && isNotNil(program.id) && this._program !== program) {
      console.debug('[strategy-table] Setting program:', program);

      this._program = program;

      this.i18nColumnPrefix = 'PROGRAM.STRATEGY.TABLE.';

      // Add a i18n suffix (e.g. in Biological sampling program)
      const i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
      this.i18nColumnPrefix += i18nSuffix !== 'legacy' && i18nSuffix || '';

      this.setFilter(StrategyFilter.fromObject({
        ...this.filter,
        levelId: program.id
      }));
    }
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  async deleteSelection(event: UIEvent): Promise<number> {
    const rowsToDelete = this.selection.selected;

    const strategyLabelsWithRealizedEffort = (rowsToDelete || [])
      .map(row => row.currentData as SamplingStrategy)
      .map(SamplingStrategy.fromObject)
      .filter(strategy => strategy.hasRealizedEffort)
      .map(s => s.label);

    // send error if one strategy has realized effort
    if (isNotEmptyArray(strategyLabelsWithRealizedEffort)) {
      this.errorDetails = {label: strategyLabelsWithRealizedEffort.join(', ')};
      this.error = strategyLabelsWithRealizedEffort.length === 1
        ? 'PROGRAM.STRATEGY.ERROR.STRATEGY_HAS_REALIZED_EFFORT'
        : 'PROGRAM.STRATEGY.ERROR.STRATEGIES_HAS_REALIZED_EFFORT';
      return 0;
    }

    // delete if strategy has not effort
    await super.deleteSelection(event);

    //TODO FIX : After delete first time, _dirty = false; Cannot delete second times cause try to save
    super.markAsPristine();
    //

    this.error = null;
  }


}

