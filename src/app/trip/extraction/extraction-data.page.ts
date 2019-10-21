import {ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, OnInit, ViewChild} from '@angular/core';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import {isNil, isNotNil} from '../../shared/shared.module';
import {TableDataSource} from "angular4-material-table";
import {
  AggregationType,
  ExtractionColumn,
  ExtractionResult,
  ExtractionRow,
  ExtractionType
} from "../services/extraction.model";
import {MatExpansionPanel, MatPaginator, MatSort, MatTable} from "@angular/material";
import {merge} from "rxjs/observable/merge";
import {TableSelectColumnsComponent} from "../../core/table/table-select-columns.component";
import {SETTINGS_DISPLAY_COLUMNS} from "../../core/table/table.class";
import {AlertController, ModalController} from "@ionic/angular";
import {Location} from "@angular/common";
import {AccountService, LocalSettingsService} from "../../core/core.module";
import {delay, map} from "rxjs/operators";
import {firstNotNilPromise} from "../../shared/observables";
import {ExtractionAbstractPage} from "./extraction-abstract.page";
import {ActivatedRoute, Router} from "@angular/router";
import {TranslateService} from "@ngx-translate/core";
import {ExtractionService} from "../services/extraction.service";
import {FormBuilder} from "@angular/forms";

export const DEFAULT_PAGE_SIZE = 20;
export const DEFAULT_CRITERION_OPERATOR = '=';

@Component({
  selector: 'app-extraction-data-page',
  templateUrl: './extraction-data.page.html',
  styleUrls: ['./extraction-data.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExtractionDataPage extends ExtractionAbstractPage<ExtractionType> implements OnInit {

  data: ExtractionResult;
  $title = new Subject<string>();
  sortedColumns: ExtractionColumn[];
  displayedColumns: string[];
  $columns = new BehaviorSubject<ExtractionColumn[]>(undefined);
  dataSource: TableDataSource<ExtractionRow>;
  settingsId: string;
  showHelp = true;
  canAggregate = false;
  isAdmin = false;

  $typesMap: { [category: string]: Observable<ExtractionType[]>};

  @ViewChild(MatTable) table: MatSort;
  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;
  @ViewChild(MatExpansionPanel) filterExpansionPanel: MatExpansionPanel;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    protected location: Location,
    protected modalCtrl: ModalController,
    protected accountService: AccountService,
    protected service: ExtractionService,
    protected settings: LocalSettingsService,
    protected formBuilder: FormBuilder,
    protected cd: ChangeDetectorRef
  ) {
    super(route, router, alertCtrl, translate, accountService, service, settings, formBuilder);

    this.displayedColumns = [];
    this.dataSource = new TableDataSource<ExtractionRow>([], ExtractionRow);
    this.isAdmin = this.accountService.isAdmin();
  }

  ngOnInit() {

    super.ngOnInit();

    // Create a map by category (use for type sub menu)
    this.$typesMap =  ['live', 'product'].reduce((res, category) => {
      res[category] = this.$types.pipe(map( types => types.filter(t => t.category === category)));
      return res;
    }, {});

    // If the user changes the sort order, reset back to the first page.
    this.sort && this.paginator && this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(
      this.sort && this.sort.sortChange || EventEmitter.empty(),
      this.paginator && this.paginator.page || EventEmitter.empty(),
      this.onRefresh
    )
      .subscribe(() => {
        if (this.loading || isNil(this.type)) return; // avoid multiple load
        return this.loadData();
      });
  }

  async updateView(data: ExtractionResult) {

    this.data = data;

    // Translate names
    this.translateColumns(data.columns);

    // Sort columns, by rankOrder
    this.sortedColumns = data.columns.slice()
      // Sort by rankOder
      .sort((col1, col2) => col1.rankOrder - col2.rankOrder);

    this.displayedColumns = this.sortedColumns
      .map(column => column.columnName)
      .filter(columnName => columnName !== "id"); // Remove id

    this.$columns.next(data.columns); // WARN: must keep the original column order

    // Update rows
    this.dataSource.updateDatasource(data.rows || []);

    // Update title
    await this.updateTitle();

    this.dataSource.connect().first().subscribe(() => {
      this.loading = false;
      this.enable();
      this.markAsUntouched();
      this.markAsPristine();
      this.markForCheck();
    });
  }

  async setType(type: ExtractionType, opts?: { emitEvent?: boolean; skipLocationChange?: boolean; sheetName?: string }): Promise<boolean> {
    const changed = await super.setType(type, opts);

    if (changed) {

      this.canAggregate = this.type && !this.type.isSpatial && this.accountService.isSupervisor();

      // Close the filter panel
      if (this.filterExpansionPanel && this.filterExpansionPanel.expanded) {
        this.filterExpansionPanel.close();
      }
    }

    return changed;

  }

  setSheetName(sheetName: string, opts?: { emitEvent?: boolean; skipLocationChange?: boolean; }) {
    opts = opts || {emitEvent: !this.loading};

    // Reset sort and paginator
    const resetPaginator = (opts.emitEvent !== false && isNotNil(sheetName) && this.sheetName !== sheetName);

    super.setSheetName(sheetName, opts);

    if (resetPaginator) {
      this.sort.active = undefined;
      this.paginator.pageIndex = 0;
    }
  }

  async openSelectColumnsModal(event: any): Promise<any> {
    const columns = this.sortedColumns
      .map((column) => {
        return {
          name: column.columnName,
          label: column.name,
          visible: this.displayedColumns.indexOf(column.columnName) !== -1
        };
      });

    const modal = await this.modalCtrl.create({
      component: TableSelectColumnsComponent,
      componentProps: {columns: columns}
    });

    // On dismiss
    modal.onDidDismiss()
      .then(res => {
        if (!res) return; // CANCELLED

        // Apply columns
        this.displayedColumns = columns && columns.filter(c => c.visible).map(c => c.name) || [];

        // Update local settings
        return this.settings.savePageSetting(this.settingsId, this.displayedColumns, SETTINGS_DISPLAY_COLUMNS);
      });
    return modal.present();
  }

  onCellValueClick($event: MouseEvent, column: ExtractionColumn, value: string) {
    const hasChanged = this.criteriaForm.addFilterCriterion({
      name: column.columnName,
      operator: DEFAULT_CRITERION_OPERATOR,
      value: value,
      sheetName: this.sheetName
    }, {
      appendValue: $event.ctrlKey
    });
    if (!hasChanged) return;

    if (!this.filterExpansionPanel.expanded) {
      this.filterExpansionPanel.open();
    }

    if (!$event.ctrlKey) {
      this.onRefresh.emit();
    }
  }


  async aggregate() {
    if (!this.type || !this.canAggregate) return; // Skip

    this.loading = true;
    this.error = null;
    this.markForCheck();

    const extractionFilter = this.getFilterValue();
    this.disable();

    try {

      const name = await this.translate.get('EXTRACTION.AGGREGATION.NEW_NAME', {name: this.type.name}).toPromise();
      // Compute a new name
      const aggType = AggregationType.fromObject({
        label: `${this.type.label}-${this.accountService.account.id}-${Date.now()}`,
        category: this.type.category,
        name: name
      });

      // Save aggregation
      const savedAggType = await this.service.saveAggregation(aggType, extractionFilter);

      // Wait for types cache updates
      await setTimeout(async() => {

        // Open the new aggregation
        await this.openAggregationType(savedAggType);

        await this.setType(savedAggType, {emitEvent: true, skipLocationChange: false, sheetName: undefined});

        this.loading = false;
        this.markForCheck();

      }, 1000);

    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
      this.loading = false;
      this.markAsDirty();
    } finally {
      this.enable();
    }

  }

  async deleteAggregation() {
    if (!this.type || isNil(this.type.id)) return;

    if (this.type.category !== 'product') {
      console.warn("[extraction-table] Only product extraction can be deleted !");
      return;
    }

    this.loading = true;

    try {
      await this.service.deleteAggregations([this.type as AggregationType]);

      // Wait propagation to types
      await delay(2000);

      // Choose another type
      const types = await firstNotNilPromise(this.$types);
      if (types && types.length) {
        await this.setType(types[0], {emitEvent: true});
      }
      else {
        this.loading = false;
        this.markForCheck();
      }
    }
    catch(err) {
      console.error(err);
      this.error = err && err.message || err;
      this.markAsDirty();
    }

  }

  async openMap() {
    if (!this.type || !this.type.isSpatial) return; // Skip

    // open the map
    await this.router.navigateByUrl('/map', {
      queryParams:  this.getFilterAsQueryParams()
    });
  }

  async openAggregationType(type?: ExtractionType) {
    type = type || this.type;

    if (!type) return; // skip if not a aggregation type

    console.debug(`[extraction-table] Opening aggregation type {${type.label}`);

    // open the aggregation type
    await this.router.navigateByUrl(`/extraction/aggregation/${type.id}`);

  }

  /* -- protected method -- */

  protected watchTypes(): Observable<ExtractionType[]> {
    return this.service.watchTypes()
      .pipe(
        map(types => {
          // Compute name, if need
          types.forEach(t => t.name = t.name || this.getI18nTypeName(t));
          // Sort by name
          types.sort((t1, t2) => t1.name > t2.name ? 1 : (t1.name < t2.name ? -1 : 0) );

          return types;
        })
      );
  }

  protected async loadData() {

    if (!this.type || !this.type.category || !this.type.label) return; // skip

    this.settingsId = this.generateTableId();
    this.error = null;
    console.debug(`[extraction-table] Loading ${this.type.category} ${this.type.label}`);

    this.loading = true;

    const filter = this.getFilterValue();

    this.disable();
    this.markForCheck();

    try {
      // Load rows
      const data = await this.service.loadRows(this.type,
        this.paginator && this.paginator.pageIndex * this.paginator.pageSize,
        this.paginator && this.paginator.pageSize || DEFAULT_PAGE_SIZE,
        this.sort && this.sort.active,
        this.sort && this.sort.direction,
        filter
      );

      // Update the view
      await this.updateView(data);
    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
      this.loading = false;
      this.enable();
      this.form.markAsDirty();
    }
  }

  protected fromObject(json: any): ExtractionType {
    return ExtractionType.fromObject(json);
  }

  protected isEquals(t1: ExtractionType, t2: ExtractionType): boolean {
    return ExtractionType.equals(t1, t2);
  }

  /* -- private method -- */

  private async updateTitle() {
    const key = `EXTRACTION.CATEGORY.${this.type.category.toUpperCase()}`;
    const title = await this.translate.get(key).toPromise();
    if (title === key) {
      console.warn("Missing i18n key '" + key + "'");
      this.$title.next("");
    } else {
      this.$title.next(title);
    }
  }

  private generateTableId() {
    const id = this.location.path(true).replace(/[?].*$/g, '').replace(/\/[\d]+/g, '_id') + "_" + this.constructor.name;
    return id;
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
