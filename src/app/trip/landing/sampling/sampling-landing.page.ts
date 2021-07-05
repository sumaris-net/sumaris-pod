import {ChangeDetectionStrategy, Component, Injector} from '@angular/core';
import {FormGroup, ValidationErrors} from '@angular/forms';
import {BehaviorSubject, Subscription} from 'rxjs';
import {DenormalizedPmfmStrategy} from '../../../referential/services/model/pmfm-strategy.model';
import {ParameterLabelGroups, PmfmIds} from '../../../referential/services/model/model.enum';
import {PmfmService} from '../../../referential/services/pmfm.service';
import {EntityServiceLoadOptions, fadeInOutAnimation, firstNotNilPromise, HistoryPageReference, isInstanceOf, isNil, isNotNil, ObjectMap, SharedValidators} from '@sumaris-net/ngx-components';
import {BiologicalSamplingValidators} from '../../services/validator/biological-sampling.validators';
import {LandingPage} from '../landing.page';
import {Landing} from '../../services/model/landing.model';
import {filter, tap, throttleTime} from 'rxjs/operators';
import {ObservedLocation} from '../../services/model/observed-location.model';
import {SamplingStrategyService} from '../../../referential/services/sampling-strategy.service';
import {Strategy} from '../../../referential/services/model/strategy.model';
import {ProgramProperties} from '@app/referential/services/config/program.config';


@Component({
  selector: 'app-sampling-landing-page',
  templateUrl: './sampling-landing.page.html',
  styleUrls: ['./sampling-landing.page.scss'],
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingLandingPage extends LandingPage {

  $pmfmGroups = new BehaviorSubject<ObjectMap<number[]>>(null);
  showSamplesTable = false;
  zeroEffortWarning = false;
  noEffortError = false;

  constructor(
    injector: Injector,
    protected samplingStrategyService: SamplingStrategyService,
    protected pmfmService: PmfmService
  ) {
    super(injector, {
      pathIdAttribute: 'samplingId',
      autoOpenNextTab: true
    });
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

    // Check strategy effort
    this.$strategy.subscribe(strategy => this.checkStrategyEffort(strategy));

    // Use landing location as default location for samples
    // TODO: BLA review this : a quoi sert defaultLocation ?
    this.registerSubscription(
      this.landingForm.form.get('location').valueChanges
        .pipe(
          throttleTime(200),
          filter(isNotNil),
          tap(location => this.samplesTable.defaultLocation = location)
        )
        .subscribe());

    // Load Pmfm IDS, group by parameter labels
    this.pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups)
      .then(pmfmGroups => this.$pmfmGroups.next(pmfmGroups));
  }

  ngOnDestroy() {
    super.ngOnDestroy();

    this.$pmfmGroups.complete();
  }

  /* -- protected functions -- */


  protected async checkStrategyEffort(strategy: Strategy): Promise<void> {

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

    await this.samplesTable.ready();
    this.showSamplesTable = this.samplesTable.$pmfms.getValue()?.length > 0;
    this.markForCheck();
  }

  protected async onNewEntity(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onNewEntity(data, options);
    // By default, set location to parent location
    if (this.parent && this.parent instanceof ObservedLocation) {
      this.landingForm.form.get('location').patchValue(data.location);
    }
  }

  protected async setValue(data: Landing): Promise<void> {
    if (!data) return; // Skip

    const strategyLabel = data.measurementValues && data.measurementValues[PmfmIds.STRATEGY_LABEL.toString()]
    if (strategyLabel) {
      this.samplesTable.strategyLabel = strategyLabel;
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

    // TODO generate new label ?
    //this.samplingStrategyService.computeNextSampleTagId(this.$strategyLabel.getValue())

    return BiologicalSamplingValidators.addSampleValidators(form, pmfms, this.$pmfmGroups.getValue() || {}, {
      markForCheck: () => this.markForCheck()
    });
  }


  protected async computeTitle(data: Landing): Promise<string> {

    const program = await firstNotNilPromise(this.$program);
    let i18nSuffix = program.getProperty(ProgramProperties.I18N_SUFFIX);
    i18nSuffix = i18nSuffix !== 'legacy' && i18nSuffix || '';

    const titlePrefix = this.parent && isInstanceOf(this.parent, ObservedLocation) &&
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
