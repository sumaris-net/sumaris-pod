import {ChangeDetectionStrategy, Component, Injector, Optional} from '@angular/core';
import {FormGroup} from "@angular/forms";
import {BehaviorSubject, Subscription} from "rxjs";
import {PmfmStrategy} from "../../../referential/services/model/pmfm-strategy.model";
import {Program} from "../../../referential/services/model/program.model";
import {ParameterLabelGroups} from "../../../referential/services/model/model.enum";
import {PmfmService} from "../../../referential/services/pmfm.service";
import {ObjectMap} from "../../../shared/types";
import {BiologicalSamplingValidators} from "../../services/validator/biological-sampling.validators";
import {AppEditorOptions} from "../../../core/form/editor.class";
import {LandingPage} from "../landing.page";

const DEFAULT_I18N_PREFIX = 'LANDING.EDIT.';

@Component({
  selector: 'app-sampling-landing-page',
  templateUrl: './sampling-landing.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SamplingLandingPage extends LandingPage {

  protected pmfmService: PmfmService;

  $pmfmGroups = new BehaviorSubject<ObjectMap<number[]>>(null);

  constructor(
    injector: Injector,
    @Optional() options: AppEditorOptions
  ) {
    super(injector, {
      pathIdAttribute: 'samplingId',
      tabCount: 2,
      autoUpdateRoute: false,
      autoOpenNextTab: false,
      ...options
    });
    this.pmfmService = injector.get(PmfmService);
  }

  protected async setProgram(program: Program) {
    if (!program) return; // Skip

    super.setProgram(program);

    this.samplesTable.program = program.label;

    // TODO: load pmfm map by program properties ?
    // Load Pmfm IDS, group by parameter labels
    const pmfmGroups = await this.pmfmService.loadIdsGroupByParameterLabels(ParameterLabelGroups);
    this.$pmfmGroups.next(pmfmGroups);

  }

  protected computePageUrl(id: number|'new') {
    const parentUrl = this.getParentPageUrl();
    return `${parentUrl}/sampling/${id}`;
  }

  /*async updateRoute(data: Landing, opts?: { openTabIndex?: number }): Promise<boolean> {

    if (data && isNotNil(data.id)) {
      await this.router.navigateByUrl(`/observations/${this.parent.id}/sampling/${data.id}`, {
        replaceUrl: true
      });
      return true;
    }
    return false;
  }*/

  protected computeSampleValidator(form: FormGroup, pmfms: PmfmStrategy[]): Subscription {
    return BiologicalSamplingValidators.addSampleValidators(form, pmfms, this.$pmfmGroups.getValue() || {}, {
      markForCheck: () => this.markForCheck()
    });
  }

}
