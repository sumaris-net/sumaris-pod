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
          'sampleRowCode',
          'eotp',
          'laboratory',
          'fishingArea',
          'targetSpecie',
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
    let pmfmStrategies: string[] = [];
    console.log(data.pmfmStrategies);
    let age = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label === "AGE");
    //console.log(age);
    if(age.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.AGE'));
    }
    let sex = data.pmfmStrategies.filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label === "SEX");
    //console.log(sex);
    if(sex.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.SEX'));
    }
    let weightPmfmStrategy = (data.pmfmStrategies || []).filter(p => p.pmfm && p.pmfm.parameter && p.pmfm.parameter.label === 'WEIGHT');
    //console.log(weightPmfmStrategy);
    if(weightPmfmStrategy.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.WEIGHT_TABLE'));
    }
    const sizeValues = ['LENGTH_PECTORAL_FORK', 'LENGTH_CLEITHRUM_KEEL_CURVE', 'LENGTH_PREPELVIC', 'LENGTH_FRONT_EYE_PREPELVIC', 'LENGTH_LM_FORK', 'LENGTH_PRE_SUPRA_CAUDAL', 'LENGTH_CLEITHRUM_KEEL', 'LENGTH_LM_FORK_CURVE', 'LENGTH_PECTORAL_FORK_CURVE', 'LENGTH_FORK_CURVE', 'STD_STRAIGTH_LENGTH', 'STD_CURVE_LENGTH', 'SEGMENT_LENGTH', 'LENGTH_MINIMUM_ALLOWED', 'LENGTH', 'LENGTH_TOTAL', 'LENGTH_STANDARD', 'LENGTH_PREANAL', 'LENGTH_PELVIC', 'LENGTH_CARAPACE', 'LENGTH_FORK', 'LENGTH_MANTLE'];
    let sizePmfmStrategy = (data.pmfmStrategies || []).filter(p => p.pmfm && p.pmfm.parameter && sizeValues.includes(p.pmfm.parameter.label));
    //console.log(sizePmfmStrategy);
    if(sizePmfmStrategy.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.SIZE_TABLE'));
    }
    const maturityValues = ['MATURITY_STAGE_3_VISUAL', 'MATURITY_STAGE_4_VISUAL', 'MATURITY_STAGE_5_VISUAL', 'MATURITY_STAGE_6_VISUAL', 'MATURITY_STAGE_7_VISUAL', 'MATURITY_STAGE_9_VISUAL'];
    let maturityPmfmStrategy = (data.pmfmStrategies || []).filter(p => p.pmfm && p.pmfm.parameter && maturityValues.includes(p.pmfm.parameter.label));
    //console.log(maturityPmfmStrategy);
    if(maturityPmfmStrategy.length > 0) {
      pmfmStrategies.push(this.translate.instant('PROGRAM.STRATEGY.MATURITY_TABLE'));
    }
    //console.log(pmfmStrategies);
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

  /**
   * Add landing to the given row, or if not specified the currently edited row
   * @param event
   * @param row
   */
  addLanding(event?: any, row?: TableElement<Strategy>): boolean {
    // FIXME CLT IMAGINE 143

    console.debug('addLanding called');
    // row = row || this.editedRow;
    // if (row && row.editing) {
    //   if (event) event.stopPropagation();
    //   // confirmation edition or creation
    //   if (!row.confirmEditCreate()) {
    //     // If pending, wait end of validation, then loop
    //     if (row.validator && row.validator.pending) {
    //       AppFormUtils.waitWhilePending(row.validator)
    //         .then(() => this.confirmEditCreate(event, row));
    //     }
    //     else {
    //       if (this.debug) {
    //         console.warn("[table] Row not valid: unable to confirm", row);
    //         AppFormUtils.logFormErrors(row.validator, '[table] ');
    //       }
    //     }
    //     return false;
    //   }
    //   // If edit finished, forget edited row
    //   if (row === this.editedRow) {
    //     this.editedRow = undefined; // unselect row
    //     this.onConfirmEditCreateRow.next(row);
    //     this.markAsDirty();
    //   }
    // }
    return true;
  }


}

