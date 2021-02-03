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


@Component({
  selector: 'app-sampling-landing-page',
  templateUrl: './sampling-landing.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingLandingPage extends LandingPage {

  protected pmfmService: PmfmService;

  $pmfmGroups = new BehaviorSubject<ObjectMap<number[]>>(null);

  constructor(
    injector: Injector
  ) {
    super(injector, {
      pathIdAttribute: 'samplingId',
      autoOpenNextTab: false
    });
    this.pmfmService = injector.get(PmfmService);
  }

  ngOnInit() {
    super.ngOnInit();

    // Load Pmfm IDS, group by parameter labels
    this.pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups)
      .then(pmfmGroups => this.$pmfmGroups.next(pmfmGroups));
  }

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

  protected computePageUrl(id: number|'new') {
    const parentUrl = this.getParentPageUrl();
    return `${parentUrl}/sampling/${id}`;
  }

  protected computeSampleRowValidator(form: FormGroup, pmfms: PmfmStrategy[]): Subscription {
    return BiologicalSamplingValidators.addSampleValidators(form, pmfms, this.$pmfmGroups.getValue() || {}, {
      markForCheck: () => this.markForCheck()
    });
  }

}
