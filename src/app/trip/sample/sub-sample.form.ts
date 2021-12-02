import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit} from '@angular/core';
import {MeasurementValuesForm} from '../measurement/measurement-values.form.class';
import {DateAdapter} from '@angular/material/core';
import {Moment} from 'moment';
import {MeasurementsValidatorService} from '../services/validator/measurement.validator';
import {FormBuilder} from '@angular/forms';
import {AppFormUtils, EntityUtils, isNil, joinPropertiesPath, LocalSettingsService, startsWithUpperCase, toNumber, UsageMode} from '@sumaris-net/ngx-components';
import {AcquisitionLevelCodes, PmfmIds} from '../../referential/services/model/model.enum';
import {Sample} from '../services/model/sample.model';
import {DenormalizedPmfmStrategy} from '../../referential/services/model/pmfm-strategy.model';
import {environment} from '../../../environments/environment';
import {ProgramRefService} from '../../referential/services/program-ref.service';
import {SubSampleValidatorService} from '@app/trip/services/validator/sub-sample.validator';
import {IPmfm, PmfmUtils} from '@app/referential/services/model/pmfm.model';
import {PmfmValueUtils} from '@app/referential/services/model/pmfm-value.model';

const SAMPLE_FORM_DEFAULT_I18N_PREFIX = 'TRIP.INDIVIDUAL_RELEASE.EDIT.';

@Component({
  selector: 'app-sub-sample-form',
  templateUrl: 'sub-sample.form.html',
  styleUrls: ['sub-sample.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubSampleForm extends MeasurementValuesForm<Sample>
  implements OnInit, OnDestroy {

  focusFieldName: string;
  displayAttributes: string[];
  @Input() i18nPrefix = SAMPLE_FORM_DEFAULT_I18N_PREFIX;

  @Input() mobile: boolean;
  @Input() tabindex: number;
  @Input() usageMode: UsageMode;
  @Input() showLabel = false;
  @Input() enableParent = true;
  @Input() showComment = true;
  @Input() showError = true;
  @Input() maxVisibleButtons: number;
  @Input() availableParents: Sample[];
  @Input() mapPmfmFn: (pmfms: DenormalizedPmfmStrategy[]) => DenormalizedPmfmStrategy[];
  @Input() displayParentPmfm: IPmfm;

  constructor(
    protected dateAdapter: DateAdapter<Moment>,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programRefService: ProgramRefService,
    protected cd: ChangeDetectorRef,
    protected validatorService: SubSampleValidatorService,
    protected settings: LocalSettingsService,
  ) {
    super(dateAdapter, measurementValidatorService, formBuilder, programRefService, settings, cd,
      validatorService.getFormGroup(),
      {
        mapPmfms: (pmfms) => this.mapPmfms(pmfms)
      }
    );

    // Set default acquisition level
    this._acquisitionLevel = AcquisitionLevelCodes.INDIVIDUAL_RELEASE;
    this._enable = true;

    // for DEV only
    this.debug = !environment.production;
  }

  ngOnInit() {
    super.ngOnInit();

    this.tabindex = toNumber(this.tabindex, 1);
    this.maxVisibleButtons = toNumber(this.maxVisibleButtons, 4);

    // TODO CC : Fix display attributes when come from sample table
    // this.displayAttributes = this.settings.getFieldDisplayAttributes('taxonName')
    //   .map(key => 'taxonName.' + key);

    // Parent combo
    // this.registerAutocompleteField('parent', {
    //   suggestFn: (value: any, options?: any) => this.suggestParent(value),
    //   showAllOnFocus: true,
    //   attributes: ['rankOrder'].concat(this.displayAttributes),
    //   mobile: this.mobile,
    //   displayWith: (obj) => obj && joinPropertiesPath(obj, ['rankOrder'].concat(this.displayAttributes)) || undefined
    // });

    // Check if there a tag id in pmfms
    // this.registerSubscription(
    //   filterNotNil(this.$pmfms)
    //     .subscribe((_) => {
    //       this.displayParentPmfm = this._intialPmfms.find(p => p.id === PmfmIds.TAG_ID);
    //
    //       if (this.displayParentPmfm) {
    //         this.autocompleteFields.parent.attributes = [`measurementValues.${this.displayParentPmfm.id}`].concat(displayAttributes);
    //         this.autocompleteFields.parent.columnSizes = [4].concat(displayAttributes.map(attr =>
    //           // If label then col size = 2
    //           attr.endsWith('label') ? 2 : undefined));
    //         this.autocompleteFields.parent.columnNames = [PmfmUtils.getPmfmName(this.displayParentPmfm)];
    //         this.autocompleteFields.parent.displayWith = (obj) => obj && obj.measurementValues
    //           && PmfmValueUtils.valueToString(obj.measurementValues[this.displayParentPmfm.id], {pmfm: this.displayParentPmfm})
    //           || undefined;
    //
    //         this.markForCheck();
    //       }
    //     }));

    this.focusFieldName = !this.mobile && this.showLabel && 'label';

    if (!this.enableParent) {
      this.form.parent?.disable();
    }
  }

  mapPmfms(pmfms: IPmfm[]): IPmfm[] {

    this.displayAttributes = this.settings.getFieldDisplayAttributes('taxonName')
      .map(key => 'taxonName.' + key);

    this.registerAutocompleteField('parent', {
      suggestFn: (value: any, options?: any) => this.suggestParent(value),
      showAllOnFocus: true,
      mobile: this.mobile
    });

    this.displayParentPmfm = pmfms.find(p => p.id === PmfmIds.TAG_ID);
    if (this.displayParentPmfm) {
      this.autocompleteFields.parent.attributes = [`measurementValues.${this.displayParentPmfm.id}`].concat(this.displayAttributes);
      this.autocompleteFields.parent.columnSizes = [4].concat(this.displayAttributes.map(attr =>
        // If label then col size = 2
        attr.endsWith('label') ? 2 : undefined));
      this.autocompleteFields.parent.columnNames = [PmfmUtils.getPmfmName(this.displayParentPmfm)];
      this.autocompleteFields.parent.displayWith = (obj) => obj && obj.measurementValues
        && PmfmValueUtils.valueToString(obj.measurementValues[this.displayParentPmfm.id], {pmfm: this.displayParentPmfm})
        || undefined;
    } else {
      this.autocompleteFields.parent.attributes = ['rankOrder'].concat(this.displayAttributes);
      this.autocompleteFields.parent.displayWith = (obj) => obj && joinPropertiesPath(obj, ['rankOrder'].concat(this.displayAttributes)) || undefined;
    }

    this.markForCheck();

    return pmfms.filter(pmfm => pmfm.id !== PmfmIds.TAG_ID && pmfm.id !== PmfmIds.DRESSING);
  }

  /* -- protected methods -- */
  protected getValue(): Sample {
    const value = super.getValue();
    if (!this.showComment) value.comments = undefined;
    return value;
  }

  protected async suggestParent(value: any): Promise<any[]> {
    if (EntityUtils.isNotEmpty(value, 'label')) {
      return [value];
    }
    value = (typeof value === 'string' && value !== '*') && value || undefined;
    if (isNil(value)) return this.availableParents; // All

    if (this.debug) console.debug(`[sub-sample-table] Searching parent {${value || '*'}}...`);
    if (this.displayParentPmfm) { // Search on a specific Pmfm (e.g Tag-ID)
      return this.availableParents.filter(p => startsWithUpperCase(p.measurementValues[this.displayParentPmfm.id], value));
    }
    // Search on rankOrder
    return this.availableParents.filter(p => p.rankOrder.toString().startsWith(value));
  }

  selectInputContent = AppFormUtils.selectInputContent;

  protected markForCheck() {
    this.cd.markForCheck();
  }
}
