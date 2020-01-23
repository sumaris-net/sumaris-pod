import {ChangeDetectionStrategy, Component, Inject, Injector, Input, OnInit, ViewChild} from "@angular/core";
import {TableElement, ValidatorService} from "angular4-material-table";
import {Batch} from "../services/model/batch.model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {SubBatchForm} from "./sub-batch.form";
import {SubBatchValidatorService} from "../services/sub-batch.validator";
import {SUB_BATCHES_TABLE_OPTIONS, SubBatchesTable} from "./sub-batches.table";
import {AppMeasurementsTableOptions} from "../measurement/measurements.table.class";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {AppFormUtils, EntityUtils, environment, isNil} from "../../core/core.module";
import {ModalController} from "@ionic/angular";
import {isNotNilOrBlank} from "../../shared/functions";
import {AudioProvider} from "../../shared/audio/audio";
import {Alerts} from "../../shared/alerts";


export const SUB_BATCH_MODAL_RESERVED_START_COLUMNS: string[] = ['parent', 'taxonName'];
export const SUB_BATCH_MODAL_RESERVED_END_COLUMNS: string[] = ['comments']; // do NOT use individual count

@Component({
  selector: 'app-sub-batches-modal',
  styleUrls: ['sub-batches.modal.scss'],
  templateUrl: 'sub-batches.modal.html',
  providers: [
    {provide: ValidatorService, useExisting: SubBatchValidatorService},
    {
      provide: SUB_BATCHES_TABLE_OPTIONS,
      useFactory: () => {
        return {
          prependNewElements: true,
          suppressErrors: true,
          reservedStartColumns: SUB_BATCH_MODAL_RESERVED_START_COLUMNS,
          reservedEndColumns: SUB_BATCH_MODAL_RESERVED_END_COLUMNS
        };
      }
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchesModal extends SubBatchesTable implements OnInit {

  private _initialMaxRankOrder: number;
  private _previousMaxRankOrder: number;
  private _selectedParent: Batch;
  private _hiddenData: Batch[];

  @Input() onNewParentClick: () => Promise<Batch | undefined>;

  @Input()
  availableSubBatchesFn: () => Promise<Batch[]>;

  @ViewChild('form', { static: true }) form: SubBatchForm;

  get dirty(): boolean {
    return this._dirty || this.form.dirty;
  }

  get valid(): boolean {
    return this.form.valid;
  }

  get invalid(): boolean {
    return this.form.invalid;
  }

  set selectedParent(parent: Batch) {
    this._selectedParent = parent;
  }

  constructor(
    protected injector: Injector,
    protected viewCtrl: ModalController,
    protected settings: LocalSettingsService,
    protected audio: AudioProvider,
    @Inject(SUB_BATCHES_TABLE_OPTIONS) options: AppMeasurementsTableOptions<Batch>
  ) {
    super(injector,
      null/*no validator = not editable*/,
      options);
    this.inlineEdition = false; // Disable row edition (readonly)
    this.confirmBeforeDelete = true; // Ask confirmation before delete
    this.allowRowDetail = false; // Disable click on a row

    // default values
    this.showCommentsColumn = false;
    this.showParentColumn = false;
    this.showIndividualCount = !this.isOnFieldMode; // Hide individual count on mobile device

    // TODO: for DEV only ---
    this.debug = !environment.production;

  }

  async ngOnInit() {
    super.ngOnInit();


    await this.form.ready();

    const data = (this.availableSubBatchesFn && (await this.availableSubBatchesFn()) || [])
      .sort(EntityUtils.sortComparator('rankOrder', 'desc'));

    // Compute the first rankOrder to save
    this._initialMaxRankOrder = data.length && data[0].rankOrder || 0;

    let defaultBatch: Batch;
    if (this._selectedParent) {
      defaultBatch = new Batch();
      defaultBatch.parent = this._selectedParent;
    }

    // Reset the form, using default value
    await this.resetForm(defaultBatch);

    // Update table content when changing parent
    this.registerSubscription(
      this.form.form.get('parent').valueChanges
        // Init table with existing values
        //.pipe(startWith(() => this._defaultValue && this._defaultValue.parent))
        .subscribe(parent => this.onParentChange(parent))
    );

    // Apply data to table
    this.value = data;
  }

  async cancel(event?: UIEvent) {

    if (this.dirty) {
      const saveBeforeLeave = await Alerts.askSaveBeforeLeave(this.alertCtrl, this.translate, event);

      // User cancelled
      if (isNil(saveBeforeLeave) || event && event.defaultPrevented) {
        return;
      }

      // Is user confirm: close normally
      if (saveBeforeLeave === true) {
        this.close(event);
        return;
      }
    }

    await this.viewCtrl.dismiss();
  }

  async close(event?: UIEvent) {
    if (this.loading) return; // avoid many call

    if (this.debug) console.debug("[sub-batch-modal] Closing modal...");
    if (this.debug && this.form.dirty && this.form.invalid) {
      AppFormUtils.logFormErrors(this.form.form, "[sub-batch-modal] ");
      // Continue
    }

    this.markAsLoading();
    this.error = undefined;

    try {
      // Save changes
      const saved = await this.save();
      if (!saved) return; // Error

      await this.viewCtrl.dismiss(this.getValue());
    } catch (err) {
      console.error(err);
      this.error = err && err.message || err;
      this.markAsLoaded();
    }
  }

  isNewRow(row: TableElement<Batch>): boolean {
    return row.currentData.rankOrder > this._initialMaxRankOrder;
  }

  onEditRow(event: MouseEvent, row?: TableElement<Batch>): boolean {

    row = row || (!this.selection.isEmpty() && this.selection.selected[0]);
    if (!row) throw new Error ("Missing row argument, or a row selection.");

    // Confirm last edited row
    if (this.editedRow) {
      this.confirmEditCreate();
    }

    // Copy the row into the form
    this.form.setValue(row.validator ? Batch.fromObject(row.currentData) : row.currentData, {emitEvent: true});
    this.markForCheck();

    // Then remove the row
    row.startEdit();
    this.editedRow = row;
    return true;
  }

  selectRow(event: MouseEvent|null, row: TableElement<Batch>) {
    if (event && event.defaultPrevented || !row) return;
    if (event) event.preventDefault();

    this.selection.clear();
    this.selection.toggle(row);
  }

  /* -- protected methods -- */

  protected async onParentChange(parent?: Batch) {
    // Skip if same parent
    if (Batch.equals(this._selectedParent, parent)) return;

    // Store the new parent, in order apply filter in onLoadData()
    this._selectedParent = isNotNilOrBlank(parent) ? parent : undefined;

    // If pending changes, save new rows
    if (this._dirty) {
      const saved = await this.save();
      if (!saved) {
        console.error('Could not save the table');
        this.form.error = 'ERROR.SAVE_DATA_ERROR';
        return;
      }
    }

    // Call refresh on datasource, to force a data reload (will apply filter calling onLoadData())
    this.onRefresh.emit();
  }

  protected onLoadData(data: Batch[]): Batch[] {

    // Filter by parent
    if (data && this._selectedParent) {
      const showIndividualCount = this.showIndividualCount; // Read once the getter value
      const filteredData = [];
      const hiddenData = [];
      let maxRankOrder = this._previousMaxRankOrder || this._initialMaxRankOrder;
      data.forEach(b => {
        // Filter on individual count = 1 when individual count is hide
        // AND same parent
        if ( (showIndividualCount || b.individualCount === 1)
          && Batch.equals(this._selectedParent, b.parent)) {
          filteredData.push(b);
        }
        else {
          hiddenData.push(b);
        }
        maxRankOrder = Math.max(maxRankOrder, b.rankOrder || 0);
      });
      this._hiddenData = hiddenData;
      this._previousMaxRankOrder = maxRankOrder;
      return super.onLoadData(filteredData);
    }
    // Not filtered
    else {
      this._hiddenData = [];
      return super.onLoadData(data);
    }
  }

  protected onSaveData(data: Batch[]): Batch[] {
    // Append hidden data to the list, then save
    return super.onSaveData(data.concat(this._hiddenData || []));
  }

  protected async getMaxRankOrder(): Promise<number> {
    return Math.max(await super.getMaxRankOrder(), this._previousMaxRankOrder || this._initialMaxRankOrder);
  }

  protected async addBatchToTable(newBatch: Batch): Promise<TableElement<Batch>> {
    const row = await super.addBatchToTable(newBatch);

    // Highlight the row, few seconds
    this.onRowChanged(row);

    return row;
  }

  protected updateRowFromBatch(updatedBatch: Batch, row: TableElement<Batch>): boolean {
    const res = super.updateRowFromBatch(updatedBatch, row);

    // Highlight the row, few seconds
    this.onRowChanged(row);

    return res;
  }

  protected onInvalidForm() {

    // Play a error beep, if on field
    if (this.isOnFieldMode) this.audio.playBeepError();

    super.onInvalidForm();
  }

  /**
   * When a row has been edited, play a beep and highlight the row (during few seconds)
   * @param row
   * @pram times duration of hightligh
   */
  protected onRowChanged(row: TableElement<Batch>) {

    // Play a beep, if on field
    if (this.isOnFieldMode) {
      this.audio.playBeepConfirm();
    }

    // Unselect previous selected rows
    this.selection.clear();

    // Selection the row (this will apply CSS class mat-row-selected)
    this.selection.select(row);


    setTimeout(() => {
      // If row is still selected: unselect it
      if (this.selection.isSelected(row)) this.selection.toggle(row);
      this.markForCheck();
    }, 1500);
  }

  measurementValueToString = MeasurementValuesUtils.valueToString;

}
