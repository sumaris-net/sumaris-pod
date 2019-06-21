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
import {
  AcquisitionLevelCodes,
  EntityUtils,
  ReferentialRef,
  referentialToString,
  UsageMode
} from "../../core/services/model";
import {debounceTime, map, switchMap, tap} from "rxjs/operators";
import {isNil, isNotNil, PmfmStrategy, TaxonGroupIds, TaxonomicLevelIds} from "../../referential/services/model";
import {Observable, Subject, merge} from "rxjs";
import {startsWithUpperCase} from "../../shared/functions";
import {LocalSettingsService} from "../../core/services/local-settings.service";

@Component({
  selector: 'app-sub-batch-form',
  templateUrl: 'sub-batch.form.html',
  providers: [
    {provide: ValidatorService, useClass: SubBatchValidatorService},
    LocalSettingsService,
    MeasurementsValidatorService,
    ProgramService,
    ReferentialRefService
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchForm extends MeasurementValuesForm<Batch>
  implements OnInit, OnDestroy {

  $taxonNames: Observable<ReferentialRef[]>;
  $filteredParents: Observable<Batch[]>;

  onShowParentDropdown = new Subject<any>();

  @Input() usageMode: UsageMode;

  @Input() showParent = true;

  @Input() showTaxonName = true;

  @Input() displayParentPmfm = PmfmStrategy;

  @Input() availableParents: Batch[] = [];

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected cd: ChangeDetectorRef,
    protected validatorService: ValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, cd, validatorService.getRowValidator());

    // Set default acquisition level
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL;
    //this.program = 'ADAP-MER';

    //
    this.debug = true;
  }

  ngOnInit() {
    super.ngOnInit();

    // Parent combo
    this.$filteredParents = merge(
      this.onShowParentDropdown,
      this.form.get('parent').valueChanges
    )
      .pipe(
        debounceTime(250),
        map((value) => {
          if (EntityUtils.isNotEmpty(value)) return [value];
          value = (typeof value === "string" && value !== "*") && value || undefined;
          if (this.debug) console.debug(`[sub-batch-table] Searching parent {${value || '*'}}...`);
          if (isNil(value)) return this.availableParents; // All
          const ucValueParts = value.trim().toUpperCase().split(" ", 1);
          // Search on labels (taxonGroup or taxonName)
          return this.availableParents.filter(p =>
            (p.taxonGroup && startsWithUpperCase(p.taxonGroup.label, ucValueParts[0])) ||
            (p.taxonName && startsWithUpperCase(p.taxonName.label, ucValueParts.length === 2 ? ucValueParts[1] : ucValueParts[0]))
          );
        }),
        // Save implicit value
        tap(res => this.updateImplicitValue('parent', res))
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

    //if (isNotNil(this._program) && isNotNil(this._acquisitionLevel)) {
    //  this.refreshPmfms();
    //}
  }

  parentToString(batch: Batch) {
    if (!batch) return null;
    const hasTaxonGroup = EntityUtils.isNotEmpty(batch.taxonGroup);
    const hasTaxonName = EntityUtils.isNotEmpty(batch.taxonName);
    if (hasTaxonName && (!hasTaxonGroup || batch.taxonGroup.label === batch.taxonName.label)) {
      return `${batch.taxonName.label} - ${batch.taxonName.name}`;
    }
    if (hasTaxonName && hasTaxonGroup) {
      return `${batch.taxonGroup.label} / ${batch.taxonName.label} - ${batch.taxonName.name}`;
    }
    if (hasTaxonGroup) {
      return `${batch.taxonGroup.label} - ${batch.taxonGroup.name}`;
    }
    return `#${batch.rankOrder}`;
  }

  referentialToString = referentialToString;

}
