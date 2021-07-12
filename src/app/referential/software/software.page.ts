import {ChangeDetectionStrategy, Component, Inject, Injector, Optional} from "@angular/core";
import {Software} from "@sumaris-net/ngx-components";
import {FormFieldDefinitionMap} from "@sumaris-net/ngx-components";
import {SoftwareService} from "../services/software.service";
import {SoftwareValidatorService} from "../services/validator/software.validator";
import {APP_CONFIG_OPTIONS}  from "@sumaris-net/ngx-components";
import {AbstractSoftwarePage} from "./abstract-software.page";
import {HistoryPageReference}  from "@sumaris-net/ngx-components";


@Component({
  selector: 'app-software-page',
  templateUrl: 'software.page.html',
  styleUrls: ['./software.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SoftwarePage extends AbstractSoftwarePage<Software, SoftwareService> {

  constructor(
    injector: Injector,
    dataService: SoftwareService,
    validatorService: SoftwareValidatorService,
    @Optional() @Inject(APP_CONFIG_OPTIONS) configOptions: FormFieldDefinitionMap
    ) {
    super(injector,
      Software,
      dataService,
      validatorService,
      configOptions);

    // default values
    this.defaultBackHref = "/referential/list?entity=Software";

    //this.debug = !environment.production;
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return {
      ...(await super.computePageHistory(title)),
      subtitle: 'REFERENTIAL.ENTITY.SOFTWARE',
      icon: 'server'
    };
  }
}

