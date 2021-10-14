import {ChangeDetectionStrategy, Component, Injector} from '@angular/core';
import {FormGroup, ValidationErrors} from '@angular/forms';
import {BehaviorSubject, Subscription} from 'rxjs';
import {DenormalizedPmfmStrategy} from '@app/referential/services/model/pmfm-strategy.model';
import {ParameterLabelGroups, PmfmIds} from '@app/referential/services/model/model.enum';
import {PmfmService} from '@app/referential/services/pmfm.service';
import { EntityServiceLoadOptions, fadeInOutAnimation, firstNotNilPromise, HistoryPageReference, isNil, isNotEmptyArray, isNotNil, ObjectMap, SharedValidators } from '@sumaris-net/ngx-components';
import {BiologicalSamplingValidators} from '../../services/validator/biological-sampling.validators';
import {LandingPage} from '../landing.page';
import {Landing} from '../../services/model/landing.model';
import { filter, first, tap, throttleTime } from 'rxjs/operators';
import {ObservedLocation} from '../../services/model/observed-location.model';
import {SamplingStrategyService} from '@app/referential/services/sampling-strategy.service';
import {Strategy} from '@app/referential/services/model/strategy.model';
import {ProgramProperties} from '@app/referential/services/config/program.config';
import {LandingSaveOptions} from '@app/trip/services/landing.service';


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
    protected pmfmService: PmfmService
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

  protected async setStrategy(strategy: Strategy) {
    await super.setStrategy(strategy);

    if (!strategy) return; // Skip if empty

    await this.checkStrategyEffort(strategy);
  }

  protected async checkStrategyEffort(strategy: Strategy) {

    const [program] = await Promise.all([
      firstNotNilPromise(this.$program),
      this.landingForm.ready()
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
    const data = await super.getValue();

    // update samples tag id on save
    const landing = Landing.fromObject(data);
    if (landing && landing.samples)
    {
      landing.samples.map(sample => {
        if (sample.measurementValues && sample.measurementValues.hasOwnProperty(PmfmIds.TAG_ID) && landing.measurementValues && landing.measurementValues.hasOwnProperty(PmfmIds.STRATEGY_LABEL))
        {
          const strategyLabel = landing.measurementValues[PmfmIds.STRATEGY_LABEL];
          const tagIdConcatenatedWithStrategyLabel = sample.measurementValues[PmfmIds.TAG_ID] ? strategyLabel + "-" + sample.measurementValues[PmfmIds.TAG_ID] : null;
          sample.measurementValues[PmfmIds.TAG_ID] = tagIdConcatenatedWithStrategyLabel;
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
      this.warning = recorderIsNotObserver ? 'LANDING.ERROR.NOT_OBSERVER_ERROR' : null;
    }

    // Update landing samples tag_id. We store the tag_id with a concatenation of sample label and sample tag_id but we only display sample tag_id
    if (data.samples) {
    data.samples.map(sample => {
      if (sample.measurementValues.hasOwnProperty(PmfmIds.TAG_ID)) {
        const storedTagId = sample.measurementValues[PmfmIds.TAG_ID];
        if (storedTagId && storedTagId.length > 4 && storedTagId.split('-').length > 1) {
          const tagIdWithoutSampleLabel = storedTagId.split('-')[1];
          sample.measurementValues[PmfmIds.TAG_ID] = tagIdWithoutSampleLabel;
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

  async save(event, options?: any): Promise<boolean> {

    const saveOptions: LandingSaveOptions = {
      withoutExpectedSales: true // indicate service to save landings without expected sales
    };

    return await super.save(event, {...options, ...saveOptions});
  }
}
