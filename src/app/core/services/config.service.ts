import {Inject, Injectable, InjectionToken, Optional} from "@angular/core";
import {FetchPolicy, gql} from "@apollo/client/core";
import {Configuration} from "./model/config.model";
import {Storage} from "@ionic/storage";
import {BehaviorSubject, Observable, Subject, Subscription} from "rxjs";
import {ErrorCodes} from "./errors";
import {GraphqlService} from "../graphql/graphql.service";
import {FormFieldDefinition, FormFieldDefinitionMap} from "../../shared/form/field.model";
import {isNotEmptyArray, isNotNil} from "../../shared/functions";
import {FileService} from "../../shared/file/file.service";
import {NetworkService} from "./network.service";
import {PlatformService} from "./platform.service";
import {CORE_CONFIG_OPTIONS} from "./config/core.config";
import {SoftwareService} from "../../referential/services/software.service";
import {LocationLevelIds, ParameterLabelGroups} from "../../referential/services/model/model.enum";
import {ToastController} from "@ionic/angular";
import {ShowToastOptions, Toasts} from "../../shared/toasts";
import {TranslateService} from "@ngx-translate/core";
import {filter} from "rxjs/operators";
import {EntityServiceLoadOptions} from "../../shared/services/entity-service.class";
import {ENVIRONMENT} from "../../../environments/environment.class";
import {UserProfileLabels} from "./model/person.model";
import {REFERENTIAL_CONFIG_OPTIONS} from "../../referential/services/config/referential.config";


const CONFIGURATION_STORAGE_KEY = "configuration";

/* ------------------------------------
 * GraphQL queries
 * ------------------------------------*/
export const Fragments = {
  config: gql`
    fragment ConfigFragment on ConfigurationVO {
      id
      label
      name
      properties
      smallLogo
      largeLogo
      backgroundImages
      partners {
        id
        label
        name
        logo
        siteUrl
        __typename
      }
      updateDate
      creationDate
      statusId
      __typename
    }
  `
};


const LoadDefaultQuery: any = gql`
query Configuration {
  configuration {
    ...ConfigFragment
  }
}
  ${Fragments.config}
`;

const LoadQuery: any = gql`
query Configuration($id: Int, $label: String) {
  configuration(id: $id, label: $label){
    ...ConfigFragment
  }
}
  ${Fragments.config}
`;

// Save (create or update) account mutation
const SaveMutation: any = gql`
  mutation SaveConfiguration($config:ConfigurationVOInput){
    saveConfiguration(config: $config){
       ...ConfigFragment
    }
  }
  ${Fragments.config}
`;

const ClearCache: any = gql`
  query ClearCache($name: String) {
    clearCache(name: $name)
  }
`;

const CacheStatistics: any = gql`
  query CacheStatistics {
    cacheStatistics
  }
`;

export const APP_CONFIG_OPTIONS = new InjectionToken<FormFieldDefinitionMap>('defaultOptions');

@Injectable({
  providedIn: 'root',
  deps: [APP_CONFIG_OPTIONS]
})
export class ConfigService extends SoftwareService<Configuration> {

  private _started = false;
  private _startPromise: Promise<any>;
  private _subscription = new Subscription();
  private _optionDefs: FormFieldDefinition[];

  private $data = new BehaviorSubject<Configuration>(null);

  get started(): boolean {
    return this._started;
  }

  get config(): Observable<Configuration> {
    // If first call: start loading
    if (!this._started) {
      this.start();
    }

    return this.$data.pipe(filter(isNotNil));
  }

  constructor(
    protected graphql: GraphqlService,
    protected storage: Storage,
    protected network: NetworkService,
    protected platform: PlatformService,
    protected file: FileService,
    protected toastController: ToastController,
    protected translate: TranslateService,
    @Inject(ENVIRONMENT) protected environment,
    @Optional() @Inject(APP_CONFIG_OPTIONS) defaultOptionsMap: FormFieldDefinitionMap
  ) {
    super(graphql, environment);

    this._debug = !environment.production;
    if (this._debug) console.debug("[config] Creating service");

    this._optionDefs = Object.values({...CORE_CONFIG_OPTIONS, ...defaultOptionsMap});

    // Restart if graphql service restart
    this._subscription.add(
      this.graphql.onStart.subscribe(() => this.restart()));


    // Start
    if (this.graphql.started) {
      this.start();
    }


  }

  start(): Promise<void> {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    console.info("[config] Starting configuration...");

    this._startPromise = this.graphql.ready()
      .then(() => this.loadOrRestoreLocally())
      .then(() => {
        this._started = true;
        this._startPromise = undefined;
      })
      .catch((err) => {
        console.error(err && err.message || err, err);
        this._startPromise = undefined;
      });
    return this._startPromise;

  }

  stop() {
    this._subscription.unsubscribe();
    this._subscription = new Subscription();
    this._started = false;
    this._startPromise = undefined;
  }

  restart(): Promise<void> {
    if (this.started) this.stop();
    return this.start();
  }

  ready(): Promise<void> {
    if (this._started) return Promise.resolve();
    if (this._startPromise) return this._startPromise;
    return this.start();
  }

  async loadDefault(
    opts?: {
      fetchPolicy?: FetchPolicy
    }): Promise<Configuration> {
    return this.loadQuery({query: LoadDefaultQuery, ...opts});
  }

  async load(
    id: number,
    opts?: EntityServiceLoadOptions & { label?: string; query?: any }): Promise<Configuration> {

    return this.loadQuery({
      variables: {
        id
      },
      ...opts});
  }

  loadByLabel(
    label: string,
    opts?: EntityServiceLoadOptions): Promise<Configuration> {

    return this.loadQuery({
      variables: {
        label
      },
      ...opts});
  }

  async existsByLabel(label: string): Promise<boolean> {
    const existingConfig = await this.loadByLabel(label, {fetchPolicy: "network-only"});
    return isNotNil(existingConfig && existingConfig.id);
  }

  /**
   * Save a configuration
   * @param config
   */
  async save(config: Configuration): Promise<Configuration> {

    console.debug("[config] Saving configuration...", config);

    const json = config.asObject();

    // Execute mutation
    const res = await this.graphql.mutate<{ saveConfiguration: any }>({
      mutation: SaveMutation,
      variables: {
        config: json
      },
      error: {
        code: ErrorCodes.SAVE_CONFIG_ERROR,
        message: "ERROR.SAVE_CONFIG_ERROR"
      }
    });

    const savedConfig = res && res.saveConfiguration;

    // Copy update properties
    config.id = savedConfig && savedConfig.id || config.id;
    config.updateDate = savedConfig && savedConfig.updateDate || config.updateDate;

    console.debug("[config] Configuration saved!");

    const reloadedConfig = await this.loadByLabel(config.label, {fetchPolicy: "network-only"});

    // If this is the default config
    const defaultConfig = this.$data.getValue();
    if (isNotNil(defaultConfig) && reloadedConfig.label === defaultConfig.label) {

      // Override enumerations
      this.updateModelEnumerations(reloadedConfig);

      // Emit update event when is default config
      this.$data.next(reloadedConfig);
    }

    return reloadedConfig;
  }

  async getCacheStatistics(): Promise<string> {

    const now = Date.now();
    console.debug("[config] Loading server cache statistics...");

    const res = await this.graphql.query<{ cacheStatistics: any }>({
      query: CacheStatistics,
      variables: null,
      error: {code: ErrorCodes.LOAD_CONFIG_ERROR, message: "ERROR.LOAD_CONFIG_ERROR"},
      fetchPolicy: "network-only"
    });

    const data = res && res.cacheStatistics || undefined;
    console.info(`[config] Server cache statistics loaded in ${Date.now() - now}ms:`, data);
    return data;
  }

  async clearCache(opts?: {cacheName?: string}) {
    const now = Date.now();
    console.debug("[config] Clear server cache...");

    const variables = {name: opts && opts.cacheName} || undefined;

    const res = await this.graphql.query<{ clearCache: boolean }>({
      query: ClearCache,
      variables,
      error: {code: ErrorCodes.LOAD_CONFIG_ERROR, message: "ERROR.LOAD_CONFIG_ERROR"},
      fetchPolicy: "network-only"
    });

    const data = res && res.clearCache || undefined;
    console.info(`[config] Clear server cache in ${Date.now() - now}ms:`, data);
    return data;
  }

  delete(data: Configuration, options?: any): Promise<any> {
    throw new Error("Not implemented yet!");
  }

  listenChanges(id: number, options?: any): Observable<Configuration | undefined> {
    // if (this.$data.getValue() && this.$data.getValue().id === id) {
    //   return this.$data;
    // }
    return new Subject(); // TODO
  }

  get optionDefs(): FormFieldDefinition[] {
    return this._optionDefs;
  }

  /* -- protected method -- */

  protected async loadQuery(opts?:
    {
      query?: any,
      variables?: any,
      fetchPolicy?: FetchPolicy
    }): Promise<Configuration> {

    const now = Date.now();
    console.debug("[config] Loading software configuration...");

    const query = opts && opts.query || LoadQuery;
    const variables = opts && opts.variables || undefined;
    const res = await this.graphql.query<{ configuration: Configuration }>({
      query,
      variables,
      error: {code: ErrorCodes.LOAD_CONFIG_ERROR, message: "ERROR.LOAD_CONFIG_ERROR"},
      fetchPolicy: opts && opts.fetchPolicy || undefined/*default*/
    });

    const data = res && res.configuration ? Configuration.fromObject(res.configuration) : undefined;
    console.info(`[config] Software configuration loaded in ${Date.now() - now}ms:`, data);
    return data;
  }

  private async loadOrRestoreLocally() {
    let data;
    let wasJustLoaded = false;
    try {
      data = await this.loadDefault({ fetchPolicy: "network-only" });
      wasJustLoaded = true;
    } catch (err) {
      // Log, then continue
      console.error(err && err.message || err, err);
    }

    // Save it into local storage, for next startup
    if (data) {
      setTimeout(() => this.storeLocally(data), 1000);
    }

    // If not loaded remotely: try to restore it
    else {
      data = await this.restoreLocally();
    }

    // Make sure label has been filled
    data.label = data.label || this.environment.name;

    // Reset name (if same as label)
    data.name = (data.name !== data.label) ? data.name : undefined;

    // Override enumerations
    this.updateModelEnumerations(data);

    // Check compatible version
    if (wasJustLoaded) {
      // TODO
    }

    this.$data.next(data);

  }

  private async restoreLocally(): Promise<Configuration> {
    let data: Configuration;

    // Try to load from local storage
    const value: any = await this.storage.get(CONFIGURATION_STORAGE_KEY);
    if (value) {
      console.debug("[config] Restoring configuration from local storage...");
      if (typeof value === "string") {
        try {
          data = Configuration.fromObject(JSON.parse(value));
        } catch (err) {
          console.error(`Failed to parse config found in local storage: ${err && err.message || err}`, err);
        }
      }
      else if (typeof value === "object") {
        data = Configuration.fromObject(value);
      }

      console.debug("[config] Restoring configuration [OK]");
    }

    // Or load default value, from the environment
    if (!data) {
      console.debug("[config] No configuration found. Using environment...");
      data = Configuration.fromObject(this.environment as any);
    }

    return data;
  }

  private async storeLocally(data?: Configuration) {
    // Nothing to store : reset
    if (!data) {
      await this.storage.remove(CONFIGURATION_STORAGE_KEY);
    }
    // Config exists: store it in the local storage
    else {
      let now = this._debug && Date.now();

      // Convert images, for offline usage
      if (this.network.online && this.platform.mobile) {
        const jobs = [];

        // Download logos
        if (data.largeLogo && data.largeLogo.startsWith('http')) {
          jobs.push(this.file.getImage(data.largeLogo)
            .then(imgUrl => data.largeLogo = imgUrl));
        }
        if (data.smallLogo && data.smallLogo.startsWith('http')) {
          jobs.push(this.file.getImage(data.smallLogo)
            .then(imgUrl => data.smallLogo = imgUrl));
        }

        // Background images
        if (isNotEmptyArray(data.backgroundImages)) {
          const options = {
            maxWidth: this.platform.width(),
            maxHeight: this.platform.height()
          };

          // Convert the FIRST image found
          // WARN: it's NOT necessary to convert AL image, but only one, for smaller memory footprint
          const index = data.backgroundImages.findIndex((img) => img && img.startsWith('http'));
          if (index !== -1) {
            data.backgroundImages = data.backgroundImages.slice(); // Copy
            jobs.push(
              this.file.getImage(data.backgroundImages[index], options)
                .then(dataUrl => data.backgroundImages[index] = dataUrl));
            }
        }

        // Partners
        if (isNotEmptyArray(data.partners)) {
          data.partners.forEach((dep, index) => {
            if (dep && dep.logo && dep.logo.startsWith('http')) {
              jobs.push(this.file.getImage(dep.logo, {
                maxHeight: 50/*see home page CSS */
              })
              .then(img => dep.logo = img)
              .catch(err => {
                console.error(err && err.message || err);
                delete dep.logo;
              }));
            }
          });
        }

        if (jobs.length) {
          if (this._debug) console.debug(`[config] Fetching ${jobs.length} images...`);
          try {
            await Promise.all(jobs);
            if (this._debug) console.debug(`[config] Fetching ${jobs.length} images [OK] in ${Date.now() - now}ms`);
          }
          catch (err) {
            console.error(`[config] Failed to fetch image(s): ${err && err.message || err}`, err);
          }
        }
      }

      // Saving config to storage
      {
        now = this._debug && Date.now();
        if (this._debug) console.debug("[config] Saving config into local storage...");
        await this.storage.set(CONFIGURATION_STORAGE_KEY, data.asObject());
        if (this._debug) console.debug(`[config] Saving config into local storage [OK] in ${Date.now() - now}ms`);
      }
    }
  }

  private updateModelEnumerations(config: Configuration) {
    if (!config.properties) {
      console.warn("[config] No properties found in pod config! Skip model enumerations update");
      return;
    }
    console.info("[config] Updating model enumerations...");

    // User profiles
    UserProfileLabels.ADMIN = config.getProperty(CORE_CONFIG_OPTIONS.PROFILE_ADMIN_LABEL);
    UserProfileLabels.SUPERVISOR = config.getProperty(CORE_CONFIG_OPTIONS.PROFILE_SUPERVISOR_LABEL);
    UserProfileLabels.USER = config.getProperty(CORE_CONFIG_OPTIONS.PROFILE_USER_LABEL);

    // Location Levels
    LocationLevelIds.COUNTRY = config.getProperty(REFERENTIAL_CONFIG_OPTIONS.LOCATION_LEVEL_COUNTRY_ID);
    LocationLevelIds.PORT = config.getProperty(REFERENTIAL_CONFIG_OPTIONS.LOCATION_LEVEL_PORT_ID);
    LocationLevelIds.AUCTION = config.getProperty(REFERENTIAL_CONFIG_OPTIONS.LOCATION_LEVEL_AUCTION_ID);
    LocationLevelIds.ICES_RECTANGLE = config.getProperty(REFERENTIAL_CONFIG_OPTIONS.LOCATION_LEVEL_ICES_RECTANGLE_ID);
    LocationLevelIds.ICES_DIVISION = config.getProperty(REFERENTIAL_CONFIG_OPTIONS.LOCATION_LEVEL_ICES_DIVISION_ID);

    // Parameters
    ParameterLabelGroups.AGE = config.getProperty(REFERENTIAL_CONFIG_OPTIONS.STRATEGY_PARAMETER_AGE_LABEL);
    ParameterLabelGroups.SEX = config.getProperty(REFERENTIAL_CONFIG_OPTIONS.STRATEGY_PARAMETER_SEX_LABEL);
    ParameterLabelGroups.WEIGHT = config.getPropertyAsStrings(REFERENTIAL_CONFIG_OPTIONS.STRATEGY_PARAMETER_WEIGHT_LABELS);
    ParameterLabelGroups.LENGTH = config.getPropertyAsStrings(REFERENTIAL_CONFIG_OPTIONS.STRATEGY_PARAMETER_LENGTH_LABELS);
    ParameterLabelGroups.MATURITY = config.getPropertyAsStrings(REFERENTIAL_CONFIG_OPTIONS.STRATEGY_PARAMETER_MATURITY_LABELS);

    // Taxon group
    // TODO: add all enumerations
    //TaxonGroupIds.FAO =
  }

  protected async showToast(opts: ShowToastOptions) {
    await Toasts.show(this.toastController, this.translate, opts);
  }
}


