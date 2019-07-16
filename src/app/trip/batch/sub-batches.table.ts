import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Inject,
  InjectionToken,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";
import {Observable, Subscription} from 'rxjs';
import {debounceTime, map, mergeMap, tap} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {AcquisitionLevelCodes, AppFormUtils, EntityUtils, environment, ReferentialRef} from "../../core/core.module";
import {Batch, PmfmStrategy, referentialToString} from "../services/trip.model";
import {PmfmIds, QualitativeLabels, ReferentialRefService} from "../../referential/referential.module";
import {FormGroup, Validators} from "@angular/forms";
import {isNil, isNotNil, startsWithUpperCase, toBoolean} from "../../shared/shared.module";
import {FieldOptions, UsageMode} from "../../core/services/model";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {AppMeasurementsTable, AppMeasurementsTableOptions} from "../measurement/measurements.table.class";
import {BatchUtils} from "../services/model/batch.model";
import {SubBatchValidatorService} from "../services/sub-batch.validator";
import {SubBatchForm} from "./sub-batch.form";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {selectInputContent} from "../../core/form/form.utils";

export const SUB_BATCH_RESERVED_START_COLUMNS: string[] = ['parent', 'taxonName'];
export const SUB_BATCH_RESERVED_END_COLUMNS: string[] = ['individualCount', 'comments'];


export const SubBatchesTableOptions = new InjectionToken<AppMeasurementsTableOptions<Batch>>('options');

export interface SubBatchFilter {
  parentId?: number;
  operationId?: number;
  landingId?: number;
}

@Component({
  selector: 'app-sub-batches-table',
  templateUrl: 'sub-batches.table.html',
  styleUrls: ['sub-batches.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: SubBatchValidatorService},
    {
      provide: SubBatchesTableOptions,
      useValue: {
        prependNewElements: false,
        suppressErrors: false,
        reservedStartColumns: SUB_BATCH_RESERVED_START_COLUMNS,
        reservedEndColumns: SUB_BATCH_RESERVED_END_COLUMNS
      }
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchesTable extends AppMeasurementsTable<Batch, SubBatchFilter>
  implements OnInit, OnDestroy {


  private _parentSubscription: Subscription;
  private _availableParents: Batch[] = [];

  protected _availableSortedParents: Batch[] = [];

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryTableDataService<Batch, SubBatchFilter>;
  protected fieldsOptions: {
    taxonName?: FieldOptions
  } = {};

  @Input() displayParentPmfm: PmfmStrategy;

  $taxonNames: Observable<ReferentialRef[]>;
  $filteredParents: Observable<Batch[]>;

  @Input() showForm = false;

  @Input() qvPmfm: PmfmStrategy;

  @Input() tabindex: number;

  @Input()
  set availableParents(parents: Observable<Batch[]> | Batch[]) {
    if (parents instanceof Observable) {
      if (this._parentSubscription) this._parentSubscription.unsubscribe();
      this._parentSubscription = parents.subscribe((values) => this.setAvailableParents(values));
      this.registerSubscription(this._parentSubscription);
    } else if (parents instanceof Array && parents !== this._availableParents) {
      this.setAvailableParents(parents);
    }
  }

  get availableParents(): Observable<Batch[]> | Batch[] {
    return this._availableParents;
  }

  set value(data: Batch[]) {
    this.memoryDataService.value = data;
  }

  get value(): Batch[] {
    return this.memoryDataService.value;
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  @Input()
  set showParentColumn(value: boolean) {
    this.setShowColumn('parent', value);
  }

  get showParentColumn(): boolean {
    return this.getShowColumn('parent');
  }

  @Input()
  set showTaxonNameColumn(value: boolean) {
    this.setShowColumn('taxonName', value);
  }

  get showTaxonNameColumn(): boolean {
    return this.getShowColumn('taxonName');
  }

  @Input()
  set showIndividualCount(value: boolean) {
    this.setShowColumn('individualCount', value);
  }

  get showIndividualCount(): boolean {
    return this.getShowColumn('individualCount');
  }

  get dirty(): boolean {
    return this._dirty || this.memoryDataService.dirty;
  }

  @Input() showCommentsColumn = true;
  @Input() usageMode: UsageMode;

  @ViewChild('form') form: SubBatchForm;

  constructor(
    protected injector: Injector,
    protected validatorService: ValidatorService,
    @Inject(SubBatchesTableOptions) options: AppMeasurementsTableOptions<Batch>
  ) {
    super(injector,
      Batch,
      new InMemoryTableDataService<Batch, SubBatchFilter>(Batch, {
        onSort: (data, sortBy, sortDirection) => this.sortData(data, sortBy, sortDirection),
        onLoad: (data) => {
          this.linkDataToParent(data);
          return data;
        }
      }),
      validatorService,
      options
    );
    this.cd = injector.get(ChangeDetectorRef);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.memoryDataService = (this.dataService as InMemoryTableDataService<Batch, SubBatchFilter>);
    this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
    this.inlineEdition = true;
    this.tabindex = 1;

    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL;

    //this.debug = false;
    this.debug = !environment.production;
  }

  async ngOnInit() {
    super.ngOnInit();

    await this.settings.ready();

    // Read fields options, from settings
    this.fieldsOptions.taxonName = this.settings.getFieldOptions('taxonName');

    this.setShowColumn('comments', this.showCommentsColumn);

    if (this.inlineEdition) { // can be override bu subclasses

      // Parent combo
      this.$filteredParents = this.registerCellValueChanges('parent')
        .pipe(
          debounceTime(250),
          map((value) => {
            if (EntityUtils.isNotEmpty(value)) {
              console.log("TODO: load taxon names ??");
              return [value];
            }
            value = (typeof value === "string" && value !== "*") && value || undefined;
            if (this.debug) console.debug(`[sub-batch-table] Searching parent {${value || '*'}}...`);
            if (isNil(value)) return this._availableSortedParents; // All
            const ucValueParts = value.trim().toUpperCase().split(" ", 1);
            // Search on labels (taxonGroup or taxonName)
            return this._availableSortedParents.filter(p =>
              (p.taxonGroup && startsWithUpperCase(p.taxonGroup.label, ucValueParts[0])) ||
              (p.taxonName && startsWithUpperCase(p.taxonName.label, ucValueParts.length === 2 ? ucValueParts[1] : ucValueParts[0]))
            );
          }),
          // Save implicit value
          tap(res => this.updateImplicitValue('parent', res))
        );

      // Taxon name combo
      this.$taxonNames = this.registerCellValueChanges('taxonName')
        .pipe(
          debounceTime(250),
          mergeMap(async (value) => {
              const parent = this.editedRow.validator.get('parent').value;
              return this.programService.suggestTaxonNames(value,
                {
                  program: this.program,
                  searchAttribute: this.fieldsOptions.taxonName.searchAttribute,
                  taxonGroupId: parent && parent.taxonGroup && parent.taxonGroup.id || undefined
                });
            }
          ),
          // Save implicit value
          tap(items => this.updateImplicitValue('taxonName', items))
        );

      // Listening on column 'IS_DEAD' value changes
      this.registerCellValueChanges('discard', "measurementValues." + PmfmIds.DISCARD_OR_LANDING.toString())
        .subscribe((value) => {
          if (!this.editedRow) return; // Should never occur
          const row = this.editedRow;
          const controls = (row.validator.controls['measurementValues'] as FormGroup).controls;
          if (EntityUtils.isNotEmpty(value) && value.label == QualitativeLabels.DISCARD_OR_LANDING.DISCARD) {
            if (controls[PmfmIds.DISCARD_REASON]) {
              if (row.validator.enabled) {
                controls[PmfmIds.DISCARD_REASON].enable();
              }
              controls[PmfmIds.DISCARD_REASON].setValidators(Validators.required);
              controls[PmfmIds.DISCARD_REASON].updateValueAndValidity();
            }
          } else {
            if (controls[PmfmIds.DISCARD_REASON]) {
              controls[PmfmIds.DISCARD_REASON].disable();
              controls[PmfmIds.DISCARD_REASON].setValue(null);
              controls[PmfmIds.DISCARD_REASON].setValidators([]);
            }
          }
        });
    }
  }

  async toggleForm() {
    if (this.form && !this.showForm) {

      await this.resetForm(null, {focusFirstEmpty: true});
      this.showForm = true;
      this.markForCheck();
    } else if (this.showForm) {
      this.showForm = false;
      this.markForCheck();
    }
  }

  async addBatch(event?: UIEvent) {
    if (this.loading || !this.confirmEditCreate()) return; // skip

    if (this.form.invalid) {
      if (this.debug) AppFormUtils.logFormErrors(this.form.form, "[sub-batch-table] ");
      return;
    }

    const subBatch = this.form.form.value;
    subBatch.individualCount = isNotNil(subBatch.individualCount) ? subBatch.individualCount : 1;

    await this.resetForm(subBatch, {focusFirstEmpty: true});

    // Add the row
    await this.addBatchToTable(subBatch);
  }

  async add(batches: Batch[], opts?: {linkDataToParent?: boolean}) {
    if (opts && toBoolean(opts.linkDataToParent, true)) {
      this.linkDataToParent(batches);
    }

    for (let b of batches) {
      await this.addBatchToTable(b);
    }
  }


  protected async resetForm(previousBatch?: Batch, options?: {focusFirstEmpty?: boolean}) {

    this.form.availableParents = this._availableSortedParents;

    // Create a new batch
    const newBatch = new Batch();
    await this.onNewEntity(newBatch);

    // Reset individual count, if manual mode
    if (this.form.enableIndividualCount) {
      newBatch.individualCount = null;
    } else if (isNil(newBatch.individualCount)) {
      newBatch.individualCount = 1;
    }

    // Copy QV value from previous
    if (previousBatch) {
      // Copy parent
      newBatch.parent = previousBatch.parent;

      // Copy QV PMFM value, if any
      if (this.qvPmfm) {
        newBatch.measurementValues[this.qvPmfm.pmfmId] = previousBatch.measurementValues[this.qvPmfm.pmfmId];
      }

      // Copy taxonName
      newBatch.taxonName = previousBatch.taxonName;
    }

    MeasurementValuesUtils.normalizeFormEntity(newBatch, this.pmfms.getValue(), this.form.form);

    if (this.form.disabled) {
      this.form.enable();
      this.form.value = newBatch;
    } else {
      this.form.form.patchValue(newBatch, {emitEvent: false});
      this.form.markAsPristine();
      this.form.markAsUntouched();
      this.form.form.updateValueAndValidity();
    }

    if (options && toBoolean(options.focusFirstEmpty, false)) {
      setTimeout(() => this.form.focusFirstEmpty());
    }
  }

  async setValueFromParent(parents: Batch[], qvPmfm?: PmfmStrategy) {

    this.qvPmfm = qvPmfm;
    const subBatches = BatchUtils.prepareSubBatchesForTable(parents, this.acquisitionLevel, qvPmfm);
    console.debug("TODO: check subbatches: ", subBatches);

    await this.setAvailableParents(parents, {emitEvent: false, linkDataToParent: false});

    this.value = subBatches;
  }

  markAsPristine() {
    super.markAsPristine();
    this.form.markAsPristine();
  }

  markAsUntouched() {
    super.markAsUntouched();
    this.form.markAsUntouched();
  }

  enable() {
    super.enable();

    if (this.showForm && this.form.disabled) {
      this.form.enable();
    }
  }

  disable() {
    super.disable();

    if (this.showForm && this.form.enabled) {
      this.form.disable();
    }
  }

  /* -- protected methods -- */

  protected async addBatchToTable(newBatch: Batch): Promise<TableElement<Batch>> {
    if (this.debug) console.debug("[batches-table] Adding batch to table:", newBatch);

    // Make sure individual count if init
    newBatch.individualCount = isNotNil(newBatch.individualCount) ? newBatch.individualCount : 1;

    const pmfms = this.pmfms.getValue() || [];
    MeasurementValuesUtils.normalizeFormEntity(newBatch, pmfms);

    const rows = await this.dataSource.getRows();

    let row = this.showIndividualCount ? rows.find(r => BatchUtils.canMergeSubBatch(newBatch, r.currentData, pmfms)) : undefined;

    // Already exists: increment individual count
    if (row) {
      if (row.validator) {
        const control = row.validator.get('individualCount');
        control.setValue((control.value || 0) + newBatch.individualCount);
        control.markAsDirty();
      } else {
        row.currentData.individualCount = (row.currentData.individualCount || 0) + newBatch.individualCount;
        this.markForCheck();
      }
    }

    // New batch: add to table
    else {
      row = await this.addRowToTable();
      if (!row) throw new Error("Could not add row t table");

      // Override rankOrder (keep computed value)
      newBatch.rankOrder = row.currentData.rankOrder;

      // Affect new row
      if (row.validator) {
        row.validator.patchValue(newBatch);
        row.validator.markAsDirty();
      } else {
        row.currentData = newBatch;
      }
    }
    this.confirmEditCreate(null, row);
    this.markAsDirty();
    return row;
  }

  protected async setAvailableParents(parents: Batch[], opts?: { emitEvent?: boolean; linkDataToParent?: boolean; }) {
    opts = opts || {emitEvent: true, linkDataToParent: true};

    this._availableParents = parents;

    // Sort parents by by Tag-ID
    if (this.displayParentPmfm) {
      this._availableSortedParents = this.sortData(parents.slice(), this.displayParentPmfm.pmfmId.toString());
    } else {
      this._availableSortedParents = this.sortData(parents.slice(), 'parent');
    }

    this.form.availableParents = this._availableSortedParents;

    // Link batches to parent, and delete orphan
    if (toBoolean(opts.linkDataToParent, true)) {
      await this.linkDataToParentAndDeleteOrphan();
    }

    if (toBoolean(opts.emitEvent, true)) {
      this.markForCheck();
    }
  }

  protected async onNewEntity(data: Batch): Promise<void> {
    console.debug("[sub-batch-table] Initializing new row data...");

    await super.onNewEntity(data);

    // Generate label
    data.label = this.acquisitionLevel + "#" + data.rankOrder;

    // Set individual count to 1, if column not shown
    if (!this.showIndividualCount) {
      data.individualCount = isNotNil(data.individualCount) ? data.individualCount : 1;
    }
  }

  protected getI18nColumnName(columnName: string): string {

    // Replace parent by TAG_ID pmfms
    columnName = columnName && columnName === 'parent' && this.displayParentPmfm ? this.displayParentPmfm.pmfmId.toString() : columnName;

    return super.getI18nColumnName(columnName);
  }

  /**
   * Can be overwrite by subclasses
   **/
  protected startListenRow(row: TableElement<Batch>) {
    this.startCellValueChanges('discard', row);
  }

  protected linkDataToParent(data: Batch[]) {
    if (!this._availableParents || !data) return;

    data.forEach(s => {
      const parentId = s.parentId || (s.parent && s.parent.id);
      s.parent = isNotNil(parentId) ? this._availableParents.find(p => p.id === parentId) : null;
    });
  }

  /**
   * Remove batches in table, if there have no more parent
   */
  protected async linkDataToParentAndDeleteOrphan() {

    const rows = await this.dataSource.getRows();

    // Check if need to delete some rows
    let hasRemovedItem = false;
    const data = rows
      .filter(row => {
        const batch = row.currentData;
        const parentId = batch.parentId || (batch.parent && batch.parent.id);

        let parent;
        if (isNotNil(parentId)) {
          // Update the parent, by id
          parent = this._availableParents.find(p => p.id === parentId);
        }
        // No parent, search by taxonGroup+taxonName
        else {
          const parentTaxonGroupId = batch.parent && batch.parent.taxonGroup && batch.parent.taxonGroup.id;
          const parentTaxonNameId = batch.parent && batch.parent.taxonName && batch.parent.taxonName.id;
          if (isNil(parentTaxonGroupId) && isNil(parentTaxonNameId)) {
            parent = undefined; // remove link to parent
          } else {
            parent = this._availableParents.find(p =>
              (p && ((!p.taxonGroup && !parentTaxonGroupId) || (p.taxonGroup && p.taxonGroup.id == parentTaxonGroupId))
                && ((!p.taxonName && !parentTaxonNameId) || (p.taxonName && p.taxonName.id == parentTaxonNameId))));
          }
        }

        if (parent || row.editing) {
          if (batch.parent !== parent) {
            batch.parent = parent;
            // If row use a validator, force update
            if (!row.editing && row.validator) row.validator.patchValue(batch, {emitEvent: false});
          }
          return true; // Keep only rows with a parent (or in editing mode)
        }

        // Could not found the parent anymore (parent has been delete)
        hasRemovedItem = true;
        return false;
      })
      .map(r => r.currentData);

    if (hasRemovedItem) {
      this.value = data;
    }
    //this.markForCheck();
  }

  protected sortData(data: Batch[], sortBy?: string, sortDirection?: string): Batch[] {
    sortBy = (sortBy && sortBy !== 'parent') ? sortBy : 'parent.rankOrder'; // Replace parent by its rankOrder
    return this.memoryDataService.sort(data, sortBy, sortDirection);
  }

  selectInputContent = selectInputContent;

  parentToString(batch: Batch) {
    // TODO: use options
    return BatchUtils.parentToString(batch);
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
