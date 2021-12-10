import { ChangeDetectionStrategy, Component, Injector, Input, OnDestroy, OnInit } from '@angular/core';
import { MeasurementValuesForm } from '../measurement/measurement-values.form.class';
import { MeasurementsValidatorService } from '../services/validator/measurement.validator';
import { FormBuilder } from '@angular/forms';
import { AppFormUtils, IReferentialRef, isNil, isNilOrBlank, LoadResult, toNumber, UsageMode } from '@sumaris-net/ngx-components';
import { AcquisitionLevelCodes } from '../../referential/services/model/model.enum';
import { SampleValidatorService } from '../services/validator/sample.validator';
import { Sample } from '../services/model/sample.model';
import { environment } from '../../../environments/environment';
import { ProgramRefService } from '../../referential/services/program-ref.service';
import { PmfmUtils } from '@app/referential/services/model/pmfm.model';

@Component({
  selector: 'app-sample-form',
  templateUrl: 'sample.form.html',
  styleUrls: ['sample.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SampleForm extends MeasurementValuesForm<Sample>
  implements OnInit, OnDestroy {

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
    protected validatorService: SampleValidatorService
  ) {
    super(injector, measurementValidatorService, formBuilder, programRefService,
      validatorService.getFormGroup(),
      {
        skipDisabledPmfmControl: false,
        skipComputedPmfmControl: false
      }
    );

    // Set default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.SAMPLE;
    this._enable = true;

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

  protected getValue(): Sample {
    const value = super.getValue();
    if (!this.showComment) value.comments = undefined;
    return value;
  }

  /* -- protected methods -- */

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

  isNotHiddenPmfm = PmfmUtils.isNotHidden;
  selectInputContent = AppFormUtils.selectInputContent;
}
