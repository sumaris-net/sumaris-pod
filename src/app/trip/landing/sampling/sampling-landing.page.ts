import {ChangeDetectionStrategy, Component, Injector} from '@angular/core';
import {FormGroup} from "@angular/forms";
import {BehaviorSubject, Subscription} from "rxjs";
import {DenormalizedPmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {ParameterLabelGroups, PmfmIds} from "../../../referential/services/model/model.enum";
import {PmfmService} from "../../../referential/services/pmfm.service";
import {ObjectMap} from "../../../shared/types";
import {BiologicalSamplingValidators} from "../../services/validator/biological-sampling.validators";
import {LandingPage} from "../landing.page";
import {Landing} from "../../services/model/landing.model";
import {firstNotNilPromise} from "../../../shared/observables";
import {HistoryPageReference} from "../../../core/services/model/settings.model";
import {fadeInOutAnimation} from "../../../shared/material/material.animations";
import {filter, tap, throttleTime} from "rxjs/operators";
import {isNotNil} from "../../../shared/functions";
import {SamplingSamplesTable} from "../../sample/sampling/sampling-samples.table";
import {EntityServiceLoadOptions} from "../../../shared/services/entity-service.class";
import {ObservedLocation} from "../../services/model/observed-location.model";


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

  constructor(
    injector: Injector
  ) {
    super(injector, {
      pathIdAttribute: 'samplingId',
      autoOpenNextTab: false
    });
    this.pmfmService = injector.get(PmfmService);
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();

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

  markAsLoaded(opts?: { emitEvent?: boolean }) {
    super.markAsLoaded(opts);

    // When data loaded, wait table is ready before show it
    Promise.all([
      firstNotNilPromise(this.$strategy),
      this.samplesTable.ready()
    ])
      .then(() => this.showSamplesTable = true);
  }

  /* -- protected functions -- */

  protected async onNewEntity(data: Landing, options?: EntityServiceLoadOptions): Promise<void> {
    await super.onNewEntity(data, options);
    // By default, set location to parent location
    if (this.parent && this.parent instanceof ObservedLocation) {
      this.landingForm.form.get('location').patchValue(data.location);
    }
  }

  protected async setValue(data: Landing): Promise<void> {
    if (!data) return; // Skip

    this.samplesTable.strategyLabel = data.measurementValues[PmfmIds.STRATEGY_LABEL.toString()];
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
