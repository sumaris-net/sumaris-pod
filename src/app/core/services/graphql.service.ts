import {Observable, Subject} from "rxjs";
import {Apollo} from "apollo-angular";
import {ApolloClient, ApolloQueryResult, FetchPolicy, MutationUpdaterFn, WatchQueryFetchPolicy} from "apollo-client";
import {R} from "apollo-angular/types";
import {ErrorCodes, ServerErrorCodes, ServiceError} from "./errors";
import {catchError, first, map} from "rxjs/operators";

import {environment} from '../../../environments/environment';
import {delay} from "q";
import {Injectable} from "@angular/core";
import {HttpLink, Options} from "apollo-angular-link-http";
import {NetworkService} from "./network.service";
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

@Injectable({providedIn: 'root'})
export class GraphqlService {

  private _started = false;
  private httpParams: Options;
  private wsParams;
  private wsConnectionParams: { authToken?: string } = {};

  public onStart = new Subject<void>();

  protected _debug = false;

  protected get isOffline(): boolean {
    return !this._started;
  }

  public constructor(
    private platform: Platform,
    private apollo: Apollo,
    private httpLink: HttpLink,
    private networkService: NetworkService,
    private storage: Storage
  ) {

    // Start
    if (this.networkService.started) {
      this.start();
    }

    this.networkService.onStart.subscribe(() => this.restart());
  }

  setAuthToken(authToken: string) {
    if (authToken) {
      console.debug("[graphql] Setting new authentication token");
      this.wsConnectionParams.authToken = authToken;
    } else {
      console.debug("[graphql] Resetting authentication token");
      delete this.wsConnectionParams.authToken;
      // Clear cache
      this.resetCache();
    }
  }

  public async query<T, V = R>(opts: {
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
        fetchPolicy: opts.fetchPolicy || (environment.apolloFetchPolicy as FetchPolicy) || undefined
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

  public watchQuery<T, V = R>(opts: {
    query: any,
    variables: V,
    error?: ServiceError,
    fetchPolicy?: WatchQueryFetchPolicy
  }): Observable<T> {
    return this.apollo.watchQuery<T, V>({
      query: opts.query,
      variables: opts.variables,
      fetchPolicy: opts.fetchPolicy || (environment.apolloFetchPolicy as FetchPolicy) || undefined,
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

  public mutate<T, V = R>(opts: {
    mutation: any,
    variables: V,
    error?: ServiceError,
    context?: {
      serializationKey?: string;
      tracked?: boolean;
      optimisticResponse?: any;
    },
    optimisticResponse?: T;
    update?: MutationUpdaterFn<T>
  }): Promise<T> {
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
        .subscribe(({data, errors}) => {
          if (errors) {
            const error = errors[0] as any;
            if (error && error.code && error.message) {
              if (error && error.code == ServerErrorCodes.BAD_UPDATE_DATE) {
                reject({code: ServerErrorCodes.BAD_UPDATE_DATE, message: "ERROR.BAD_UPDATE_DATE"});
              } else if (error && error.code == ServerErrorCodes.DATA_LOCKED) {
                reject({code: ServerErrorCodes.DATA_LOCKED, message: "ERROR.DATA_LOCKED"});
              } else {
                reject(error);
              }
            } else {
              console.error("[data-service] " + error.message);
              reject(opts.error ? opts.error : error.message);
            }
          } else {
            resolve(data as T);
          }
        });
    });
  }

  public subscribe<T, V = R>(opts: {
    query: any,
    variables: V,
    error?: ServiceError
  }): Observable<T> {

    return this.apollo.subscribe({
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

  public addToQueryCache<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, newValue: any) {

    try {
      const values = this.apollo.getClient().readQuery(opts);

      if (values && values[propertyName]) {
        values[propertyName].push(newValue);

        this.apollo.getClient().writeQuery({
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
    if (this._debug) console.debug("[data-service] Unable to add entity to cache. Please check query has been cached, and {" + propertyName + "} exists in the result:", opts.query);
  }

  public addManyToQueryCache<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, newValues: any[]) {

    if (!newValues || !newValues.length) return; // nothing to process

    try {
      const values = this.apollo.getClient().readQuery(opts);

      if (values && values[propertyName]) {
        // Keep only not existing values
        newValues = newValues.filter(nv => !values[propertyName].find(v => nv['id'] === v['id'] && nv['entityName'] === v['entityName']));

        if (!newValues.length) return; // No new value

        // Update the cache
        values[propertyName] = values[propertyName].concat(newValues);
        this.apollo.getClient().writeQuery({
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

    if (this._debug) console.debug("[data-service] Unable to add entities to cache. Please check query has been cached, and {" + propertyName + "} exists in the result:", opts.query);
  }

  public removeToQueryCacheById<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, idToRemove: number) {

    try {
      const values = this.apollo.getClient().readQuery(opts);

      if (values && values[propertyName]) {

        values[propertyName] = (values[propertyName] || []).filter(item => item['id'] !== idToRemove);
        this.apollo.getClient().writeQuery({
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
    console.warn("[data-service] Unable to remove id from cache. Please check {" + propertyName + "} exists in the result:", opts.query);
  }

  public removeToQueryCacheByIds<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, idsToRemove: number[]) {

    try {
      const values = this.apollo.getClient().readQuery(opts);

      if (values && values[propertyName]) {

        values[propertyName] = (values[propertyName] || []).reduce((result: any[], item: any) => {
          return idsToRemove.indexOf(item['id']) === -1 ? result.concat(item) : result;
        }, []);
        this.apollo.getClient().writeQuery({
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
    console.warn("[data-service] Unable to remove id from cache. Please check {" + propertyName + "} exists in the result:", opts.query);
  }

  public updateToQueryCache<V = R>(opts: {
    query: any,
    variables: V
  }, propertyName: string, newValue: any) {

    try {
      const values = this.apollo.getClient().readQuery(opts);

      if (values && values[propertyName]) {
        const existingIndex = (values[propertyName] || []).findIndex(v => EntityUtils.equals(newValue, v));
        if (existingIndex !== -1) {
          values[propertyName].splice(existingIndex, 1, newValue);
        }
        else {
          values[propertyName].push(newValue);
        }

        this.apollo.getClient().writeQuery({
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
    if (this._debug) console.debug("[data-service] Unable to update entity to cache. Please check query has been cached, and {" + propertyName + "} exists in the result:", opts.query);
  }

  /* -- protected methods -- */


  protected async start() {
    console.info("[graphql] Starting graphql...");

    // Waiting for network service
    await this.networkService.ready();
    const peer = this.networkService.peer;
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


    const client = this.apollo.getClient();
    if (!client) {
      console.debug("[apollo] Creating GraphQL client...");

      // Websocket link
      const wsLink = new WebSocketLink(this.wsParams);

      const queueLink = new QueueLink();
      const serializingLink = new SerializingLink();
      const retryLink = new RetryLink();
      const trackerLink = createTrackerLink({
        storage: {
          getItem: (key: string) => this.storage.get(key),
          setItem: (key: string, data: any) => this.storage.set(key, data),
          removeItem: (key: string) => this.storage.remove(key)
        },
        onNetworkStatusChange: this.networkService.onNetworkStatusChanges.asObservable(),
        debounce: 1000,
        debug: true
      });

      const authLink = new ApolloLink((operation, forward) => {

        // Use the setContext method to set the HTTP headers.
        operation.setContext(Object.assign(operation.getContext() || {},
          {
            headers: {
              authorization: this.wsConnectionParams.authToken ? `token ${this.wsConnectionParams.authToken}` : ''
            }
          }));

        // Call the next link in the middleware chain.
        return forward(operation);
      });

      // Http link
      const httpLink = this.httpLink.create(this.httpParams);

      const cache = new InMemoryCache({
        dataIdFromObject: dataIdFromObject
      });


      // Enable cache persistence
      if (this.platform.is('mobile') && environment.persistCache) {
        console.debug("[apollo] Starting persistence cache...");
        await persistCache({
          cache,
          storage: {
            getItem: (key: string) => this.storage.get(key),
            setItem: (key: string, data: any) => this.storage.set(key, data),
            removeItem: (key: string) => this.storage.remove(key)
          },
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

            // Handle mutations
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

      this.networkService.onNetworkStatusChanges.subscribe(() => {
        if (this.networkService.online) {
          console.debug("[graphql] Disabling mutations queue");
          queueLink.open();
          //queueLink.close();
        } else {
          console.debug("[graphql] Enabling mutations queue");
          queueLink.close();
        }
      });

    }

    this._started = true;

    // Emit event
    this.onStart.next();

    // Enable tracked queries persistence
    if (this.platform.is('mobile') && environment.persistCache) {

      try {
        await restoreTrackedQueries({
          apolloClient: this.apollo.getClient(),
          storage: {
            getItem: (key: string) => this.storage.get(key),
            setItem: (key: string, data: any) => this.storage.set(key, data),
            removeItem: (key: string) => this.storage.remove(key)
          },
          debug: true
        });
      } catch(err) {
        console.error('[graphql] Failed to restore tracked queries from storage: ' + (err && err.message || err), err);
      }
    }
  }

  protected async stop() {
    this._started = false;
    await this.resetClient();
  }

  protected async restart() {
    if (this._started) await this.stop();
    await this.start();
  }


  protected async resetClient(client?: ApolloClient<any>) {
    client = client || this.apollo.getClient();
    if (!client) return;

    console.info("[apollo] Reset GraphQL client...");
    client.stop();
    await Promise.all([
      client.clearStore(),
      client.cache.reset()
    ]);
  }

  private onApolloError<T>(err: any): Observable<ApolloQueryResult<T>> {
    return Observable.of(this.toApolloError(err));
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
    while (!this._started) {
      console.debug("[graphql] Waiting apollo client... ");
      await delay(500);
    }
    return this.apollo;
  }

  private async resetCache(client?: ApolloClient<any>) {
    client = client || this.apollo.getClient();
    if (client) {
      console.debug("[graphql] Reset graphql cache... ");
      await client.cache.reset();
    }
  }

}
