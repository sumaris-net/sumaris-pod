import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  Output, ViewChild
} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {
  environment,
  IReferentialRef,
  isNil,
  ReferentialRef,
  referentialToString, RESERVED_END_COLUMNS,
  RESERVED_START_COLUMNS
} from "../../core/core.module";
import {SampleValidatorService} from "../services/validator/sample.validator";
import {isNilOrBlank, isNotNil} from "../../shared/functions";
import {UsageMode} from "../../core/services/model/settings.model";
import * as moment from "moment";
import {Moment} from "moment";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {SampleModal} from "./sample.modal";
import {FormGroup} from "@angular/forms";
import {TaxonNameRef} from "../../referential/services/model/taxon.model";
import {Sample} from "../services/model/sample.model";
import {getPmfmName, PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {BATCH_RESERVED_END_COLUMNS, BATCH_RESERVED_START_COLUMNS} from "../batch/table/batches.table";
import {FormFieldDefinition} from "../../shared/form/field.model";
import {BehaviorSubject} from "rxjs";

export interface SampleFilter {
  operationId?: number;
  landingId?: number;
}
export const SAMPLE2_RESERVED_START_COLUMNS: string[] = ['sampleCode','morseCode','comment'/*,'weight','totalLenghtCm','totalLenghtMm','indexGreaseRate'*/];
export const SAMPLE2_RESERVED_END_COLUMNS: string[] = [];

declare interface ColumnDefinition extends FormFieldDefinition {
  computed: boolean;
  unitLabel?: string;
  rankOrder: number;
  qvIndex: number;
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

  static BASE_DYNAMIC_COLUMNS = [
    // Column on total (weight, nb indiv)
    {
      type: 'double',
      key: 'TOTAL_WEIGHT',
      label: 'TRIP.BATCH.TABLE.TOTAL_WEIGHT',
      minValue: 0,
      maxValue: 10000,
      maximumNumberDecimals: 1
    },
    {
      type: 'double',
      key: 'TOTAL_INDIVIDUAL_COUNT',
      label: 'TRIP.BATCH.TABLE.TOTAL_INDIVIDUAL_COUNT',
      minValue: 0,
      maxValue: 10000,
      maximumNumberDecimals: 2
    },

    // Column on sampling (ratio, nb indiv, weight)
    {
      type: 'integer',
      key: 'SAMPLING_RATIO',
      label: 'TRIP.BATCH.TABLE.SAMPLING_RATIO',
      unitLabel: '%',
      minValue: 0,
      maxValue: 100,
      maximumNumberDecimals: 2
    },
    {
      type: 'double',
      key: 'SAMPLING_WEIGHT',
      label: 'TRIP.BATCH.TABLE.SAMPLING_WEIGHT',
      minValue: 0,
      maxValue: 1000,
      maximumNumberDecimals: 1
    },
    {
      type: 'string',
      key: 'SAMPLING_INDIVIDUAL_COUNT',
      label: 'TRIP.BATCH.TABLE.SAMPLING_INDIVIDUAL_COUNT',
      computed: true
    }
  ];


  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryEntitiesService<Sample, SampleFilter>;

  public appliedPmfmStrategies : PmfmStrategy []=[];


  @Input()
  set value(data: Sample[]) {
    this.memoryDataService.value = data;
    let samplesWithDefinedTaxonOnly = data.filter(sample => sample.taxonName);
    if (samplesWithDefinedTaxonOnly && samplesWithDefinedTaxonOnly[0])
    {
      this.defaultTaxonName = data[0].taxonName;
    }
  }

  @Input()
  set appliedPmfmStrategy(data: PmfmStrategy[]) {
    this.appliedPmfmStrategies = data;
  }

  get value(): Sample[] {
    return this.memoryDataService.value;
  }

  get $dynamicPmfms(): BehaviorSubject<PmfmStrategy[]> {
    return this.measurementsDataService.$pmfms;
  }

  @Input() usageMode: UsageMode;
  @Input() showLabelColumn = false;
  // @Input() showCommentsColumn = true;
  // @Input() showDateTimeColumn = true;
  @Input() showFabButton = false;
  /*
    @Input()
    set showTaxonGroupColumn(value: boolean) {
      this.setShowColumn('taxonGroup', value);
    }

    get showTaxonGroupColumn(): boolean {
      return this.getShowColumn('taxonGroup');
    }

    @Input()
    set showTaxonNameColumn(value: boolean) {
      this.setShowColumn('taxonName', value);
    }*/

  /*get showTaxonNameColumn(): boolean {
    return this.getShowColumn('taxonName');
  }*/

  @Input() defaultSampleDate: Moment;
  @Input() defaultTaxonGroup: ReferentialRef;
  @Input() defaultTaxonName: ReferentialRef;

  @Output() onInitForm = new EventEmitter<{form: FormGroup, pmfms: PmfmStrategy[]}>();

  dynamicColumns: ColumnDefinition[];

  constructor(
    injector: Injector
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
        reservedEndColumns: SAMPLE2_RESERVED_END_COLUMNS
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
        this.onStartEditingRow.subscribe(row => this.onInitForm.emit({
          form: row.validator,
          pmfms: this.$pmfms.getValue()
        })));
    }
  }

  ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('label', this.showLabelColumn);
    // this.setShowColumn('sampleDate', this.showDateTimeColumn);
    //  this.setShowColumn('comments', this.showCommentsColumn);

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


  protected getDisplayColumns(): string[] {

    const pmfms = this.$pmfms.getValue();
    if (!pmfms) return this.columns;

    const userColumns = this.getUserColumns();

    let dynamicWeightColumnNames = [];
    let dynamicSizeColumnNames = [];
    let dynamicMaturityColumnNames = [];
    let dynamicSexColumnNames = [];
    let dynamicAgeColumnNames = [];
    let dynamicOthersColumnNames = [];

    // FIXME CLT WIP
    // filtrer sur les pmfms pour les mettres dans les différents tableaux

    const pmfmColumnNames = pmfms
      //.filter(p => p.isMandatory || !userColumns || userColumns.includes(p.pmfmId.toString()))
      .map(p => p.pmfmId.toString());

    const startColumns = (this.options && this.options.reservedStartColumns || []).filter(c => !userColumns || userColumns.includes(c));
    const endColumns = (this.options && this.options.reservedEndColumns || []).filter(c => !userColumns || userColumns.includes(c));

    // const dynamicPmfmColumnNames = [];
    // const dynamicPmfmColumnNames = (pmfms || [])
    //   .map(c => {
    //     return {
    //       id: c.id,
    //       rankOrder: c.rankOrder + (inverseOrder &&
    //         ((c.key.endsWith('_WEIGHT') && 1) || (c.key.endsWith('_INDIVIDUAL_COUNT') && -1)) || 0)
    //     };
    //   })
    //   .sort((c1, c2) => c1.rankOrder - c2.rankOrder)
    //   .map(c => c.key);
    let dynamicPmfmColumnNames = pmfmColumnNames.sort((a, b) => b.localeCompare(a));

    return RESERVED_START_COLUMNS
      .concat(startColumns)
      // .concat(pmfmColumnNames)
      .concat(dynamicPmfmColumnNames)
      .concat(endColumns)
      .concat(RESERVED_END_COLUMNS)
      // Remove columns to hide
      .filter(column => !this.excludesColumns.includes(column));

    //console.debug("[measurement-table] Updating columns: ", this.displayedColumns)
    //if (!this.loading) this.markForCheck();
  }

  protected getDisplayColumns2(): string[] {
    if (!this.dynamicColumns) return this.columns;

    const userColumns = this.getUserColumns();

    const weightIndex = userColumns.findIndex(c => c === 'weight');
    let individualCountIndex = userColumns.findIndex(c => c === 'individualCount');
    individualCountIndex = (individualCountIndex !== -1 && weightIndex === -1 ? 0 : individualCountIndex);
    const inverseOrder = individualCountIndex < weightIndex;

    const dynamicColumnKeys = (this.dynamicColumns || [])
      .map(c => {
        return {
          key: c.key,
          rankOrder: c.rankOrder + (inverseOrder &&
            ((c.key.endsWith('_WEIGHT') && 1) || (c.key.endsWith('_INDIVIDUAL_COUNT') && -1)) || 0)
        };
      })
      .sort((c1, c2) => c1.rankOrder - c2.rankOrder)
      .map(c => c.key);

    return RESERVED_START_COLUMNS
      .concat(SAMPLE2_RESERVED_START_COLUMNS)
      .concat(dynamicColumnKeys)
      .concat(SAMPLE2_RESERVED_END_COLUMNS)
      .concat(RESERVED_END_COLUMNS)
      .filter(name => !this.excludesColumns.includes(name));
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

      let taxonGroup = await  this.getTaxoGroupByTaxonNameId(this.defaultTaxonName.id,  "TaxonGroup");
      data.taxonGroup = TaxonNameRef.fromObject(taxonGroup[0]);
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

  referentialToString = referentialToString;
  getPmfmColumnHeader = getPmfmName;

  public markForCheck() {
    this.cd.markForCheck();
  }


  /**
   * getTaxonGroup
   * @param id
   * @param entityName
   * @protected
   */
  protected async getTaxoGroupByTaxonNameId(id : any, entityName : string){
    const res = await this.referentialRefService.loadAll(0, 100, null,null,
      {
        entityName: entityName,
        id : id
      },
      {
        withTotal: false
      });
    return res.data;

  }
}
