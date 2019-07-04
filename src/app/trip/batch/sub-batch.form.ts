import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChildren
} from "@angular/core";
import {ValidatorService} from "angular4-material-table";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {AbstractControl, FormBuilder, Validators} from "@angular/forms";
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
import {debounceTime, filter, map, startWith, switchMap, tap} from "rxjs/operators";
import {getPmfmName, isNil, isNotNil, PmfmStrategy, TaxonomicLevelIds} from "../../referential/services/model";
import {merge, Observable} from "rxjs";
import {isNilOrBlank, startsWithUpperCase} from "../../shared/functions";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {BatchesContext} from "./batches-context.class";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {PlatformService} from "../../core/services/platform.service";
import {AppFormUtils} from "../../core/core.module";
import {MeasurementFormField} from "../measurement/measurement.form-field.component";
import {MeasurementQVFormField} from "../measurement/measurement-qv.form-field.component";

@Component({
  selector: 'app-sub-batch-form',
  templateUrl: 'sub-batch.form.html',
  providers: [
    {provide: ValidatorService, useClass: SubBatchValidatorService}
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchForm extends MeasurementValuesForm<Batch>
  implements OnInit, OnDestroy {

  $taxonNames: Observable<ReferentialRef[]>;
  $filteredParents: Observable<Batch[]>;

  _availableParents: Batch[] = [];
  onShowParentDropdown = new EventEmitter<UIEvent>(true);
  mobile: boolean;
  enableIndividualCountControl: AbstractControl;

  @Input() context: BatchesContext;

  @Input() tabindex: number;

  @Input() usageMode: UsageMode;

  @Input() showParent = true;

  @Input() showTaxonName = true;

  @Input() displayParentPmfm: PmfmStrategy;

  @Input() onNewParentClick: () => Promise<Batch | undefined>;

  @Input() showError = true;

  @Input() set availableParents(parents: Batch[]) {
    if (this._availableParents === parents) return; // skip
    this._availableParents = parents;
  }

  get availableParents(): Batch[] {
    return this._availableParents;
  }

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  get enableIndividualCount(): boolean {
    return this.enableIndividualCountControl.value;
  }

  @ViewChildren(MeasurementFormField) measurementFormFields: QueryList<MeasurementFormField>;
  @ViewChildren('matInput') matInputs: QueryList<ElementRef>;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected cd: ChangeDetectorRef,
    protected validatorService: ValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService,
    protected platform: PlatformService
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, cd, validatorService.getRowValidator());

    this.mobile = platform.mobile;

    // Set default values
    this._acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL;

    // Control for indiv. count enable
    this.enableIndividualCountControl = this.formBuilder.control(this.mobile, Validators.required);
    this.enableIndividualCountControl.setValue(false, {emitEvet: false});

    // For DEV only
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;

    // Parent combo
    this.$filteredParents = merge(
      this.onShowParentDropdown
        .pipe(
          filter(event => !event.defaultPrevented),
          map((_) => this.form.get('parent').value)
        ),
      this.form.get('parent').valueChanges
        .pipe(debounceTime(250))
    )
      .pipe(
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

    this.enableIndividualCountControl.valueChanges
      .pipe(startWith(() => this.enableIndividualCountControl.value))
      .subscribe((enable) => {
        if (enable) {
          this.form.get('individualCount').enable();
          this.form.get('individualCount').setValidators(Validators.compose([Validators.required, Validators.min(0)]));
        } else {
          this.form.get('individualCount').disable();
          this.form.get('individualCount').setValue(null);
        }
      });

    this.updateTabIndex();
  }

  async doNewParentClick(event: UIEvent) {
    if (!this.onNewParentClick) return;
    const res = await this.onNewParentClick();

    if (res && res instanceof Batch) {
      this.form.get('parent').setValue(res);
    }
  }

  doSubmitIfEnter(event: KeyboardEvent): boolean{
    if (event.keyCode == 13) {
      this.doSubmit(event);
      return false;
    }
    return true;
  }

  focusFirstEmpty(event?: UIEvent) {
    // Focus to first input
    this.matInputs
      .map((input) => {
        if (input instanceof MeasurementFormField) {
          return input as MeasurementFormField;
        } else if (input.nativeElement as HTMLInputElement) {
          return input.nativeElement;
        }
        return undefined;
      })
      .filter(isNotNil)
      .find(input => {
        if (isNilOrBlank(input.value)) {
          if (event) event.preventDefault();
          input.focus();
          return true;
        }
        return false;
      });
  }

  doSubmitLastMeasurementField(event: KeyboardEvent) {
    if (!this.enableIndividualCount) {
      this.doSubmit(event);
    }
    else {
      // Focus to last (=individual count input)
      this.matInputs.last.nativeElement.focus();
    }
  }

  /* -- protected methods -- */


  setValue(data: Batch) {
    // Replace parent with value of availableParents
    this.linkToParent(data);

    // Inherited method
    super.setValue(data);
  }

  protected getValue(): Batch {

    const json = this.form.value;
    const pmfmForm = this.form.get('measurementValues');

    // Adapt measurement values for entity
    if (pmfmForm) {
      json.measurementValues = Object.assign({},
        this.data.measurementValues || {}, // Keep additionnal PMFM values
        MeasurementValuesUtils.toEntityValues(pmfmForm.value, this.$pmfms.getValue() || []));
    } else {
      json.measurementValues = {};
    }

    // Replace by the right parent
    const parent = this.form.get('parent').value;
    if (parent) {
      console.log("TODO Update parent to the right QV children");
    }

    this.data.fromObject(json);

    return this.data;
  }


  protected linkToParent(value: Batch) {
    // Find the parent
    const entityParent = value.parent || (value.parentId && {id: value.parentId});
    if (!entityParent) return; // no parent = nothing to link

    let formParent = this._availableParents.find(p => (p.label === value.parent.label) || (p.id === value.parentId));

    if (!formParent.hasTaxonNameOrGroup && formParent.parent && formParent.parent.hasTaxonNameOrGroup) {
      formParent = formParent.parent;
    }

    if (formParent !== entityParent) {
      this.form.get('parent').patchValue(formParent, {emitEvent: !this.loading});
      if (!this.loading) this.markForCheck();
    }
  }


  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean }): void {
    super.enable(opts);

    if (!this.enableIndividualCount) {
      this.form.get('individualCount').disable(opts);
    }
  }

  parentToString(batch: Batch) {
    // TODO: use options, to enable/disable code
    return BatchUtils.parentToString(batch);
  }

  referentialToString = referentialToString;
  getPmfmName = getPmfmName;
  selectInputContent = AppFormUtils.selectInputContent;
  filterNumberInput = AppFormUtils.filterNumberInput;


  /* -- protected method -- */

  protected updateTabIndex() {
    if (this.tabindex && this.tabindex !== -1) {
      setTimeout(async () => {
        // Make sure form is ready
        await this.onReady();


        let tabindex = this.tabindex+1;
        this.matInputs.forEach(input => {
          if (input instanceof MeasurementFormField || input instanceof MeasurementQVFormField) {
            input.tabindex = tabindex;
          }
          else if (input.nativeElement instanceof HTMLInputElement){
            input.nativeElement.setAttribute('tabindex', tabindex.toString());
          }
          else {
            console.warn("Could not set tabindex on element: ", input);
          }
          tabindex++;
        });
        this.markForCheck();
      });
    }
  }

}
