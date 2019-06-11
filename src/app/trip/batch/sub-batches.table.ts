import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {Observable} from 'rxjs';
import {debounceTime, map, switchMap, tap} from "rxjs/operators";
import {TableElement, ValidatorService} from "angular4-material-table";
import {EntityUtils, environment, ReferentialRef} from "../../core/core.module";
import {Batch, PmfmStrategy, referentialToString, TaxonGroupIds} from "../services/trip.model";
import {
  PmfmIds,
  QualitativeLabels,
  ReferentialRefService,
  TaxonomicLevelIds
} from "../../referential/referential.module";
import {BatchValidatorService} from "../services/batch.validator";
import {FormGroup, Validators} from "@angular/forms";
import {isNil, isNotNil} from "../../shared/shared.module";
import {UsageMode} from "../../core/services/model";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";

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
    {provide: ValidatorService, useClass: BatchValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchesTable extends AppMeasurementsTable<Batch, SubBatchFilter>
  implements OnInit, OnDestroy {

  static RESERVED_START_COLUMNS: string[] = ['parent', 'taxonName'];
  static RESERVED_END_COLUMNS: string[] = ['comments'];

  private _availableSortedParents: Batch[] = [];
  private _availableParents: Batch[] = [];

  protected cd: ChangeDetectorRef;
  protected referentialRefService: ReferentialRefService;
  protected memoryDataService: InMemoryTableDataService<Batch, SubBatchFilter>;

  displayParentPmfm: PmfmStrategy;

  $taxonGroups: Observable<ReferentialRef[]>;
  $taxonNames: Observable<ReferentialRef[]>;
  $filteredParents: Observable<Batch[]>;

  @Input()
  set availableParents(parents: Batch[]) {
    if (this._availableParents !== parents) {

      this._availableParents = parents;

      // Sort parents by by Tag-ID
      if (this.displayParentPmfm) {
        this._availableSortedParents = this.sortData(parents.slice(), this.displayParentPmfm.pmfmId.toString());
      }
      else {
        this._availableSortedParents = this.sortData(parents.slice(), 'taxonGroup');
      }

      // Link samples to parent, and delete orphan
      this.linkDataToParentAndDeleteOrphan();
    }
  }

  get availableParents(): Batch[] {
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

  @Input() showCommentsColumn = true;
  @Input() usageMode: UsageMode;

  constructor(
    protected injector: Injector
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
      injector.get(ValidatorService),
      {
        prependNewElements: false,
        suppressErrors: false,
        reservedStartColumns: SubBatchesTable.RESERVED_START_COLUMNS,
        reservedEndColumns: SubBatchesTable.RESERVED_END_COLUMNS
      }
    );
    this.cd = injector.get(ChangeDetectorRef);
    this.referentialRefService = injector.get(ReferentialRefService);
    this.memoryDataService = (this.dataService as InMemoryTableDataService<Batch, SubBatchFilter>);
    this.i18nColumnPrefix = 'TRIP.BATCH.TABLE.';
    this.inlineEdition = true;

    //this.debug = false;
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('comments', this.showCommentsColumn);

    // Taxon group combo
    this.$taxonGroups = this.registerCellValueChanges('taxonGroup')
      .pipe(
        debounceTime(250),
        switchMap((value) => this.referentialRefService.suggest(value,
          {
            entityName: 'TaxonGroup',
            levelId: TaxonGroupIds.FAO,
            searchAttribute: 'label'
          })
        ),
        // Save implicit value
        tap(items => this.updateImplicitValue('taxonGroup', items))
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
            (p.taxonGroup && this.startsWithUpperCase(p.taxonGroup.label, ucValueParts[0])) ||
            (p.taxonName && this.startsWithUpperCase(p.taxonName.label, ucValueParts.length === 2 ? ucValueParts[1] : ucValueParts[0]))
          );
        }),
        // Save implicit value
        tap(res => this.updateImplicitValue('parent', res))
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

  /* -- protected methods -- */

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
        const item = row.currentData;
        const parentId = item.parentId || (item.parent && item.parent.id);

        // No parent, search from attributes
        if (isNil(parentId)) {
          const parentTaxonGroupId = item.parent && item.parent.taxonGroup && item.parent.taxonGroup.id;
          const parentTaxonNameId = item.parent && item.parent.taxonName && item.parent.taxonName.id;
          if (isNil(parentTaxonGroupId) && isNil(parentTaxonNameId)) {
            item.parent = undefined; // remove link to parent
            return true; // not yet a parent: keep (.e.g new row)
          }
          // Update the parent, by taxonGroup+taxonName
          item.parent = this._availableParents.find(p =>
            (p && ((!p.taxonGroup && !parentTaxonGroupId) || (p.taxonGroup && p.taxonGroup.id == parentTaxonGroupId))
              && ((!p.taxonName && !parentTaxonNameId) || (p.taxonName && p.taxonName.id == parentTaxonNameId))));

        } else {
          // Update the parent, by id
          item.parent = this._availableParents.find(p => p.id === parentId);
        }

        // Could not found the parent anymore (parent has been delete)
        if (!item.parent) {
          hasRemovedItem = true;
          return false;
        }

        if (!row.editing) row.currentData = item;

        return true; // Keep only if data still have a parent
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
    if (!batch) return null;
    const hasTaxonGroup = EntityUtils.isNotEmpty(batch.taxonGroup) ;
    const hasTaxonName = EntityUtils.isNotEmpty(batch.taxonName);
    if (hasTaxonName && (!hasTaxonGroup || batch.taxonGroup.label === batch.taxonName.label)) {
      return `${batch.taxonName.label} - ${batch.taxonName.name}`;
    }
    if (hasTaxonName && hasTaxonGroup) {
      return `${batch.taxonGroup.label} / ${batch.taxonName.label} - ${batch.taxonName.name}`;
    }
    if (hasTaxonGroup) {
      return `${batch.taxonGroup.label} - ${batch.taxonGroup.name}`;
    }
    return `#${batch.rankOrder}`;
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
