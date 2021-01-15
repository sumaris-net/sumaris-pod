import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit } from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {
  AppFormUtils,
  environment,
  fromDateISOString,
  referentialToString,
  RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {StrategyValidatorService} from "../services/validator/strategy.validator";
import {AppliedPeriod, AppliedStrategy, Strategy} from "../services/model/strategy.model";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {DefaultStatusList} from "../../core/services/model/referential.model";
import {AppInMemoryTable} from "../../core/table/memory-table.class";
import {strategyDepartmentsToString, appliedStategiesToString, taxonsNameStrategyToString} from "../../referential/services/model/strategy.model";
import {ParameterLabelStrategies} from "../../referential/services/model/model.enum";

export declare interface StrategyFilter {
}

@Component({
  selector: 'app-simple-strategies-table',
  templateUrl: 'simple-strategies.table.html',
  styleUrls: ['simple-strategies.table.scss'],
  providers: [
    { provide: ValidatorService, useExisting: StrategyValidatorService },
    {
      provide: InMemoryEntitiesService,
      useFactory: () => new InMemoryEntitiesService<Strategy, StrategyFilter>(Strategy, {})
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SimpleStrategiesTable extends AppInMemoryTable<Strategy, StrategyFilter> implements OnInit, OnDestroy {

  statusList = DefaultStatusList;
  statusById: any;

  @Input() canEdit = false;
  @Input() canDelete = false;


  constructor(
    protected injector: Injector,
    protected memoryDataService: InMemoryEntitiesService<Strategy, StrategyFilter>,
    protected cd: ChangeDetectorRef
  ) {
    super(injector,
      // columns
      RESERVED_START_COLUMNS
        .concat([
          'label',
          'analyticReference',
          'strategyDepartments',
          'appliedStrategies',
          'taxonNames',
          'comment',
          'parametersTitleTable',
          'quarter_1_table',
          'quarter_2_table',
          'quarter_3_table',
          'quarter_4_table'])
        .concat(RESERVED_END_COLUMNS),
      Strategy,
      memoryDataService,
      null,
      null,
      {});

    this.i18nColumnPrefix = 'PROGRAM.STRATEGY.';
    this.autoLoad = false; // waiting parent to load

    this.confirmBeforeDelete = true;
    this.inlineEdition = false;

    // Fill statusById
    this.statusById = {};
    this.statusList.forEach((status) => this.statusById[status.id] = status);

    this.debug = !environment.production;
  }

  ngOnInit() {

    //this.inlineEdition = toBoolean(this.inlineEdition, true);
    super.ngOnInit();
  }

  effortToString(data: Strategy, quarter) {
    const appliedPeriods = data.appliedStrategies.length && data.appliedStrategies[0].appliedPeriods || [];
    let quarterEffort = null;
    for (let fishingAreaAppliedPeriod of appliedPeriods) {
      let startDateMonth = fromDateISOString(fishingAreaAppliedPeriod.startDate).month();
      let endDateMonth = fromDateISOString(fishingAreaAppliedPeriod.endDate).month();
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

  parametersToString(data: Strategy) {
    //console.log("pmfmStrategies", data.pmfmStrategies);
    let pmfmStrategies: string[] = [];

    let agePmfmStrategy = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label === ParameterLabelStrategies.AGE);
    if(agePmfmStrategy.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.AGE'));
    }

    let sexPmfmStrategy = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label === ParameterLabelStrategies.SEX);
    if(sexPmfmStrategy.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.SEX'));
    }

    let weightPmfmStrategy = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && ParameterLabelStrategies.WEIGHTS.includes(p.pmfm.parameter.label));
    if(weightPmfmStrategy.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.WEIGHT_TABLE'));
    }

    let sizePmfmStrategy = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && ParameterLabelStrategies.LENGTHS.includes(p.pmfm.parameter.label));
    if(sizePmfmStrategy.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.SIZE_TABLE'));
    }

    let maturityPmfmStrategy = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && ParameterLabelStrategies.MATURITIES.includes(p.pmfm.parameter.label));
    if(maturityPmfmStrategy.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.MATURITY_TABLE'));
    }

    //console.log("pmfmStrategies result", pmfmStrategies);
    return pmfmStrategies.join(', ');
  }

  setValue(value: Strategy[]) {
    super.setValue(value);
  }

  referentialToString = referentialToString;
  strategyDepartmentsToString = strategyDepartmentsToString;
  appliedStategiesToString = appliedStategiesToString;
  taxonsNameStrategyToString = taxonsNameStrategyToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }




}

