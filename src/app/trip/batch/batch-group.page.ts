import {ChangeDetectionStrategy, Component, Injector, OnInit, ViewChild} from '@angular/core';
import {isNil, isNotNilOrBlank} from '../../shared/shared.module';
import {Batch} from "../services/trip.model";
import {EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";
import {ModalController} from "@ionic/angular";
import {BehaviorSubject, Observable, Subject} from "rxjs";
import {AppEditorPage} from "../../core/form/editor-page.class";
import {environment} from "../../../environments/environment";
import {FormGroup} from "@angular/forms";
import {OperationService} from "../services/operation.service";
import {ProgramService} from "../../referential/services/program.service";
import {Program, ProgramProperties} from "../../referential/services/model";
import {filter, switchMap} from "rxjs/operators";
import {TripService} from "../services/trip.service";
import {BatchUtils} from "../services/model/batch.model";
import {BatchGroupForm} from "./batch-group.form";

@Component({
  selector: 'app-batch-group-page',
  templateUrl: './batch-group.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchGroupPage extends AppEditorPage<Batch, any> implements OnInit {

  programSubject = new BehaviorSubject<string>(undefined);

  acquisitionLevelSubject = new Subject<string>();

  @ViewChild('form') batchGroupForm: BatchGroupForm;

  constructor(
    injector: Injector,
    protected programService: ProgramService,
    protected tripService: TripService,
    protected operationService: OperationService,
    protected modalCtrl: ModalController
  ) {
    super(injector,
      Batch,
      {
        load: async (id, {operationId}) => {
          // Load operation
          const operation = await this.operationService.load(operationId, {fetchPolicy: "cache-first"});

          // Load batch
          const batch = operation && operation.catchBatch && (operation.catchBatch.children || []).find(b => b.id === id) || new Batch();

          // Load trip
          const trip = operation && await this.tripService.load(operation.tripId, {fetchPolicy: "cache-first"});

          // Emit events
          this.defaultBackHref = `/trips/${trip.id}/operations/${operationId}`;
          this.programSubject.next(trip && trip.program && trip.program.label);
          this.acquisitionLevelSubject.next(BatchUtils.getAcquisitionLevelFromLabal(batch));

          return batch;
        },
        delete: async (batch) => {},
        listenChanges: (id) => {return Observable.of()},
        save: async (batch) => {return batch}
      });

    this.idAttribute = 'batchId';

    // FOR DEV ONLY ----
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    // Watch program, to configure tables from program properties
    this.registerSubscription(
      this.programSubject.asObservable()
        .pipe(
          filter(isNotNilOrBlank),
          switchMap(programLabel => this.programService.watchByLabel(programLabel, true))
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

  protected registerFormsAndTables() {
    this.registerForm(this.batchGroupForm);
  }

  protected async onNewEntity(data: Batch, options?: EditorDataServiceLoadOptions): Promise<void> {
    // If is on field mode, fill default values
    if (this.isOnFieldMode) {
      //data.startDateTime = moment();
    }
  }

  protected async onEntityLoaded(data: Batch, options?: EditorDataServiceLoadOptions): Promise<void> {
    // nothing to do
  }

  protected setValue(data: Batch) {
    this.batchGroupForm.value = data;
  }

  protected async computeTitle(data: Batch): Promise<string> {
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
