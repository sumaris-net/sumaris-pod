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
import {environment, IReferentialRef, isNil, ReferentialRef, referentialToString} from "../../core/core.module";
import {SampleValidatorService} from "../services/validator/sample.validator";
import {isNilOrBlank, isNotNil} from "../../shared/functions";
import {UsageMode} from "../../core/services/model/settings.model";
import * as moment from "moment";
import {Moment} from "moment";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryEntitiesService} from "../../shared/services/memory-entity-service.class";
import {SampleModal} from "./sample.modal";
import {FormGroup} from "@angular/forms";
import {TaxonGroupRef, TaxonNameRef} from "../../referential/services/model/taxon.model";
import {Sample} from "../services/model/sample.model";
import {getPmfmName, PmfmStrategy} from "../../referential/services/model/pmfm-strategy.model";
import {AcquisitionLevelCodes} from "../../referential/services/model/model.enum";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";

export interface SampleFilter {
  operationId?: number;
  landingId?: number;
}

export const SAMPLE_RESERVED_START_COLUMNS: string[] = ['label', 'taxonGroup', 'taxonName', 'sampleDate'];
export const SAMPLE_RESERVED_END_COLUMNS: string[] = ['comments'];


@Component({
  selector: 'app-samples-table2',
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

  @Input()
  set value(data: Sample[]) {
    this.memoryDataService.value = data;
  }

  get value(): Sample[] {
    return this.memoryDataService.value;
  }

  @Input() usageMode: UsageMode;
  @Input() showLabelColumn = false;
  @Input() showCommentsColumn = true;
  @Input() showDateTimeColumn = true;
  @Input() showFabButton = false;

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
        reservedStartColumns: SAMPLE_RESERVED_START_COLUMNS,
        reservedEndColumns: SAMPLE_RESERVED_END_COLUMNS
      }
    );
    this.cd = injector.get(ChangeDetectorRef);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.memoryDataService = (this.dataService as InMemoryEntitiesService<Sample, SampleFilter>);
    this.i18nColumnPrefix = 'TRIP.SAMPLE.TABLE.';
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

    // Taxon group
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
        showTaxonGroup: this.showTaxonGroupColumn,
        showTaxonName: this.showTaxonNameColumn,
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
}

