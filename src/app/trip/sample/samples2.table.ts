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
import {SampleValidatorService} from "../services/validator/sample.validator";
import {isEmptyArray, isNotEmptyArray, isNotNil} from "../../shared/functions";
import {UsageMode} from "../../core/services/model/settings.model";
import * as moment from "moment";
import {Moment} from "moment";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {SampleModal} from "./sample.modal";
import {FormGroup} from "@angular/forms";
import {TaxonNameRef} from "../../referential/services/model/taxon.model";
import {Sample} from "../services/model/sample.model";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {FormFieldDefinition} from "../../shared/form/field.model";
import {RESERVED_END_COLUMNS, RESERVED_START_COLUMNS} from "../../core/table/table.class";
import {ReferentialRef} from "../../core/services/model/referential.model";
import {environment} from "../../../environments/environment";
import {TableAddPmfmsComponent} from "./table-add-pmfms.component";
import {ProgramService} from "../../referential/services/program.service";
import {StrategyService} from "../../referential/services/strategy.service";
import {PmfmService} from "../../referential/services/pmfm.service";
import {BehaviorSubject} from "rxjs";
import {ObjectMap} from "../../shared/types";
import {firstNotNilPromise} from "../../shared/observables";
import {SelectReferentialModal} from "../../referential/list/select-referential.modal";

export interface SampleFilter {
  operationId?: number;
  landingId?: number;
}
export const SAMPLE2_RESERVED_START_COLUMNS: string[] = ['sampleCode', 'morseCode', 'comment' /*,'weight','totalLenghtCm','totalLenghtMm','indexGreaseRate'*/];
export const SAMPLE2_RESERVED_END_COLUMNS: string[] = [];

const SAMPLE_PARAMETER_GROUPS = ['WEIGHT', 'LENGTH', 'MATURITY', 'SEX', 'AGE', 'OTHER'];

declare interface ColumnDefinition extends FormFieldDefinition {
  computed: boolean;
  unitLabel?: string;
  rankOrder: number;
  groupIndex: number;
}


@Component({
  selector: 'app-samples2-table',
  templateUrl: 'samples2.table.html',
  styleUrls: ['samples2.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: SampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Samples2Table extends AppMeasurementsTable<Sample, SampleFilter>
  implements OnInit, OnDestroy {

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryEntitiesService<Sample, SampleFilter>;
  protected _$pmfmGroups = new BehaviorSubject<ObjectMap<number[]>>(null);

  @Input() i18nFieldPrefix = 'TRIP.SAMPLE.TABLE.SAMPLING.';

  @Input() set pmfmGroups(value: ObjectMap<number[]>) {
    this._$pmfmGroups.next(value);
  }

  get pmfmGroups(): ObjectMap<number[]> {
    return this._$pmfmGroups.getValue();
  }

  @Input()
  set value(data: Sample[]) {
    this.memoryDataService.value = data;
  }

  get value(): Sample[] {
    return this.memoryDataService.value;
  }

  dynamicColumns: ColumnDefinition[];

  @Input() usageMode: UsageMode;
  @Input() showLabelColumn = false;
  @Input() showDateTimeColumn = true;
  @Input() showFabButton = false;

  @Input() defaultSampleDate: Moment;
  @Input() defaultTaxonName: ReferentialRef;

  // tslint:disable-next-line:no-output-on-prefix
  @Output() onInitForm = new EventEmitter<{form: FormGroup, pmfms: PmfmStrategy[]}>();

  constructor(
    protected injector: Injector,
    protected programService: ProgramService,
    protected strategyService: StrategyService
  ) {
    super(injector,
      Sample,
      new InMemoryEntitiesService<Sample, SampleFilter>(Sample, {
        equals: Sample.equals
      }),
      injector.get(ValidatorService),
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: SAMPLE2_RESERVED_START_COLUMNS,
        reservedEndColumns: SAMPLE2_RESERVED_END_COLUMNS,
        mapPmfms: pmfms => this.mapPmfms(pmfms)
      }
    );
    this.cd = injector.get(ChangeDetectorRef);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.memoryDataService = (this.dataService as InMemoryEntitiesService<Sample, SampleFilter>);
    this.i18nColumnPrefix = 'TRIP.SAMPLE2.TABLE.';
    this.inlineEdition = !this.mobile;

    // Set default value
    this.acquisitionLevel = AcquisitionLevelCodes.SAMPLE; // Default value, can be override by subclasses

    //this.debug = false;
    this.debug = !environment.production;

    // If init form callback exists, apply it when start row edition
    if (this.onInitForm) {
      this.registerSubscription(
        this.onStartEditingRow.subscribe(row => row && this.onInitForm.emit({
              form: row.validator,
              pmfms: this.$pmfms.getValue()
            })));
    }
  }

  ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('label', this.showLabelColumn);
    // this.setShowColumn('sampleDate', this.showDateTimeColumn);
    // this.setShowColumn('comments', this.showCommentsColumn);

    // Taxon group combo
    /* this.registerAutocompleteField('taxonGroup', {
       suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options)
     });*/

    // Taxon name combo
    /*this.registerAutocompleteField('taxonName', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonNames(value, options),
      showAllOnFocus: this.showTaxonGroupColumn /*show all, because limited to taxon group*/
    //  });



  }

  /**
   * Use in ngFor, for trackBy
   * @param index
   * @param column
   */
  trackColumnDef(index: number, column: ColumnDefinition) {
    return column.rankOrder;
  }

  isGroupEven(column: ColumnDefinition) {
    return (column.groupIndex % 2 === 0);
  }

  isGroupOdd(column: ColumnDefinition) {
    return (column.groupIndex % 2 !== 0);
  }

  getFlexSize(columns: ColumnDefinition[], column: ColumnDefinition) {
    let columnSize = 0;
    let columnDisplayLabel = false;
    columns.forEach(colIter => {
      if (colIter.defaultValue === column.defaultValue)
      {
        columnSize = columnSize + 1;
        if ((columnSize === 1) && colIter === column) {
          columnDisplayLabel = true;
        }
      }
    });
    if (columnDisplayLabel)
    {
      return columnSize;
    }
    return 0;
  }

  /**
   * Force to wait PMFM map to be loaded
   * @param pmfms
   */
  protected async mapPmfms(pmfms: PmfmStrategy[]): Promise<PmfmStrategy[]> {

    // Wait until map is loaded
    await firstNotNilPromise(this._$pmfmGroups);

    return pmfms;
  }

  protected getDisplayColumns(): string[] {

    const pmfms = this.$pmfms.getValue();
    const pmfmIdsMap = this.pmfmGroups;

    if (!pmfms || !pmfmIdsMap) return this.columns;

    const userColumns = this.getUserColumns();
    const startColumns = (this.options && this.options.reservedStartColumns || []).filter(c => !userColumns || userColumns.includes(c));
    const endColumns = (this.options && this.options.reservedEndColumns || []).filter(c => !userColumns || userColumns.includes(c));

    // Group pmfms by parameter group label
    const allPmfmIds = Object.values(pmfmIdsMap).reduce((res, pmfmIds) => res.concat(pmfmIds), []);
    let pmfmColumnNames = [];
    const columnNamesByGroup = pmfms && SAMPLE_PARAMETER_GROUPS.reduce((res, group) => {
      let columnNames: string[];
      if (group === 'OTHER') {
        columnNames = pmfms.filter(p => !allPmfmIds.includes(p.pmfmId)).map(p => p.pmfmId.toString());
      }
      else {
        const groupPmfmIds = pmfmIdsMap[group];
        if (isNotEmptyArray(groupPmfmIds)) {
          columnNames = pmfms.filter(p => groupPmfmIds.includes(p.pmfmId)).map(p => p.pmfmId.toString());
        }
      }

      if (isNotEmptyArray(columnNames)) {
        res[group] = columnNames;
        pmfmColumnNames = pmfmColumnNames.concat(...columnNames);
      }
      return res;
    }, {}) || {};

    let groupIndex = 0;
    let rankOrderIdx = 1; // TODO to delete
    this.dynamicColumns = SAMPLE_PARAMETER_GROUPS.reduce((res, group) => {
      const columnNames = columnNamesByGroup[group];
      if (isEmptyArray(columnNames)) return res; // Skip
      groupIndex++;
      return res.concat(columnNames.map(columnName => {
        return <ColumnDefinition>{
          key: columnName,
          label: this.i18nFieldPrefix + group,
          defaultValue: group,
          type: 'string',
          computed : false,
          groupIndex : groupIndex,
          rankOrder : rankOrderIdx++,
          disabled : false
        };
      }));
    }, []);


    return RESERVED_START_COLUMNS
      .concat(startColumns)
      .concat(pmfmColumnNames)
      .concat(endColumns)
      .concat(RESERVED_END_COLUMNS)
      // Remove columns to hide
      .filter(column => !this.excludesColumns.includes(column));
  }

  async getMaxRankOrder(): Promise<number> {
    return super.getMaxRankOrder();
  }

  /* -- protected methods -- */

  /*protected async suggestTaxonGroups(value: any, options?: any): Promise<IReferentialRef[]> {
    //if (isNilOrBlank(value)) return [];
    return this.programService.suggestTaxonGroups(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute
      });
  }*/

  /*protected async suggestTaxonNames(value: any, options?: any): Promise<IReferentialRef[]> {
    const taxonGroup = this.editedRow && this.editedRow.validator.get('taxonGroup').value;

    // IF taxonGroup column exists: taxon group must be filled first
    if (this.showTaxonGroupColumn && isNilOrBlank(value) && isNil(taxonGroup)) return [];

    return this.programService.suggestTaxonNames(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute,
        taxonGroupId: taxonGroup && taxonGroup.id || undefined
      });
  }*/

  protected async onNewEntity(data: Sample): Promise<void> {
    console.debug("[sample-table] Initializing new row data...");

    await super.onNewEntity(data);

    // Default date
    if (isNotNil(this.defaultSampleDate)) {
      data.sampleDate = this.defaultSampleDate;
    } else if (this.settings.isOnFieldMode(this.usageMode)) {
      data.sampleDate = moment();
    }

    // set  taxonName, taxonGroup
    if (isNotNil(this.defaultTaxonName)) {
      data.taxonName = TaxonNameRef.fromObject(this.defaultTaxonName);
    }

  }

  protected async openNewRowDetail(): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    const data = await this.openDetailModal();
    if (data) {
      await this.addEntityToTable(data);
    }
    return true;
  }

  protected async openRow(id: number, row: TableElement<Sample>): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    if (this.onOpenRow.observers.length) {
      this.onOpenRow.emit({id, row});
      return true;
    }

    const data = this.toEntity(row, true);

    // Prepare entity measurement values
    this.prepareEntityToSave(data);

    const updatedData = await this.openDetailModal(data);
    if (updatedData) {
      await this.updateEntityToTable(updatedData, row);
    }
    else {
      this.editedRow = null;
    }
    return true;
  }

  async openDetailModal(sample?: Sample): Promise<Sample | undefined> {
    const isNew = !sample && true;
    if (isNew) {
      sample = new Sample();
      await this.onNewEntity(sample);
    }

    this.markAsLoading();

    const modal = await this.modalCtrl.create({
      component: SampleModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: this.acquisitionLevel,
        disabled: this.disabled,
        value: sample,
        isNew,
        showLabel: this.showLabelColumn,
        //  showTaxonGroup: this.showTaxonGroupColumn,
        //   showTaxonName: this.showTaxonNameColumn,
        onReady: (obj) => this.onInitForm && this.onInitForm.emit({form: obj.form.form, pmfms: obj.$pmfms.getValue()})
      },
      keyboardClose: true
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug("[samples-table] Modal result: ", data);
    this.markAsLoaded();

    // Exit if empty
    if (!(data instanceof Sample)) {
      return undefined;
    }

    return data;
  }


  /* -- protected methods -- */

  protected prepareEntityToSave(sample: Sample) {
    // Override by subclasses
  }

  public markForCheck() {
    this.cd.markForCheck();
  }


  /**
   * getTaxonGroup
   * @param id
   * @param entityName
   * @protected
   */
  protected async getTaxoGroupByTaxonNameId(id: any, entityName: string){
    const res = await this.referentialRefService.loadAll(0, 100, null, null,
      { entityName, id },
      { withTotal: false });
    return res.data;

  }



  protected async addRowToTable(): Promise<TableElement<Sample>> {
    this.focusFirstColumn = true;
    await this._dataSource.asyncCreateNew();
    this.editedRow = this._dataSource.getRow(-1);
    const sample = this.editedRow.currentData;
    // Initialize default parameters
    await this.onNewEntity(sample);
    // Update row
    await this.updateEntityToTable(sample, this.editedRow);

    // Emit start editing event
    this.onStartEditingRow.emit(this.editedRow);
    this._dirty = true;
    this.resultsLength++;
    this.visibleRowCount++;
    this.markForCheck();
    return this.editedRow;
  }


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

    /*const modal = await this.modalCtrl.create({
      component: TableAddPmfmsComponent,
      componentProps: {
        excludedIds: existingPmfmIds
      }
    });*/

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res) return; // CANCELLED

    console.log('TODO Modal result ', res);
    /*this.pmfms = [
      ...pmfms,
      ...res.pmfms
    ];*/

  }

  async openChangePmfmsModal(event?: UIEvent): Promise<any> {
    const modal = await this.modalCtrl.create({
      component: TableAddPmfmsComponent,
      componentProps: {}
    });

    // Open the modal
    await modal.present();

    // On dismiss
    const res = await modal.onDidDismiss();
    if (!res) return; // CANCELLED

    // Apply new pmfm
    this.markForCheck();
  }


}
