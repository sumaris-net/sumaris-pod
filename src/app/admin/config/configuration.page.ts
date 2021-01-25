import {ChangeDetectionStrategy, Component, Inject, Injector, Optional} from "@angular/core";
import {firstNotNilPromise} from "../../shared/observables";
import {SoftwareValidatorService} from "../../referential/services/validator/software.validator";
import {Configuration} from "../../core/services/model/config.model";
import {Department} from "../../core/services/model/department.model";
import {isEmptyArray, isNilOrBlank, isNotEmptyArray} from "../../shared/functions";
import {BehaviorSubject} from "rxjs";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {NetworkService} from "../../core/services/network.service";
import {Alerts} from "../../shared/alerts";
import {CORE_CONFIG_OPTIONS} from "../../core/services/config/core.config";
import {APP_CONFIG_OPTIONS, ConfigService} from "../../core/services/config.service";
import {FormFieldDefinitionMap} from "../../shared/form/field.model";
import {AbstractSoftwarePage} from "../../referential/software/abstract-software.page";
import {HistoryPageReference} from "../../core/services/model/history.model";

declare interface CacheStatistic {
  name: string;
  size: number;
  heapSize: number;
  offHeapSize: number;
  diskSize: number;
}

@Component({
  selector: 'app-configuration-page',
  templateUrl: './configuration.page.html',
  styleUrls: ['./configuration.page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConfigurationPage extends AbstractSoftwarePage<Configuration, ConfigService> {

  partners = new BehaviorSubject<Department[]>(null);
  cacheStatistics = new BehaviorSubject<CacheStatistic[]>(null);
  cacheStatisticTotal = new BehaviorSubject<CacheStatistic>(null);

  get config(): Configuration {
    return this.data && (this.data as Configuration) || undefined;
  }

  constructor(
    injector: Injector,
    dataService: ConfigService,
    validatorService: SoftwareValidatorService,
    public network: NetworkService,
    @Optional() @Inject(APP_CONFIG_OPTIONS) configOptions: FormFieldDefinitionMap,
  ) {
    super(injector,
      Configuration,
      dataService,
      validatorService,
      configOptions,
      {
        tabCount: 2
      });

    // default values
    this.defaultBackHref = null;

    //this.debug = !environment.production;
  }

  async load(id?: number, opts?: EntityServiceLoadOptions): Promise<void> {

    const config = await firstNotNilPromise(this.service.config);

    // Force the load of the config
    await super.load(config.id, {...opts, fetchPolicy: "network-only"});

    this.cacheStatistics.subscribe(value => this.computeStatisticTotal(value));

    // Get server cache statistics
    await this.loadCacheStat();
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

  async removePartnerAt(index: number) {

    const partners = this.partners.getValue();
    const partner = partners && index < partners.length && partners[index];
    if (!partner) return;

    console.debug(`Removing partner department {${partner && partner.label || index}}`);

    partners.splice(index, 1);

    const propertiesAsArray = (this.form.get('properties').value || []);
    const propertyIndex = propertiesAsArray.findIndex(p => p.key === CORE_CONFIG_OPTIONS.HOME_PARTNERS_DEPARTMENTS.key);
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
        } catch (err) {
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

  async clearCache(event?: UIEvent, cacheName?: string) {
    const confirm = await Alerts.askActionConfirmation(this.alertCtrl, this.translate, true, event);
    if (confirm) {
      await this.network.clearCache();
      await this.settings.removeOfflineFeatures();
      await this.service.clearCache({cacheName: cacheName});
      await this.loadCacheStat();
    }
  }

  async loadCacheStat() {
    const value = await this.service.getCacheStatistics();
    const stats: CacheStatistic[] = Object.keys(value).map(cacheName => {
      const stat = value[cacheName];
      return {
        name: cacheName,
        size: stat.size,
        heapSize: stat.heapSize,
        offHeapSize: stat.offHeapSize,
        diskSize: stat.diskSize
      };
    });
    this.cacheStatistics.next(stats);
  }

  computeStatisticTotal(stats: CacheStatistic[]) {
    const total: CacheStatistic = {name: undefined, size: 0, heapSize: 0, offHeapSize: 0, diskSize: 0};
    (stats || []).forEach(stat => {
      total.size += stat.size;
      total.heapSize += stat.heapSize;
      total.offHeapSize += stat.offHeapSize;
      total.diskSize += stat.diskSize;
    });
    this.cacheStatisticTotal.next(total);
  }

  protected async computePageHistory(title: string): Promise<HistoryPageReference> {
    return null; // No page history
  }
}

