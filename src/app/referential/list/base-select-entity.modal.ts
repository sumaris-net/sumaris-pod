import {Directive, Input, OnInit, Optional, ViewChild} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {isNotNil, toBoolean} from "../../shared/functions";
import {Subject} from "rxjs";
import {AppTableDataSourceOptions, EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {ReferentialRef} from "../../core/services/model/referential.model";
import {environment} from "../../../environments/environment";
import {IEntity} from "../../core/services/model/entity.model";
import {IEntitiesService} from "../../shared/services/entity-service.class";
import {AppTable} from "../../core/table/table.class";

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class BaseSelectEntityModal<
  T extends IEntity<T, ID>,
  F = any,
  ID = number
  > implements OnInit {

  selectedTabIndex = 0;
  $title = new Subject<string>();
  datasource: EntitiesTableDataSource<T, F, ID>;

  @ViewChild('table', { static: true }) table: AppTable<T, F, ID>;

  @Input() filter: F;
  @Input() entityName: string;
  @Input() allowMultiple: boolean;

  get loading(): boolean {
    return this.table && this.table.loading;
  }

  protected constructor(
    protected viewCtrl: ModalController,
    protected dataType: new() => T,
    protected dataService: IEntitiesService<T, F>,
    @Optional() protected options?: Partial<AppTableDataSourceOptions<T, ID>>
  ) {
  }

  ngOnInit() {

    // Init table
    if (!this.table) throw new Error("Missing table child component");
    if (!this.filter) throw new Error("Missing argument 'filter'");

    // Set defaults
    this.allowMultiple = toBoolean(this.allowMultiple, false);

    this.datasource = new EntitiesTableDataSource<T, F, ID>(this.dataType,
      this.dataService,
      null,
      {
        prependNewElements: false,
        suppressErrors: environment.production,
        ...this.options
      });

    this.table.setDatasource(this.datasource);
    this.table.filter = this.filter;

    // Compute title
    this.updateTitle();

    this.loadData();

  }

  loadData() {
    // Load data
    setTimeout(() => {

      this.table.onRefresh.next("modal");
      this.markForCheck();
    }, 200);
  }

  async selectRow({id, row}) {
    const table = this.table;
    if (row && table) {
      if (!this.allowMultiple) {
        table.selection.clear();
        table.selection.select(row);
        await this.close();
      }
      else {
        table.selection.select(row);
      }
    }
  }

  async close(event?: any): Promise<boolean> {
    try {
      if (this.hasSelection()) {
        const items = (this.table.selection.selected || [])
            .map(row => row.currentData)
            .map(ReferentialRef.fromObject)
            .filter(isNotNil);
        this.viewCtrl.dismiss(items);
      }
      return true;
    } catch (err) {
      // nothing to do
      return false;
    }
  }

  async cancel() {
    await this.viewCtrl.dismiss();
  }

  hasSelection(): boolean {
    const table = this.table;
    return table && table.selection.hasValue() && (this.allowMultiple || table.selection.selected.length === 1);
  }

  private async updateTitle(){
    const title = await this.computeTitle();
    this.$title.next(title);
  }

  protected abstract computeTitle(): Promise<string>;

  protected markForCheck() {
    // Can be override by subclasses
  }
}
