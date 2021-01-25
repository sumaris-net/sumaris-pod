import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Injector, Input} from "@angular/core";
import {Strategy} from "../services/model/strategy.model";
import {DefaultStatusList} from "../../core/services/model/referential.model";
import {appliedStrategiesToString, taxonNamesStrategyToString} from "../../referential/services/model/strategy.model";
import {StrategyFilter, StrategyService} from "../services/strategy.service";
import {AppTable, RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {ENVIRONMENT} from "../../../environments/environment.class";
import {fromDateISOString} from "../../shared/dates";
import {Program} from "../services/model/program.model";
import {firstArrayValue, isEmptyArray, isNotNil} from "../../shared/functions";
import {ActivatedRoute, Router} from "@angular/router";
import {ModalController, Platform} from "@ionic/angular";
import {Location} from "@angular/common";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {ValidatorService} from "@e-is/ngx-material-table";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {PmfmStrategy} from "../services/model/pmfm-strategy.model";
import {PmfmUtils} from "../services/model/pmfm.model";
import {ParameterLabel, ParameterLabelList} from "../services/model/model.enum";
import {PredefinedColors} from "@ionic/core";
import {ReferentialFilter} from "../services/referential.service";
import {environment} from "../../../environments/environment";

@Component({
  selector: 'app-simple-strategies-table',
  templateUrl: 'simple-strategies.table.html',
  styleUrls: ['simple-strategies.table.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleStrategiesTable extends AppTable<Strategy, ReferentialFilter> {

  private _program: Program;

  statusList = DefaultStatusList;
  statusById: any;

  @Input() canEdit = false;
  @Input() canDelete = false;

  @Input() set program(program: Program) {
    if (program && isNotNil(program.id) && this._program !== program) {
      this._program = program;
      console.debug('[strategy-table] Setting program:', program);
      this.setFilter( {
        ...this.filter,
        levelId: program.id
      });
    }
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
    dataService: StrategyService,
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
          'appliedStrategies',
          'taxonNames',
          'comment',
          'parameters',
          'effortQ1',
          'effortQ2',
          'effortQ3',
          'effortQ4'])
        .concat(RESERVED_END_COLUMNS),
      new EntitiesTableDataSource(Strategy, dataService, environment, null, {
        prependNewElements: false,
        suppressErrors: environment.production,
        dataServiceOptions: {
          readOnly: true,
        }
      }),
      null,
      injector);

    this.i18nColumnPrefix = 'PROGRAM.STRATEGY.TABLE.SAMPLING.';
    this.autoLoad = true; // waiting parent to load

    this.confirmBeforeDelete = true;
    this.inlineEdition = false;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.debug = !environment.production;
  }


  getRealizedEffortColor(data: Strategy, quarter): PredefinedColors {
    let color : PredefinedColors = 'dark';
    //TODO return 'success' if planified effort = realised effort
    if (this.effortToString(data, quarter) === 0) {
      return 'success';
    }
    return color;
  }

  getToDoEffortColor(data: Strategy, quarter): PredefinedColors {
    let color : PredefinedColors = 'danger';
    // return dark if planified effort = 0
    if (this.effortToString(data, quarter) === 0) {
      return 'dark';
    }
    //TODO return 'dark' if planified effort = realised effort
    /*if (this.effortToString(data, quarter) === 0) {
      return 'dark';
    }*/
    return color;
  }

  effortToString(data: Strategy, quarter) {
    console.debug('TODO effortToString', data);

    // TODO BLA: use this ?
    const appliedStrategy = firstArrayValue(data.appliedStrategies);
    const appliedPeriods = appliedStrategy && appliedStrategy.appliedPeriods || [];
    //const appliedPeriods = data.appliedStrategies.length && data.appliedStrategies[0].appliedPeriods || [];

    let quarterEffort = null;
    for (let fishingAreaAppliedPeriod of appliedPeriods) {
      const startDateMonth = fromDateISOString(fishingAreaAppliedPeriod.startDate).month();
      const endDateMonth = fromDateISOString(fishingAreaAppliedPeriod.endDate).month();
      if (startDateMonth >= 0 && endDateMonth < 3 && quarter === 1)
      {
        quarterEffort = fishingAreaAppliedPeriod.acquisitionNumber;
      }
      else if (startDateMonth >= 3 && endDateMonth < 6 && quarter === 2)
      {
        quarterEffort = fishingAreaAppliedPeriod.acquisitionNumber;
      }
      else if (startDateMonth >= 6 && endDateMonth < 9 && quarter === 3)
      {
        quarterEffort = fishingAreaAppliedPeriod.acquisitionNumber;
      }
      else if (startDateMonth >= 9 && endDateMonth < 12 && quarter === 4)
      {
        quarterEffort = fishingAreaAppliedPeriod.acquisitionNumber;
      }
    }
    return quarterEffort;
  }

  parametersToString(pmfmStrategies: PmfmStrategy[]) {
    if (isEmptyArray(pmfmStrategies)) return '';
    const parts = [];

    // has Age ?
    if (pmfmStrategies.find(p => PmfmUtils.hasParameterLabel(p.pmfm, ParameterLabel.AGE))) {
      parts.push(this.translate.instant('PROGRAM.STRATEGY.AGE'));
    }
    // has Sex ?
    if (pmfmStrategies.find(p => PmfmUtils.hasParameterLabel(p.pmfm, ParameterLabel.SEX))) {
      parts.push(this.translate.instant('PROGRAM.STRATEGY.SEX'));
    }
    // Has weight ?
    if (pmfmStrategies.find(p => PmfmUtils.hasParameterLabel(p.pmfm, ParameterLabel.WEIGHT))) {
      parts.push(this.translate.instant('PROGRAM.STRATEGY.WEIGHT_TABLE'));
    }
    // Has size
    if (pmfmStrategies.find(p => PmfmUtils.hasParameterLabelIncludes(p.pmfm , ParameterLabelList.LENGTH))) {
      parts.push(this.translate.instant('PROGRAM.STRATEGY.SIZE_TABLE'));
    }
    // Has maturity
    if (pmfmStrategies.find(p => PmfmUtils.hasParameterLabelIncludes(p.pmfm , ParameterLabelList.MATURITY))) {
      parts.push(this.translate.instant('PROGRAM.STRATEGY.MATURITY_TABLE'));
    }
    return parts.join(', ');
  }


  appliedStrategiesToString = appliedStrategiesToString;
  taxonNamesStrategyToString = taxonNamesStrategyToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }

}

