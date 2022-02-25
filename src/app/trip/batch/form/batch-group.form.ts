import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, QueryList, ViewChildren } from '@angular/core';
import { Batch, BatchUtils } from '../../services/model/batch.model';
import { AbstractControl, FormBuilder, FormControl } from '@angular/forms';
import { ReferentialRefService } from '@app/referential/services/referential-ref.service';
import { AcquisitionLevelCodes } from '@app/referential/services/model/model.enum';
import { AppForm, AppFormUtils, AppTable, fadeInAnimation, InputElement, isNotNil, LocalSettingsService, PlatformService, ReferentialUtils, toBoolean } from '@sumaris-net/ngx-components';
import { BatchGroupValidatorService } from '../../services/validator/batch-group.validator';
import { BehaviorSubject } from 'rxjs';
import { BatchForm } from './batch.form';
import { filter } from 'rxjs/operators';
import { BatchGroup } from '../../services/model/batch-group.model';
import { MeasurementsValidatorService } from '../../services/validator/measurement.validator';
import { IPmfm, PmfmUtils } from '@app/referential/services/model/pmfm.model';
import { ProgramRefService } from '@app/referential/services/program-ref.service';
import { createAnimation } from '@ionic/core';
import { Animation } from '@ionic/angular';

@Component({
  selector: 'app-batch-group-form',
  templateUrl: 'batch-group.form.html',
  styleUrls: ['batch-group.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [fadeInAnimation]
})
export class BatchGroupForm extends BatchForm<BatchGroup> {

  private _childrenAnimation: Animation;

  $childrenPmfms = new BehaviorSubject<IPmfm[]>(undefined);
  hasSubBatchesControl: AbstractControl;

  @Input() qvPmfm: IPmfm;
  @Input() taxonGroupsNoWeight: string[];
  @Input() showChildrenWeight = true;
  @Input() showChildrenIndividualCount = false;
  @Input() showChildrenSamplingBatch = true;
  @Input() allowSubBatches = true;
  @Input() defaultHasSubBatches = false;
  @Input() animated = false; // Animate children card

  @ViewChildren('firstInput') firstInputFields !: QueryList<InputElement>;
  @ViewChildren('childForm') childrenList !: QueryList<BatchForm>;

  get invalid(): boolean {
    return this.form.invalid || this.hasSubBatchesControl.invalid ||
      ((this.childrenList || []).find(child => child.invalid) && true) || false;
  }

  get valid(): boolean {
    // Important: Should be not invalid AND not pending, so use '!valid' (and NOT 'invalid')
    return this.form.valid && this.hasSubBatchesControl.valid &&
      (!this.childrenList || !this.childrenList.find(child => !child.valid)) || false;
  }

  get pending(): boolean {
    return this.form.pending || this.hasSubBatchesControl.pending ||
      (this.childrenList && this.childrenList.find(child => child.pending) && true) || false;
  }

  get loading(): boolean {
    return super.loading || (this.childrenList && this.childrenList.find(child => child.loading) && true) || false;
  }

  get dirty(): boolean {
    return this.form.dirty || this.hasSubBatchesControl.dirty ||
      (this.childrenList && this.childrenList.find(child => child.dirty) && true) || false;
  }

  markAllAsTouched(opts?: { onlySelf?: boolean; emitEvent?: boolean; }) {
    super.markAllAsTouched(opts);
    this.childrenList?.forEach(f => f.markAllAsTouched(opts));
    this.hasSubBatchesControl.markAsTouched(opts);
  }

  markAsPristine(opts?: { onlySelf?: boolean; }) {
    super.markAsPristine(opts);
    (this.childrenList || []).forEach(child => child.markAsPristine(opts));
    this.hasSubBatchesControl.markAsPristine(opts);
  }

  markAsUntouched(opts?: { onlySelf?: boolean; }) {
    super.markAsUntouched(opts);
    (this.childrenList || []).forEach(child => child.markAsUntouched(opts));
    this.hasSubBatchesControl.markAsUntouched(opts);
  }

  markAsDirty(opts?: {
    onlySelf?: boolean;
  }) {
    super.markAsDirty(opts);
    (this.childrenList && []).forEach(child => child.markAsDirty(opts));
    this.hasSubBatchesControl.markAsDirty(opts);
  }

  disable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }) {
    super.disable(opts);
    (this.childrenList || []).forEach(child => child.disable(opts));
    if (this._enable || (opts && opts.emitEvent)) {
      this._enable = false;
      this.markForCheck();
    }
    this.hasSubBatchesControl.disable(opts);
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }) {
    super.enable(opts);
    (this.childrenList || []).forEach(child => child.enable(opts));
    if (!this._enable || (opts && opts.emitEvent)) {
      this._enable = true;
      this.markForCheck();
    }
  }

  get hasSubBatches(): boolean {
    return this.hasSubBatchesControl.value === true;
  }

  @Input()
  set hasSubBatches(value: boolean) {
    this.hasSubBatchesControl.setValue(value);
    if (!value && this.hasSubBatchesControl.disabled && this.enabled) {
      this.hasSubBatchesControl.enable();
    }
  }


  constructor(
    injector: Injector,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programRefService: ProgramRefService,
    protected platform: PlatformService,
    protected validatorService: BatchGroupValidatorService,
    protected referentialRefService: ReferentialRefService
  ) {
    super(injector,
      measurementValidatorService,
      formBuilder,
      programRefService,
      validatorService,
      referentialRefService);

    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;
    this.hasSubBatchesControl = new FormControl(false);
    this.showSamplingBatch = false;

    // DEBUG
    //this.debug = !environment.production;
  }

  protected get logPrefix(): string {
    return '[batch-group-form]';
  }

  ngOnInit() {
    super.ngOnInit();

    this.defaultHasSubBatches = toBoolean(this.defaultHasSubBatches, false);

    // Set isSampling on each child forms, when has indiv. measure changed
    this.registerSubscription(
      this.hasSubBatchesControl.valueChanges
        .pipe(filter(() => !this.applyingValue && !this.loading))
        .subscribe(hasSubBatches => {
            hasSubBatches = hasSubBatches && !this.showIndividualCount;
            (this.childrenList || []).forEach((childForm, index) => {
              childForm.setIsSampling(hasSubBatches, {emitEvent: true} /*Important, to force async validator*/);
            });
          }));

    // Listen form changes
    this.registerSubscription(
      this.form.valueChanges
        .pipe(filter(() => !this.applyingValue && !this.loading))
        .subscribe((batch) => this.computeShowTotalIndividualCount(batch)));


    // Create animation to display children forms
    if (this.animated) {
      this._childrenAnimation = createAnimation()
        .duration(300)
        .direction('normal')
        .iterations(1)
        .beforeStyles({
          'transition-timing-function': 'ease-in',
          transform: 'scale(0.5)',
          opacity: '0'
        })
        .keyframes([
          { offset: 1, transform: 'scale(1)', opacity: '1' }
        ]);
    }
  }

  focusFirstInput() {
    const element = this.firstInputFields.first;
    if (element) element.focus();
  }

  logFormErrors(logPrefix: string) {
    logPrefix = logPrefix || '';
    AppFormUtils.logFormErrors(this.form, logPrefix);
    if (this.childrenList) this.childrenList.forEach((childForm, index) => {
      AppFormUtils.logFormErrors(childForm.form, logPrefix, `children#${index}`);
    });
  }

  async ready(): Promise<void> {
    await super.ready();

  }

  /*onInitSubForm(form: AppForm<any>) {
    if (!this.children.includes(form)) {
      this.addChildForm(form);
    }
    // Mark table as ready, if main component is ready
    if (this._$ready.value) {
      table.markAsReady();
    }
    // Mark table as loaded, if main component is loaded
    if (!this.loading) {
      table.markAsLoaded();
    }
  }*/

  /* -- protected methods -- */

  protected mapPmfms(pmfms: IPmfm[]) {

    if (this.debug) console.debug('[batch-group-form] mapPmfm()...');

    this.qvPmfm = this.qvPmfm || PmfmUtils.getFirstQualitativePmfm(pmfms);
    if (this.qvPmfm) {

      // Create a copy, to keep original pmfm unchanged
      this.qvPmfm = this.qvPmfm.clone();

      // Hide for children form, and change it as required
      this.qvPmfm.hidden = true;
      this.qvPmfm.required = true;

      // Replace in the list
      this.$childrenPmfms.next(pmfms.map(p => p.id === this.qvPmfm.id ? this.qvPmfm : p));

      // Do not display PMFM in the root batch
      pmfms = [];
    }

    return super.mapPmfms(pmfms);
  }

  protected async updateView(data: BatchGroup, opts?: { emitEvent?: boolean; onlySelf?: boolean; }) {

    if (this.debug) console.debug('[batch-group-form] updateView() with value:', data);
    let hasSubBatches = data.observedIndividualCount > 0 || this.defaultHasSubBatches || false;

    if (!this.qvPmfm) {
      await super.updateView(data);

      // Should have sub batches, when sampling batch exists
      hasSubBatches = hasSubBatches || isNotNil(BatchUtils.getSamplingChild(data));
    } else {

      // Prepare data array, for each qualitative values
      data.children = this.qvPmfm.qualitativeValues.map((qv, index) => {

        // Find existing child, or create a new one
        // tslint:disable-next-line:triple-equals
        const child = (data.children || []).find(c => +(c.measurementValues[this.qvPmfm.id]) == qv.id)
          || new Batch();

        // Make sure label and rankOrder are correct
        child.label = `${data.label}.${qv.label}`;
        child.measurementValues[this.qvPmfm.id] = qv;
        child.rankOrder = index + 1;

        // Should have sub batches, when sampling batch exists
        hasSubBatches = hasSubBatches || isNotNil(BatchUtils.getSamplingChild(child));

        // Make sure to create a sampling batch, if has sub bacthes
        if (hasSubBatches) {
          BatchUtils.getOrCreateSamplingChild(child);
        }

        return child;
      });

      // Set value (batch group)
      await super.updateView(data, opts);

      // Then set value of each child form
      this.cd.detectChanges();

      await Promise.all(
        this.childrenList.map((childForm, index) => {

          const childBatch = data.children[index] || new Batch();
          childForm.showWeight = this.showChildrenWeight;
          childForm.requiredWeight = this.showChildrenWeight && hasSubBatches;
          childForm.requiredSampleWeight = this.showChildrenWeight && hasSubBatches;
          childForm.requiredIndividualCount = !this.showChildrenWeight && hasSubBatches;
          childForm.setIsSampling(hasSubBatches, {emitEvent: true});
          if (this.enabled) {
            childForm.enable();
          } else {
            childForm.disable();
          }

          childForm.markAsReady();
          return childForm.setValue(childBatch, {emitEvent: true});
        })
      );

      this.computeShowTotalIndividualCount(data);

      // Play animation
      if (this.animated) {
        await this._childrenAnimation
          .addElement(document.querySelectorAll('ion-card.qv'))
          .play();
      }
    }

    // Apply computed value
    this.hasSubBatchesControl.setValue(hasSubBatches, {emitEvent: false});

    // If there is already some measure
    // Not allow to change 'has measure' field
    if (data.observedIndividualCount > 0) {
      this.hasSubBatchesControl.disable();
    } else if (this.enabled) {
      this.hasSubBatchesControl.enable();
    }
  }

  protected getValue(): BatchGroup {
    const data = super.getValue();
    if (!data) return; // No set yet

    // If has children form
    if (this.qvPmfm) {
      data.children = this.childrenList.map((form, index) => {
        const qv = this.qvPmfm.qualitativeValues[index];
        const child = form.value;
        if (!child) return; // No set yet

        child.rankOrder = index + 1;
        child.label = `${data.label}.${qv.label}`;
        child.measurementValues = child.measurementValues || {};
        child.measurementValues[this.qvPmfm.id.toString()] = '' + qv.id;

        // Special case: when sampling on individual count only (e.g. RJB - Pocheteau)
        const sampleBatch = BatchUtils.getSamplingChild(child);
        if (sampleBatch && !form.showWeight && isNotNil(sampleBatch.individualCount) && isNotNil(child.individualCount)) {
          sampleBatch.samplingRatio = sampleBatch.individualCount / child.individualCount;
          sampleBatch.samplingRatioText = `${sampleBatch.individualCount}/${child.individualCount}`;
        }

        // Other Pmfms
        Object.keys(form.measurementValuesForm.value).filter(key => !child.measurementValues[key]).forEach(key =>{
          child.measurementValues[key] = form.measurementValuesForm.value[key];
        });

        return child;
      });
    }

    if (this.debug) console.debug('[batch-group-form] getValue():', data);

    return data;
  }

  protected computeShowTotalIndividualCount(data?: Batch) {
    data = data || this.data;
    if (this.debug) console.debug('[batch-group-form] computeShowTotalIndividualCount():', data);

    // Generally, individual count are not need, on a root species batch, because filled in sub-batches,
    // but some species (e.g. RJB) can have no weight.
    const showChildrenIndividualCount = data && ReferentialUtils.isNotEmpty(data.taxonGroup) &&
      (this.taxonGroupsNoWeight || []).includes(data.taxonGroup.label);

    if (this.showChildrenIndividualCount !== showChildrenIndividualCount) {
      this.showChildrenIndividualCount = showChildrenIndividualCount;
      this.showSampleIndividualCount = showChildrenIndividualCount;
      this.showChildrenWeight = !showChildrenIndividualCount; // Hide weight
      this.showChildrenSamplingBatch = !showChildrenIndividualCount;
      this.markForCheck();
    }
  }
}
