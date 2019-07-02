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
import {debounceTime, map, switchMap, tap} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {AppFormUtils, EntityUtils, environment, ReferentialRef} from "../../core/core.module";
import {Batch, PmfmStrategy, referentialToString} from "../services/trip.model";
import {
  PmfmIds,
  QualitativeLabels,
  ReferentialRefService,
  TaxonomicLevelIds
} from "../../referential/referential.module";
import {FormGroup, Validators} from "@angular/forms";
import {isNil, isNotNil, startsWithUpperCase, toBoolean} from "../../shared/shared.module";
import {UsageMode} from "../../core/services/model";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {AppMeasurementsTable, AppMeasurementsTableOptions} from "../measurement/measurements.table.class";
import {BatchUtils} from "../services/model/batch.model";
import {BatchesContext} from "./batches-context.class";
import {SubBatchValidatorService} from "../services/sub-batch.validator";
import {SubBatchForm} from "./sub-batch.form";
import {MeasurementValuesUtils} from "../services/model/measurement.model";

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
        reservedStartColumns: SubBatchesTable.RESERVED_START_COLUMNS,
        reservedEndColumns: SubBatchesTable.RESERVED_END_COLUMNS
      }
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchesTable extends AppMeasurementsTable<Batch, SubBatchFilter>
  implements OnInit, OnDestroy {

  static RESERVED_START_COLUMNS: string[] = ['parent', 'taxonName'];
  static RESERVED_END_COLUMNS: string[] = ['individualCount', 'comments'];

  private _parentSubscription: Subscription;
  private _availableParents: Batch[] = [];

  protected _availableSortedParents: Batch[] = [];

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryTableDataService<Batch, SubBatchFilter>;

  displayParentPmfm: PmfmStrategy;

  $taxonGroups: Observable<ReferentialRef[]>;
  $taxonNames: Observable<ReferentialRef[]>;
  $filteredParents: Observable<Batch[]>;

  @Input() showForm = false;

  @Input() qvPmfm: PmfmStrategy;

  @Input() tabindex: number;

  @Input()
  set availableParents(parents: Observable<Batch[]> | Batch[]) {
    console.log("availableParents -> ", parents);
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
    return this.usageMode ? this.usageMode === 'FIELD' : this.settingsService.isUsageMode('FIELD');
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

  @Input() showCommentsColumn = true;
  @Input() usageMode: UsageMode;
  @Input() context: BatchesContext;

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

    //this.debug = false;
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('comments', this.showCommentsColumn);

    if (!this.context) {
      console.warn("[sub-batches-table] No input context! Creating new...");
      this.context = new BatchesContext();
    }

    if (this.inlineEdition) { // can be override bu subclasses

      // Parent combo
      this.$filteredParents = this.registerCellValueChanges('parent')
        .pipe(
          debounceTime(250),
          map((value) => {
            if (EntityUtils.isNotEmpty(value)) return [value];
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
          switchMap((value) => this.referentialRefService.suggest(value,
            {
              entityName: 'TaxonName',
              levelId: TaxonomicLevelIds.SPECIES,
              searchAttribute: 'label'
            })
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

      await this.resetForm();
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

    await this.resetForm(subBatch);

    // Add the row
    await this.addBatchToTable(subBatch);
  }

  protected async resetForm(previousBatch?: Batch) {

    this.form.availableParents = this._availableSortedParents;

    // Create a new batch
    const newBatch = new Batch();
    await this.onNewEntity(newBatch);

    // Copy QV value from previous
    if (previousBatch) {
      // Copy parent
      newBatch.parent = previousBatch.parent;

      // Copy taxonName
      newBatch.taxonName = previousBatch.taxonName;

      // Copy QV PMFM value, if any
      if (this.qvPmfm) {
        newBatch.measurementValues[this.qvPmfm.pmfmId] = previousBatch.measurementValues[this.qvPmfm.pmfmId];
      }
    }

    MeasurementValuesUtils.normalizeFormEntity(newBatch, this.pmfms.getValue(), this.form.form);

    if (this.form.disabled) {
      this.form.enable();
      this.form.value = newBatch;
    }
    else {
      this.form.form.patchValue(newBatch, {emitEvent: false});
      this.form.markAsPristine();
      this.form.markAsUntouched();
      this.form.form.updateValueAndValidity();
    }

    if (previousBatch) {
      this.form.focusFirstEmpty();
    }
  }

  async setValueFromParent(parents: Batch[], qvPmfm?: PmfmStrategy) {

    this.qvPmfm = qvPmfm;
    const subBatches = BatchUtils.prepareSubBatchesForTable(parents, this.acquisitionLevel, qvPmfm);
    console.debug("TODO: subbatches=", subBatches);

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

  /* -- protected methods -- */

  protected async addBatchToTable(newBatch: Batch): Promise<TableElement<Batch>> {
    console.debug("[batches-table] Adding batch to table:", newBatch);

    // Make sure individual count if init
    newBatch.individualCount = newBatch.individualCount || 1;

    const pmfms = this.pmfms.getValue() || [];
    MeasurementValuesUtils.normalizeFormEntity(newBatch, pmfms);

    const rows = await this.dataSource.getRows();

    let row = rows.find(r => BatchUtils.canMergeSubBatch(newBatch, r.currentData, pmfms));

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
        this.confirmEditCreate(null, row);
        row.validator.markAsDirty();
      }
      else {
        row.currentData = newBatch;
        this.markForCheck();
      }
    }
    this.markAsDirty();
    return row;
  }

  protected async setAvailableParents(parents: Batch[], opts?: {emitEvent?: boolean; linkDataToParent?: boolean;}) {
    opts = opts || {emitEvent: true, linkDataToParent: true};

    this._availableParents = parents;

    // Sort parents by by Tag-ID
    if (this.displayParentPmfm) {
      this._availableSortedParents = this.sortData(parents.slice(), this.displayParentPmfm.pmfmId.toString());
    } else {
      this._availableSortedParents = this.sortData(parents.slice(), 'taxonGroup');
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
    sortBy = (sortBy !== 'parent') && sortBy || 'parent.rankOrder'; // Replace parent by its rankOrder
    return this.memoryDataService.sort(data, sortBy, sortDirection);
  }


  parentToString(batch: Batch) {
    // TODO: use options
    return BatchUtils.parentToString(batch);
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
