import {Injectable} from "@angular/core";
import gql from "graphql-tag";
import {Apollo} from "apollo-angular";
import {BaseDataService} from "./base.data-service.class";
import {Configuration} from "./model";
import {environment} from "../../../environments/environment";
import {Storage} from "@ionic/storage";
import {BehaviorSubject, Observable} from "rxjs";
import {ErrorCodes} from "./errors";
import {filter, first, map} from "rxjs/operators";
import {FetchPolicy} from "apollo-client";


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

const LoadQuery: any = gql`
query Configuration {
  configuration{
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


@Injectable()
export class ConfigService extends BaseDataService {

  private _started = false;
  private _startPromise: Promise<any>;

  private $data = new BehaviorSubject<Configuration>(null);

  get started(): boolean {
    return this._started;
  }

  get config(): Observable<Configuration> {
    // If first call: start loading
    if (!this._started) this.start();
    return this.$data
      .pipe(
        filter(config => config != null)
      );
  }

  constructor(
    protected apollo: Apollo,
    protected storage: Storage
  ) {
    super(apollo);
  }

  async start(): Promise<any> {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    console.debug("[config] Loading configuration (from pod)...");

    this._startPromise = new Promise(async (resolve) => {
      let data;
      try {
        data = await this.load();

        if (data) {
          // Save it into local storage, for next startup
          setTimeout(() => this.saveLocally(data), 500);
        }
      } catch (err) {
        console.error(err && err.message || err);
      }

      // If not loaded remotely: try to restore it
      if (!data) {
        data = await this.restoreLocally();
      }

      // Make sure label has been filled
      data.label = data.label || environment.name;

      // Reset name if same
      data.name = (data.name !== data.label) ? data.name : undefined;

      this.$data.next(data);
      this._started = true;
      this._startPromise = undefined;

      resolve();
    });
    return this._startPromise;

  }

  async load(options?: {
    fetchPolicy?: FetchPolicy
  }): Promise<Configuration> {

    console.debug("[config] Loading pod configuration...");

    const res = await this.query<{ configuration: Configuration }>({
      query: LoadQuery,
      variables: {},
      error: {code: ErrorCodes.LOAD_CONFIG_ERROR, message: "ERROR.LOAD_CONFIG_ERROR"},
      fetchPolicy: options && options.fetchPolicy || undefined
    });

    const data = res && res.configuration && Configuration.fromObject(res && res.configuration);
    console.info("[config] Configuration loaded (from pod): ", data);
    return data;
  }

  /**
   * Save a configuration
   * @param config
   */
  async save(config: Configuration): Promise<Configuration> {

    console.debug("[config] Saving configuration...", config);

    const json = config.asObject();

    // Execute mutation
    const res = await this.mutate<{ saveConfiguration: any }>({
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

    const reloadedConfig = await this.load({fetchPolicy: "network-only"});
    this.$data.next(reloadedConfig); // emit

    return reloadedConfig;
  }

  /* -- private method -- */

  private async restoreLocally(): Promise<Configuration> {
    let data: Configuration;

    // Try to load from local storage
    const value: any = await this.storage.get(CONFIGURATION_STORAGE_KEY);
    if (value && true) {
      try {
        console.debug("[config] Restoring configuration (from local storage)...");

        const json = JSON.parse(value);
        data = Configuration.fromObject(json as any);
      } catch (err) {
        console.error(err && err.message || err);
      }
    }

    // Or load default value, from the environment
    if (!data) {
      console.debug("[config] No configuration found. Using environment...");
      data = Configuration.fromObject(environment as any);
    }

    return data;
  }

  private async saveLocally(data?: Configuration) {
    // Nothing to store : reset
    if (!data) {
      await this.storage.remove(CONFIGURATION_STORAGE_KEY);
    }
    // Config exists: store it in the local storage
    else {
      const jsonStr = JSON.stringify(data);
      await this.storage.set(CONFIGURATION_STORAGE_KEY, jsonStr);
    }
  }

}


