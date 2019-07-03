import {Batch} from "../services/model/batch.model";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {BatchFilter} from "./batches.table";
import {Input} from "@angular/core";
import {toBoolean} from "../../shared/functions";


export class BatchesContext {

  batchesDataService = new InMemoryTableDataService<Batch, BatchFilter>(Batch, {});

  private _default: Batch;

  @Input()
  set value(data: Batch[]) {
    this.batchesDataService.value = data;
  }

  get value(): Batch[] {
    return this.batchesDataService.value;
  }

  get default(): Batch {
    return this._default;
  }

  constructor() {
    this._default = new Batch();
  }

  async getIndividualMeasureParent(): Promise<Batch[]> {
    //if (this._dirty) await this.save();
    const batches = this.batchesDataService.value;
    return batches;
  }

  createBatch(opts?: {useDefault?: boolean}): Batch {
    opts = opts || {};
    if (this._default && toBoolean(opts.useDefault, true)) {
      return this._default.clone();
    }
    return new Batch();
  }

  setDefault(batch?: Batch) {
    if (batch) {
      this._default = batch.clone();
    }
    else {
      this._default = new Batch();
    }
  }

}
