import {ChangeDetectionStrategy, Component, Injector} from '@angular/core';
import {FormGroup, ValidationErrors} from "@angular/forms";
import {BehaviorSubject, Subscription} from "rxjs";
import {DenormalizedPmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {ParameterLabelGroups, PmfmIds} from "../../../referential/services/model/model.enum";
import {PmfmService} from "../../../referential/services/pmfm.service";
import {ObjectMap} from "@sumaris-net/ngx-components";
import {BiologicalSamplingValidators} from "../../services/validator/biological-sampling.validators";
import {LandingPage} from "../landing.page";
import {Landing} from "../../services/model/landing.model";
import {firstNotNilPromise} from "@sumaris-net/ngx-components";
import {HistoryPageReference}  from "@sumaris-net/ngx-components";
import {fadeInOutAnimation} from "@sumaris-net/ngx-components";
import {filter, tap, throttleTime} from "rxjs/operators";
import {isNotNil} from "@sumaris-net/ngx-components";
import {SamplingSamplesTable} from "../../sample/sampling/sampling-samples.table";
import {EntityServiceLoadOptions} from "@sumaris-net/ngx-components";
import {ObservedLocation} from "../../services/model/observed-location.model";
import {SharedValidators} from "@sumaris-net/ngx-components";
import {SamplingStrategyService} from "../../../referential/services/sampling-strategy.service";
import {Strategy} from "../../../referential/services/model/strategy.model";


@Component({
  selector: 'app-sampling-landing-page',
  templateUrl: './sampling-landing.page.html',
  animations: [fadeInOutAnimation],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingLandingPage extends LandingPage {

  protected pmfmService: PmfmService;

  $pmfmGroups = new BehaviorSubject<ObjectMap<number[]>>(null);
  showSamplesTable = false;
  zeroEffortWarning = false;
  noEffortError = false;

  constructor(
    injector: Injector,
    protected samplingStrategyService: SamplingStrategyService,
  ) {
    super(injector, {
      pathIdAttribute: 'samplingId',
      autoOpenNextTab: false
    });
    this.pmfmService = injector.get(PmfmService);


  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

    // Check strategy effort
    this.$strategy.subscribe(strategy => this.checkStrategyEffort(strategy));

    // Use landing location as default location for samples
    this.registerSubscription(
      this.landingForm.form.get('location').valueChanges
        .pipe(
          throttleTime(200),
          filter(isNotNil),
          tap(location => (this.samplesTable as SamplingSamplesTable).defaultLocation = location)
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
    this.showSamplesTable = true;
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
    return BiologicalSamplingValidators.addSampleValidators(form, pmfms, this.$pmfmGroups.getValue() || {}, {
      markForCheck: () => this.markForCheck()
    });
  }

}
