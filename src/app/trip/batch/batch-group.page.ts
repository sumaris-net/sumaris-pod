import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {isNil, isNotNilOrBlank} from '../../shared/functions';
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject, of, Subject} from "rxjs";
import {AppEntityEditor} from "../../core/form/editor.class";
import {FormGroup} from "@angular/forms";
import {OperationService} from "../services/operation.service";
import {ProgramService} from "../../referential/services/program.service";
import {ProgramProperties} from "../../referential/services/config/program.config";
import {filter, switchMap} from "rxjs/operators";
import {TripService} from "../services/trip.service";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {BatchGroupForm} from "./batch-group.form";
import {BatchGroup} from "../services/model/batch-group.model";

@Component({
  selector: 'app-batch-group-page',
  templateUrl: './batch-group.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchGroupPage extends AppEntityEditor<BatchGroup, any> implements OnInit {

  programSubject = new BehaviorSubject<string>(undefined);

  acquisitionLevelSubject = new Subject<string>();

  @ViewChild('form', { static: true }) batchGroupForm: BatchGroupForm;

  constructor(
    injector: Injector,
    protected programService: ProgramService,
    protected tripService: TripService,
    protected operationService: OperationService,
    protected modalCtrl: ModalController
  ) {
    super(injector,
      BatchGroup,
      {
        load: async (id, {operationId}) => {
          // Load operation
          const operation = await this.operationService.load(operationId, {fetchPolicy: "cache-first"});

          // Load batch
          const obj = operation && operation.catchBatch && (operation.catchBatch.children || []).find(b => b.id === id) || undefined;
          const batch = BatchGroup.fromBatch(obj);

          // Load trip
          const trip = operation && await this.tripService.load(operation.tripId, {fetchPolicy: "cache-first"});

          // Emit events
          this.defaultBackHref = `/trips/${trip.id}/operations/${operationId}`;
          this.programSubject.next(trip && trip.program && trip.program.label);
          this.acquisitionLevelSubject.next(BatchUtils.getAcquisitionLevelFromLabel(batch));

          return batch;
        },
        delete: async (batch) => {}, // TODO
        listenChanges: (id) => of(), // TODO
        save: async (batch) => batch // TODO
      }, {
        pathIdAttribute: 'batchId'
      });

    // FOR DEV ONLY ----
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject
        .pipe(
          filter(isNotNilOrBlank),
          switchMap(programLabel => this.programService.watchByLabel(programLabel))
        )
      .subscribe(program => {
        if (this.debug) console.debug(`[batch-group-page] Program ${program.label} loaded, with properties: `, program.properties);
        this.batchGroupForm.showTaxonGroup = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_GROUP_ENABLE);
        this.batchGroupForm.showTaxonName = program.getPropertyAsBoolean(ProgramProperties.TRIP_BATCH_TAXON_NAME_ENABLE);
      })
    );
  }

  /* -- protected method -- */

  protected canUserWrite(data: Batch): boolean {
    return true;
  }

  protected get form(): FormGroup {
    return this.batchGroupForm.form;
  }

  protected registerForms() {
    this.addChildForm(this.batchGroupForm);
  }

  protected async onNewEntity(data: Batch, options?: EntityServiceLoadOptions): Promise<void> {
    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      //data.startDateTime = moment();
    }
  }

  protected async onEntityLoaded(data: Batch, options?: EntityServiceLoadOptions): Promise<void> {
    // nothing to do
  }

  protected setValue(data: BatchGroup) {
    this.batchGroupForm.value = data;
  }

  protected async computeTitle(data: BatchGroup): Promise<string> {
    // new data
    if (!data || isNil(data.id)) {
      return await this.translate.get('TRIP.BATCH.NEW.TITLE').toPromise();
    }

    // Existing data
    const label = BatchUtils.parentToString(data);
    return await this.translate.get('TRIP.BATCH.EDIT.TITLE', {label}).toPromise();
  }

  protected getFirstInvalidTabIndex(): number {
    return -1;
  }

  /* -- protected methods -- */

}
