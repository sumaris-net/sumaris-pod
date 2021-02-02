import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  Output
} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {SampleValidatorService} from "../../services/validator/sample.validator";
import {isEmptyArray, isNotEmptyArray, isNotNil} from "../../../shared/functions";
import {UsageMode} from "../../../core/services/model/settings.model";
import * as moment from "moment";
import {Moment} from "moment";
import {AppMeasurementsTable} from "../../measurement/measurements.table.class";
import {InMemoryEntitiesService} from "../../../shared/services/memory-entity-service.class";
import {SampleModal} from "../sample.modal";
import {FormGroup} from "@angular/forms";
import {TaxonNameRef} from "../../../referential/services/model/taxon.model";
import {Sample} from "../../services/model/sample.model";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {AcquisitionLevelCodes} from "../../../referential/services/model/model.enum";
import {ReferentialRefService} from "../../../referential/services/referential-ref.service";
import {FormFieldDefinition} from "../../../shared/form/field.model";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../../core/table/table.class";
import {ReferentialRef} from "../../../core/services/model/referential.model";
import {environment} from "../../../../environments/environment";
import {TableAddPmfmsComponent} from "../table-add-pmfms.component";
import {ProgramService} from "../../../referential/services/program.service";
import {StrategyService} from "../../../referential/services/strategy.service";
import {BehaviorSubject} from "rxjs";
import {ObjectMap} from "../../../shared/types";
import {firstNotNilPromise} from "../../../shared/observables";
import {SelectReferentialModal} from "../../../referential/list/select-referential.modal";
import {SamplesTableOptions, SamplesTable} from "../samples.table";

export interface SampleFilter {
  operationId?: number;
  landingId?: number;
}

const SAMPLE_RESERVED_START_COLUMNS: string[] = ['label', 'morseCode', 'comment'];
const SAMPLE_RESERVED_END_COLUMNS: string[] = []; // TODO mettre comment ici ?
const SAMPLE_PARAMETER_GROUPS = ['WEIGHT', 'LENGTH', 'MATURITY', 'SEX', 'AGE', 'OTHER'];

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

  protected referentialRefService: ReferentialRefService;
  protected _$pmfmGroups = new BehaviorSubject<ObjectMap<number[]>>(null);

  $pmfmGroupColumns = new BehaviorSubject<GroupColumnDefinition[]>([]);

  @Input() set pmfmGroups(value: ObjectMap<number[]>) {
    this._$pmfmGroups.next(value);
  }

  get pmfmGroups(): ObjectMap<number[]> {
    return this._$pmfmGroups.getValue();
  }

  constructor(
    protected injector: Injector,
    protected programService: ProgramService,
    protected strategyService: StrategyService
  ) {
    super(injector,
      <SamplesTableOptions>{
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: SAMPLE_RESERVED_START_COLUMNS,
        reservedEndColumns: SAMPLE_RESERVED_END_COLUMNS,
        mapPmfms: pmfms => this.mapPmfms(pmfms)
      }
    );
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
   * Force to wait PMFM map to be loaded
   * @param pmfms
   */
  protected async mapPmfms(pmfms: PmfmStrategy[]): Promise<PmfmStrategy[]> {
    if (isEmptyArray(pmfms)) return pmfms;

    // Wait until map is loaded
    const groupedPmfmIdsMap = await firstNotNilPromise(this._$pmfmGroups);

    // Create a list of known pmfm ids
    const groupedPmfmIds = Object.values(groupedPmfmIdsMap).reduce((res, pmfmIds) => res.concat(...pmfmIds), []);

    // Create pmfms group
    let orderedPmfmIds: number[] = [];
    let orderedPmfms: PmfmStrategy[] = [];
    let groupIndex = 0;
    const pmfmGroupColumns: GroupColumnDefinition[] = SAMPLE_PARAMETER_GROUPS.reduce((res, group) => {
      let groupPmfms: PmfmStrategy[];
      if (group === 'OTHER') {
        groupPmfms = pmfms.filter(p => !groupedPmfmIds.includes(p.pmfmId));
      }
      else {
        const groupPmfmIds = groupedPmfmIdsMap[group];
        if (isNotEmptyArray(groupPmfmIds)) {
          groupPmfms = pmfms.filter(p => groupPmfmIds.includes(p.pmfmId));
        }
      }

      if (isEmptyArray(groupPmfms)) return res; // Skip group

      orderedPmfms = orderedPmfms.concat(groupPmfms);
      const groupPmfmCount = groupPmfms.length;
      const cssClass = (++groupIndex) % 2 === 0 ? 'odd' : 'even';
      return res.concat(
          ...groupPmfms.reduce((res, pmfm, index) => {
            if (orderedPmfmIds.includes(pmfm.pmfmId)) return res; // Skip
            orderedPmfmIds.push(pmfm.pmfmId);
            return res.concat(<GroupColumnDefinition>{
              key: pmfm.pmfmId.toString(),
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

  async getMaxRankOrder(): Promise<number> {
    return super.getMaxRankOrder();
  }

  /* -- protected methods -- */

  async openAddPmfmsModal(event?: UIEvent): Promise<any> {
    //const columns = this.displayedColumns;
    const existingPmfmIds = (this.$pmfms.getValue() || []).map(p => p.pmfmId).filter(isNotNil);

    const modal = await this.modalCtrl.create({
      component: SelectReferentialModal,
      componentProps: {
        filter: {
          entityName: 'Pmfm',
          excludedIds: existingPmfmIds
        }
      }
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res) return; // CANCELLED

    console.log('TODO Modal result ', res);
    // this.pmfms = [
    //   ...pmfms,
    //   ...res.pmfms
    // ];

  }

  async openChangePmfmsModal(event?: UIEvent): Promise<any> {
//const columns = this.displayedColumns;
    const existingPmfmIds = (this.$pmfms.getValue() || []).map(p => p.pmfmId).filter(isNotNil);

    const modal = await this.modalCtrl.create({
      component: SelectReferentialModal,
      componentProps: {
        filter: {
          entityName: 'Pmfm',
          excludedIds: existingPmfmIds
        }
      }
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res) return; // CANCELLED

    console.log('TODO Modal result ', res);
    // this.pmfms = [
    //   ...pmfms,
    //   ...res.pmfms
    // ];

    // Apply new pmfm
    //this.markForCheck();
  }


}
