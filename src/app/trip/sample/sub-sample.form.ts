import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, Input, OnDestroy, OnInit } from '@angular/core';
import {MeasurementValuesForm} from '../measurement/measurement-values.form.class';
import {DateAdapter} from '@angular/material/core';
import {Moment} from 'moment';
import {MeasurementsValidatorService} from '../services/validator/measurement.validator';
import {FormBuilder} from '@angular/forms';
import { AppFormUtils, EntityUtils, isNil, isNotEmptyArray, isNotNil, joinPropertiesPath, LocalSettingsService, startsWithUpperCase, toNumber, UsageMode } from '@sumaris-net/ngx-components';
import {AcquisitionLevelCodes, PmfmIds} from '@app/referential/services/model/model.enum';
import {Sample} from '../services/model/sample.model';
import {DenormalizedPmfmStrategy} from '@app/referential/services/model/pmfm-strategy.model';
import {environment} from '@environments/environment';
import {ProgramRefService} from '@app/referential/services/program-ref.service';
import {SubSampleValidatorService} from '@app/trip/services/validator/sub-sample.validator';
import {IPmfm, PmfmUtils} from '@app/referential/services/model/pmfm.model';
import {PmfmValueUtils} from '@app/referential/services/model/pmfm-value.model';
import { merge, Subject } from 'rxjs';
import { mergeMap } from 'rxjs/internal/operators';
import { distinctUntilChanged, filter } from 'rxjs/operators';
import { SortDirection } from '@angular/material/sort';

const SAMPLE_FORM_DEFAULT_I18N_PREFIX = 'TRIP.INDIVIDUAL_RELEASE.EDIT.';

@Component({
  selector: 'app-sub-sample-form',
  templateUrl: 'sub-sample.form.html',
  styleUrls: ['sub-sample.form.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubSampleForm extends MeasurementValuesForm<Sample>
  implements OnInit, OnDestroy {

  private _availableParents: Sample[] = [];
  private _availableSortedParents: Sample[] = [];
  focusFieldName: string;
  displayAttributes: string[];
  linkToParentWithTagId: boolean = true;
  onParentChanges = new Subject();

  @Input() i18nPrefix = SAMPLE_FORM_DEFAULT_I18N_PREFIX;

  @Input() mobile: boolean;
  @Input() tabindex: number;
  @Input() usageMode: UsageMode;
  @Input() showLabel = false;
  @Input() enableParent = true;
  @Input() showComment = true;
  @Input() showError = true;
  @Input() maxVisibleButtons: number;
  @Input() defaultLatitudeSign: '+' | '-';
  @Input() defaultLongitudeSign: '+' | '-';
  @Input() displayParentPmfm: IPmfm;

  @Input()
  set availableParents(parents: Sample[]) {
    if (this._availableParents !== parents) {
      this._availableParents = parents;
      if (!this.loading) this.onParentChanges.next();
    }
  }

  get availableParents(): Sample[] {
    return this._availableParents;
  }

  constructor(
    protected injector: Injector,
    protected measurementValidatorService: MeasurementsValidatorService,
    protected formBuilder: FormBuilder,
    protected programRefService: ProgramRefService,
    protected cd: ChangeDetectorRef,
    protected validatorService: SubSampleValidatorService,
    protected settings: LocalSettingsService,
  ) {
    super(injector, measurementValidatorService, formBuilder, programRefService,
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

    // Parent combo
    this.registerAutocompleteField('parent', {
      suggestFn: (value: any, options?: any) => this.suggestParent(value),
      showAllOnFocus: true
    });

    this.registerSubscription(
      merge(
        this.onParentChanges.pipe(mergeMap(() => this.$pmfms)),
        this.$pmfms.pipe(distinctUntilChanged())
      )
        .pipe(
          filter(isNotEmptyArray),
        ).subscribe((pmfms) => this.updateParents(pmfms))
    )

    this.focusFieldName = !this.mobile && this.showLabel && 'label';

    if (!this.enableParent) {
      this.form.parent?.disable();
    }
  }

  mapPmfms(pmfms: IPmfm[]): IPmfm[] {
    // Hide pmfm TAG_ID and DRESSING
    return pmfms.map(pmfm => {
      if ((pmfm.id === PmfmIds.TAG_ID || pmfm.id === PmfmIds.DRESSING) && !pmfm.hidden) {
        pmfm = pmfm.clone();
        pmfm.hidden = true;
      }
      return pmfm;
    });
  }


  /* -- protected methods -- */
  protected getValue(): Sample {
    const value = super.getValue();
    if (!this.showComment) value.comments = undefined;
    return value;
  }

  protected async updateParents(pmfms: IPmfm[]) {
    // DEBUG
    console.debug('[sub-samples-form] Update parents...');

    const parents = this._availableParents || [];
    const hasTaxonName = parents.some(s => isNotNil(s.taxonName?.id));
    const attributeName = hasTaxonName ? 'taxonName' : 'taxonGroup';
    const baseDisplayAttributes = this.settings.getFieldDisplayAttributes(attributeName)
      .map(key => `${attributeName}.${key}`);

    const tagIdPmfm = pmfms.find(p => p.id === PmfmIds.TAG_ID);
    this.displayParentPmfm = tagIdPmfm?.required ? tagIdPmfm : null;

    // If display parent using by a pmfm
    if (this.displayParentPmfm) {
      const parentDisplayPmfmIdStr = this.displayParentPmfm.id.toString();
      const parentDisplayPmfmPath = `measurementValues.${parentDisplayPmfmIdStr}`;
      // Keep parents without this pmfms
      const filteredParents = parents.filter(s => isNotNil(s.measurementValues[parentDisplayPmfmIdStr]));
      this._availableSortedParents = EntityUtils.sort(filteredParents, parentDisplayPmfmPath);

      this.autocompleteFields.parent.attributes = [parentDisplayPmfmPath].concat(baseDisplayAttributes);
      this.autocompleteFields.parent.columnSizes = [4].concat(baseDisplayAttributes.map(attr =>
        // If label then col size = 2
        attr.endsWith('label') ? 2 : undefined));
      this.autocompleteFields.parent.columnNames = [PmfmUtils.getPmfmName(this.displayParentPmfm)];
      this.autocompleteFields.parent.displayWith = (obj) => obj && obj.measurementValues
        && PmfmValueUtils.valueToString(obj.measurementValues[parentDisplayPmfmIdStr], {pmfm: this.displayParentPmfm})
        || undefined;
    }
    else {
      const displayAttributes = ['rankOrder'].concat(baseDisplayAttributes);
      this._availableSortedParents = EntityUtils.sort(parents.slice(), 'rankOrder');
      this.autocompleteFields.parent.attributes = displayAttributes;
      this.autocompleteFields.parent.columnSizes = undefined; // use defaults
      this.autocompleteFields.parent.columnNames = undefined; // use defaults
      this.autocompleteFields.parent.displayWith = (obj) => obj && joinPropertiesPath(obj, displayAttributes) || undefined;
    }

    this.markForCheck();
  }

  protected async suggestParent(value: any): Promise<any[]> {
    if (EntityUtils.isNotEmpty(value, 'label')) {
      return [value];
    }
    value = (typeof value === 'string' && value !== '*') && value || undefined;
    if (isNil(value)) return this._availableSortedParents; // All

    if (this.debug) console.debug(`[sub-sample-form] Searching parent {${value || '*'}}...`);
    if (this.displayParentPmfm) { // Search on a specific Pmfm (e.g Tag-ID)
      return this._availableSortedParents.filter(p => startsWithUpperCase(p.measurementValues[this.displayParentPmfm.id], value));
    }
    // Search on rankOrder
    return this._availableSortedParents.filter(p => p.rankOrder.toString().startsWith(value));
  }

  protected markForCheck() {
    this.cd.markForCheck();
  }

  isNotHiddenPmfm = PmfmUtils.isNotHidden;
  selectInputContent = AppFormUtils.selectInputContent;

}
