import {ChangeDetectionStrategy, Component, Injector, Optional} from '@angular/core';
import {AppEditorOptions} from "../../../core/form/editor.class";
import {LandingPage} from "../landing.page";


@Component({
  selector: 'app-biological-sampling-landing-page',
  templateUrl: './biological-sampling-landing.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BiologicalSamplingLandingPage extends LandingPage {

  constructor(
    injector: Injector,
    @Optional() options: AppEditorOptions
  ) {
    super(injector, {
        tabCount: 1,
        pathIdAttribute: 'landingId',
        ...options
      });
  }

}
