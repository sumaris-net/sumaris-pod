import {Observable, of, Subject, Subscription} from "rxjs";
import {Apollo} from "apollo-angular";
import {ApolloClient, ApolloQueryResult, FetchPolicy, MutationUpdaterFn, WatchQueryFetchPolicy} from "apollo-client";
import {R} from "apollo-angular/types";
import {ErrorCodes, ServerErrorCodes, ServiceError} from "./errors";
import {catchError, distinctUntilChanged, filter, first, map, mergeMap} from "rxjs/operators";

import {environment} from '../../../environments/environment';
import {Injectable} from "@angular/core";
import {HttpLink, Options} from "apollo-angular-link-http";
import {ConnectionType, NetworkService} from "./network.service";
import {WebSocketLink} from "apollo-link-ws";
import {ApolloLink} from "apollo-link";
import {InMemoryCache} from "apollo-cache-inmemory";
import {AppWebSocket, createTrackerLink, dataIdFromObject, restoreTrackedQueries} from "../graphql/graphql.utils";
import {getMainDefinition} from 'apollo-utilities';
import {persistCache} from "apollo-cache-persist";
import {Storage} from "@ionic/storage";
import {RetryLink} from 'apollo-link-retry';
import QueueLink from 'apollo-link-queue';
import SerializingLink from 'apollo-link-serialize';
import loggerLink from 'apollo-link-logger';
import {Platform} from "@ionic/angular";
import {EntityUtils, IEntity} from "./model/entity.model";
import {DataProxy} from 'apollo-cache';
import {isNotNil, toNumber} from "../../shared/functions";
import {Resolvers} from "apollo-client/core/types";

export interface WatchQueryOptions<V> {
  query: any,
  variables: V,
  error?: ServiceError,
  fetchPolicy?: WatchQueryFetchPolicy
}

export interface MutateQueryOptions<T, V = R> {
  mutation: any;
  variables: V;
  error?: ServiceError;
  context?: {
    serializationKey?: string;
    tracked?: boolean;
  };
  optimisticResponse?: T;
  offlineResponse?: T | ((context: any) => Promise<T>);
  update?: MutationUpdaterFn<T>;
  forceOffline?: boolean;
}

@Injectable({providedIn: 'root'})
export class GraphqlService {

  private _started = false;
  private _startPromise: Promise<any>;
  private _subscription = new Subscription();
  private _$networkStatusChanged: Observable<ConnectionType>;

  private httpParams: Options;
  private wsParams;
  private wsConnectionParams: { authToken?: string } = {};
  private readonly _defaultFetchPolicy: WatchQueryFetchPolicy;

  public onStart = new Subject<void>();

  protected _debug = false;

  get started(): boolean {
    return this._started;
  }

  constructor(
    private platform: Platform,
    private apollo: Apollo,
    private httpLink: HttpLink,
    private network: NetworkService,
    private storage: Storage
  ) {

    this._defaultFetchPolicy = environment.apolloFetchPolicy;

    // Start
    if (this.network.started) {
      this.start();
    }

    // Restart if network restart
    this.network.onStart.subscribe(() => this.restart());

    // Clear cache
    this.network.onResetNetworkCache
      .pipe(
        mergeMap(() => this.ready())
      )
      .subscribe(() => this.clearCache());

    // Listen network status
    this._$networkStatusChanged = network.onNetworkStatusChanges
      .pipe(
        filter(isNotNil),
        distinctUntilChanged()
      );

    this._debug = !environment.production;
  }

  ready(): Promise<void> {
    if (this._started) return Promise.resolve();
    if (this._startPromise) return this._startPromise;
    return this.start();
  }

  start(): Promise<void> {
    if (this._startPromise) return this._startPromise;
    if (this._started) return;

    console.info("[graphql] Starting graphql...");

    // Waiting for network service
    this._startPromise = this.network.ready()
      .then(() => this.initApollo())
      .then(() => {
        this._started = true;
        this._startPromise = undefined;

        // Emit event
        this.onStart.next();

        console.info("[graphql] Starting graphql [OK]");
      })
      .catch((err) => {
        console.error(err && err.message || err, err);
        this._startPromise = undefined;
      });
    return this._startPromise;
  }

  setAuthToken(authToken: string) {
    if (authToken) {
      console.debug("[graphql] Apply authentication token to headers");
      this.wsConnectionParams.authToken = authToken;
    } else {
      console.debug("[graphql] Remove authentication token from headers");
      delete this.wsConnectionParams.authToken;
      // Clear cache
      this.clearCache();
    }
  }

  /**
   * Allow to add a field resolver
   *  (see doc: https://www.apollographql.com/docs/react/data/local-state/#handling-client-fields-with-resolvers)
   * @param resolvers
   */
  async addResolver(resolvers: Resolvers | Resolvers[]) {
    if (!this._started) {
      this.onStart.toPromise().then(() => this.addResolver(resolvers)); // Loop
    }
    const client = this.apollo.getClient();
    client.addResolvers(resolvers);
  }

  async query<T, V = R>(opts: {
    query: any,
    variables: V,
    error?: ServiceError,
    fetchPolicy?: FetchPolicy
  }): Promise<T> {
    let res;
    try {
      res = await (await this.getApollo()).query<ApolloQueryResult<T>, V>({
        query: opts.query,
        variables: opts.variables,
        fetchPolicy: opts.fetchPolicy || (this._defaultFetchPolicy as FetchPolicy) || undefined
      }).toPromise();
    } catch (err) {
      res = this.toApolloError<T>(err, opts.error);
    }
    if (res.errors) {
      throw res.errors[0];
    }
    return res.data;
  }

  watchQuery<T, V = R>(opts: WatchQueryOptions<V>): Observable<T> {
    return this.apollo.watchQuery<T, V>({
      query: opts.query,
      variables: opts.variables,
      fetchPolicy: opts.fetchPolicy || (this._defaultFetchPolicy as FetchPolicy) || undefined,
      notifyOnNetworkStatusChange: true
    })
      .valueChanges
      .pipe(
        catchError(error => this.onApolloError<T>(error, opts.error)),
        map(({data, errors}) => {
          if (errors) {
            throw errors[0];
          }
          return data;
        })
      );
  }

  async mutate<T, V = R>(opts: MutateQueryOptions<T, V>): Promise<T> {

    // If offline, compute an optimistic response for tracked queries
    if ((opts.forceOffline || this.network.offline) && opts.offlineResponse) {
      if (typeof opts.offlineResponse === 'function') {
        opts.context = opts.context || {};
        const optimisticResponseFn = (opts.offlineResponse as ((context: any) => Promise<T>));
        opts.optimisticResponse = await optimisticResponseFn(opts.context);
        if (this._debug) console.debug("[graphql] [offline] Using an optimistic response: ", opts.optimisticResponse);
      }
      else {
        opts.optimisticResponse = opts.offlineResponse as T;
      }
      if (opts.forceOffline) {
        const res = {data: opts.optimisticResponse};
        if (opts.update) {
          opts.update(this.apollo.getClient(), res);
        }
        return res.data;
      }
    }

    const res = await this.apollo.mutate<ApolloQueryResult<T>, V>({
        mutation: opts.mutation,
        variables: opts.variables,
        context: opts.context,
        optimisticResponse: opts.optimisticResponse as any,
        update: opts.update as any
      })
        .pipe(
          catchError(error => this.onApolloError<T>(error, opts.error)),
          first(),
          // To debug, if need:
          //tap((res) => (!res) && console.error('[graphql] Unknown error during mutation. Check errors in console (may be an invalid generated cache id ?)'))
        ).toPromise();
    if (res.errors instanceof Array) {
      throw res.errors[0];
    }
    return res.data as T;
  }

  subscribe<T, V = R>(opts: {
    query: any,
    variables: V,
    error?: ServiceError
  }): Observable<T> {

    return this.apollo.subscribe<T>({
      query: opts.query,
      variables: opts.variables
    }, {
      useZone: true
    })
      .pipe(
        catchError(error => this.onApolloError<T>(error, opts.error)),
        map(({data, errors}) => {
          if (errors) {
            throw errors[0];
          }
          return data;
        })
      );
  }

  insertIntoQueryCache<T, V = R>(proxy: DataProxy,
                                 opts: DataProxy.Query<V> & {
                                   arrayFieldName: string;
                                   totalFieldName?: string;
                                   data: T
                                 }) {

    proxy = proxy || this.apollo.getClient();
    opts.arrayFieldName = opts.arrayFieldName || 'data';

    try {
      const res = proxy.readQuery<any, V>(opts);

      if (res && res[opts.arrayFieldName]) {
        // Append to result array
        res[opts.arrayFieldName].push(opts.data);

        // Increment total
        if (isNotNil(opts.totalFieldName)) {
          if (res[opts.totalFieldName]) {
            res[opts.totalFieldName] += 1;
          }
          else {
            console.warn('[graphql] Unable to update cached query. Unknown result part: ' + opts.totalFieldName);
          }
        }

        proxy.writeQuery<T[], V>({
          query: opts.query,
          variables: opts.variables,
          data: res
        });
      }
      else {
        console.warn('[graphql] Unable to update cached query. Unknown result part: ' + opts.arrayFieldName);
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
      if (this._debug) console.error("[graphql] Error while updating cache: ", err);
    }
  }

  addManyToQueryCache<T = any, V = R>(proxy: DataProxy,
                             opts: DataProxy.Query<V> & {
                               arrayFieldName: string;
                               totalFieldName?: string;
                               data: T[];
                               equalsFn?: (d1: T, d2: T) => boolean;
                               sortFn?: (d1: T, d2: T) => number;
                             }) {

    if (!opts.data || !opts.data.length) return; // nothing to process

    proxy = proxy || this.apollo.getClient();
    opts.arrayFieldName = opts.arrayFieldName || 'data';

    try {
      const res = proxy.readQuery(opts);

      if (res && res[opts.arrayFieldName]) {
        // Keep only not existing res
        const equalsFn = opts.equalsFn || ((d1, d2) => d1['id'] === d2['id'] && d1['entityName'] === d2['entityName']);
        let data = opts.data.filter(inputValue => res[opts.arrayFieldName].findIndex(existingValue => equalsFn(inputValue, existingValue)) === -1);

        if (!data.length) return; // No new value

        // Append to result array
        res[opts.arrayFieldName] = res[opts.arrayFieldName].concat(data);

        // Resort, if need
        if (opts.sortFn) {
          res[opts.arrayFieldName].sort(opts.sortFn);
        }

        // Exclude if exceed max size
        const size = toNumber(opts.variables && opts.variables['size'], -1);
        if (size > 0 && data.length > size) {
          res[opts.arrayFieldName].splice(size, data.length - size);
        }

        // Increment the total
        if (isNotNil(opts.totalFieldName)) {
          if (res[opts.totalFieldName]) {
            res[opts.totalFieldName] += data.length;
          }
          else {
            console.warn('[graphql] Unable to update cached query. Unknown result part: ' + opts.totalFieldName);
          }
        }

        // Write to cache
        proxy.writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: res
        });
      }
      else {
        console.warn('[graphql] Unable to update cached query. Unknown result part: ' + opts.arrayFieldName);
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
      if (this._debug) console.warn("[graphql] Error while updating cache: ", err);
    }
  }

  removeFromCachedQueryById<V = R>(proxy: DataProxy,
                                   opts: DataProxy.Query<V> & {
                                     arrayFieldName: string;
                                     totalFieldName?: string;
                                     id: number
                                   }) {

    proxy = proxy || this.apollo.getClient();
    opts.arrayFieldName = opts.arrayFieldName || 'data';

    try {
      const res = proxy.readQuery(opts);

      if (res && res[opts.arrayFieldName]) {

        const index = res[opts.arrayFieldName].findIndex(item => item['id'] === opts.id);
        if (index === -1) return; // Skip (nothing removed)

        // Remove the item
        res[opts.arrayFieldName].splice(index, 1);

        // Increment the total
        if (isNotNil(opts.totalFieldName)) {
          if (res[opts.totalFieldName]) {
            res[opts.totalFieldName] -= 1;
          }
          else {
            console.warn('[graphql] Unable to update cached query. Unknown result part: ' + opts.totalFieldName);
          }
        }

        // Write to cache
        proxy.writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: res
        });
      }
      else {
        console.warn('[graphql] Unable to update cached query. Unknown result part: ' + opts.arrayFieldName);
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
      if (this._debug) console.warn("[graphql] Error while removing from cache: ", err);
    }
  }

  removeFromCachedQueryByIds<V = R>(proxy: DataProxy,
                                    opts: DataProxy.Query<V> & {
                                      arrayFieldName: string;
                                      totalFieldName?: string;
                                      ids: number[]
                                    }) {

    proxy = proxy || this.apollo.getClient();
    opts.arrayFieldName = opts.arrayFieldName || 'data';

    try {
      const res = proxy.readQuery(opts);

      if (res && res[opts.arrayFieldName]) {

        const newArray = res[opts.arrayFieldName].reduce((result: any[], item: any) => {
          return opts.ids.includes(item['id']) ?
              // Remove it
            result :
            // Or keep it
            result.concat(item);
        }, []);

        const deleteCount = res[opts.arrayFieldName].length - newArray.length;
        if (deleteCount <= 0) return; // Skip (nothing removed)

        res[opts.arrayFieldName] = newArray;

        // Increment the total
        if (isNotNil(opts.totalFieldName)) {
          if (res[opts.totalFieldName]) {
            res[opts.totalFieldName] -= deleteCount; // Remove deletion count
          }
          else {
            console.warn('[graphql] Unable to update cached query. Unknown result part: ' + opts.totalFieldName);
          }
        }

        proxy.writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: res
        });
      }
      else {
        console.warn('[graphql] Unable to update cached query. Unknown result part: ' + opts.arrayFieldName);
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
      if (this._debug) console.warn("[graphql] Error while removing from cache: ", err);
    }
  }

  updateToQueryCache<T extends IEntity<any>, V = R>(proxy: DataProxy,
                      opts:DataProxy.Query<V> & {
                        arrayFieldName: string;
                        totalFieldName?: string;
                        data: any,
                        equalsFn?: (d1: T, d2: T) => boolean
                      }) {
    proxy = proxy || this.apollo.getClient();
    opts.arrayFieldName = opts.arrayFieldName || 'data';

    try {
      const res = proxy.readQuery(opts);

      if (res && res[opts.arrayFieldName]) {
        const equalsFn = opts.equalsFn || ((d1,d2) => EntityUtils.equals(d1, d2, 'id'));
        const index = res[opts.arrayFieldName].findIndex(v => equalsFn(opts.data, v));
        if (index !== -1) {
          res[opts.arrayFieldName].splice(index, 1, opts.data);
        }
        else {
          res[opts.arrayFieldName].push(opts.data);

          // Increment the total
          if (isNotNil(opts.totalFieldName)) {
            if (res[opts.totalFieldName]) {
              res[opts.totalFieldName] += 1;
            }
            else {
              console.warn('[graphql] Unable to update cached query. Unknown result part: ' + opts.totalFieldName);
            }
          }
        }

        proxy.writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: res
        });
        return; // OK: stop here
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
      if (this._debug) console.warn("[graphql] Error while updating cache: ", err);
    }
  }

  async clearCache(client?: ApolloClient<any>): Promise<void> {
    client = client || this.apollo.getClient();
    if (client) {
      let now = this._debug && Date.now();
      console.info("[graphql] Clearing Apollo client's cache... ");
      await client.cache.reset();
      if (this._debug) console.debug(`[graphql] Apollo client's cache cleared, in ${Date.now() - now}ms`);
    }
  }

  /* -- protected methods -- */

  protected async initApollo() {

    const peer = this.network.peer;
    if (!peer) throw Error("[graphql] Missing peer. Unable to start graphql service");

    const uri = peer.url + '/graphql';
    const wsUri = String.prototype.replace.call(uri, /^http(s)?:/, "ws$1:") + '/websocket';
    console.info("[graphql] Base uri: " + uri);
    console.info("[graphql] Subscription uri: " + wsUri);

    this.httpParams = this.httpParams || {};
    this.httpParams.uri = uri;

    this.wsParams = this.wsParams || {
      options: {
        lazy: true,
        reconnect: true,
        connectionParams: this.wsConnectionParams,
        addTypename: true
      },
      webSocketImpl: AppWebSocket
    };
    this.wsParams.uri = wsUri;

    // Create a storage configuration
    const storage = {
      getItem: (key: string) => this.storage.get(key),
      setItem: (key: string, data: any) => this.storage.set(key, data),
      removeItem: (key: string) => this.storage.remove(key)
    };

    let client = this.apollo.getClient();
    if (!client) {
      console.debug("[apollo] Creating GraphQL client...");

      // Websocket link
      const wsLink = new WebSocketLink(this.wsParams);

      // Creating a mutation queue
      const queueLink = new QueueLink();
      this._subscription.add(this._$networkStatusChanged
        .subscribe(type => {
          // Network is offline: start buffering into queue
          if (type === 'none') {
            console.info("[graphql] offline mode: enable mutations buffer");
            queueLink.close();
          }
          // Network is online
          else {
            console.info("[graphql] online mode: disable mutations buffer");
            queueLink.open();
          }
        }));

      const serializingLink = new SerializingLink();
      const retryLink = new RetryLink();
      const trackerLink = createTrackerLink({
        storage,
        onNetworkStatusChange: this._$networkStatusChanged,
        debounce: 1000,
        debug: true
      });

      const authLink = new ApolloLink((operation, forward) => {

        // Use the setContext method to set the HTTP headers.
        operation.setContext({
          ...operation.getContext(),
          ...{
            headers: {
              authorization: this.wsConnectionParams.authToken ? `token ${this.wsConnectionParams.authToken}` : ''
            }
          }
        });

        // Call the next link in the middleware chain.
        return forward(operation);
      });

      // Http link
      const httpLink = this.httpLink.create(this.httpParams);

      const cache = new InMemoryCache({
        dataIdFromObject
      });

      // Enable cache persistence
      if ((environment.offline || this.platform.is('mobile')) && environment.persistCache) {
        console.debug("[graphql] Starting persistence cache...");
        await persistCache({
          cache,
          storage,
          trigger: this.platform.is("android") ? "background" : "write",
          debounce: 1000,
          debug: true
        });
      }

      // create Apollo
      this.apollo.create({
        link:
          ApolloLink.split(
            ({query}) => {
              const def = getMainDefinition(query);
              return def.kind === 'OperationDefinition' && def.operation === 'mutation';
            },

            // Handle mutations (with offline queue)
            ApolloLink.from([
              loggerLink,
              trackerLink,
              queueLink,
              serializingLink,
              retryLink,
              authLink,
              httpLink
            ]),

            ApolloLink.split(
              (operation) => {
                const def = getMainDefinition(operation.query);
                return def.kind === 'OperationDefinition' && def.operation === 'subscription';
              },

              // Handle subscriptions
              wsLink,

              // Handle queries
              ApolloLink.from([
                //loggerLink,
                retryLink,
                authLink,
                httpLink
              ])
            )
          ),
        cache,
        connectToDevTools: !environment.production
      }, 'default');

      client = this.apollo.getClient();
    }

    // Enable tracked queries persistence
    if ((environment.offline || this.platform.is('mobile')) && environment.persistCache) {

      try {
        await restoreTrackedQueries({
          apolloClient: client,
          storage,
          debug: true
        });
      } catch(err) {
        console.error('[graphql] Failed to restore tracked queries from storage: ' + (err && err.message || err), err);
      }
    }
  }

  protected async stop() {
    console.info('[graphql] Stopping graphql service...');
    this._subscription.unsubscribe();
    this._subscription = new Subscription();
    await this.resetClient();
    this._started = false;
    this._startPromise = undefined;
  }

  protected async restart(): Promise<void> {
    if (this.started) {
      return this.stop().then(() => this.start());
    }
    return this.start();
  }


  protected async resetClient(client?: ApolloClient<any>) {
    client = client || this.apollo.getClient();
    if (!client) return;

    console.info("[graphql] Resetting apollo client...");
    client.stop();
    await Promise.all([
      client.clearStore(),
      this.clearCache(client)
    ]);
  }

  private onApolloError<T>(err: any, defaultError?: any): Observable<ApolloQueryResult<T>> {
    return of(this.toApolloError(err, defaultError));
  }

  private toApolloError<T>(err: any, defaultError?: any): ApolloQueryResult<T> {
    let error = (err.networkError && (this.toAppError(err.networkError) || this.createAppErrorByCode(ErrorCodes.UNKNOWN_NETWORK_ERROR))) ||
      (err.graphQLErrors && err.graphQLErrors.length && this.toAppError(err.graphQLErrors[0])) ||
      this.toAppError(err) ||
      this.toAppError(err.originalError) ||
      (err.graphQLErrors && err.graphQLErrors[0]) ||
      err;
    console.error("[graphql] " + (error && error.message || error), error.stack || '');
    if (error && error.code === ErrorCodes.UNKNOWN_NETWORK_ERROR && err.networkError && err.networkError.message)
      console.error("[graphql] original error: " + err.networkError.message);
    if ((!error || !error.code) && defaultError) {
      error = {...defaultError, details: error, stack: err.stack};
    }
    return {
      data: null,
      errors: [error],
      loading: false,
      networkStatus: null,
      stale: null
    };
  }

  private createAppErrorByCode(errorCode: number): any | undefined {
    const message = this.getI18nErrorMessageByCode(errorCode);
    if (message) return {
      code: errorCode,
      message: this.getI18nErrorMessageByCode(errorCode)
    };
    return undefined;
  }

  private getI18nErrorMessageByCode(errorCode: number): string | undefined {
    switch (errorCode) {
      case ServerErrorCodes.UNAUTHORIZED:
        return "ERROR.UNAUTHORIZED";
      case ServerErrorCodes.FORBIDDEN:
        return "ERROR.FORBIDDEN";
      case ErrorCodes.UNKNOWN_NETWORK_ERROR:
        return "ERROR.UNKNOWN_NETWORK_ERROR";
      case ServerErrorCodes.BAD_UPDATE_DATE:
        return "ERROR.BAD_UPDATE_DATE";
      case ServerErrorCodes.DATA_LOCKED:
        return "ERROR.DATA_LOCKED";
    }

    return undefined;
  }

  private toAppError(err: any): any | undefined {
    const message = err && err.message || err;
    if (typeof message === "string" && message.trim().indexOf('{"code":') === 0) {
      const error = JSON.parse(message);
      return error && this.createAppErrorByCode(error.code) || error && error.message || err;
    }
    return undefined;
  }


  private async getApollo(): Promise<Apollo> {
    if (!this._started) {
      console.debug("[graphql] Waiting apollo client... ");
      await this.onStart.toPromise();
    }
    return this.apollo;
  }

}
