import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {Batch} from "../services/model/batch.model";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {FormBuilder} from "@angular/forms";
import {ProgramService} from "../../referential/services/program.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {SubBatchValidatorService} from "../services/sub-batch.validator";
import {AcquisitionLevelCodes, ReferentialRef, referentialToString, UsageMode} from "../../core/services/model";
import {debounceTime, switchMap, tap} from "rxjs/operators";
import {TaxonGroupIds, TaxonomicLevelIds} from "../../referential/services/model";
import {Observable} from "rxjs";

@Component({
  selector: 'app-sub-batch-form',
  templateUrl: 'sub-batch.form.html',
  //styleUrls: ['sub-batch.form.scss'],
  providers: [
    {provide: ValidatorService, useClass: SubBatchValidatorService}
    // {
    //   provide: InMemoryTableDataService,
    //   useFactory: () => new InMemoryTableDataService<Batch, BatchFilter>(Batch, {})
    // }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchForm extends MeasurementValuesForm<Batch>
  implements OnInit, OnDestroy {

  $taxonGroups: Observable<ReferentialRef[]>;
  $taxonNames: Observable<ReferentialRef[]>;

  @Input() usageMode: UsageMode;

  @Input() showTaxonGroup = true;

  @Input() showTaxonName = true;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected cd: ChangeDetectorRef,
    protected validatorService: ValidatorService,
    protected referentialRefService: ReferentialRefService
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, cd, validatorService.getRowValidator());

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL;

  }

  ngOnInit() {
    super.ngOnInit();

    // Taxon group combo
    this.$taxonGroups = this.form.get('taxonGroup').valueChanges
      .pipe(
        debounceTime(250),
        switchMap((value) => this.referentialRefService.suggest(value, {
          entityName: 'TaxonGroup',
          levelId: TaxonGroupIds.FAO,
          searchAttribute: 'label'
        })),
        // Remember implicit value
        tap(res => this.updateImplicitValue('taxonGroup', res))
      );

    // Taxon name combo
    this.$taxonNames = this.form.get('taxonName').valueChanges
      .pipe(
        debounceTime(250),
        switchMap((value) => this.referentialRefService.suggest(value, {
          entityName: 'TaxonName',
          levelId: TaxonomicLevelIds.SPECIES,
          searchAttribute: 'label'
        })),
        // Remember implicit value
        tap(res => this.updateImplicitValue('taxonName', res))
      );
  }

  public setValue(value: Batch) {

    console.log("TODO: form->setValue()", value);

    // Send value for form
    super.setValue(value);

    this.enable();
  }

  referentialToString = referentialToString;
}
