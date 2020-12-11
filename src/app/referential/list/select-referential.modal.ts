import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, Input, OnInit, ViewChild} from "@angular/core";
import {ModalController} from "@ionic/angular";
import {changeCaseToUnderscore, isNotNil, toBoolean} from "../../shared/functions";
import {ReferentialFilter} from "../services/referential.service";
import {Subject} from "rxjs";
import {ReferentialRefFilter, ReferentialRefService} from "../services/referential-ref.service";
import {EntitiesTableDataSource} from "../../core/table/entities-table-datasource.class";
import {ReferentialRefTable} from "./referential-ref.table";
import {ReferentialRef} from "../../core/services/model/referential.model";
import {EnvironmentService} from "../../../environments/environment.class";

@Component({
  selector: 'app-select-referential-modal',
  templateUrl: './select-referential.modal.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SelectReferentialModal implements OnInit {

  selectedTabIndex = 0;
  $title = new Subject<string>();

  @ViewChild(ReferentialRefTable, { static: true }) table: ReferentialRefTable;

  @Input() filter: ReferentialFilter;
  @Input() entityName: string;
  @Input() allowMultiple: boolean;

  get loading(): boolean {
    return this.table && this.table.loading;
  }

  constructor(
    protected viewCtrl: ModalController,
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef,
    @Inject(EnvironmentService) protected environment
  ) {
  }

  ngOnInit() {
    // Init table
    if (!this.filter || !this.filter.entityName) {
      throw new Error("Missing argument 'filter'");
    }
    this.table.setDatasource(new EntitiesTableDataSource<ReferentialRef, ReferentialRefFilter>(ReferentialRef,
      this.referentialRefService,
      this.environment,
      null,
      {
        prependNewElements: false,
        suppressErrors: true
      }));
    this.table.filter = this.filter;

    // Compute title
    this.$title.next('REFERENTIAL.ENTITY.' + changeCaseToUnderscore(this.filter.entityName).toUpperCase());

    // Set defaults
    this.allowMultiple = toBoolean(this.allowMultiple, false);

    // Load data
    setTimeout(() => {

      this.table.onRefresh.next("modal");
      this.markForCheck();
    }, 200);

  }

  loadData() {
    /*this.referentialRefService.loadAll()*/
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

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
