import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChildren
} from "@angular/core";
import {Batch} from "../../services/model/batch.model";
import {MeasurementValuesForm} from "../../measurement/measurement-values.form.class";
import {DateAdapter} from "@angular/material/core";
import {Moment} from "moment";
import {MeasurementsValidatorService} from "../../services/validator/measurement.validator";
import {AbstractControl, FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ProgramService} from "../../../referential/services/program.service";
import {ReferentialRefService} from "../../../referential/services/referential-ref.service";
import {SubBatchValidatorService} from "../../services/validator/sub-batch.validator";
import {EntityUtils} from "../../../core/services/model/entity.model";
import {ReferentialUtils} from "../../../core/services/model/referential.model";
import {UsageMode} from "../../../core/services/model/settings.model";
import {debounceTime, delay, distinctUntilChanged, filter, mergeMap, skip, startWith, tap} from "rxjs/operators";
import {AcquisitionLevelCodes, PmfmIds, QualitativeLabels} from "../../../referential/services/model/model.enum";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {BehaviorSubject, combineLatest} from "rxjs";
import {
  getPropertyByPath,
  isNil,
  isNilOrBlank,
  isNotNil,
  isNotNilOrBlank,
  startsWithUpperCase,
  toBoolean
} from "../../../shared/functions";
import {LocalSettingsService} from "../../../core/services/local-settings.service";
import {MeasurementValuesUtils} from "../../services/model/measurement.model";
import {PlatformService} from "../../../core/services/platform.service";
import {AppFormUtils} from "../../../core/core.module";
import {PmfmFormField} from "../../../referential/pmfm/pmfm.form-field.component";
import {focusNextInput, focusPreviousInput, GetFocusableInputOptions} from "../../../shared/inputs";
import {SharedValidators} from "../../../shared/validator/validators";
import {TaxonNameRef} from "../../../referential/services/model/taxon.model";
import {SubBatch} from "../../services/model/subbatch.model";
import {BatchGroup} from "../../services/model/batch-group.model";


@Component({
  selector: 'app-sub-batch-form',
  templateUrl: 'sub-batch.form.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubBatchForm extends MeasurementValuesForm<SubBatch>
  implements OnInit, OnDestroy {

  protected _qvPmfm: PmfmStrategy;
  protected _availableParents: BatchGroup[] = [];
  protected _parentAttributes: string[];
  protected _showTaxonName: boolean;

  protected _disableByDefaultControls: AbstractControl[] = [];

  mobile: boolean;
  enableIndividualCountControl: AbstractControl;
  freezeTaxonNameControl: AbstractControl;
  freezeQvPmfmControl: AbstractControl;
  $taxonNames = new BehaviorSubject<TaxonNameRef[]>(undefined);
  selectedTaxonNameIndex = -1;


  @Input() tabindex: number;

  @Input() usageMode: UsageMode;

  @Input() showParentGroup = true;

  @Input() set showTaxonName(show) {
    this._showTaxonName = show;
    const taxonNameControl = this.form && this.form.get('taxonName');
    if (taxonNameControl) {
      if (show) {
        taxonNameControl.setValidators(Validators.compose([SharedValidators.entity, Validators.required]));
      }
      else {
        taxonNameControl.setValidators([]);
      }
    }
  }

  get showTaxonName(): boolean {
    return this._showTaxonName;
  }

  get taxonNames(): TaxonNameRef[] {
    return this.$taxonNames.getValue();
  }

  @Input() showIndividualCount = true;

  @Input() displayParentPmfm: PmfmStrategy;

  @Input() onNewParentClick: () => Promise<BatchGroup | undefined>;

  @Input() showError = true;

  @Input() showSubmitButton = true;

  @Input() isNew: boolean;

  @Input() set qvPmfm(value: PmfmStrategy) {
    this._qvPmfm = value;
    // If already loaded, re apply pmfms, to be able to execute mapPmfms
    if (value && !this.loadingPmfms) {
      this.setPmfms(this.$pmfms);
    }
  };

  get qvPmfm(): PmfmStrategy {
    return this._qvPmfm;
  };

  @Input() set availableParents(parents: BatchGroup[]) {
    if (this._availableParents === parents) return; // skip
    this._availableParents = parents;
  }

  get availableParents(): BatchGroup[] {
    return this._availableParents;
  }

  get enableIndividualCount(): boolean {
    return this.enableIndividualCountControl.value;
  }

  get freezeTaxonName(): boolean {
    return this.freezeTaxonNameControl.value;
  }

  @Input() set freezeTaxonName(value: boolean) {
    this.freezeTaxonNameControl.setValue(value);
    if (!value) {
      this.form.get('taxonName').reset(null);
    }
  }

  get freezeQvPmfm(): boolean {
    return this.freezeQvPmfmControl.value;
  }

  @Input() set freezeQvPmfm(value: boolean) {
    this.freezeQvPmfmControl.setValue(value);
    if (!value) {
      this.form.get('measurements.' + this.qvPmfm.pmfmId).reset(null);
    }
  }

  get parentGroup(): any {
    return this.form.controls.parentGroup.value;
  }

  @ViewChildren(PmfmFormField) measurementFormFields: QueryList<PmfmFormField>;
  @ViewChildren('inputField') inputFields: QueryList<ElementRef>;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected validatorService: SubBatchValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService,
    protected platform: PlatformService,
    protected zone: NgZone,
    protected cd: ChangeDetectorRef
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd,
      validatorService.getFormGroup(null, {
        rankOrderRequired: false, // Avoid to have form.invalid, in Burst mode
      }),
      {
        mapPmfms: (pmfms) => this.mapPmfms(pmfms),
        onUpdateControls: (form) => this.onUpdateControls(form)
      });

    // Remove required label/rankOrder
    this.form.controls.label.setValidators(null);
    this.form.controls.rankOrder.setValidators(null);

    this.mobile = platform.mobile;
    this._enable = false;

    // Set default values
    this._acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH_INDIVIDUAL;

    // Control for indiv. count enable
    this.enableIndividualCountControl = this.formBuilder.control(false, Validators.required);
    this.enableIndividualCountControl.setValue(false, {emitEvent: false});

    // Freeze QV value control
    this.freezeQvPmfmControl = this.formBuilder.control(true, Validators.required);
    this.freezeQvPmfmControl.setValue(true, {emitEvent: false});

    // Freeze taxon name value control (default: true if NOT mobile)
    this.freezeTaxonNameControl = this.formBuilder.control(!this.mobile, Validators.required);
    this.freezeTaxonNameControl.setValue(!this.mobile, {emitEvent: false});

    // For DEV only
    //this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;
    this.isNew = toBoolean(this.isNew, false);

    // Get display attributes for parent
    this._parentAttributes = this.settings.getFieldDisplayAttributes('taxonGroup').map(attr => 'taxonGroup.' + attr)
      .concat(!this.showTaxonName ? this.settings.getFieldDisplayAttributes('taxonName').map(attr => 'taxonName.' + attr) : []);

    // Parent combo
    const parentControl = this.form.get('parentGroup');
    this.registerAutocompleteField('parentGroup', {
      suggestFn: (value: any, options?: any) => this.suggestParents(value, options),
      attributes: ['rankOrder'].concat(this._parentAttributes),
      showAllOnFocus: true
    });

    // Taxon name
    const taxonNameControl = this.form.get('taxonName');
    if (this.showTaxonName) {
      // Add required validator on TaxonName
      taxonNameControl.setValidators(Validators.compose([SharedValidators.entity, Validators.required]));
    }
    this.registerAutocompleteField('taxonName', {
      items: this.$taxonNames,
      mobile: this.mobile
    });


    // Mobile
    if (this.mobile) {

      this.ready().then(() => {
        let currentParenLabel;

        // Compute taxon names when parent has changed
        parentControl.valueChanges
          // Compute taxon names when parent has changed
          .pipe(
            filter(parent => isNotNilOrBlank(parent) && isNotNilOrBlank(parent.label) && currentParenLabel !== parent.label),
            tap(parent => currentParenLabel = parent.label),
            mergeMap((_) => this.suggestTaxonNames())
          )
          .subscribe(items => this.$taxonNames.next(items));

        // Update taxonName when need
        let lastTaxonName: TaxonNameRef;
        this.registerSubscription(
          combineLatest([
            this.$taxonNames,
            taxonNameControl.valueChanges.pipe(
              tap(v => lastTaxonName = v)
            )
          ])
            .pipe(
              filter(([items, value]) => isNotNil(items))
            )
            .subscribe(([items, value]) => {
              let newTaxonName: TaxonNameRef;
              let index = -1;
              // Compute index in list, and get value
              if (items && items.length === 1) {
                index = 0;
              }
              else if (ReferentialUtils.isNotEmpty(lastTaxonName)) {
                index = items.findIndex(v => TaxonNameRef.equalsOrSameReferenceTaxon(v, lastTaxonName));
              }
              newTaxonName = (index !== -1) ? items[index] : null;

              // Apply to form, if need
              if (!ReferentialUtils.equals(lastTaxonName, newTaxonName)) {
                taxonNameControl.setValue(newTaxonName, {emitEvent: false});
                lastTaxonName = newTaxonName;
                this.markAsDirty();
              }

              // Apply to button index, if need
              if (this.selectedTaxonNameIndex !== index) {
                this.selectedTaxonNameIndex = index;
                this.markForCheck();
              }
            }));
      });
    }

    // Desktop
    else {

      // Reset taxon name combo when parent changed
      this.registerSubscription(
        parentControl.valueChanges
          .pipe(
            // Warn: skip the first trigger (ignore set value)
            skip(1),
            debounceTime(250),
            // Ignore changes if parent is not an entity (WARN: we use 'label' because id can be null, when not saved yet)
            filter(parent => this.form.enabled && EntityUtils.isNotEmpty(parent, 'label')),
            distinctUntilChanged(Batch.equals),
            mergeMap(() => this.suggestTaxonNames())
          )
          .subscribe((taxonNames) => {
            this.$taxonNames.next(taxonNames);
            if (taxonNames.length === 1) {
              taxonNameControl.patchValue(taxonNames[0], {emitEVent: false});
            }
            else {
              taxonNameControl.reset(null, {emitEVent: false});
            }
          }));
    }

    this.registerSubscription(
      this.enableIndividualCountControl.valueChanges
        .pipe(
          startWith<any, any>(this.enableIndividualCountControl.value)
        )
        .subscribe((enable) => {
          const individualCountControl = this.form.get('individualCount');
          if (enable) {
            individualCountControl.enable();
            individualCountControl.setValidators(Validators.compose([Validators.required, Validators.min(0)]));
          } else {
            individualCountControl.disable();
            individualCountControl.setValue(null);
          }
        }));

    this.ngInitExtension();
  }

  async doNewParentClick(event: UIEvent) {
    if (!this.onNewParentClick) return; // No callback: skip
    const res = await this.onNewParentClick();

    if (res && res instanceof Batch) {
      this.form.get('parent').setValue(res);
    }
  }

  setValue(data: SubBatch, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; linkToParent?: boolean; }) {
    // Replace parent with value from availableParents
    if (!opts || opts.linkToParent !== false) {
      this.linkToParentGroup(data);
    }

    // Reset taxon name button index
    if (this.mobile && data && data.taxonName && isNotNil(data.taxonName.id)) {
      this.selectedTaxonNameIndex = (this.$taxonNames.getValue() || []).findIndex(tn => tn.id === data.taxonName.id);
    }
    else {
      this.selectedTaxonNameIndex = -1;
    }

    // Inherited method
    super.setValue(data, {...opts, linkToParent: false /* avoid to be relink, if loop to setValue() */ });
  }

  reset(data?: SubBatch, opts?: {emitEvent?: boolean; onlySelf?: boolean; normalizeEntityToForm?: boolean; linkToParent?: boolean; }) {
    // Replace parent with value from availableParents
    if (!opts || opts.linkToParent !== false) {
      this.linkToParentGroup(data);
    }

    // Reset taxon name button index
    if (this.mobile && data && data.taxonName && isNotNil(data.taxonName.id)) {
      this.selectedTaxonNameIndex = (this.$taxonNames.getValue() || []).findIndex(tn => tn.id === data.taxonName.id);
    }
    else {
      this.selectedTaxonNameIndex = -1;
    }

    // Inherited method
    super.reset(data, {...opts, linkToParent: false /* avoid to be relink, if loop to setValue() */ });
  }

  enable(opts?: { onlySelf?: boolean; emitEvent?: boolean }): void {
    super.enable(opts);

    if (!this.enableIndividualCount) {
      this.form.get('individualCount').disable(opts);
    }

    // Other field to disable by default (e.g. discard reason, in SUMARiS program)
    this._disableByDefaultControls.forEach(c => c.disable(opts));
  }

  protected restoreFormStatus(opts?: { emitEvent?: boolean; onlySelf?: boolean }) {
    super.restoreFormStatus(opts);

    if (this._enable) {
      // Other field to disable by default (e.g. discard reason, in SUMARiS program)
      this._disableByDefaultControls.forEach(c => c.disable(opts));
    }
  }

  onTaxonNameButtonClick(event: UIEvent|undefined, value: TaxonNameRef, minTabindex: number) {
    this.form.get('taxonName').setValue(value);
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.focusNextInput(null, {minTabindex});
  }

  focusFirstEmptyInput(event?: UIEvent): boolean {
    return focusNextInput(event, this.inputFields, {
      excludeEmptyInput: true,
      minTabindex: -1,
      debug: this.debug
    });
  }

  focusNextInput(event: UIEvent, opts?: Partial<GetFocusableInputOptions>): boolean {
    return focusNextInput(event, this.inputFields, {debug: this.debug, ...opts});
  }

  focusPreviousInput(event: UIEvent, opts?: Partial<GetFocusableInputOptions>): boolean {
    return focusPreviousInput(event, this.inputFields, {debug: this.debug, ...opts});
  }

  focusNextInputOrSubmit(event: UIEvent, isLastPmfm: boolean) {

    if (isLastPmfm) {
      if (this.enableIndividualCount) {
        // Focus to last (=individual count input)
        this.inputFields.last.nativeElement.focus();
        return true;
      }

      this.doSubmit(event);
      return true;
    }

    return this.focusNextInput(event);
  }

  selectInputContent = AppFormUtils.selectInputContent;
  filterNumberInput = AppFormUtils.filterNumberInput;

  /* -- protected method -- */

  protected async ngInitExtension() {

    await this.ready();

    const discardOrLandingControl = this.form.get('measurementValues.' + PmfmIds.DISCARD_OR_LANDING);
    const discardReasonControl = this.form.get('measurementValues.' + PmfmIds.DISCARD_REASON);

    // Manage DISCARD_REASON validator
    if (discardOrLandingControl && discardReasonControl) {
      // Always disable by default, while discard/Landing not set
      this._disableByDefaultControls.push(discardReasonControl);

      this.registerSubscription(discardOrLandingControl.valueChanges
        .pipe(
          // IMPORTANT: add a delay, to make sure to be executed AFTER the form.enable()
          delay(200)
        )
        .subscribe((value) => {

          if (ReferentialUtils.isNotEmpty(value) && value.label === QualitativeLabels.DISCARD_OR_LANDING.DISCARD) {
            if (this.form.enabled) {
              discardReasonControl.enable();
            }
            discardReasonControl.setValidators(Validators.required);
            discardReasonControl.updateValueAndValidity({onlySelf: true});
          } else {
            discardReasonControl.setValue(null);
            discardReasonControl.setValidators([]);
            discardReasonControl.disable();
          }
        }));
    }
  }

  protected async suggestParents(value: any, options?: any): Promise<Batch[]> {
    // Has select a valid parent: return the parent
    if (EntityUtils.isNotEmpty(value, 'label')) return [value];
    value = (typeof value === "string" && value !== "*") && value || undefined;
    if (isNilOrBlank(value)) return this._availableParents; // All
    const ucValueParts = value.trim().toUpperCase().split(" ", 1);
    if (this.debug) console.debug(`[sub-batch-form] Searching parent {${value || '*'}}...`);
    // Search on attributes
    return this._availableParents.filter(parent => ucValueParts
        .filter(valuePart => this._parentAttributes
          .findIndex(attr => startsWithUpperCase(getPropertyByPath(parent, attr), valuePart.trim())) !== -1
        ).length === ucValueParts.length
    );
  }

  protected suggestTaxonNames(value?: any, options?: any): Promise<TaxonNameRef[]> {
    const parentGroup = this.parentGroup;
    if (isNil(parentGroup)) return Promise.resolve([]);
    //if (this.debug)
      console.debug(`[sub-batch-form] Searching taxon name {${value || '*'}}...`);
    return this.programService.suggestTaxonNames(value,
      {
        program: this.program,
        searchAttribute: options && options.searchAttribute,
        taxonGroupId: parentGroup && parentGroup.taxonGroup && parentGroup.taxonGroup.id || undefined
      });
  }

  protected mapPmfms(pmfms: PmfmStrategy[]) {

    if (this._qvPmfm) {
      // Remove QV pmfms
      const index = pmfms.findIndex(pmfm => pmfm.pmfmId === this._qvPmfm.pmfmId);
      if (index !== -1) {
        const qvPmfm = this._qvPmfm.clone();
        qvPmfm.hidden = true;
        qvPmfm.required = true;
        pmfms[index] = qvPmfm;
      }
    }

    return pmfms;
  }

  protected onUpdateControls(form: FormGroup) {
    if (this._qvPmfm) {
      const measFormGroup = form.get('measurementValues') as FormGroup;
      const qvControl = measFormGroup.get(this._qvPmfm.pmfmId.toString());

      // Make sure QV is required
      qvControl.setValidators(Validators.required);
    }
  }

  protected getValue(): SubBatch {
    if (!this.form.dirty) return this.data;

    const json = this.form.value;

    // Read the individual count (if has been disable)
    if (!this.enableIndividualCount) {
      json.individualCount = this.form.get('individualCount').value || 1;
    }

    const pmfmForm = this.form.get('measurementValues');

    // Adapt measurement values for entity
    if (pmfmForm) {
      json.measurementValues = Object.assign({},
        this.data.measurementValues || {}, // Keep additionnal PMFM values
        MeasurementValuesUtils.normalizeValuesToModel(pmfmForm.value, this.$pmfms.getValue() || []));
    } else {
      json.measurementValues = {};
    }

    this.data.fromObject(json);

    return this.data;
  }


  protected linkToParentGroup(data?: SubBatch) {
    if (!data) return;
    // Find the parent
    const parentGroup = data.parentGroup;
    if (!parentGroup) return; // no parent = nothing to link

    data.parentGroup = this._availableParents.find(p => Batch.equals(p, parentGroup));

    // Parent not found
    if (!data.parentGroup) {
      // Force to allow parent selection
      this.showParentGroup = this.showParentGroup || true;
    }

    // Get the parent of the parent (e.g. if parent is a sample batch)
    else if (data.parent && !data.parent.hasTaxonNameOrGroup && data.parent.parent && data.parent.parent.hasTaxonNameOrGroup) {
      data.parentGroup = BatchGroup.fromBatch(data.parent.parent);
    }
  }

}
