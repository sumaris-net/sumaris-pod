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
import {EntityUtils} from "./model";
import {DataProxy} from 'apollo-cache';
import {isNotNil} from "../../shared/functions";

@Injectable({providedIn: 'root'})
export class GraphqlService {

  private _started = false;
  private _startPromise: Promise<any>;
  private _subscription = new Subscription();
  private _$networkStatusChanged: Observable<ConnectionType>;

  private httpParams: Options;
  private wsParams;
  private wsConnectionParams: { authToken?: string } = {};
  private _defaultFetchPolicy: WatchQueryFetchPolicy;

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
        filter(type => isNotNil(type)),
        distinctUntilChanged()
      );
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
      })
      .catch((err) => {
        console.error(err && err.message || err, err);
        this._startPromise = undefined;
      });
    return this._startPromise;
  }

  setAuthToken(authToken: string) {
    if (authToken) {
      console.debug("[graphql] Setting new authentication token");
      this.wsConnectionParams.authToken = authToken;
    } else {
      console.debug("[graphql] Resetting authentication token");
      delete this.wsConnectionParams.authToken;
      // Clear cache
      this.clearCache();
    }
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
      res = this.toApolloError<T>(err);
    }
    if (res.errors) {
      const error = res.errors[0] as any;
      if (error && error.code && error.message) {
        throw error;
      }
      console.error("[data-service] " + error.message);
      throw opts.error ? opts.error : error.message;
    }
    return res.data;
  }

  watchQuery<T, V = R>(opts: {
    query: any,
    variables: V,
    error?: ServiceError,
    fetchPolicy?: WatchQueryFetchPolicy
  }): Observable<T> {
    return this.apollo.watchQuery<T, V>({
      query: opts.query,
      variables: opts.variables,
      fetchPolicy: opts.fetchPolicy || (this._defaultFetchPolicy as FetchPolicy) || undefined,
      notifyOnNetworkStatusChange: true
    })
      .valueChanges
      .pipe(
        catchError(error => this.onApolloError<T>(error)),
        map(({data, errors}) => {
          if (errors) {
            const error = errors[0] as any;
            if (error && error.code && error.message) {
              throw error;
            }
            console.error("[data-service] " + error.message);
            throw opts.error ? opts.error : error.message;
          }
          return data;
        })
      );
  }

  async mutate<T, V = R>(opts: {
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
  }): Promise<T> {

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

    return new Promise<T>((resolve, reject) => {
      this.apollo.mutate<ApolloQueryResult<T>, V>({
        mutation: opts.mutation,
        variables: opts.variables,
        context: opts.context,
        optimisticResponse: opts.optimisticResponse as any,
        update: opts.update as any
      })
        .pipe(
          catchError(error => this.onApolloError<T>(error)),
          first()
        )
        .subscribe(res => {
          if (!res) {
            reject('Unknown GraphQL error. Please check previous errors in console');
            return;
          }
          else if (res.errors) {
            let error = res.errors[0] as any;

            if (error && error.code && error.message) {
              if (error && error.code == ServerErrorCodes.BAD_UPDATE_DATE) {
                reject({code: ServerErrorCodes.BAD_UPDATE_DATE, message: "ERROR.BAD_UPDATE_DATE"});
              } else if (error && error.code == ServerErrorCodes.DATA_LOCKED) {
                reject({code: ServerErrorCodes.DATA_LOCKED, message: "ERROR.DATA_LOCKED"});
              } else {
                reject(error);
              }
            } else {
              console.error("[graphql] " + error.message, error.stack);
              error = opts.error ? opts.error : error.message;
              reject(error);
              if (opts.error && opts.error.reject) opts.error.reject(error);
            }
          } else {
            resolve(res.data as T);
          }
        });
    });
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
        catchError(error => this.onApolloError<T>(error)),
        map(({data, errors}) => {
          if (errors) {
            const error = errors[0];
            if (error /*&& error.code*/ && error.message) {
              throw error;
            }
            console.error("[graphql] " + error.message);
            throw opts.error ? opts.error : error.message;
          }
          return data;
        })
      );
  }

  addToQueryCache<V = R>(proxy: DataProxy,
                         opts: {
    query: any,
    variables: V
  }, propertyName: string, newValue: any) {

    proxy = proxy || this.apollo.getClient();

    try {
      const values = proxy.readQuery(opts);

      if (values && values[propertyName]) {
        values[propertyName].push(newValue);

        proxy.writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: values
        });
        return; // OK: stop here
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
    }
    if (this._debug) console.debug("[graphql] Unable to add entity to cache. Please check query has been cached, and {" + propertyName + "} exists in the result:", opts.query);
  }

  addManyToQueryCache<V = R>(proxy: DataProxy,
                             opts: {
    query: any,
    variables: V
  }, propertyName: string, newValues: any[]) {

    if (!newValues || !newValues.length) return; // nothing to process

    proxy = proxy || this.apollo.getClient();

    try {
      const values = proxy.readQuery(opts);

      if (values && values[propertyName]) {
        // Keep only not existing values
        newValues = newValues.filter(nv => !values[propertyName].find(v => nv['id'] === v['id'] && nv['entityName'] === v['entityName']));

        if (!newValues.length) return; // No new value

        // Update the cache
        values[propertyName] = values[propertyName].concat(newValues);
        proxy.writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: values
        });
        return; // OK: stop here
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
    }

    if (this._debug) console.debug("[graphql] Unable to add entities to cache. Please check query has been cached, and {" + propertyName + "} exists in the result:", opts.query);
  }

  removeToQueryCacheById<V = R>(proxy: DataProxy,
                                opts: {
    query: any,
    variables: V
  }, propertyName: string, idToRemove: number) {

    proxy = proxy || this.apollo.getClient();

    try {
      const values = proxy.readQuery(opts);

      if (values && values[propertyName]) {

        values[propertyName] = (values[propertyName] || []).filter(item => item['id'] !== idToRemove);
        proxy.writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: values
        });

        return;
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
    }
    console.warn("[graphql] Unable to remove id from cache. Please check {" + propertyName + "} exists in the result:", opts.query);
  }

  removeToQueryCacheByIds<V = R>(proxy: DataProxy, opts: {
    query: any,
    variables: V
  }, propertyName: string, idsToRemove: number[]) {

    try {
      const values = proxy.readQuery(opts);

      if (values && values[propertyName]) {

        values[propertyName] = (values[propertyName] || []).reduce((result: any[], item: any) => {
          return idsToRemove.indexOf(item['id']) === -1 ? result.concat(item) : result;
        }, []);
        proxy.writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: values
        });

        return;
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
    }
    console.warn("[graphql] Unable to remove id from cache. Please check {" + propertyName + "} exists in the result:", opts.query);
  }

  updateToQueryCache<V = R>(proxy: DataProxy,
                            opts: {
    query: any,
    variables: V
  }, propertyName: string, newValue: any) {

    try {
      const values = proxy.readQuery(opts);

      if (values && values[propertyName]) {
        const existingIndex = (values[propertyName] || []).findIndex(v => EntityUtils.equals(newValue, v));
        if (existingIndex !== -1) {
          values[propertyName].splice(existingIndex, 1, newValue);
        }
        else {
          values[propertyName].push(newValue);
        }

        proxy.writeQuery({
          query: opts.query,
          variables: opts.variables,
          data: values
        });
        return; // OK: stop here
      }
    } catch (err) {
      // continue
      // read in cache is not guaranteed to return a result. see https://github.com/apollographql/react-apollo/issues/1776#issuecomment-372237940
    }
    if (this._debug) console.debug("[graphql] Unable to update entity to cache. Please check query has been cached, and {" + propertyName + "} exists in the result:", opts.query);
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
            console.debug("[graphql] offline mode: enable mutations buffer");
            queueLink.close();
          }
          // Network is online
          else {
            console.debug("[graphql] online mode: disable mutations buffer");
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

  private onApolloError<T>(err: any): Observable<ApolloQueryResult<T>> {
    return of(this.toApolloError(err));
  }

  private toApolloError<T>(err: any): ApolloQueryResult<T> {
    const appError = (err.networkError && (this.toAppError(err.networkError) || this.createAppErrorByCode(ErrorCodes.UNKNOWN_NETWORK_ERROR))) ||
      (err.graphQLErrors && this.toAppError(err.graphQLErrors[0])) ||
      this.toAppError(err) ||
      this.toAppError(err.originalError);
    return {
      data: null,
      errors: appError && [appError] || err.graphQLErrors || [err],
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
    }
    return undefined;
  }

  private toAppError(err: any): any | undefined {
    const message = err && err.message || err;
    if (typeof message == "string" && message.trim().indexOf('{"code":') == 0) {
      const error = JSON.parse(message);
      return error && this.createAppErrorByCode(error.code) || err;
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

  private async clearCache(client?: ApolloClient<any>) {
    client = client || this.apollo.getClient();
    if (client) {
      console.debug("[graphql] Clear apollo client cache... ");
      await client.cache.reset();
    }
  }

}
