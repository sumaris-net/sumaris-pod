import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Injector, OnInit} from "@angular/core";
import {ActivatedRouteSnapshot} from "@angular/router";
import {PlatformService} from "../../core/services/platform.service";
import {ConfigService} from "../../core/services/config.service";
import {AccountService} from "../../core/services/account.service";
import {SoftwarePage} from "../../referential/software/software.page";
import {firstNotNilPromise} from "../../shared/observables";
import {SoftwareValidatorService} from "../../referential/services/software.validator";
import {ConfigOptions, Configuration, Department, EntityUtils} from "../../core/services/model";
import {SoftwareService} from "../../referential/services/software.service";
import {isEmptyArray, isNilOrBlank, isNotEmptyArray} from "../../shared/functions";
import {BehaviorSubject} from "rxjs";
import {EditorDataServiceLoadOptions} from "../../shared/services/data-service.class";



@Component({
  selector: 'app-configuration-page',
  templateUrl: './configuration.page.html',
  styleUrls: ['./configuration.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfigurationPage extends SoftwarePage {


  partners = new BehaviorSubject<Department[]>(null);

  constructor(
    protected injector: Injector,
    protected validatorService: SoftwareValidatorService
      ) {
    super(injector,
      validatorService);
    this.dataService = this.configService;

    // default values
    this.defaultBackHref = null;

    //this.debug = !environment.production;
  }

  async load(id?: number, opts?: EditorDataServiceLoadOptions): Promise<void> {

    const config = await firstNotNilPromise(this.configService.config);

    // Force the load of the config
    super.load(config.id, {...opts, fetchPolicy: "network-only"});
  }

  protected setValue(data: Configuration) {
    if (!data) return; // Skip

    const json = data.asObject();
    this.partners.next(json.partners);

    super.setValue(data);
  }

  protected async getJsonValueToSave(): Promise<any> {
    const json = await super.getJsonValueToSave();

    // Re add partners
    json.partners = this.partners.getValue();

    return json;
  }

  async removePartnerAt(index: number){

    const partners = this.partners.getValue();
    const partner = partners && index < partners.length && partners[index];
    if (!partner) return;

    console.debug(`Removing partner department {${partner && partner.label || index}}`);

    partners.splice(index, 1);

    const propertiesAsArray = (this.form.get('properties').value || []);
    const propertyIndex = propertiesAsArray.findIndex(p => p.key === ConfigOptions.HOME_PARTNERS_DEPARTMENTS.key);
    if (propertyIndex === -1) return;

    const propertyControl = this.propertiesFormHelper.at(propertyIndex);
    let propertyValue = propertyControl.get('value').value;
    if (isNilOrBlank(propertyValue)) return;

    let arrayValue = (typeof propertyValue === 'string' ? JSON.parse(propertyValue) : propertyValue) as any[];
    if (isEmptyArray(arrayValue)) return;

    let found = false;
    arrayValue = arrayValue.filter(dep => {
      if (dep && typeof dep === 'string') {
        if (dep.startsWith('department:')) {
          found = found || (dep === ('department:' + partner.id));
          return dep !== ('department:' + partner.id);
        }
        try {
          dep = JSON.parse(dep);
        }
        catch(err){
          // Unknown format: keep it
          return true;
        }
      }
      found = found || (dep.id === partner.id);
      return dep.id !== partner.id;
    });
    if (!found) {
      console.warn("Unable to find partner inside the property value: ", propertyValue);
      return;
    }

    // Update view
    propertyValue = isNotEmptyArray(arrayValue) ? JSON.stringify(arrayValue) : null;
    propertyControl.get("value").setValue(propertyValue);
    this.partners.next(partners);
    this.markAsTouched();

  }
}

