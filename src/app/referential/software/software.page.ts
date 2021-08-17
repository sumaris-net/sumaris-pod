import {ChangeDetectionStrategy, Component, Inject, Injector, Optional} from '@angular/core';
import {APP_CONFIG_OPTIONS, FormFieldDefinitionMap, HistoryPageReference, Software} from '@sumaris-net/ngx-components';
import {SoftwareService} from '../services/software.service';
import {SoftwareValidatorService} from '../services/validator/software.validator';
import {AbstractSoftwarePage} from './abstract-software.page';


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
    this.defaultBackHref = '/referential/list?entity=Software';

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

