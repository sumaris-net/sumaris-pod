import {ChangeDetectionStrategy, Component, Injector, Input, OnDestroy, OnInit} from '@angular/core';
import {MeasurementValuesForm} from '../measurement/measurement-values.form.class';
import {MeasurementsValidatorService} from '../services/validator/measurement.validator';
import {FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {AppFormUtils, EntityUtils, FormArrayHelper, IReferentialRef, isNil, isNilOrBlank, isNotNil, LoadResult, ReferentialUtils, toNumber, UsageMode} from '@sumaris-net/ngx-components';
import {AcquisitionLevelCodes, PmfmIds, QualitativeLabels} from '../../referential/services/model/model.enum';
import {SampleValidatorService} from '../services/validator/sample.validator';
import {Sample} from '../services/model/sample.model';
import {environment} from '../../../environments/environment';
import {ProgramRefService} from '../../referential/services/program-ref.service';
import {PmfmUtils} from '@app/referential/services/model/pmfm.model';
import {Batch} from '@app/trip/services/model/batch.model';
import {SubSampleValidatorService} from '@app/trip/services/validator/sub-sample.validator';
import {delay, filter} from 'rxjs/operators';

@Component({
  selector: 'app-sample-form',
  templateUrl: 'sample.form.html',
  styleUrls: ['sample.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SampleForm extends MeasurementValuesForm<Sample>
  implements OnInit, OnDestroy {

  childrenArrayHelper: FormArrayHelper<Sample>;
  focusFieldName: string;

  @Input() i18nSuffix: string;
  @Input() mobile: boolean;
  @Input() tabindex: number;
  @Input() usageMode: UsageMode;
  @Input() showLabel = true;
  @Input() showSampleDate = true;
  @Input() showTaxonGroup = true;
  @Input() showTaxonName = true;
  @Input() showComment = true;
  @Input() showError = true;
  @Input() maxVisibleButtons: number;

  constructor(
    injector: Injector,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programRefService: ProgramRefService,
    protected validatorService: SampleValidatorService,
    protected subValidatorService: SubSampleValidatorService
  ) {
    super(injector, measurementValidatorService, formBuilder, programRefService,
      validatorService.getFormGroup(),
      {
        skipDisabledPmfmControl: false,
        skipComputedPmfmControl: false,
        onUpdateFormGroup: (form) => this.onUpdateControls(form)
      }
    );

    // Set default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.SAMPLE;
    this._enable = true;
    this.i18nPmfmPrefix = 'TRIP.SAMPLE.PMFM.';
    this.childrenArrayHelper = this.getChildrenFormHelper(this.form);

    // for DEV only
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.tabindex = toNumber(this.tabindex, 1);
    this.maxVisibleButtons = toNumber(this.maxVisibleButtons, 4);

    // Taxon group combo
    this.registerAutocompleteField('taxonGroup', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonGroups(value, options),
      mobile: this.mobile
    });
    // Taxon name combo
    this.registerAutocompleteField('taxonName', {
      suggestFn: (value: any, options?: any) => this.suggestTaxonNames(value, options),
      mobile: this.mobile
    });

    this.focusFieldName = !this.mobile && ((this.showLabel && 'label')
      || (this.showTaxonGroup && 'taxonGroup')
      || (this.showTaxonName && 'taxonName'));
  }

  setChildren(children: Sample[], opts?: { emitEvent?: boolean; }) {
    children = children || [];

    if (this.childrenArrayHelper.size() !== children.length) {
      this.childrenArrayHelper.resize(children.length);
    }

    this.form.patchValue({children}, opts);
  }

  /* -- protected methods -- */

  protected onApplyingEntity(data: Sample, opts?: { [p: string]: any }) {
    super.onApplyingEntity(data, opts);

    const childrenCount = data.children?.length || 0;
    if (this.childrenArrayHelper.size() !== childrenCount) {
      this.childrenArrayHelper.resize(childrenCount);
    }
  }

  protected getValue(): Sample {
    const value = super.getValue();
    // Reset comment, when hidden
    if (!this.showComment) value.comments = undefined;
    return value;
  }

  protected async suggestTaxonGroups(value: any, options?: any): Promise<LoadResult<IReferentialRef>> {
    return this.programRefService.suggestTaxonGroups(value,
      {
        program: this.programLabel,
        searchAttribute: options && options.searchAttribute
      });
  }

  protected async suggestTaxonNames(value: any, options?: any): Promise<LoadResult<IReferentialRef>> {
    const taxonGroup = this.form.get('taxonGroup').value;

    // IF taxonGroup column exists: taxon group must be filled first
    if (this.showTaxonGroup && isNilOrBlank(value) && isNil(parent)) return {data: []};

    return this.programRefService.suggestTaxonNames(value,
      {
        programLabel: this.programLabel,
        searchAttribute: options && options.searchAttribute,
        taxonGroupId: taxonGroup && taxonGroup.id || undefined
      });
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  protected getChildrenFormHelper(form: FormGroup): FormArrayHelper<Sample> {
    let arrayControl = form.get('children') as FormArray;
    if (!arrayControl) {
      arrayControl = this.formBuilder.array([]);
      form.addControl('children', arrayControl);
    }
    return new FormArrayHelper<Sample>(
      arrayControl,
      (value) => this.subValidatorService.getFormGroup(value, {
        measurementValuesAsGroup: false, // avoid to pass pmfms list
        requiredParent: false // Not need
      }),
      (v1, v2) => Sample.equals(v1, v2),
      (value) => isNil(value),
      {allowEmptyArray: true}
    );
  }

  protected onUpdateControls(form: FormGroup) {

    const pmfms = this.$pmfms.getValue();

    if (pmfms) {
      const individualOnDeckPmfm = pmfms.find(pmfm => pmfm.id === PmfmIds.INDIVIDUAL_ON_DECK);
      if (individualOnDeckPmfm) {

        const measFormGroup = (form.controls['measurementValues'] as FormGroup);
        const individualOnDeckControl = measFormGroup.controls[individualOnDeckPmfm.id];
        const disableControls = pmfms.filter(pmfm => pmfm.rankOrder > individualOnDeckPmfm.rankOrder).map(pmfm => measFormGroup.controls[pmfm.id]);

        this.registerSubscription(individualOnDeckControl.valueChanges
          .pipe(
            // IMPORTANT: add a delay, to make sure to be executed AFTER the form.enable()
            delay(200),
            filter(isNotNil)
          )
          .subscribe((value) => {
            if (value) {
              if (this.form.enabled) {
                disableControls.forEach(control => {
                  control.enable();
                });
                pmfms.filter(pmfm => pmfm.rankOrder > individualOnDeckPmfm.rankOrder && pmfm.required).forEach(pmfm => {
                  measFormGroup.controls[pmfm.id].setValidators(Validators.required);
                  measFormGroup.controls[pmfm.id].updateValueAndValidity({onlySelf: true})
                });

                measFormGroup.updateValueAndValidity({onlySelf: true});
                this.form.updateValueAndValidity({onlySelf: true});
              }
            } else {
              disableControls.forEach(control => {
                control.setValue(null);
                control.setValidators(null);
                control.disable();
              });
            }
          }));
      }
    }
  }

  isNotHiddenPmfm = PmfmUtils.isNotHidden;
  selectInputContent = AppFormUtils.selectInputContent;
}
