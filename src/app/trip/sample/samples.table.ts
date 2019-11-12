import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component, EventEmitter,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  Output
} from "@angular/core";
import {Observable} from 'rxjs';
import {debounceTime, switchMap, tap} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {environment, IReferentialRef, isNil, ReferentialRef} from "../../core/core.module";
import {
  getPmfmName,
  Landing,
  Operation,
  PhysicalGear, PmfmStrategy,
  referentialToString,
  Sample,
  TaxonGroupIds
} from "../services/trip.model";
import {AcquisitionLevelCodes, ReferentialRefService, TaxonomicLevelIds} from "../../referential/referential.module";
import {SampleValidatorService} from "../services/sample.validator";
import {isNilOrBlank, isNotNil} from "../../shared/shared.module";
import {UsageMode} from "../../core/services/model";
import * as moment from "moment";
import {Moment} from "moment";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {PhysicalGearModal} from "../physicalgear/physicalgear.modal";
import {SampleModal} from "./sample.modal";
import {AuctionControlValidators} from "../services/validator/auction-control.validators";
import {FormGroup} from "@angular/forms";

export const SAMPLE_RESERVED_START_COLUMNS: string[] = ['label', 'taxonGroup', 'taxonName', 'sampleDate'];
export const SAMPLE_RESERVED_END_COLUMNS: string[] = ['comments'];

export interface SampleFilter {
  operationId?: number;
  landingId?: number;
}

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
  implements OnInit, OnDestroy {

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryTableDataService<Sample, SampleFilter>;

  @Input()
  set value(data: Sample[]) {
    this.memoryDataService.value = data;
  }

  get value(): Sample[] {
    return this.memoryDataService.value;
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
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
      new InMemoryTableDataService<Sample, SampleFilter>(Sample, {
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
    this.memoryDataService = (this.dataService as InMemoryTableDataService<Sample, SampleFilter>);
    this.i18nColumnPrefix = 'TRIP.SAMPLE.TABLE.';
    this._acquisitionLevel = AcquisitionLevelCodes.SAMPLE; // Default value, can be override by subclasses
    this.inlineEdition = true;

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
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options),
      showAllOnFocus: true
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

    const isOnFieldMode = this.isOnFieldMode;

    // generate label
    if (!this.showLabelColumn) {
      data.label = `${this.acquisitionLevel}#${data.rankOrder}`;
    }

    // Default date
    if (isNotNil(this.defaultSampleDate)) {
      data.sampleDate = this.defaultSampleDate;
    } else if (isOnFieldMode) {
      data.sampleDate = moment();
    }

    // Taxon group
    if (isNotNil(this.defaultTaxonName)) {
      data.taxonName = ReferentialRef.fromObject(this.defaultTaxonName);
    }

    // Default taxon group
    if (isNotNil(this.defaultTaxonGroup)) {
      data.taxonGroup = ReferentialRef.fromObject(this.defaultTaxonGroup);
    }
  }

  protected async openNewRowDetail(): Promise<boolean> {
    if (!this.allowRowDetail) return false;

    const sample = await this.openDetailModal();
    if (sample) {
      await this.addSampleToTable(sample);
    }
    return true;
  }

  protected async openRow(id: number, row: TableElement<Sample>): Promise<boolean> {
    const data = row.validator ? Sample.fromObject(row.currentData) : row.currentData;

    const updatedData = await this.openDetailModal(data);
    if (updatedData) {
      await this.addSampleToTable(updatedData, row);
    }
    return true;
  }

  async openDetailModal(sample?: Sample): Promise<Sample | undefined> {

    const isNew = !sample;
    if (isNew) {
      sample = new Sample();
      await this.onNewEntity(sample);
    }

    const modal = await this.modalCtrl.create({
      component: SampleModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: this.acquisitionLevel,
        disabled: this.disabled,
        value: sample.clone(), // Do a copy, because edition can be cancelled
        isNew: isNew,
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

    return (data instanceof Sample) ? data : undefined;
  }

  protected async addSampleToTable(sample: Sample, row?: TableElement<Sample>): Promise<TableElement<Sample>> {
    if (this.debug) console.debug("[samples-table] Adding new sample", sample);

    // Create a new row, if need
    if (!row) {
      row = await this.addRowToTable();
      if (!row) throw new Error("Could not add row t table");

      // Use the generated rankOrder
      sample.rankOrder = row.currentData.rankOrder;

    }

    // Adapt measurement values to row
    this.normalizeEntityToRow(sample, row);

    // Affect new row
    if (row.validator) {
      row.validator.patchValue(sample);
      row.validator.markAsDirty();
    }
    else {
      row.currentData = sample;
    }

    this.confirmEditCreate(null, row);
    this.markAsDirty();

    return row;
  }

  referentialToString = referentialToString;
  getPmfmColumnHeader = getPmfmName;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}

