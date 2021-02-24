import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input} from "@angular/core";
import {ValidatorService} from "@e-is/ngx-material-table";
import {SampleValidatorService} from "../../services/validator/sample.validator";
import {isEmptyArray, isNil, isNotEmptyArray, isNotNil} from "../../../shared/functions";
import {DenormalizedPmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {ReferentialRefService} from "../../../referential/services/referential-ref.service";
import {environment} from "../../../../environments/environment";
import {BehaviorSubject} from "rxjs";
import {ObjectMap} from "../../../shared/types";
import {firstNotNilPromise} from "../../../shared/observables";
import {SamplesTable, SamplesTableOptions} from "../samples.table";
import {PmfmFilter, PmfmService} from "../../../referential/services/pmfm.service";
import {ProgramRefService} from "../../../referential/services/program-ref.service";
import {SelectPmfmModal} from "../../../referential/pmfm/select-pmfm.modal";
import {ReferentialRef} from "../../../core/services/model/referential.model";
import {Sample} from "../../services/model/sample.model";
import {TaxonUtils} from "../../../referential/services/model/taxon.model";
import {SamplingStrategyService} from "../../../referential/services/sampling-strategy.service";
import {IPmfm} from "../../../referential/services/model/pmfm.model";
import {Moment} from "moment";

export interface SampleFilter {
  operationId?: number;
  landingId?: number;
}

const SAMPLE_RESERVED_START_COLUMNS: string[] = ['label'];
const SAMPLE_RESERVED_END_COLUMNS: string[] = ['comments'];
const SAMPLE_PARAMETER_GROUPS = ['ANALYTIC_REFERENCE', 'WEIGHT', 'LENGTH', 'SEX', 'MATURITY', 'AGE', 'OTHER'];

declare interface GroupColumnDefinition {
  key: string;
  label?: string;
  name?: string;
  colSpan: number;
  cssClass?: string;
}

@Component({
  selector: 'app-sampling-samples-table',
  templateUrl: 'sampling-samples.table.html',
  styleUrls: ['sampling-samples.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: SampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingSamplesTable extends SamplesTable {

  private $strategyLabel = new BehaviorSubject<string>(undefined);

  $pmfmGroups = new BehaviorSubject<ObjectMap<number[]>>(null);
  $pmfmGroupColumns = new BehaviorSubject<GroupColumnDefinition[]>([]);

  @Input() set pmfmGroups(value: ObjectMap<number[]>) {
    this.$pmfmGroups.next(value);
  }

  get pmfmGroups(): ObjectMap<number[]> {
    return this.$pmfmGroups.getValue();
  }

  @Input() defaultLocation: ReferentialRef;

  @Input()
  set strategyLabel(value: string) {
    if (this._strategyLabel !== value && isNotNil(value)) {
      super.strategyLabel = value;
      this.$strategyLabel.next(value);
    }
  }

  constructor(
    protected injector: Injector,
    protected programRefService: ProgramRefService,
    protected pmfmService: PmfmService,
    protected samplingStrategyService: SamplingStrategyService
  ) {
    super(injector,
      <SamplesTableOptions>{
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: SAMPLE_RESERVED_START_COLUMNS,
        reservedEndColumns: SAMPLE_RESERVED_END_COLUMNS,
        mapPmfms: pmfms => this.mapPmfms(pmfms),
        requiredStrategy: true
      }
    );

    this.$strategyLabel.subscribe((strategyLabel) => this.onStrategyChanged(strategyLabel));
  }

  protected async onNewEntity(data: Sample): Promise<void> {
    await super.onNewEntity(data);

    const groupAge = this.$pmfmGroupColumns.getValue().find(c => c.label === 'AGE');
    const rubinCode = TaxonUtils.rubinCode(this.defaultTaxonName.name);

    // Generate label if age in pmfm strategies and rubinCode computable (locationCodeDDMMYYrubinCodeXXXX)
    if (groupAge && rubinCode && isNotNil(this.defaultLocation) && isNotNil(this.defaultSampleDate) && isNotNil(this.defaultTaxonName)) {
      data.label = `${this.defaultLocation.label}${this.defaultSampleDate.format("DDMMYY")}${rubinCode}${data.rankOrder.toString().padStart(4, "0")}`;
      console.debug("[sample-table] Generated label: ", data.label);
    }
  }

  /**
   * Use in ngFor, for trackBy
   * @param index
   * @param column
   */
  trackColumnDef(index: number, column: GroupColumnDefinition) {
    return column.key;
  }

  /**
   * Not used yet. Implementation must manage stored samples values and different pmfms types (number, string, qualitative values...)
   * @param event
   */
  async openChangePmfmsModal(event?: UIEvent) {
    const existingPmfmIds = (this.$pmfms.getValue() || []).map(p => p.id).filter(isNotNil);

    const pmfmIds = await this.openSelectPmfmsModal(event, {
      excludedIds: existingPmfmIds
    }, {
      allowMultiple: false
    });
    if (!pmfmIds) return; // USer cancelled

    console.debug('TODO changes to pmfm: ', pmfmIds);
  }


  async openAddPmfmsModal(event?: UIEvent) {
    const existingPmfmIds = (this.$pmfms.getValue() || []).map(p => p.id).filter(isNotNil);

    const pmfmIds = await this.openSelectPmfmsModal(event, {
      excludedIds: existingPmfmIds
    }, {
      allowMultiple: false
    });
    if (!pmfmIds) return; // USer cancelled
    await this.addPmfmColumns(pmfmIds);

  }

  /* -- protected methods -- */

  /**
   * Force to wait PMFM map to be loaded
   * @param pmfms
   */
  protected async mapPmfms(pmfms: IPmfm[]): Promise<IPmfm[]> {
    if (isEmptyArray(pmfms)) return pmfms;

    // Wait until map is loaded
    const groupedPmfmIdsMap = await firstNotNilPromise(this.$pmfmGroups);

    // Create a list of known pmfm ids
    const groupedPmfmIds = Object.values(groupedPmfmIdsMap).reduce((res, pmfmIds) => res.concat(...pmfmIds), []);

    // Create pmfms group
    const orderedPmfmIds: number[] = [];
    const orderedPmfms: IPmfm[] = [];
    let groupIndex = 0;
    const pmfmGroupColumns: GroupColumnDefinition[] = SAMPLE_PARAMETER_GROUPS.reduce((pmfmGroups, group) => {
      let groupPmfms: IPmfm[];
      if (group === 'OTHER') {
        groupPmfms = pmfms.filter(p => !groupedPmfmIds.includes(p.id));
      }
      else {
        const groupPmfmIds = groupedPmfmIdsMap[group];
        if (isNotEmptyArray(groupPmfmIds)) {
          groupPmfms = pmfms.filter(p => groupPmfmIds.includes(p.id));
        }
      }

      if (isEmptyArray(groupPmfms)) return pmfmGroups; // Skip group


      const groupPmfmCount = groupPmfms.length;
      let cssClass = (++groupIndex) % 2 === 0 ? 'even' : 'odd';


      groupPmfms.forEach(pmfm =>  {
        pmfm = pmfm.clone(); // Clone, to leave original PMFM unchanged

        // Use rankOrder as a group index (will be used in template, to computed column class)
        if (pmfm instanceof DenormalizedPmfmStrategy) {
          pmfm.rankOrder = groupIndex;
        }

        // Add pmfm into the final list of ordered pmfms
        orderedPmfms.push(pmfm);
      });

      // The analytic reference has no visible header group
      if (group === 'ANALYTIC_REFERENCE') cssClass += ' hidden';

      return pmfmGroups.concat(
        ...groupPmfms.reduce((res, pmfm, index) => {
          if (orderedPmfmIds.includes(pmfm.id)) return res; // Skip if already proceed
          orderedPmfmIds.push(pmfm.id);
          return res.concat(<GroupColumnDefinition>{
            key: pmfm.id.toString(),
            label: group,
            name: this.i18nColumnPrefix + group,
            cssClass,
            colSpan: index === 0 ? groupPmfmCount : 0
          });
        }, []));
    }, []);

    this.$pmfmGroupColumns.next(pmfmGroupColumns);

    return orderedPmfms;
  }


  protected async openSelectPmfmsModal(event?: UIEvent, filter?: PmfmFilter,
                                       opts?: {
                                         allowMultiple?: boolean;
                                       }): Promise<number[]> {

    const modal = await this.modalCtrl.create({
      component: SelectPmfmModal,
      componentProps: {
        filter,
        allowMultiple: opts && opts.allowMultiple
      },
      keyboardClose: true,
      cssClass: 'modal-large'
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res || isEmptyArray(res.data)) return; // CANCELLED

    // Return pmfm ids
    return res.data.map(p => p.id);
  }

  protected async addPmfmColumns(pmfmIds: number[]) {
    if (isEmptyArray(pmfmIds)) return; // Skip if empty

    // Load each pmfms, by id
    const pmfms = (await Promise.all(pmfmIds.map(id => this.pmfmService.load(id))));

    this.pmfms = [
      ...this.$pmfms.getValue(),
      ...pmfms
    ];
  }

  protected async onStrategyChanged(strategyLabel: string) {
    // IMAGINE-230 Strategy must have defined and positive expected effort to add samples
    if (isNil(this.programLabel) || isNil(strategyLabel) || isNil(this.defaultSampleDate)) {
      this.disable();
      return;
    }

    const strategyEffort = await this.samplingStrategyService.loadStrategyEffortByDate(this.programLabel,
      strategyLabel,
      this.defaultSampleDate);
    const enable = strategyEffort && isNotNil(strategyEffort.expectedEffort);
    if (enable) {
      this.enable();
    }
    else {
      this.disable();
    }
  }

}
