import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, Output, ViewChild} from '@angular/core';
import {
  AppTable,
  DefaultStatusList,
  EntitiesTableDataSource,
  fromDateISOString, isEmptyArray,
  isNotEmptyArray,
  isNotNil,
  LocalSettingsService,
  PersonService,
  PersonUtils,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS,
  SharedValidators,
  StatusIds, toBoolean
} from '@sumaris-net/ngx-components';
import {Program} from '../../services/model/program.model';
import {ActivatedRoute, Router} from '@angular/router';
import {ModalController, Platform} from '@ionic/angular';
import {Location} from '@angular/common';
import {LocationLevelIds, ParameterLabelGroups, TaxonomicLevelIds} from '../../services/model/model.enum';
import {ReferentialFilter} from '../../services/filter/referential.filter';
import {ReferentialRefService} from '../../services/referential-ref.service';
import {ProgramProperties, SAMPLING_STRATEGIES_FEATURE_NAME} from '../../services/config/program.config';
import {environment} from '@environments/environment';
import {SamplingStrategy} from '../../services/model/sampling-strategy.model';
import {SamplingStrategyService} from '../../services/sampling-strategy.service';
import {StrategyFilter, StrategyService} from '../../services/strategy.service';
import * as momentImported from 'moment';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ParameterService} from '@app/referential/services/parameter.service';
import {debounceTime, filter, tap} from 'rxjs/operators';
import {AppRootTableSettingsEnum} from '@app/data/table/root-table.class';
import {MatExpansionPanel} from '@angular/material/expansion';
import {TableElement} from '@e-is/ngx-material-table/src/app/ngx-material-table/table-element';
import {Subject} from 'rxjs';

const moment = momentImported;

export const SamplingStrategiesPageSettingsEnum = {
  PAGE_ID: "samplingStrategies",
  FILTER_KEY: "filter",
  FEATURE_ID: SAMPLING_STRATEGIES_FEATURE_NAME
};

@Component({
  selector: 'app-sampling-strategies-table',
  templateUrl: 'sampling-strategies.table.html',
  styleUrls: ['sampling-strategies.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingStrategiesTable extends AppTable<SamplingStrategy, StrategyFilter> {

  private _program: Program;
  errorDetails: any;

  statusList = DefaultStatusList;
  statusById: any;
  quarters = [1, 2, 3, 4];
  readonly parameterGroupLabels: string[];

  filterForm: FormGroup;
  filterCriteriaCount = 0;

  @Input() showToolbar = true;
  @Input() canEdit = false;
  @Input() canDelete = false;
  @Input() showError = true;
  @Input() showPaginator = true;

  @Input() set program(program: Program) {
   this.setProgram(program);
  }

  get program(): Program {
    return this._program;
  }

  @Output() onNewDataFromRow = new Subject<TableElement<SamplingStrategy>>()

  @ViewChild(MatExpansionPanel, {static: true}) filterExpansionPanel: MatExpansionPanel;

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
    protected personService: PersonService,
    protected strategyService: StrategyService,
    protected parameterService: ParameterService,
    protected formBuilder: FormBuilder,
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

    this.parameterGroupLabels = Object.keys(ParameterLabelGroups)
      .filter(label => label !== 'ANALYTIC_REFERENCE');

    this.filterForm = formBuilder.group({
      levelId: [null, Validators.required], // the program id
      analyticReference: [null],
      department: [null, SharedValidators.entity],
      location: [null, SharedValidators.entity],
      taxonName: [null, SharedValidators.entity],
      startDate: [null, SharedValidators.validDate],
      endDate: [null, SharedValidators.validDate],
      recorderPerson: [null, SharedValidators.entity],
      periods : formBuilder.group({
        effortQ1: [null],
        effortQ2: [null],
        effortQ3: [null],
        effortQ4: [null]
      }),
      parameterGroups : formBuilder.group(
        this.parameterGroupLabels.reduce((controlConfig, label) => {
          controlConfig[label] = [null];
          return controlConfig;
        }, {})
      )
    });

    this.i18nColumnPrefix = 'PROGRAM.STRATEGY.TABLE.'; // Can be overwrite by a program property - see setProgram()
    this.autoLoad = false; // waiting program to be loaded - see setProgram()
    this.defaultSortBy = 'label';
    this.defaultSortDirection = 'asc';
    this.confirmBeforeDelete = true;
    this.inlineEdition = false;

    // Will be override when getting program - see setProgram()
    this.settingsId = SamplingStrategiesPageSettingsEnum.PAGE_ID + '#?';

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Remove error after changed selection
    this.selection.changed.subscribe(() => {
      this.error = null;
    });

    // Analytic reference autocomplete
    this.registerAutocompleteField('analyticReference', {
      items: []
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


    // Combo: recorder person (filter)
    this.registerAutocompleteField('person', {
      service: this.personService,
      filter: {
        statusIds: [StatusIds.TEMPORARY, StatusIds.ENABLE]
      },
      attributes: ['lastName', 'firstName', 'department.name'],
      displayWith: PersonUtils.personToString,
      mobile: this.mobile
    });

    // Update filter when changes
    this.registerSubscription(
      this.filterForm.valueChanges
        .pipe(
          debounceTime(250),
          filter((_) => this.filterForm.valid),
          tap(async (value) => {
            const filter = await this.asFilter(value);
            this.filterCriteriaCount = filter.countNotEmptyCriteria() - 1 /*levelId*/;
            this.markForCheck();
            // Update the filter, without reloading the content
            this.setFilter(filter, {emitEvent: false});
          }),
          // Save filter in settings (after a debounce time)
          debounceTime(500),
          tap(json => this.settings.savePageSetting(this.settingsId, json, SamplingStrategiesPageSettingsEnum.FILTER_KEY))
        )
        .subscribe());

  }

  async getParameterIds(parameterGroups: Map<string, boolean>): Promise<number[]> {

    const parameters = await Promise.all(
      Object.keys(parameterGroups || {})
        // Keep only checked parameter group
        .filter(label => parameterGroups[label])
        // Then load corresponding parameter
        .map(label => this.parameterService.loadByLabel(label)));

    if (isEmptyArray(parameters)) return undefined

    // Keep only parameter's ids
    return parameters.map(p => p.id);
  }

  getPeriods(jsonFilter: any): any[] {
    const periods = [];
    if (jsonFilter.startDate && jsonFilter.endDate && jsonFilter.periods) {
      const startYear = moment(new Date(jsonFilter.startDate)).year();
      const endYear = moment(jsonFilter.endDate).year();
      for (let i = startYear; i <= endYear; i++) {
        let y = 1;
        Object.keys(jsonFilter.periods).forEach(period => {
          if (jsonFilter.periods[period]) {
            const startMonth = (y - 1) * 3 + 1;
            const startDate = fromDateISOString(`${i}-${startMonth.toString().padStart(2, '0')}-01T00:00:00.000Z`).utc();
            const endDate = startDate.clone().add(2, 'month').endOf('month').startOf('day');
            if ((startDate >= moment(jsonFilter.startDate) && moment(jsonFilter.endDate) >= startDate) && (endDate >= moment(jsonFilter.startDate) && endDate <= moment(jsonFilter.endDate))) {
              periods.push({startDate, endDate});
            }
          }
          y++;
        });
      }
    }
    return isNotEmptyArray(periods) ? periods : undefined;
  }

  protected setProgram(program: Program) {
    if (program && isNotNil(program.id) && this._program !== program) {
      console.debug('[strategy-table] Setting program:', program);

      this._program = program;
      this.settingsId = SamplingStrategiesPageSettingsEnum.PAGE_ID + '#' + program.id;

      this.i18nColumnPrefix = 'PROGRAM.STRATEGY.TABLE.';

      // Add a i18n suffix (e.g. in Biological sampling program)
      const i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
      this.i18nColumnPrefix += i18nSuffix !== 'legacy' && i18nSuffix || '';

      // Restore filter from settings, or load all
      this.restoreFilterOrLoad(program.id);
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

  applyFilterAndClosePanel(event?: UIEvent) {
    this.onRefresh.emit(event);
    if (this.filterExpansionPanel) this.filterExpansionPanel.close();
  }

  resetFilter(event?: UIEvent) {
    this.filterForm.reset();
    this.setFilter(null, {emitEvent: true});
    if (this.filterExpansionPanel) this.filterExpansionPanel.close();
  }

  onNewData(event: UIEvent, row: TableElement<SamplingStrategy>) {

  }

  /* -- protected methods -- */

  protected async restoreFilterOrLoad(programId: number) {
    this.markAsLoading();

    console.debug("[root-table] Restoring filter from settings...");

    const json = this.settings.getPageSettings(this.settingsId, AppRootTableSettingsEnum.FILTER_KEY) || {};
    json.levelId = programId;

    const filter = await this.asFilter(json);

    this.filterForm.patchValue(json);
    this.setFilter(filter, {emitEvent: true});
  }

  protected async asFilter(source?: any): Promise<StrategyFilter> {
    source = source || this.filterForm.value;

    const filter = StrategyFilter.fromObject(source);

    filter.periods = this.getPeriods(source);
    filter.parameterIds = await this.getParameterIds(source.pmfmIds);

    return filter;
  }
}

