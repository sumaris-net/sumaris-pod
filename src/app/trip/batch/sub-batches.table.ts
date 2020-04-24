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
import {TableElement, ValidatorService} from "angular4-material-table";
import {AppFormUtils, EntityUtils, environment, IReferentialRef} from "../../core/core.module";
import {Batch, PmfmStrategy, referentialToString} from "../services/trip.model";
import {
  AcquisitionLevelCodes,
  PmfmIds,
  QualitativeLabels,
  ReferentialRefService
} from "../../referential/referential.module";
import {FormGroup, Validators} from "@angular/forms";
import {isNil, isNilOrBlank, isNotNil, startsWithUpperCase, toBoolean} from "../../shared/shared.module";
import {UsageMode} from "../../core/services/model";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {AppMeasurementsTable, AppMeasurementsTableOptions} from "../measurement/measurements.table.class";
import {BatchUtils} from "../services/model/batch.model";
import {SubBatchValidatorService} from "../services/sub-batch.validator";
import {SubBatchForm} from "./sub-batch.form";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {selectInputContent} from "../../core/form/form.utils";
import {SubBatchModal} from "./sub-batch.modal";

export const SUB_BATCH_RESERVED_START_COLUMNS: string[] = ['parent', 'taxonName'];
export const SUB_BATCH_RESERVED_END_COLUMNS: string[] = ['individualCount', 'comments'];


export const SUB_BATCHES_TABLE_OPTIONS = new InjectionToken<AppMeasurementsTableOptions<Batch>>('SubBatchesTableOptions');

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
    {provide: ValidatorService, useExisting: SubBatchValidatorService},
    {
      provide: SUB_BATCHES_TABLE_OPTIONS,
      useValue: {
        prependNewElements: false,
        suppressErrors: environment.production,
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
  private _qvPmfm: PmfmStrategy;

  protected _availableSortedParents: Batch[] = [];

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryTableDataService<Batch, SubBatchFilter>;

  @Input() displayParentPmfm: PmfmStrategy;

  @Input() showForm = false;

  @Input() tabindex: number;

  @Input() set qvPmfm(value: PmfmStrategy) {
    this._qvPmfm = value;
    // If already loaded, re apply pmfms, to be able to execute mapPmfms
    if (value) {
      this.measurementsDataService.pmfms = this.pmfms;
    }
  }

  get qvPmfm(): PmfmStrategy {
    return this._qvPmfm;
  }

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
    this.setValue(data);
  }

  get value(): Batch[] {
    return this.getValue();
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
    this.updateParentAutocomplete();
  }

  get showTaxonNameColumn(): boolean {
    return this.getShowColumn('taxonName');
  }

  @Input()
  set showIndividualCount(value: boolean) {
    this.setShowColumn('individualCount', value);
  }

  get showIndividualCount(): boolean {
    return this.getShowColumn('individualCount') && this.displayedColumns.findIndex(c => c === 'individualCount') !== -1;
  }

  @Input()
  set showCommentsColumn(value: boolean) {
    this.setShowColumn('comments', value);
  }

  get showCommentsColumn(): boolean {
    return this.getShowColumn('comments');
  }

  get dirty(): boolean {
    return this._dirty || this.memoryDataService.dirty;
  }

  @Input() usageMode: UsageMode;

  @ViewChild('form', { static: true }) form: SubBatchForm;

  constructor(
    protected injector: Injector,
    protected validatorService: ValidatorService,
    @Inject(SUB_BATCHES_TABLE_OPTIONS) options: AppMeasurementsTableOptions<Batch>
  ) {
    super(injector,
      Batch,
      new InMemoryTableDataService<Batch, SubBatchFilter>(Batch, {
        onSort: (data, sortBy, sortDirection) => this.sortData(data, sortBy, sortDirection),
        onLoad: (data) => this.onLoadData(data),
        onSave: (data) => this.onSaveData(data),
        equals: Batch.equals
      }),
      validatorService,
      Object.assign(options, {
        mapPmfms: (pmfms) => this.mapPmfms(pmfms)
      })
    );
    this.cd = injector.get(ChangeDetectorRef);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.memoryDataService = (this.dataService as InMemoryTableDataService<Batch, SubBatchFilter>);
    this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
    this.tabindex = 1;
    this.inlineEdition = !this.mobile;

    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL;
    this.showCommentsColumn = true;

    //this.debug = false;
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('comments', this.showCommentsColumn);

    this.registerAutocompleteField('parent', {
      suggestFn: (value: any, options?: any) => this.suggestParent(value),
      showAllOnFocus: true
    });
    this.updateParentAutocomplete();

    this.registerAutocompleteField('taxonName', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonNames(value),
      showAllOnFocus: true
    });

    if (this.inlineEdition) { // can be override bu subclasses

      // Create listener on column 'DISCARD_OR_LANDING' value changes
      this.registerCellValueChanges('discard', "measurementValues." + PmfmIds.DISCARD_OR_LANDING.toString())
        .subscribe((value) => {
          if (!this.editedRow) return; // Should never occur
          const row = this.editedRow;
          const controls = (row.validator.controls['measurementValues'] as FormGroup).controls;
          if (EntityUtils.isNotEmpty(value) && value.label === QualitativeLabels.DISCARD_OR_LANDING.DISCARD) {
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

  async doSubmitForm(event?: UIEvent, row?: TableElement<Batch>) {
    // Skip if loading,
    // or if previous edited row not confirmed
    if (this.loading) return;
    if (row !== this.editedRow && !this.confirmEditCreate()) return;

    await AppFormUtils.waitWhilePending(this.form);

    if (this.form.invalid) {
      this.onInvalidForm();
      return;
    }

    const subBatch = this.form.form.value;
    subBatch.individualCount = isNotNil(subBatch.individualCount) ? subBatch.individualCount : 1;

    await this.resetForm(subBatch, {focusFirstEmpty: true});

    // Add batch to table
    if (!row) {
      await this.addBatchToTable(subBatch);
    }

    // Update existing row
    else {
      this.updateRowFromBatch(subBatch, row);
    }
  }

  async add(batches: Batch[], opts?: {linkDataToParent?: boolean}) {
    if (toBoolean(opts && opts.linkDataToParent, true)) {
      this.linkDataToParent(batches);
    }

    for (let b of batches) {
      await this.addBatchToTable(b);
    }
  }


  /* -- protected method -- */

  protected setValue(data: Batch[]) {
    this.memoryDataService.value = data;
  }

  protected getValue(): Batch[] {
    return this.memoryDataService.value;
  }

  protected updateParentAutocomplete() {
    if (!this.autocompleteFields.parent) return; // skip

    const taxonGroupAttributes = this.settings.getFieldDisplayAttributes('taxonGroup');
    const taxonNameAttributes = this.settings.getFieldDisplayAttributes('taxonName');

    const parentToStringOptions = {
      pmfm: this.displayParentPmfm,
      taxonGroupAttributes: taxonGroupAttributes,
      taxonNameAttributes: taxonNameAttributes
    };
    if (this.showTaxonNameColumn) {
      this.autocompleteFields.parent.attributes = ['rankOrder'].concat(taxonGroupAttributes.map(attr => 'taxonGroup.' + attr));
    }
    else {
      this.autocompleteFields.parent.attributes = ['taxonGroup.' + taxonGroupAttributes[0]]
        .concat(taxonNameAttributes.map(attr => 'taxonName.' + attr));
    }
    this.autocompleteFields.parent.displayWith = (value) => BatchUtils.parentToString(value, parentToStringOptions);
  }

  public async resetForm(previousBatch?: Batch, opts?: {focusFirstEmpty?: boolean, emitEvent?: boolean}) {
    if (!this.form) throw new Error('Form not exists');
    await this.onReady();

    this.form.availableParents = this._availableSortedParents;

    // Create a new batch
    const newBatch = new Batch();

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
      if (this.qvPmfm && this.form.freezeQvPmfm) {
        newBatch.measurementValues[this.qvPmfm.pmfmId] = previousBatch.measurementValues[this.qvPmfm.pmfmId];
      }

      // Copy taxon name (if freezed)
      if (previousBatch.taxonName && this.form.freezeTaxonName) {
        newBatch.taxonName = previousBatch.taxonName;
      }
      else {
        // Set taxonName, is only one in list
        const taxonNames = this.form.taxonNames;
        if (taxonNames && taxonNames.length === 1) {
          newBatch.taxonName = taxonNames[0];
        }
      }
    }

    // Reset the form with the new batch
    MeasurementValuesUtils.normalizeEntityToForm(newBatch, this.$pmfms.getValue(), this.form.form);
    this.form.reset(newBatch, {emitEvent: true, normalizeEntityToForm: false /*already done*/});

    // If need, enable the form
    if (this.form.disabled) {
      this.form.enable(opts);
    }

    if (opts && toBoolean(opts.focusFirstEmpty, false)) {
      setTimeout(() => {
        this.form.focusFirstEmpty();
        this.form.markAsPristine({onlySelf: true});
        this.form.markAsUntouched({onlySelf: true});
      });
    }
    else {
      this.form.markAsPristine({onlySelf: true});
      this.form.markAsUntouched({onlySelf: true});
    }

    if (!opts || opts.emitEvent !== false) {
      this.markForCheck();
    }
  }

  async setValueFromParent(parents: Batch[], qvPmfm?: PmfmStrategy) {

    this.qvPmfm = qvPmfm;
    const subBatches = BatchUtils.prepareSubBatchesForTable(parents, this.acquisitionLevel, qvPmfm);

    await this.setAvailableParents(parents, {emitEvent: false, linkDataToParent: false});

    this.value = subBatches;
  }

  markAsPristine(opts?: {onlySelf?: boolean}) {
    super.markAsPristine();
    if (this.form) this.form.markAsPristine(opts);
  }

  markAsUntouched() {
    super.markAsUntouched();
    if (this.form) this.form.markAsUntouched();
  }

  enable() {
    super.enable();

    if (this.showForm && this.form && this.form.disabled) {
      this.form.enable();
    }
  }

  disable() {
    super.disable();

    if (this.showForm && this.form && this.form.enabled) {
      this.form.disable();
    }
  }

  /* -- protected methods -- */

  protected prepareEntityToSave(batch: Batch) {
    // Override by subclasses
  }

  protected async suggestParent(value: any): Promise<any[]> {
    if (EntityUtils.isNotEmpty(value)) {
      return [value];
    }
    value = (typeof value === "string" && value !== "*") && value || undefined;
    if (isNil(value)) return this._availableSortedParents; // All

    if (this.debug) console.debug(`[sub-batch-table] Searching parent {${value || '*'}}...`);
    const ucValueParts = value.trim().toUpperCase().split(" ", 1);

    // Search on labels (taxonGroup or taxonName)
    return this._availableSortedParents.filter(p =>
      (p.taxonGroup && startsWithUpperCase(p.taxonGroup.label, ucValueParts[0])) ||
      (p.taxonName && startsWithUpperCase(p.taxonName.label, ucValueParts.length === 2 ? ucValueParts[1] : ucValueParts[0]))
    );
  }

  protected async suggestTaxonNames(value: any, options?: any): Promise<IReferentialRef[]> {
    const parent = this.editedRow && this.editedRow.validator.get('parent').value;
    if (isNilOrBlank(value) && isNil(parent)) return [];
    return this.programService.suggestTaxonNames(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute,
        taxonGroupId: parent && parent.taxonGroup && parent.taxonGroup.id || undefined
      });
  }

  protected mapPmfms(pmfms: PmfmStrategy[]) {

    if (this.qvPmfm) {
      // Remove QV pmfms
      const index = pmfms.findIndex(pmfm => pmfm.pmfmId === this.qvPmfm.pmfmId);
      if (index !== -1) {
        // Replace original pmfm by a copy, with hidden=true
        const qvPmfm = this.qvPmfm.clone();
        qvPmfm.hidden = true;
        qvPmfm.required = true;

        pmfms[index] = qvPmfm;
      }
    }

    return pmfms
      // Exclude weight Pmfm
      .filter(p => !p.isWeight);
  }

  protected async openNewRowDetail(): Promise<boolean> {
    const newBatch = await this.openDetailModal();
    if (newBatch) {
      await this.addBatchToTable(newBatch);
    }
    return true;
  }

  protected async addBatchToTable(newBatch: Batch): Promise<TableElement<Batch>> {
    if (this.debug) console.debug("[batches-table] Adding batch to table:", newBatch);

    // Make sure individual count if init
    newBatch.individualCount = isNotNil(newBatch.individualCount) ? newBatch.individualCount : 1;

    const pmfms = this.$pmfms.getValue() || [];
    MeasurementValuesUtils.normalizeEntityToForm(newBatch, pmfms);

    let row = undefined;

    // Try to find an identical sub-batch
    if (this.showIndividualCount) {
      const rows = await this.dataSource.getRows();
      row = rows.find(r => BatchUtils.canMergeSubBatch(newBatch, r.currentData, pmfms));
    }

    // Already exists: increment individual count
    if (row) {
      if (row.validator) {
        const control = row.validator.controls.individualCount;
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

      // Keep rankOrder, then make sure to entity (e.g. label should be computed)
      newBatch.rankOrder = row.currentData.rankOrder;
      await this.onNewEntity(newBatch);

      // Affect new row
      if (row.validator) {
        row.validator.patchValue(newBatch);
        this.confirmEditCreate(null, row);
        row.validator.markAsDirty();
      } else {
        row.currentData = newBatch;
        this.confirmEditCreate(null, row);
      }
    }
    this.markAsDirty();
    return row;
  }

  protected updateRowFromBatch(updatedBatch: Batch, row: TableElement<Batch>): boolean {
    if (this.debug) console.debug("[batches-table] Updating batch to table:", updatedBatch);

    // Adapt measurement values to row
    this.normalizeEntityToRow(updatedBatch, row);

    // Update the row
    if (!row.editing && row.validator) {
      row.validator.patchValue(updatedBatch);
      row.validator.markAsDirty();
    }
    else {
      row.currentData = updatedBatch;
    }

    if (this.confirmEditCreate(null, row)) {
      this.markAsDirty();
      return true;
    }
    return false;
  }

  protected async openRow(id: number, row: TableElement<Batch>): Promise<boolean> {

    if (!this.allowRowDetail) return false;

    if (this.onOpenRow.observers.length) {
      this.onOpenRow.emit({id, row});
      return true;
    }

    const batch = row.validator ? Batch.fromObject(row.currentData) : row.currentData;
    const updatedBatch = await this.openDetailModal(batch);

    // Update row in table
    if (updatedBatch) {
      return this.updateRowFromBatch(updatedBatch, row);
    }

    return false;
  }

  async openDetailModal(batch?: Batch): Promise<Batch | undefined> {

    const isNew = !batch;
    if (isNew) {
      batch = new Batch();
      await this.onNewEntity(batch);
    }
    else {
      // Do a copy, because edition can be cancelled
      batch = batch.clone();

      // Prepare entity measurement values
      this.prepareEntityToSave(batch);
    }

    const modal = await this.modalCtrl.create({
      component: SubBatchModal,
      componentProps: {
        program: this.program,
        acquisitionLevel: this.acquisitionLevel,
        availableParents: this.availableParents,
        value: batch,
        isNew: isNew,
        disabled: this.disabled,
        //canEdit: !this.disabled,
        qvPmfm: this.qvPmfm,
        showParent: this.showParentColumn,
        showTaxonName: this.showTaxonNameColumn,
        showIndividualCount: this.showIndividualCount
      }, keyboardClose: true
    });

    // Open the modal
    await modal.present();

    // Wait until closed
    const {data} = await modal.onDidDismiss();
    if (data && this.debug) console.debug("[batches-table] Batch modal result: ", data);
    return (data instanceof Batch) ? data : undefined;
  }

  protected async setAvailableParents(parents: Batch[], opts?: { emitEvent?: boolean; linkDataToParent?: boolean; }) {
    opts = opts || {emitEvent: true, linkDataToParent: true};

    this._availableParents = parents;

    // Sort parents by Tag-ID, or rankOrder
    if (this.displayParentPmfm) {
      this._availableSortedParents = this.sortData(parents.slice(), this.displayParentPmfm.pmfmId.toString());
    } else {
      this._availableSortedParents = this.sortData(parents.slice(), 'rankOrder');
    }

    await this.onReady();

    if (this.form) this.form.availableParents = this._availableSortedParents;

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

    if (isNil(data.id)) {
      // TODO : add sequence
    }

    // Set individual count to 1, if column not shown
    if (!this.showIndividualCount) {
      data.individualCount = isNotNil(data.individualCount) ? data.individualCount : 1;
    }
  }

  protected onInvalidForm() {
    this.form.markAsTouched({emitEvent: true});
    if (this.debug) AppFormUtils.logFormErrors(this.form.form, "[sub-batch-table] ");
  }

  protected getI18nColumnName(columnName: string): string {

    // Replace parent by TAG_ID pmfms
    columnName = columnName && columnName === 'parent' && this.displayParentPmfm ? this.displayParentPmfm.pmfmId.toString() : columnName;

    return super.getI18nColumnName(columnName);
  }

  protected linkDataToParent(data: Batch[]) {
    if (!this._availableParents || !data) return;

    data.forEach(s => {
      const parentId = s.parentId || (s.parent && s.parent.id);
      const parentLabel = (s.parent && s.parent.label);
      s.parent = this._availableParents.find(p => (isNotNil(parentId) && p.id === parentId) || (parentLabel && p.label === parentLabel)) || null;
      if (!s.parent) console.warn("[sub-batches-table] linkDataToParent() - Could not found parent for sub-batch:", s);
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

  protected onLoadData(data: Batch[]): Batch[] {
    this.linkDataToParent(data);
    return data;
  }

  protected onSaveData(data: Batch[]): Batch[] {
    // Can be override by subclasses
    return data;
  }

  selectInputContent = selectInputContent;
  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
