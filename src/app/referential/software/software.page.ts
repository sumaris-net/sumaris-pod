import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component, Inject,
  Injector,
  OnInit,
  Optional,
  ViewChild
} from "@angular/core";
import {ActivatedRouteSnapshot} from "@angular/router";
import {AbstractControl, FormArray, FormBuilder, FormGroup} from "@angular/forms";
import {EntityUtils} from '../../core/services/model/entity.model';
import {Software} from '../../core/services/model/config.model';
import {FormArrayHelper} from "../../core/form/form.utils";
import {FormFieldDefinition, FormFieldDefinitionMap, FormFieldValue} from "../../shared/form/field.model";
import {PlatformService} from "../../core/services/platform.service";
import {AppEditor, DataService, isNil} from "../../core/core.module";
import {AccountService} from "../../core/services/account.service";
import {ReferentialForm} from "../form/referential.form";
import {SoftwareService} from "../services/software.service";
import {SoftwareValidatorService} from "../services/validator/software.validator";
import {APP_CONFIG_OPTIONS, ConfigService} from "../../core/services/config.service";
import {EditorDataService} from "../../shared/services/data-service.class";
import {AppEditorOptions} from "../../core/form/editor.class";
import {ConfigOptions} from "../../core/services/config/core.config";
import {AbstractSoftwarePage} from "./abstract-software.page";


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

}

