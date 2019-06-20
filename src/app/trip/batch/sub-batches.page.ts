import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  ViewChild
} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {BatchValidatorService} from "../services/batch.validator";
import {InMemoryTableDataService} from "../../shared/services/memory-data-service.class";
import {Batch} from "../services/model/batch.model";
import {BatchFilter} from "./batches.table";
import {AcquisitionLevelCodes, UsageMode} from "../../core/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {SubBatchForm} from "./sub-batch.form";
import {environment} from "../../../environments/environment";

@Component({
  selector: 'app-sub-batches',
  templateUrl: 'sub-batches.page.html',
  //styleUrls: ['sub-batches.page.scss'],
  providers: [
    {provide: ValidatorService, useClass: BatchValidatorService},
    {
      provide: InMemoryTableDataService,
      useFactory: () => new InMemoryTableDataService<Batch, BatchFilter>(Batch, {})
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchesPage implements OnInit, OnDestroy {


  private cd: ChangeDetectorRef;

  debug = false;

  @Input() program: String;

  @Input() usageMode: UsageMode;

  @Input() acquisitionLevel: string;

  @ViewChild('form') form: SubBatchForm;

  constructor(
    protected injector: Injector,
    protected settings: LocalSettingsService
  ) {
    //super();
    this.cd = injector.get(ChangeDetectorRef);

    this.usageMode = this.settings.usageMode;

    // TODO: remove this
    this.program = 'ADAP-MER';

    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL;

    // TODO: for DEV only
    this.debug = !environment.production;
  }

  ngOnInit(): void {

    // TODO
    this.form.setValue(new Batch());

  }

  ngOnDestroy(): void {
  }

  markForCheck() {
    this.cd.markForCheck();
  }

}
