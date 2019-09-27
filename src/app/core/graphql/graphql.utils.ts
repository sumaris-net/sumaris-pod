import {defaultDataIdFromObject} from "apollo-cache-inmemory";
import {ApolloLink} from "apollo-link";
import * as uuidv4 from "uuid/v4";
import {EventEmitter} from "@angular/core";
import {debounceTime, filter, switchMap} from "rxjs/operators";
import {PersistedData, PersistentStorage} from "apollo-cache-persist/types";
import {BehaviorSubject, Observable} from "rxjs";
import {ApolloClient} from "apollo-client";
import {environment} from "../../../environments/environment";

declare let window: any;
const _global = typeof global !== 'undefined' ? global : (typeof window !== 'undefined' ? window : {});
export const NativeWebSocket = _global.WebSocket || _global.MozWebSocket;

/**
 * AppWebSocket class.
 * With a hack on default Websocket, to avoid the use of protocol
 */
export const AppWebSocket = function (url: string, protocols?: string | string[]) {
  return new NativeWebSocket(url/*no protocols*/);
} as any;
AppWebSocket.CLOSED = NativeWebSocket.CLOSED;
AppWebSocket.CLOSING = NativeWebSocket.CLOSING;
AppWebSocket.CONNECTING = NativeWebSocket.CONNECTING;
AppWebSocket.OPEN = NativeWebSocket.OPEN;


/**
 * Custom ID generation, for the GraphQL cache
 * @param object
 */
function dataIdFromObjectProduction(object: Object): string {
  switch (object['__typename']) {

    // For generic VO: add entityName in the cache key (to distinguish by entity)
    case 'ReferentialVO':
    case 'MetierVO':
    case 'PmfmVO':
    case 'TaxonNameVO':
    case 'TaxonNameStrategyVO':
    case 'TaxonGroupVO':
    case 'TaxonGroupStrategyVO':
    case 'MeasurementVO':
      return (object['entityName'] || object['__typename']) + ':' + object['id'];

    // Fallback to default cache key
    default:
      return defaultDataIdFromObject(object);
  }
}

function dataIdFromObjectDebug (object: Object): string {
  switch (object['__typename']) {

    // For generic VO: add entityName in the cache key (to distinguish by entity)
    case 'ReferentialVO':
    case 'MetierVO':
    case 'PmfmVO':
    case 'TaxonGroupVO':
    case 'TaxonNameVO':
    case 'TaxonNameStrategyVO':
    case 'TaxonGroupStrategyVO':
    case 'MeasurementVO':
      if (!object['entityName']) {
        console.warn("WARN: no entityName found on entity: cache can be corrupted !", object);
      }
      return object['entityName'] + ':' + object['id'];

    // Fallback to default cache key
    default:
      return defaultDataIdFromObject(object);
  }
}

export const dataIdFromObject = environment.production ? dataIdFromObjectProduction : dataIdFromObjectDebug;

export interface TrackedQuery {
  id: string;
  name: string;
  queryJSON: string;
  variablesJSON: string;
  contextJSON: string;
}

export const TRACKED_QUERIES_STORAGE_KEY = "apollo-tracker-persist";

export function createTrackerLink(opts: {
  storage?: PersistentStorage<PersistedData<TrackedQuery[]>>;
  onNetworkStatusChange: Observable<string>;
  debounce?: number;
  debug?: boolean;
  }): ApolloLink {

  const trackedQueriesUpdated = new EventEmitter();
  const trackedQueriesById: {[id: string]: TrackedQuery} = {};

  const networkStatusSubject = new BehaviorSubject<string>('none');
  opts.onNetworkStatusChange.subscribe(type => networkStatusSubject.next(type));

  trackedQueriesUpdated.pipe(
    debounceTime(opts.debounce || 1000),
    switchMap(() => networkStatusSubject),
    // Continue if offline
    filter((type) => type === 'none' && !!opts.storage)
  ).subscribe(() => {
      const trackedQueries = Object.getOwnPropertyNames(trackedQueriesById)
        .map(key => trackedQueriesById[key])
        .filter(value => value !== undefined);
      if (opts.debug) console.debug("[graphql] Saving tracked queries to storage", trackedQueries);
      return opts.storage.setItem(TRACKED_QUERIES_STORAGE_KEY, trackedQueries);
    });

  return new ApolloLink((operation, forward) => {
    if (forward === undefined) {
      return null;
    }
    const context = operation.getContext();
    if (!context || !context.tracked) return forward(operation);

    const id = uuidv4();
    console.debug(`[apollo-tracker-link] Watching tracked query {${operation.operationName}#${id}}`);
    const trackedQuery: TrackedQuery = {
      id: uuidv4(),
      name: operation.operationName,
      queryJSON: JSON.stringify(operation.query),
      variablesJSON: JSON.stringify(operation.variables),
      contextJSON: JSON.stringify(context)
    };

    // Add to map
    trackedQueriesById[id] = trackedQuery;
    trackedQueriesUpdated.emit();

    return forward(operation).map(data => {
      console.debug(`[apollo-tracker-link] Tracked query {${operation.operationName}#${id}} succeed!`, data);
      delete trackedQueriesById[id];
      trackedQueriesUpdated.emit(trackedQueriesById); // update

      return data;
    });
  });
}

export async function restoreTrackedQueries(opts: {
  apolloClient: ApolloClient<any>;
  storage: PersistentStorage<PersistedData<TrackedQuery[]>>;
  debug?: boolean;
}) {

  const list = (await opts.storage.getItem(TRACKED_QUERIES_STORAGE_KEY)) as TrackedQuery[];

  if (!list) return;
  if (opts.debug) console.debug("[apollo-tracker-link] Restoring tracked queries", list);

  const promises = (list || []).map(trackedQuery => {
    const context = JSON.parse(trackedQuery.contextJSON);
    const query = JSON.parse(trackedQuery.queryJSON);
    const variables = JSON.parse(trackedQuery.variablesJSON);
    return opts.apolloClient.mutate({
      context,
      mutation: query,
      optimisticResponse: context.optimisticResponse,
      //update: updateHandlerByName[trackedQuery.name],
      variables,
    });
  });

  return Promise.all(promises);
}
