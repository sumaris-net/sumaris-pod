import {ChangeDetectionStrategy, Component, Injector} from '@angular/core';
import {FormGroup, ValidationErrors} from '@angular/forms';
import {Subscription} from 'rxjs';
import {DenormalizedPmfmStrategy} from '@app/referential/services/model/pmfm-strategy.model';
import {ParameterLabelGroups, PmfmIds} from '@app/referential/services/model/model.enum';
import {PmfmService} from '@app/referential/services/pmfm.service';
import {AccountService, EntityServiceLoadOptions, fadeInOutAnimation, firstNotNilPromise, HistoryPageReference, isNil, isNotEmptyArray, isNotNil, SharedValidators,} from '@sumaris-net/ngx-components';
import {BiologicalSamplingValidators} from '../../services/validator/biological-sampling.validators';
import {LandingPage} from '../landing.page';
import {Landing} from '../../services/model/landing.model';
import {filter, first} from 'rxjs/operators';
import {ObservedLocation} from '../../services/model/observed-location.model';
import {SamplingStrategyService} from '@app/referential/services/sampling-strategy.service';
import {Strategy} from '@app/referential/services/model/strategy.model';
import {ProgramProperties} from '@app/referential/services/config/program.config';


@Component({
  selector: 'app-sampling-landing-page',
  templateUrl: './sampling-landing.page.html',
  styleUrls: ['./sampling-landing.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingLandingPage extends LandingPage {

  showSamplesTable = false;
  zeroEffortWarning = false;
  noEffortError = false;
  warning: string = null;

  constructor(
    injector: Injector,
    protected samplingStrategyService: SamplingStrategyService,
    protected pmfmService: PmfmService,
    protected accountService: AccountService,
  ) {
    super(injector, {
      pathIdAttribute: 'samplingId'
    });
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

    // Show table, if there is some pmfms
    this.registerSubscription(
      this.samplesTable.$pmfms
        .pipe(
          filter(pmfms => !this.showSamplesTable && isNotEmptyArray(pmfms)),
          first()
        )
        .subscribe(_ => {
          this.showSamplesTable = true;
          this.markForCheck();
        })
    );

    // Load Pmfm IDs
    this.pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups)
      .then(pmfmGroups => this.samplesTable.pmfmGroups = pmfmGroups);
  }

  /* -- protected functions -- */
  updateViewState(data: Landing, opts?: {onlySelf?: boolean; emitEvent?: boolean}) {
    super.updateViewState(data);

    // Update tabs state (show/hide)
    this.updateTabsState(data);
  }

  updateTabsState(data: Landing) {
    // Enable landings tab
    this.showSamplesTable = this.showSamplesTable || (!this.isNewData || this.isOnFieldMode);

    // confirmation pop-up on quite form if form not touch
    if (this.isNewData && this.isOnFieldMode) {
      this.markAsDirty();
    }

    // Move to second tab
    if (this.showSamplesTable && !this.isNewData && this.selectedTabIndex === 0) {
      this.selectedTabIndex = 1;
      this.tabGroup.realignInkBar();
    }
  }

  get canUserCancelOrDelete(): boolean {
    // IMAGINE-632: User can only delete landings or samples created by himself or on which he is defined as observer
    if (this.accountService.isAdmin()) {
      return true;
    }

    const entity = this.data;
    const recorder = entity.recorderPerson;
    const connectedPerson = this.accountService.person;
    if (connectedPerson.id === recorder?.id) {
      return true;
    }

    // When connected user is in observed location observers
    for (const observer of entity.observers) {
      if (connectedPerson.id === observer.id) {
        return true;
      }
    }
    return false;
  }

  protected async setStrategy(strategy: Strategy) {
    await super.setStrategy(strategy);

    if (!strategy) return; // Skip if empty

    await this.checkStrategyEffort(strategy);
  }

  protected async checkStrategyEffort(strategy: Strategy) {

    const [program] = await Promise.all([
      firstNotNilPromise(this.$program),
      this.landingForm.waitIdle()
    ]);

    if (strategy &&  strategy.label) {
      // Add validator errors on expected effort for this sampleRow (issue #175)
      const strategyEffort = await this.samplingStrategyService.loadStrategyEffortByDate(program.label, strategy.label, this.data.dateTime);

      // DEBUG
      console.debug("[sampling-landing-page] Strategy effort loaded: ", strategyEffort);

      // No effort defined
      if (!strategyEffort) {
        this.noEffortError = true;
        this.zeroEffortWarning = false;
        this.landingForm.strategyControl.setErrors(<ValidationErrors>{noEffort: true});
      }
      // Effort is set, but = 0
      else if (strategyEffort.expectedEffort === 0) {
        this.zeroEffortWarning = true;
        this.noEffortError = false;
        SharedValidators.clearError(this.landingForm.strategyControl, 'noEffort');
      }
      // And positive effort has been defined: OK
      else {
        this.zeroEffortWarning = false;
        this.noEffortError = false;
        SharedValidators.clearError(this.landingForm.strategyControl, 'noEffort');
      }
    }

    this.markForCheck();
  }

  protected async onNewEntity(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onNewEntity(data, options);
    // By default, set location to parent location
    if (this.parent && this.parent instanceof ObservedLocation) {
      this.landingForm.form.get('location').patchValue(data.location);
    }
  }

  protected async getValue(): Promise<Landing> {
    let data = await super.getValue();

    // Make to convert as an entity
    data = Landing.fromObject(data);

    // Compute final TAG_ID, using the strategy label
    const strategyLabel = data.measurementValues &&  data.measurementValues[PmfmIds.STRATEGY_LABEL];
    if (strategyLabel) {
      const sampleLabelPrefix = strategyLabel + '-';
      (data.samples || []).forEach(sample => {
        const tagId = sample.measurementValues[PmfmIds.TAG_ID];
        if (tagId && !tagId.startsWith(sampleLabelPrefix)) {
          sample.measurementValues[PmfmIds.TAG_ID] = sampleLabelPrefix + tagId;
        }
      });
    }
    return data;
  }

  protected async setValue(data: Landing): Promise<void> {
    if (!data) return; // Skip

    const strategyLabel = data.measurementValues && data.measurementValues[PmfmIds.STRATEGY_LABEL.toString()]
    if (strategyLabel) {
      this.samplesTable.strategyLabel = strategyLabel;
    }

    if (this.parent && this.parent instanceof ObservedLocation && isNotNil(data.id)) {
      const recorderIsNotObserver = !(this.parent.observers && this.parent.observers.find(p => p.equals(data.recorderPerson)));
      this.warning = recorderIsNotObserver ? 'LANDING.WARNING.NOT_OBSERVER_ERROR' : null;
    }

    // Remove sample's TAG_ID prefix
    if (strategyLabel) {
      const samplePrefix = strategyLabel + '-';
      (data.samples || []).map(sample => {
      if (sample.measurementValues.hasOwnProperty(PmfmIds.TAG_ID)) {
        const tagId = sample.measurementValues[PmfmIds.TAG_ID];
        if (tagId && tagId.startsWith(samplePrefix)) {
          sample.measurementValues[PmfmIds.TAG_ID] = tagId.substring(samplePrefix.length);
        }
      }
    });
  }

    await super.setValue(data);
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ... (await super.computePageHistory(title)),
      icon: 'boat'
    };
  }

  protected computePageUrl(id: number|'new') {
    const parentUrl = this.getParentPageUrl();
    return `${parentUrl}/sampling/${id}`;
  }

  protected computeSampleRowValidator(form: FormGroup, pmfms: DenormalizedPmfmStrategy[]): Subscription {
    console.debug('[sampling-landing-page] Adding row validator');

    return BiologicalSamplingValidators.addSampleValidators(form, pmfms, this.samplesTable.pmfmGroups || {}, {
      markForCheck: () => this.markForCheck()
    });
  }

  protected async computeTitle(data: Landing): Promise<string> {

    const program = await firstNotNilPromise(this.$program);
    let i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
    i18nSuffix = i18nSuffix !== 'legacy' && i18nSuffix || '';

    const titlePrefix = this.parent && this.parent instanceof ObservedLocation &&
      await this.translate.get('LANDING.EDIT.TITLE_PREFIX', {
        location: (this.parent.location && (this.parent.location.name || this.parent.location.label)),
        date: this.parent.startDateTime && this.dateFormat.transform(this.parent.startDateTime) as string || ''
      }).toPromise() || '';

    // new data
    if (!data || isNil(data.id)) {
      return titlePrefix + (await this.translate.get(`LANDING.NEW.${i18nSuffix}TITLE`).toPromise());
    }
    // Existing data
    const strategy = await firstNotNilPromise(this.$strategy);

    return titlePrefix + (await this.translate.get(`LANDING.EDIT.${i18nSuffix}TITLE`, {
      vessel: data.vesselSnapshot && (data.vesselSnapshot.registrationCode || data.vesselSnapshot.name),
      strategyLabel: strategy && strategy.label
    }).toPromise());
  }
}
