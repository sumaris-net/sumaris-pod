import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit} from "@angular/core";
import {Observable} from 'rxjs';
import {filter, map, tap} from "rxjs/operators";
import {ValidatorService} from "angular4-material-table";
import {EntityUtils, environment, referentialToString} from "../../core/core.module";
import {PmfmStrategy, Sample} from "../services/trip.model";
import {PmfmIds} from "../../referential/referential.module";
import {SubSampleValidatorService} from "../services/sub-sample.validator";
import {isNil, isNotNil} from "../../shared/shared.module";
import {AppMeasurementsTable} from "../measurement/measurements.table.class";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {UsageMode} from "../../core/services/model";

export const SUB_SAMPLE_RESERVED_START_COLUMNS: string[] = ['parent'];
export const SUB_SAMPLE_RESERVED_END_COLUMNS: string[] = ['comments'];


export interface SubSampleFilter {
  parentId?: number;
  operationId?: number;
  landingId?: number;
}

@Component({
  selector: 'app-sub-samples-table',
  templateUrl: 'sub-samples.table.html',
  styleUrls: ['sub-samples.table.scss'],
  providers: [
    {provide: ValidatorService, useClass: SubSampleValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubSamplesTable extends AppMeasurementsTable<Sample, SubSampleFilter>
  implements OnInit, OnDestroy {

  private _availableSortedParents: Sample[] = [];
  private _availableParents: Sample[] = [];

  protected cd: ChangeDetectorRef;
  protected memoryDataService: InMemoryTableDataService<Sample, SubSampleFilter>;

  displayParentPmfm: PmfmStrategy;

  $filteredParents: Observable<Sample[]>;

  @Input()
  set availableParents(parents: Sample[]) {
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

  get availableParents(): Sample[] {
    return this._availableParents;
  }

  set value(data: Sample[]) {
    this.memoryDataService.value = data;
  }

  get value(): Sample[] {
    return this.memoryDataService.value;
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  @Input() showLabelColumn = false;

  @Input() usageMode: UsageMode;

  constructor(
    protected injector: Injector
  ) {
    super(injector,
      Sample,
      new InMemoryTableDataService<Sample, SubSampleFilter>(Sample, {
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
        reservedStartColumns: SUB_SAMPLE_RESERVED_START_COLUMNS,
        reservedEndColumns: SUB_SAMPLE_RESERVED_END_COLUMNS
      }
    );
    this.memoryDataService = (this.dataService as InMemoryTableDataService<Sample, SubSampleFilter>);
    this.cd = injector.get(ChangeDetectorRef);
    this.i18nColumnPrefix = 'TRIP.SAMPLE.TABLE.';
    this.inlineEdition = true;

    //this.debug = false;
    this.debug = !environment.production;
  };

  ngOnInit() {
    super.ngOnInit();

    this.setShowColumn('label', this.showLabelColumn);

    // Always hide parent tag_id (if present)
    this.setShowColumn(PmfmIds.TAG_ID.toString(), false);

    // Parent combo
    this.$filteredParents = this.registerCellValueChanges('parent')
      .pipe(
        //debounceTime(250),
        map((value) => {
          if (EntityUtils.isNotEmpty(value)) return [value];

          value = (typeof value === "string") && (value as string).toUpperCase() || undefined;
          if (!value || value === '*') return this._availableSortedParents; // All
          if (this.debug) console.debug(`[sub-sample-table] Searching parent {${value}...`);
          if (this.displayParentPmfm) { // Search on a specific Pmfm (e.g Tag-ID)
            return this._availableSortedParents.filter(p => this.startsWithUpperCase(p.measurementValues[this.displayParentPmfm.pmfmId], value));
          }
          // Search on rankOrder
          return this._availableSortedParents.filter(p => p.rankOrder.toString().startsWith(value));
        }),
        // Save implicit value
        tap(res => this.updateImplicitValue('parent', res))
      );

    // Check if there a tag id in pmfms
    this.registerSubscription(
      this.measurementsDataService.$pmfms
        .pipe(filter(isNotNil))
        .subscribe((pmfms) => {
          this.displayParentPmfm = pmfms.find(p => p.pmfmId === PmfmIds.TAG_ID);
          this.markForCheck();
        }));
  }

  async autoFillTable() {
    if (this.loading) return;
    if (!this.confirmEditCreate()) return;

    const rows = await this.dataSource.getRows();
    const data = rows.map(r => r.currentData);
    const startRowCount = data.length;

    let rankOrder = await this.getMaxRankOrder();
    await this._availableParents
      .filter(p => !data.find(s => s.parent && s.parent.id === p.id))
      .map(async p => {
        const sample = new Sample();
        sample.parent = p;
        sample.rankOrder = ++rankOrder;
        await this.onNewEntity(sample);
        data.push(sample);
      });

    if (data.length > startRowCount) {
      this.value = data;
      //this.markForCheck();
    }
  }

  /* -- protected methods -- */


  protected async onNewEntity(data: Sample): Promise<void> {
    console.debug("[sample-table] Initializing new row data...");

    await super.onNewEntity(data);

    // label
    if (!this.showLabelColumn) {
      // Generate label
      data.label = this.acquisitionLevel + "#" + data.rankOrder;
    } else if (this.isOnFieldMode) {
      // Copy previous label ?
      //this.memoryDataService
    }
  }

  protected getI18nColumnName(columnName: string): string {

    // Replace parent by TAG_ID pmfms
    columnName = columnName && columnName === 'parent' && this.displayParentPmfm ? this.displayParentPmfm.pmfmId.toString() : columnName;

    return super.getI18nColumnName(columnName);
  }

  protected linkDataToParent(data: Sample[]) {
    if (!this._availableParents || !data) return;

    data.forEach(s => {
      const parentId = s.parentId || (s.parent && s.parent.id);
      s.parent = isNotNil(parentId) ? this._availableParents.find(p => p.id === parentId) : null;
    });
  }

  /**
   * Remove samples in table, if there have no more parent
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
          const parentTagId = item.parent && item.parent.measurementValues && item.parent.measurementValues[PmfmIds.TAG_ID];
          if (isNil(parentTagId)) {
            item.parent = undefined; // remove link to parent
            return true; // not yet a parent: keep (.e.g new row)
          }
          // Update the parent, by tagId
          item.parent = this._availableParents.find(p => (p && p.measurementValues && p.measurementValues[PmfmIds.TAG_ID]) === parentTagId);

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

        return true; // Keep only if sample still have a parent
      })
      .map(r => r.currentData);

    if (hasRemovedItem) {
      this.value = data;
    }
    //this.markForCheck();
  }

  protected sortData(data: Sample[], sortBy?: string, sortDirection?: string): Sample[] {
    sortBy = (sortBy !== 'parent') && sortBy || 'parent.rankOrder'; // Replace parent by its rankOrder
    return this.memoryDataService.sort(data, sortBy, sortDirection);
  }


  parentToString(parent: Sample) {
    if (!parent) return null;
    if (parent.measurementValues && parent.measurementValues[PmfmIds.TAG_ID]) {
      return parent.measurementValues[PmfmIds.TAG_ID];
    }
    const hasTaxonGroup = EntityUtils.isNotEmpty(parent.taxonGroup) ;
    const hasTaxonName = EntityUtils.isNotEmpty(parent.taxonName);
    if (hasTaxonName && (!hasTaxonGroup || parent.taxonGroup.label === parent.taxonName.label)) {
      return `${parent.taxonName.label} - ${parent.taxonName.name}`;
    }
    if (hasTaxonName && hasTaxonGroup) {
      return `${parent.taxonGroup.label} / ${parent.taxonName.label} - ${parent.taxonName.name}`;
    }
    if (hasTaxonGroup) {
      return `${parent.taxonGroup.label} - ${parent.taxonGroup.name}`;
    }
    return `#${parent.rankOrder}`;
  }

  referentialToString = referentialToString;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
