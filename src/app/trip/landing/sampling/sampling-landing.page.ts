import {ChangeDetectionStrategy, Component, Injector} from '@angular/core';
import {FormGroup} from "@angular/forms";
import {BehaviorSubject, Subscription} from "rxjs";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {ParameterLabelGroups} from "../../../referential/services/model/model.enum";
import {PmfmService} from "../../../referential/services/pmfm.service";
import {ObjectMap} from "../../../shared/types";
import {BiologicalSamplingValidators} from "../../services/validator/biological-sampling.validators";
import {LandingPage} from "../landing.page";
import {Landing} from "../../services/model/landing.model";
import {filter, first, switchMap, tap} from "rxjs/operators";
import {isNotNil} from "../../../shared/functions";
import {mergeMap} from "rxjs/internal/operators";
import {firstNotNilPromise} from "../../../shared/observables";
import {HistoryPageReference} from "../../../core/services/model/settings.model";


@Component({
  selector: 'app-sampling-landing-page',
  templateUrl: './sampling-landing.page.html',
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

  protected async setValue(data: Landing): Promise<void> {
    if (!data) return; // Skip

    // find all pmfms from samples
    // TODO : force PMFM from data
    /*this.samplesTable.dataPmfms = (data.samples || []).reduce((res, sample) => {
      const pmfmIds = Object.keys(sample.measurementValues || {});
      const newPmfmIds = pmfmIds.filter(pmfmId => !res.includes(pmfmId));
      return res.concat(...newPmfmIds);
    }, []);*/

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

  protected computeSampleRowValidator(form: FormGroup, pmfms: PmfmStrategy[]): Subscription {
    console.debug('[sampling-landing-page] Adding row validator');
    return BiologicalSamplingValidators.addSampleValidators(form, pmfms, this.$pmfmGroups.getValue() || {}, {
      markForCheck: () => this.markForCheck()
    });
  }

}
