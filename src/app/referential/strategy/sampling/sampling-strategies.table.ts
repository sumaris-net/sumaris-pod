import { Location } from "@angular/common";
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ModalController, Platform } from "@ionic/angular";
import * as momentImported from "moment";
import { debounceTime } from "rxjs/internal/operators/debounceTime";
import { filter } from "rxjs/internal/operators/filter";
import { tap } from "rxjs/internal/operators/tap";
import { PersonService } from "src/app/admin/services/person.service";
import { personToString } from "src/app/core/services/model/person.model";
import { NetworkService } from "src/app/core/services/network.service";
import { AppRootTableSettingsEnum } from "src/app/data/table/root-table.class";
import { fromDateISOString } from "src/app/shared/dates";
import { SharedValidators } from "src/app/shared/validator/validators";
import { environment } from "../../../../environments/environment";
import { LocalSettingsService } from "../../../core/services/local-settings.service";
import { StatusIds } from "../../../core/services/model/model.enum";
import { DefaultStatusList } from "../../../core/services/model/referential.model";
import { EntitiesTableDataSource } from "../../../core/table/entities-table-datasource.class";
import { AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS } from "../../../core/table/table.class";
import { isNil, isNotEmptyArray, isNotNil } from "../../../shared/functions";
import { ProgramProperties, SAMPLING_STRATEGIES_FEATURE_NAME } from "../../services/config/program.config";
import { LocationLevelIds, ParameterLabelGroups, TaxonomicLevelIds } from "../../services/model/model.enum";
import { Program } from "../../services/model/program.model";
import { SamplingStrategy } from "../../services/model/sampling-strategy.model";
import { ParameterService } from "../../services/parameter.service";
import { ReferentialRefService } from "../../services/referential-ref.service";
import { ReferentialFilter } from "../../services/referential.service";
import { SamplingStrategyService } from "../../services/sampling-strategy.service";
import { StrategyFilter, StrategyService } from "../../services/strategy.service";

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
/**
 *
 */
export class SamplingStrategiesTable extends AppTable<SamplingStrategy, StrategyFilter> {

  private _program: Program;
  errorDetails : any;
  protected network: NetworkService;

  statusList = DefaultStatusList;
  statusById: any;
  quarters = [1, 2, 3, 4];
  pmfmIds = {};

  filterIsEmpty = true;

  hasOfflineMode = false;

  @Input() canEdit = false;
  @Input() canDelete = false;

  @Input() set program(program: Program) {
   this.setProgram(program);
  }

  get program(): Program {
    return this._program;
  }

  filterForm: FormGroup;

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
    protected cd: ChangeDetectorRef,
    protected formBuilder: FormBuilder,
    protected personService: PersonService,
    protected strategyService: StrategyService,
    protected parameterService: ParameterService
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

    this.network = injector && injector.get(NetworkService);

    Object.keys(ParameterLabelGroups).forEach(parameter => {
      if (parameter !== 'ANALYTIC_REFERENCE') this.pmfmIds[parameter] = [null]
    });

    this.filterForm = formBuilder.group({
      synchronizationStatus: [null],
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
      pmfmIds : formBuilder.group(this.pmfmIds)
    });
    

    this.i18nColumnPrefix = 'PROGRAM.STRATEGY.TABLE.'; // Can be overwrite by a program property - see setProgram()
    this.autoLoad = false; // waiting parent to load

    this.confirmBeforeDelete = true;
    this.inlineEdition = false;

    this.debug = !environment.production;

    this.settingsId = SamplingStrategiesPageSettingsEnum.PAGE_ID;
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
      displayWith: personToString,
      mobile: this.mobile
    });

    // Update filter when changes
    this.registerSubscription(
      this.filterForm.valueChanges
        .pipe(
          debounceTime(250),
          filter(() => this.filterForm.valid),
          // Applying the filter
          tap(json => this.setFilter({
            synchronizationStatus: json.synchronizationStatus || undefined,
            analyticReferences: json.analyticReferences,
            departmentIds: isNotNil(json.department) ? [json.department.id] : undefined,
            locationIds: isNotNil(json.location) ? [json.location.id] : undefined,
            taxonIds: isNotNil(json.taxonName) ? [json.taxonName.id] : undefined,
            periods : this.setPeriods(json),
            parameterIds: this.setPmfmIds(json),
            levelId: this.program.id,
          }, {emitEvent: this.mobile || isNil(this.filter)})),
          // Save filter in settings (after a debounce time)
          debounceTime(500),
          tap(json => this.settings.savePageSetting(this.settingsId, json, SamplingStrategiesPageSettingsEnum.FILTER_KEY))
        )
    .subscribe());

    this.restoreFilterOrLoad();

    // Load data, if program already set
    if (this._program && !this.autoLoad) {
      this.onRefresh.emit();
    }
  }

  setPmfmIds(json) {
    const pmfmIds = [];
    if (json.pmfmIds) {
      Object.keys(json.pmfmIds).forEach(parameter => {
        if (json.pmfmIds[parameter]) {
          this.parameterService.loadByLabel(parameter).then(parameter => {
            pmfmIds.push(parameter && parameter.id);
          })
        }
      });
    }
    return isNotEmptyArray(pmfmIds) ? pmfmIds : undefined;
  }

  setPeriods(json): any[] {
    const periods = [];
    if (json.startDate && json.endDate && json.periods) {
      const startYear = moment(new Date(json.startDate)).year();
      const endYear= moment(json.endDate).year();
      for (let i = startYear; i <= endYear; i++) {
        let y = 1;
        Object.keys(json.periods).forEach(period => {
          if (json.periods[period]) {
            const startMonth = (y - 1) * 3 + 1;
            const startDate = fromDateISOString(`${i}-${startMonth.toString().padStart(2, '0')}-01T00:00:00.000Z`).utc();
            const endDate = startDate.clone().add(2, 'month').endOf('month').startOf('day');
            if ((startDate >= moment(json.startDate) && moment(json.endDate) >= startDate) && (endDate >= moment(json.startDate) && endDate <= moment(json.endDate))) {
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

      this.i18nColumnPrefix = 'PROGRAM.STRATEGY.TABLE.';

      // Add a i18n suffix (e.g. in Biological sampling program)
      const i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
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

  protected isFilterEmpty = StrategyFilter.isEmpty;

  async restoreFilterOrLoad() {
    console.debug("[root-table] Restoring filter from settings...");
    const jsonFilter = this.settings.getPageSettings(this.settingsId, AppRootTableSettingsEnum.FILTER_KEY);

    const synchronizationStatus = jsonFilter && jsonFilter.synchronizationStatus;
    const filter = jsonFilter && typeof jsonFilter === 'object' && {...jsonFilter, synchronizationStatus: undefined} || undefined;

    // this.hasOfflineMode = (synchronizationStatus && synchronizationStatus !== 'SYNC') ||
    //   (await this.dataService.hasOfflineData());

    // No default filter, nor synchronizationStatus
    if (this.isFilterEmpty(filter) && !synchronizationStatus) {
      // If offline data, show it (will refresh)
      if (this.hasOfflineMode) {
        this.filterForm.patchValue({
          synchronizationStatus: 'DIRTY'
        });
      }
      // No offline data: default load (online data)
      else {
        // To avoid a delay (caused by debounceTime in a previous pipe), to refresh content manually
        this.onRefresh.emit();
        // But set a empty filter, to avoid automatic apply of next filter changes (caused by condition '|| isNil()' in a previous pipe)
        this.filterForm.patchValue({}, {emitEvent: false});
      }
    }
    // Restore the filter (will apply it)
    else {
      // Force offline
      if (this.network.offline && this.hasOfflineMode && synchronizationStatus === 'SYNC') {
        this.filterForm.patchValue({
          ...filter,
          synchronizationStatus: 'DIRTY'
        });
      }
      else {
        this.filterForm.patchValue({...filter, synchronizationStatus});
      }
    }
  }


}

