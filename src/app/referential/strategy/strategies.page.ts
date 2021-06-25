import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ViewChild } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { TableElement } from "@e-is/ngx-material-table/src/app/ngx-material-table/table-element";
import { Subject } from "rxjs";
import { AppTable, isNotNil, EntityServiceLoadOptions } from "@sumaris-net/ngx-components";
import { ProgramProperties, StrategyEditor } from "../services/config/program.config";
import { Program } from "../services/model/program.model";
import { Strategy } from "../services/model/strategy.model";
import { ProgramService } from "../services/program.service";
import { ReferentialRefService } from "../services/referential-ref.service";
import { SamplingStrategiesTable } from "./sampling/sampling-strategies.table";
import { StrategiesTable } from "./strategies.table";

// app-strategies-page
@Component({
  selector: 'app-strategies-page',
  templateUrl: 'strategies.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StrategiesPage {

  data: Program;
  strategyEditor: StrategyEditor = 'legacy';

  error: string = null;
  enabled = false;
  i18nSuffix = '';
  $title = new Subject<string>();

  @ViewChild('legacyStrategiesTable', { static: true }) legacyStrategiesTable: StrategiesTable;
  @ViewChild('samplingStrategiesTable', { static: true }) samplingStrategiesTable: SamplingStrategiesTable;

  get strategiesTable(): AppTable<Strategy> {
    return this.strategyEditor !== 'sampling' ? this.legacyStrategiesTable : this.samplingStrategiesTable;
  }

  constructor(
    protected referentialRefService: ReferentialRefService,
    protected cd: ChangeDetectorRef,
    protected router: Router,
    protected programService: ProgramService,
    protected route: ActivatedRoute
  ) { }

  ngOnInit() {
    const id = this.route.snapshot.params['programId'];
    if (isNotNil(id)) this.load(+id);
  }

  async load(id?: number, opts?: EntityServiceLoadOptions) {
    try {
      // Force the load from network
      this.data = await this.programService.load(id, { ...opts, fetchPolicy: "network-only" });
      this.strategyEditor = this.data.getProperty<StrategyEditor>(ProgramProperties.PROGRAM_STRATEGY_EDITOR);
      this.i18nSuffix = this.data.getProperty<StrategyEditor>(ProgramProperties.I18N_SUFFIX);
      this.$title.next(this.data.label);
    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
    }
  }

  async onOpenStrategy({ id, row }: { id?: number; row: TableElement<any>; }) {

    // Skip
    if (this.loading) return;

    this.markAsLoading();
    setTimeout(async () => {
      await this.router.navigate(['referential', 'programs', this.data.id, 'strategy', this.strategyEditor, id], {
        queryParams: {}
      });
      this.markAsLoaded();
    });

  }

  async onNewStrategy(event?: any) {

    // Skip
    if (this.loading) return;

    this.markAsLoading();
    setTimeout(async () => {
      await this.router.navigate(['referential', 'programs', this.data.id, 'strategy', this.strategyEditor, 'new'], {
        queryParams: {}
      });
      this.markAsLoaded();
    });

  }

  markAsLoading(opts?: { emitEvent?: boolean }) {
    this.strategiesTable?.markAsLoading(opts);
  }

  markAsLoaded(opts?: { emitEvent?: boolean }) {
    this.strategiesTable?.markAsLoaded(opts);
  }

  get loading(): boolean {
    return this.strategiesTable && this.strategiesTable.loading;
  }
}
