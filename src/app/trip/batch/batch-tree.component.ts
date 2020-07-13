import {AfterViewInit, ChangeDetectionStrategy, Component, Injector, Input, OnInit, ViewChild} from '@angular/core';
import {isNil, isNotEmptyArray, isNotNil, isNotNilOrBlank, toBoolean} from '../../shared/functions';
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {AlertController, ModalController} from "@ionic/angular";
import {BehaviorSubject, Subject} from "rxjs";
import {AppEntityEditor} from "../../core/form/editor.class";
import {FormGroup} from "@angular/forms";
import {OperationService} from "../services/operation.service";
import {ProgramService} from "../../referential/services/program.service";
import {debounceTime, filter, map, switchMap} from "rxjs/operators";
import {TripService} from "../services/trip.service";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {BatchGroup} from "../services/model/batch-group.model";
import {PlatformService} from "../../core/services/platform.service";
import {BatchGroupsTable} from "./table/batch-groups.table";
import {SubBatchesTable} from "./table/sub-batches.table";
import {CatchBatchForm} from "../catch/catch.form";
import {AcquisitionLevelCodes, PmfmIds} from "../../referential/services/model/model.enum";
import {AppTabEditor, AppTableUtils, environment} from "../../core/core.module";
import {ActivatedRoute, Router} from "@angular/router";
import {TranslateService} from "@ngx-translate/core";
import {UsageMode} from "../../core/services/model/settings.model";
import {MatTabChangeEvent} from "@angular/material/tabs";
import {firstTruePromise} from "../../shared/observables";
import {ProgramProperties} from "../../referential/services/config/program.config";
import {ReferentialUtils} from "../../core/services/model/referential.model";

@Component({
  selector: 'app-batch-tree',
  templateUrl: './batch-tree.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchTreeComponent extends AppTabEditor<Batch, any> implements OnInit, AfterViewInit{

  private _gearId: number;
  private _defaultTaxonGroups: string[];

  showSubBatchesTable: boolean;
  enableCatchForm = false;
  enableSubBatchesTab = false;
  tempSubBatches: Batch[];
  saveOptions = {
    computeBatchRankOrder: false,
    computeBatchIndividualCount: false,
  }

  data: Batch;
  programSubject = new BehaviorSubject<string>(undefined);

  @Input() debug: boolean;
  @Input() mobile: boolean;
  @Input() usageMode: UsageMode;

  get isNewData(): boolean {
    return false;
  }

  @Input()
  set value(catchBatch: Batch) {
    this.setValue(catchBatch);
  }

  get value(): Batch {
    return this.getValue();
  }

  @Input()
  set program(value: string) {
    this.programSubject.next(value);
  }

  @Input()
  set gearId(value: number) {
    if (this._gearId !== value && isNotNil(value)) {
      this._gearId = value;
      this.catchBatchForm.gearId = value;
    }
  }

  @Input() set defaultTaxonGroups(value: string[]) {
    if (this._defaultTaxonGroups !== value) {
      this._defaultTaxonGroups = value;
      if (!this.loading) this.batchGroupsTable.defaultTaxonGroups = value;
    }
  }

  get defaultTaxonGroups(): string[] {
    return this._defaultTaxonGroups;
  }

  @ViewChild('catchBatchForm', { static: true }) catchBatchForm: CatchBatchForm;
  @ViewChild('batchGroupsTable', { static: true }) batchGroupsTable: BatchGroupsTable;
  @ViewChild('subBatchesTable', { static: false }) subBatchesTable: SubBatchesTable;

  constructor(
    protected route: ActivatedRoute,
    protected router: Router,
    protected alertCtrl: AlertController,
    protected translate: TranslateService,
    injector: Injector,
    protected programService: ProgramService,
    protected tripService: TripService,
    protected operationService: OperationService,
    protected modalCtrl: ModalController,
    protected platform: PlatformService
  ) {
    super(route, router, alertCtrl, translate,
          {
            tabCount: 2
          });

    // Defaults
    this.mobile = this.platform.mobile;


    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    // Hide sub batches table, when mobile
    this.showSubBatchesTable = !this.mobile;

    super.ngOnInit();

    this.registerForms();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject
        .pipe(
          filter(isNotNilOrBlank),
          switchMap(programLabel => this.programService.watchByLabel(programLabel))
        )
      .subscribe(program => {
        if (this.debug) console.debug(`[batch-tree] Program ${program.label} loaded, with properties: `, program.properties);

        this.batchGroupsTable.showTaxonGroupColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_GROUP_ENABLE);
        this.batchGroupsTable.showTaxonNameColumn = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_NAME_ENABLE);

        // Some specific taxon groups have no weight collected
        const taxonGroupsNoWeight = program.getProperty(ProgramProperties.TRIP_BATCH_TAXON_GROUPS_NO_WEIGHT);
        this.batchGroupsTable.taxonGroupsNoWeight = taxonGroupsNoWeight && taxonGroupsNoWeight.split(',')
          .map(label => label.trim().toUpperCase())
          .filter(isNotNilOrBlank) || undefined;

        // Force taxon name in sub batches, if not filled in root batch
        if (this.subBatchesTable) {
          this.subBatchesTable.showTaxonNameColumn = !this.batchGroupsTable.showTaxonNameColumn;
          this.subBatchesTable.showIndividualCount = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_MEASURE_INDIVIDUAL_COUNT_ENABLE);
        }
        this.saveOptions.computeBatchRankOrder = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_MEASURE_RANK_ORDER_COMPUTE);
        this.saveOptions.computeBatchIndividualCount = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_INDIVIDUAL_COUNT_COMPUTE);

      })
    );
  }

  ngAfterViewInit() {

    if (this.showSubBatchesTable) {

      // Enable sub batches table, only when table pmfms ready
      firstTruePromise(this.subBatchesTable.$pmfms.pipe(map(isNotEmptyArray)))
        .then(() => {
          this.enableSubBatchesTab = true;
          this.markForCheck();
        });

      // Update available parent on individual batch table, when batch group changes
      this.registerSubscription(
        this.batchGroupsTable.dataSource.datasourceSubject
          .pipe(
            debounceTime(400),
            // skip if loading
            filter(() => !this.loading && this.enableSubBatchesTab)
          )
          // Will refresh the tables (inside the setter):
          .subscribe(rootBatches => this.subBatchesTable.availableParents = (rootBatches || []))
      );

      // Link group table to sub batches table
      this.batchGroupsTable.availableSubBatchesFn = async () => {
        if (this.subBatchesTable.dirty) await this.subBatchesTable.save();
        return this.subBatchesTable.value;
      };
    }
    else {
      this.batchGroupsTable.availableSubBatchesFn = (() => Promise.resolve(this.tempSubBatches));
    }
  }

  async load(id?: number, options?: any): Promise<any> {
    return Promise.resolve(undefined);
  }

  async save(event?: UIEvent, options?: any): Promise<any> {
    if (this.batchGroupsTable.dirty) {
      await this.batchGroupsTable.save();
    }
    if (this.subBatchesTable && this.subBatchesTable.dirty) {
      await this.subBatchesTable.save();
    }

    const source = this.getJsonValueToSave();

    if (this.data) {
      this.data.fromObject(source);
    }
    else {
      this.data = Batch.fromObject(source);
    }

    return true;
  }

  protected getJsonValueToSave(): any {
    const catchBatch = this.form.value;

    const batchGroups = this.batchGroupsTable.value;
    const subBatches = this.showSubBatchesTable ? this.subBatchesTable.value : this.tempSubBatches;

    catchBatch.children = BatchUtils.prepareRootBatchesForSaving(batchGroups, subBatches, this.batchGroupsTable.qvPmfm);

    // DEBUG
    if (this.debug) BatchUtils.logTree(catchBatch);

    return catchBatch;
  }

  async reload() {

  }

  getValue(): Batch {
    return this.data;
  }

  ready(): Promise<any> {
    const promises = [
      this.catchBatchForm.ready(),
      this.batchGroupsTable.onReady()
    ];
    if (this.showSubBatchesTable) {
      promises.push(this.subBatchesTable.onReady());
    }
    return Promise.all(promises);
  }

  /* -- protected method -- */

  async setValue(catchBatch: Batch) {
    // Make sure this is catch batch
    if (catchBatch && catchBatch.label !== AcquisitionLevelCodes.CATCH_BATCH) {
      throw new Error('Catch batch should have label=' + AcquisitionLevelCodes.CATCH_BATCH)
    }

    catchBatch = catchBatch || Batch.fromObject({
      rankOrder: 1,
      label: AcquisitionLevelCodes.CATCH_BATCH
    });

    this.data = catchBatch;

    // Set catch batch
    this.catchBatchForm.gearId = this._gearId;
    this.catchBatchForm.value = catchBatch;

    // Get all batches (and children), and samples
    const batches = catchBatch.children || [];

    // Retrieve batch group (make sure label start with acquisition level)
    // Then convert into batch group entities
    const speciesBatchGroups = batches.filter(s => s.label && s.label.startsWith(this.batchGroupsTable.acquisitionLevel + "#"))
      .map(BatchGroup.fromBatch);

    // Apply to table
    this.batchGroupsTable.value = speciesBatchGroups;

    // Wait batch group table ready (need to be sure the QV pmfm is set)
    this.batchGroupsTable.onReady()
      .then(() => {
        const qvPmfm = this.batchGroupsTable.qvPmfm;
        const subBatches = BatchUtils.prepareSubBatchesForTable(speciesBatchGroups, AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL, qvPmfm);
        if (this.subBatchesTable) {
          this.subBatchesTable.qvPmfm = qvPmfm;
          this.subBatchesTable.setAvailableParents(speciesBatchGroups, {emitEvent: false, linkDataToParent: false})
          this.subBatchesTable.value = subBatches;
        }
        else {
          this.tempSubBatches = subBatches;
        }
      });
  }

  protected get form(): FormGroup {
    return this.catchBatchForm.form;
  }

  protected registerForms() {
    this.addChildForms([this.catchBatchForm, this.batchGroupsTable]);
    if (!this.mobile) {
      this.addChildForm(() => this.subBatchesTable);
    }
  }


  async onSubBatchesChanges(subbatches: Batch[]) {
    if (isNil(subbatches)) return; // user cancelled

    if (!this.mobile) {
      this.subBatchesTable.value = subbatches;

      // Wait table not busy
      await AppTableUtils.waitIdle(this.subBatchesTable);

      this.subBatchesTable.markAsDirty();
    }
    else {
      this.tempSubBatches = subbatches;
      if (!this._dirty) this.markAsDirty();
    }
  }

  onTabChange(event: MatTabChangeEvent, queryTabIndexParamName?: string) {
    const result = super.onTabChange(event, queryTabIndexParamName);
    if (this.loading) return result;

    // On each tables, confirm the current editing row
    this.batchGroupsTable.confirmEditCreate();
    if (!this.mobile) this.subBatchesTable.confirmEditCreate();

    return result;
  }


  autoFill(): Promise<void> {
    return this.batchGroupsTable.autoFillTable();
  }


  /* -- protected methods -- */

  protected getFirstInvalidTabIndex(): number {
    if (this.enableCatchForm && this.catchBatchForm.invalid) return 0;
    if (this.batchGroupsTable.invalid) return 0;
    if (this.showSubBatchesTable && this.subBatchesTable.invalid) return 1;
    return -1;
  }
}
