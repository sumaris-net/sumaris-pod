import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from "@angular/core";
import {Batch, BatchUtils} from "../services/model/batch.model";
import {MeasurementValuesForm} from "../measurement/measurement-values.form.class";
import {DateAdapter} from "@angular/material";
import {Moment} from "moment";
import {MeasurementsValidatorService} from "../services/measurement.validator";
import {FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {ProgramService} from "../../referential/services/program.service";
import {ReferentialRefService} from "../../referential/services/referential-ref.service";
import {EntityUtils, referentialToString, UsageMode} from "../../core/services/model";
import {filter, first} from "rxjs/operators";
import {isNil, isNotNil, PmfmStrategy, PmfmUtils} from "../../referential/services/model";
import {LocalSettingsService} from "../../core/services/local-settings.service";
import {environment} from "../../../environments/environment";
import {AppFormUtils, FormArrayHelper, PlatformService} from "../../core/core.module";
import {MeasurementValuesUtils} from "../services/model/measurement.model";
import {BatchGroupValidatorService} from "../services/batch-groups.validator";
import {BehaviorSubject, Subject} from "rxjs";

@Component({
  selector: 'app-batch-group-form',
  templateUrl: 'batch-group.form.html',
  styleUrls: ['batch-group.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BatchGroupForm extends MeasurementValuesForm<Batch>
  implements OnInit, OnDestroy {

  mobile: boolean;

  childrenFormHelper: FormArrayHelper<Batch>;
  $childrenPmfms = new BehaviorSubject<PmfmStrategy[]>(undefined);

  @Input() tabindex: number;

  @Input() usageMode: UsageMode;

  @Input() showTaxonGroup = true;

  @Input() showTaxonName = true;

  @Input() showTotalIndividualCount = true;

  @Input() showIndividualCount = true;

  @Input() showError = true;

  @Input() qvPmfm: PmfmStrategy;


  get isOnFieldMode(): boolean {
    return this.usageMode ? this.usageMode === 'FIELD' : this.settings.isUsageMode('FIELD');
  }

  get childrenFormArray(): FormArray {
    return this.form && this.form.get('children') as FormArray;
  }

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programService: ProgramService,
    protected platform: PlatformService,
    protected cd: ChangeDetectorRef,
    protected validatorService: BatchGroupValidatorService,
    protected referentialRefService: ReferentialRefService,
    protected settings: LocalSettingsService
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programService, settings, cd,
      null,
      {
        mapPmfms: (pmfms) => this.mapPmfms(pmfms),
        onUpdateControls: (form) => this.onUpdateControls(form)
      });
    this.mobile = platform.mobile;

    // for DEV only
    this.debug = !environment.production;
  }

  ngOnInit() {
    this.form = this.form || this.validatorService.getFormGroup();

    this.childrenFormHelper = new FormArrayHelper<Batch>(
      this.formBuilder,
      this.form,
      'children',
      (value) => this.validatorService.getFormGroup(),
      (v1, v2) => EntityUtils.equals(v1, v2),
      (value) => isNil(value),
      {allowEmptyArray: true}
    );

    super.ngOnInit();

    this.tabindex = isNotNil(this.tabindex) ? this.tabindex : 1;
  }

  public setValue(data: Batch) {
    console.log("TODO check setValue group form", data)

    if (this.qvPmfm) {
      data.children = this.qvPmfm.qualitativeValues.map((qv, index) => {
        let child = (data.children || []).find(c => c.measurementValues[this.qvPmfm.pmfmId] == qv.id);
        if (!child) {
          child = new Batch();
          child.measurementValues[this.qvPmfm.pmfmId] = qv;
        }
        return child;
      });
    }

    // Resize form to children length
    this.childrenFormHelper.resize(data.children.length);
  }

  protected getValue(): Batch {
    const data = super.getValue();


    console.log("TODO check getValue group form", data)

    return data;
  }

  /* -- protected methods -- */

  protected mapPmfms(pmfms: PmfmStrategy[]) {

    this.qvPmfm = this.qvPmfm || PmfmUtils.getFirstQualitativePmfm(pmfms);
    if (this.qvPmfm) {
      this.qvPmfm = this.qvPmfm.clone();
      this.qvPmfm.hidden = true;

      // Replace in the list
      this.$childrenPmfms.next(pmfms.map(p => p.pmfmId === this.qvPmfm.pmfmId ? this.qvPmfm : p));

      // Do not display PMFM in the root batch
      return [];
    }

    return pmfms;
  }

  protected onUpdateControls(form: FormGroup) {

    const childrenFormHelper = this.getChildrenFormHelper(form);

    if (this.qvPmfm) {
      childrenFormHelper.resize(this.qvPmfm.qualitativeValues.length);
    }
  }


  protected getChildrenFormHelper(form: FormGroup): FormArrayHelper<Batch> {
    return new FormArrayHelper<Batch>(
      this.formBuilder,
      form,
      'children',
      (value) => this.validatorService.getFormGroup(),
      (v1, v2) => EntityUtils.equals(v1, v2),
      (value) => isNil(value),
      {allowEmptyArray: true}
    );
  }

  referentialToString = referentialToString;
  selectInputContent = AppFormUtils.selectInputContent;
}
