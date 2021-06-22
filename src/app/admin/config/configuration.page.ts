import {ChangeDetectionStrategy, Component, Inject, Injector, Optional} from "@angular/core";
import {firstNotNilPromise} from "@sumaris-net/ngx-components";
import {SoftwareValidatorService} from "../../referential/services/validator/software.validator";
import {Configuration}  from "@sumaris-net/ngx-components";
import {Department}  from "@sumaris-net/ngx-components";
import {isEmptyArray, isNilOrBlank, isNotEmptyArray} from "@sumaris-net/ngx-components";
import {BehaviorSubject} from "rxjs";
import {EntityServiceLoadOptions} from "@sumaris-net/ngx-components";
import {NetworkService}  from "@sumaris-net/ngx-components";
import {Alerts} from "@sumaris-net/ngx-components";
import {CORE_CONFIG_OPTIONS}  from "@sumaris-net/ngx-components";
import {APP_CONFIG_OPTIONS, ConfigService}  from "@sumaris-net/ngx-components";
import {FormFieldDefinitionMap} from "@sumaris-net/ngx-components";
import {AbstractSoftwarePage} from "../../referential/software/abstract-software.page";
import {HistoryPageReference}  from "@sumaris-net/ngx-components";
import {SoftwareService} from '@app/referential/services/software.service';

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
export class ConfigurationPage extends AbstractSoftwarePage<Configuration, SoftwareService<Configuration>> {

  partners = new BehaviorSubject<Department[]>(null);
  cacheStatistics = new BehaviorSubject<CacheStatistic[]>(null);
  cacheStatisticTotal = new BehaviorSubject<CacheStatistic>(null);

  get config(): Configuration {
    return this.data && (this.data as Configuration) || undefined;
  }

  constructor(
    injector: Injector,
    dataService: SoftwareService,
    validatorService: SoftwareValidatorService,
    public network: NetworkService,
    public configService: ConfigService,
    @Optional() @Inject(APP_CONFIG_OPTIONS) configOptions: FormFieldDefinitionMap,
  ) {
    super(injector,
      Configuration,
      dataService as SoftwareService<Configuration>,
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

    const config = await firstNotNilPromise(this.configService.config);

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
      await this.configService.clearCache({cacheName: cacheName});
      await this.loadCacheStat();
    }
  }

  async loadCacheStat() {
    const value = await this.configService.getCacheStatistics();
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

