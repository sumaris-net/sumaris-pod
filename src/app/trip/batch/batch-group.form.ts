import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  QueryList,
  ViewChild,
  ViewChildren
} from "@angular/core";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {AbstractControl, FormBuilder, FormControl} from "@angular/forms";
import {ProgramService} from "../../referential/services/program.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {UsageMode} from "../../core/services/model";
import {AcquisitionLevelCodes, isNotNil, PmfmStrategy, PmfmUtils} from "../../referential/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {AppForm, AppFormUtils} from "../../core/core.module";
import {BatchGroupValidatorService} from "../services/batch-groups.validator";
import {BehaviorSubject} from "rxjs";
import {BatchForm} from "./batch.form";
import {filter, switchMap, takeWhile} from "rxjs/operators";
import {PlatformService} from "../../core/services/platform.service";
import {firstNotNilPromise} from "../../shared/observables";
import {toBoolean} from "../../shared/functions";
import {fadeInAnimation} from "../../shared/shared.module";

@Component({
  selector: 'app-batch-group-form',
  templateUrl: 'batch-group.form.html',
  styleUrls: ['batch-group.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [fadeInAnimation]
})
export class BatchGroupForm extends AppForm<Batch> implements OnInit, OnDestroy {

  private _ready = false;

  mobile: boolean;
  loading = true;

  $childrenPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);
  mapPmfmsFn: (pmfms: PmfmStrategy[]) => PmfmStrategy[];
  hasMeasureControl: AbstractControl;

  @Input() debug = false;

  @Input() tabindex: number;

  @Input() usageMode: UsageMode;

  @Input() showTaxonGroup = true;

  @Input() showTaxonName = true;

  @Input() showTotalIndividualCount = true;

  @Input() showIndividualCount = true;

  @Input() showError = true;

  @Input() qvPmfm: PmfmStrategy;

  @Input() acquisitionLevel: string;

  @Input() program: string;

  @Output()
  valueChanges: EventEmitter<any> = new EventEmitter<any>();

  @ViewChild('batchForm', { static: true }) batchForm: BatchForm;
  @ViewChildren('childForm') childrenForms !: QueryList<BatchForm>;

  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  get value(): Batch {
    return this.getValue();
  }

  set value(data: Batch) {
    this.safeSetValue(data);
  }

  get invalid(): boolean {
    return this.batchForm.invalid || this.hasMeasureControl.invalid ||
      ((this.childrenForms || []).find(child => child.invalid) && true) || false;
  }

  get valid(): boolean {
    // Important: Should be not invalid AND not pending, so use '!valid' (and NOT 'invalid')
    return this.batchForm.valid && this.hasMeasureControl.valid &&
      (!this.childrenForms || !this.childrenForms.find(child => !child.valid)) || false;
  }

  get pending(): boolean {
    return this.batchForm.pending || this.hasMeasureControl.pending ||
       (this.childrenForms && this.childrenForms.find(child => child.pending) && true) || false;
  }

  get dirty(): boolean {
    return this.batchForm.dirty || this.hasMeasureControl.dirty ||
      (this.childrenForms && this.childrenForms.find(child => child.dirty) && true) || false;
  }

  markAsTouched(opts?: {onlySelf?: boolean; emitEvent?: boolean; }) {
    this.batchForm.markAsTouched(opts);
    (this.childrenForms || []).forEach(child => child.markAsTouched(opts));
    this.hasMeasureControl.markAsTouched(opts);
  }

  markAsPristine(opts?: {onlySelf?: boolean; }) {
    this.batchForm.markAsPristine(opts);
    (this.childrenForms || []).forEach(child => child.markAsPristine(opts));
    this.hasMeasureControl.markAsPristine(opts);
  }

  markAsDirty(opts?: {
    onlySelf?: boolean;
  }) {
    this.batchForm.markAsDirty(opts);
    (this.childrenForms && []).forEach(child => child.markAsDirty(opts));
    this.hasMeasureControl.markAsDirty(opts);
  }

  disable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }) {
    this.batchForm.disable(opts);
    (this.childrenForms || []).forEach(child => child.disable(opts));
    if (this._enable || (opts && opts.emitEvent)) {
      this._enable = false;
      this.markForCheck();
    }
    this.hasMeasureControl.disable(opts);
  }

  enable(opts?: {
    onlySelf?: boolean;
    emitEvent?: boolean;
  }) {
    this.batchForm.enable(opts);
    (this.childrenForms || []).forEach(child => child.enable(opts));
    if (!this._enable || (opts && opts.emitEvent)) {
      this._enable = true;
      this.markForCheck();
    }
  }

  get hasMeasure(): boolean {
    return this.hasMeasureControl.value === true;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef,
    protected validatorService: BatchGroupValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService
  ) {
    super(dateAdapter, null, settings);
    this.mobile = platform.mobile;
    this.mapPmfmsFn = this.createMapPmfmsFn();

    // Default value
    this.acquisitionLevel = AcquisitionLevelCodes.SORTING_BATCH;
    this.hasMeasureControl = new FormControl(false);

    // for DEV only
    this.debug = !environment.production;
  }

  async ngOnInit() {

    this.form = this.batchForm.form;

    super.ngOnInit();

    // Listen form changes
    this.registerSubscription(
      this.batchForm.form.valueChanges
        .pipe(takeWhile(() => !this.loading))
        .subscribe((_) => {
          if (!this.loading && this.valueChanges.observers.length) {
            this.valueChanges.emit(this.value);
          }
        })
    );
  }

  setValue(data: Batch, opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {
    if (!this._ready) {
      this.safeSetValue(data, opts);
      return;
    }

    if (this.debug) console.debug("[batch-group-form] setValue() with value:", data);

    if (!this.qvPmfm) {
      this.batchForm.value = data;

      // Check if measure
      const enableMeasure = isNotNil(BatchUtils.getSamplingChild(data));
      this.hasMeasureControl.setValue(enableMeasure);

    }
    else {
      let hasSamplingBatch = false;

      // Prepare data array, for each qualitative values
      data.children = this.qvPmfm.qualitativeValues.map((qv, index) => {

        // Find existing child, or create a new one
        const child = (data.children || []).find(c => +(c.measurementValues[this.qvPmfm.pmfmId]) === qv.id)
          || new Batch();

        // Make sure label and rankOrder are correct
        child.label = `${data.label}.${qv.label}`;
        child.measurementValues[this.qvPmfm.pmfmId] = qv;
        child.rankOrder = index + 1;

        // Check there is a sampling batch
        hasSamplingBatch = hasSamplingBatch || isNotNil(BatchUtils.getSamplingChild(child));

        return child;
      });

      // Set value of the species form
      this.batchForm.value = data;

      // Then set value of each child form
      this.childrenForms.forEach((childForm, index) => {
        childForm.setValue(data.children[index]);
        if (this.enabled) {
          childForm.enable();
        }
        else {
          childForm.disable();
        }
      });

      // Enable measure, when there is a sampling batch
      this.hasMeasureControl.setValue(hasSamplingBatch);
    }
  }

  setHasMeasure(value: boolean) {
    this.hasMeasureControl.setValue(value);
    if (this.childrenForms) {
      this.childrenForms.forEach((childForm, index) => {
        childForm.setIsSampling(value, {emitEvent: true}/*Important, to force async validator*/);
      });
    }
    this.markForCheck();
  }

  logFormErrors(logPrefix: string) {
    AppFormUtils.logFormErrors(this.batchForm.form, logPrefix);
    if (this.childrenForms) this.childrenForms.forEach((childForm, index) => {
        AppFormUtils.logFormErrors(childForm.form, logPrefix, `children#${index}`);
      });
  }

  createMapPmfmsFn() {
    const self = this;
    return (pmfms: PmfmStrategy[]) => {
      self.qvPmfm = self.qvPmfm || PmfmUtils.getFirstQualitativePmfm(pmfms);
      if (self.qvPmfm) {

        // Create a copy, to keep original pmfm unchanged
        self.qvPmfm = this.qvPmfm.clone();

        // Hide for children form, and change it as required
        self.qvPmfm.hidden = true;
        self.qvPmfm.isMandatory = true;

        // Replace in the list
        self.$childrenPmfms.next(pmfms.map(p => p.pmfmId === this.qvPmfm.pmfmId ? this.qvPmfm : p));

        self.loading = false;
        // Do not display PMFM in the root batch
        return [];
      }

      self.loading = false;
      return pmfms;
    };
  }

  /* -- protected methods -- */


  protected async safeSetValue(data: Batch, opts?: {emitEvent?: boolean; onlySelf?: boolean; }) {

    if (!this._ready) await this.ready();

    this.setValue(data, {...opts, emitEvent: true});
  }


  // Wait form controls ready
  protected async ready(): Promise<void> {
    if (this._ready) return;

    // Wait for species form to be ready
    await this.batchForm.ready();

    // Then wait children forms to be ready
    if (this.qvPmfm) {
      await firstNotNilPromise(this.childrenForms.changes
        .pipe(
          // Wait children component created
          filter(() => this.childrenForms.length > 0),
          // Then wait children forms are all ready
          switchMap((childrenForms) => Promise.all(childrenForms.map(c => c.ready())))
        )
      );
    }

    this._ready = true;
  }

  protected getValue(): Batch {
    const data = this.batchForm.value;

    // If has children form
    if (this.qvPmfm) {
      data.children = this.childrenForms.map((form, index) => {
        const qv = this.qvPmfm.qualitativeValues[index];
        const child = form.value;
        child.rankOrder = index + 1;
        child.label = `${data.label}.${qv.label}`;
        child.measurementValues = child.measurementValues || {};
        child.measurementValues[this.qvPmfm.pmfmId.toString()] = '' + qv.id;
        return child;
      });
    }

    if (this.debug) console.debug("[batch-group-form] getValue():", data);

    return data;
  }


}
