import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit, Optional,
  Output
} from "@angular/core";
import {TableElement, ValidatorService} from "@e-is/ngx-material-table";
import {SampleValidatorService} from "../services/validator/sample.validator";
import {isEmptyArray, isNil, isNilOrBlank, isNotNil, toNumber} from "../../shared/functions";
import {UsageMode} from "../../core/services/model/settings.model";
import * as momentImported from "moment";
const moment = momentImported;
import {Moment} from "moment";
import {AppMeasurementsTable, AppMeasurementsTableOptions} from "../measurement/measurements.table.class";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {SampleModal, ISampleModalOptions} from "./sample.modal";
import {FormGroup} from "@angular/forms";
import {TaxonGroupRef, TaxonNameRef} from "../../referential/services/model/taxon.model";
import {Sample} from "../services/model/sample.model";
import {PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {PlatformService} from "../../core/services/platform.service";
import {BatchGroup} from "../services/model/batch-group.model";
import {IReferentialRef, ReferentialRef} from "../../core/services/model/referential.model";
import {environment} from "../../../environments/environment";

export interface SampleFilter {
  operationId?: number;
  landingId?: number;
}

export class LandingEditorOptions extends AppMeasurementsTableOptions<Sample> {

}

export const SAMPLE_RESERVED_START_COLUMNS: string[] = ['label', 'taxonGroup', 'taxonName', 'sampleDate'];
export const SAMPLE_RESERVED_END_COLUMNS: string[] = ['comments'];
export const SAMPLE_TABLE_DEFAULT_I18N_PREFIX = 'TRIP.SAMPLE.TABLE.SAMPLING.';

@Component({
  selector: 'app-samples-table',
  templateUrl: 'samples.table.html',
  styleUrls: ['samples.table.scss'],
  providers: [
    {provide: ValidatorService, useExisting: SampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplesTable extends AppMeasurementsTable<Sample, SampleFilter>
  implements OnInit {

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryEntitiesService<Sample, SampleFilter>;

  @Input() i18nFieldPrefix = SAMPLE_TABLE_DEFAULT_I18N_PREFIX;
  @Input('useSticky') useSticky = false;

  @Input()
  set value(data: Sample[]) {
    this.memoryDataService.value = data;
  }

  get value(): Sample[] {
    return this.memoryDataService.value;
  }

  @Input() usageMode: UsageMode;
  @Input() showLabelColumn = false;
  @Input() showDateTimeColumn = true;
  @Input() showFabButton = false;

  @Input() modalOptions: Partial<ISampleModalOptions>;

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
  }

  get showTaxonNameColumn(): boolean {
    return this.getShowColumn('taxonName');
  }

  @Input() defaultSampleDate: Moment;
  @Input() defaultTaxonGroup: ReferentialRef;
  @Input() defaultTaxonName: ReferentialRef;

  @Output() onInitForm = new EventEmitter<{form: FormGroup, pmfms: PmfmStrategy[]}>();

  constructor(
    injector: Injector,
    @Optional() options?: LandingEditorOptions
  ) {
    super(injector,
      Sample,
      new InMemoryEntitiesService<Sample, SampleFilter>(Sample, {
        equals: Sample.equals
      }),
      injector.get(PlatformService).mobile ? null : injector.get(ValidatorService),
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        reservedStartColumns: SAMPLE_RESERVED_START_COLUMNS,
        reservedEndColumns: SAMPLE_RESERVED_END_COLUMNS,
        ...options
      }
    );
    this.cd = injector.get(ChangeDetectorRef);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.memoryDataService = (this.dataService as InMemoryEntitiesService<Sample, SampleFilter>);
    this.i18nColumnPrefix = 'TRIP.SAMPLE.TABLE.';
    this.inlineEdition = !this.mobile;
    this.defaultSortBy = 'rankOrder';
    this.defaultSortDirection = 'asc';

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
    this.setShowColumn('sampleDate', this.showDateTimeColumn);
    this.setShowColumn('comments', this.showCommentsColumn);

    // Taxon group combo
    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options)
    });

    // Taxon name combo
    this.registerAutocompleteField('taxonName', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonNames(value, options),
      showAllOnFocus: this.showTaxonGroupColumn /*show all, because limited to taxon group*/
    });
  }

  async getMaxRankOrder(): Promise<number> {
    return super.getMaxRankOrder();
  }

  /* -- protected methods -- */

  protected async suggestTaxonGroups(value: any, options?: any): Promise<IReferentialRef[]> {
    //if (isNilOrBlank(value)) return [];
    return this.programService.suggestTaxonGroups(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute
      });
  }

  protected async suggestTaxonNames(value: any, options?: any): Promise<IReferentialRef[]> {
    const taxonGroup = this.editedRow && this.editedRow.validator.get('taxonGroup').value;

    // IF taxonGroup column exists: taxon group must be filled first
    if (this.showTaxonGroupColumn && isNilOrBlank(value) && isNil(taxonGroup)) return [];

    return this.programService.suggestTaxonNames(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute,
        taxonGroupId: taxonGroup && taxonGroup.id || undefined
      });
  }

  protected async onNewEntity(data: Sample): Promise<void> {
    console.debug("[sample-table] Initializing new row data...");

    await super.onNewEntity(data);

    // generate label
    if (!this.showLabelColumn) {
      data.label = `${this.acquisitionLevel}#${data.rankOrder}`;
    }

    // Default date
    if (isNotNil(this.defaultSampleDate)) {
      data.sampleDate = this.defaultSampleDate;
    } else if (this.settings.isOnFieldMode(this.usageMode)) {
      data.sampleDate = moment();
    }

    // Default taxon name
    if (isNotNil(this.defaultTaxonName)) {
      data.taxonName = TaxonNameRef.fromObject(this.defaultTaxonName);
    }

    // Default taxon group
    if (isNotNil(this.defaultTaxonGroup)) {
      data.taxonGroup = TaxonGroupRef.fromObject(this.defaultTaxonGroup);
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

    const updatedData = await this.openDetailModal(data, row);
    if (updatedData) {
      await this.updateEntityToTable(updatedData, row);
    }
    else {
      this.editedRow = null;
    }
    return true;
  }

  async openDetailModal(sample?: Sample, row?: TableElement<Sample>): Promise<Sample | undefined> {
    console.debug('[samples-table] Opening detail modal...');

    const isNew = !sample && true;
    if (isNew) {
      sample = new Sample();
      await this.onNewEntity(sample);
    }

    this.markAsLoading();

    const modal = await this.modalCtrl.create({
      component: SampleModal,
      componentProps: <ISampleModalOptions>{
        program: undefined, // Prefer to pass PMFMs directly, to avoid a reloading
        pmfms: this.$pmfms,
        acquisitionLevel: this.acquisitionLevel,
        disabled: this.disabled,
        value: sample,
        isNew,
        usageMode: this.usageMode,
        showLabel: this.showLabelColumn,
        showDateTime: this.showDateTimeColumn,
        showTaxonGroup: this.showTaxonGroupColumn,
        showTaxonName: this.showTaxonNameColumn,
        onReady: (obj) => this.onInitForm && this.onInitForm.emit({form: obj.form.form, pmfms: obj.$pmfms.getValue()}),
        onSaveAndNew: async (data) => {
          if (isNil(data.id)) {
            await this.addEntityToTable(data);
          }
          else {
            this.updateEntityToTable(data, row);
            row = null; // Avoid to update twice (should never occur, because validateAndContinue always create a new entity)
          }
          const newData = new Sample();
          await this.onNewEntity(newData);
          return newData;
        },
        onDelete: (event, data) => this.delete(event, data),

        // Override using given options
        ...this.modalOptions,
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

  filterColumnsByTaxonGroup(taxonGroup: TaxonGroupRef) {
    const toggleLoading = !this.loading;
    if (toggleLoading) this.markAsLoading();
    const taxonGroupId = toNumber(taxonGroup && taxonGroup.id, null);
    (this.$pmfms.getValue() || []).forEach(pmfm => {
      const show = isNil(taxonGroupId) || isEmptyArray(pmfm.taxonGroupIds) || pmfm.taxonGroupIds.includes(taxonGroupId);
      this.setShowColumn(pmfm.pmfmId.toString(), show);
    });

    this.updateColumns();
    if (toggleLoading) this.markAsLoaded();
  }

  /* -- protected methods -- */

  protected prepareEntityToSave(sample: Sample) {
    // Override by subclasses
  }

  protected async findRowBySample(data: Sample): Promise<TableElement<Sample>> {
    if (!data || isNil(data.rankOrder)) throw new Error("Missing argument data or data.rankOrder");
    return (await this.dataSource.getRows())
      .find(r => r.currentData.rankOrder === data.rankOrder);
  }

  async delete(event: UIEvent, data: Sample): Promise<boolean> {
    const row = await this.findRowBySample(data);

    // Row not exists: OK
    if (!row) return true;

    const canDeleteRow = await this.canDeleteRows([row]);
    if (canDeleteRow === true) {
      this.cancelOrDelete(event, row, true /*already confirmed*/);
    }
    return canDeleteRow;
  }

  public markForCheck() {
    this.cd.markForCheck();
  }
}

