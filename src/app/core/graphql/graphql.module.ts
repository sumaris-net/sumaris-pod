import { NgModule } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AccountService } from '../services/account.service';

// Apollo
import { Apollo, ApolloModule } from 'apollo-angular';
import { HttpLink, HttpLinkModule } from 'apollo-angular-link-http';
import { InMemoryCache, defaultDataIdFromObject } from 'apollo-cache-inmemory';
import { ApolloLink } from 'apollo-link';
import { WebSocketLink } from 'apollo-link-ws';
import { getMainDefinition } from 'apollo-utilities';
import {isNilOrBlank} from "../../shared/functions";
import {NetworkService} from "../services/network.service";
import {delay} from "q";


/* Hack on Websocket, to avoid the use of protocol */
declare let window: any;
const _global = typeof global !== 'undefined' ? global : (typeof window !== 'undefined' ? window : {});
const NativeWebSocket = _global.WebSocket || _global.MozWebSocket;
const AppWebSocket = function (url: string, protocols?: string | string[]) {
  return new NativeWebSocket(url/*no protocols*/);
} as any;
AppWebSocket.CLOSED = NativeWebSocket.CLOSED;
AppWebSocket.CLOSING = NativeWebSocket.CLOSING;
AppWebSocket.CONNECTING = NativeWebSocket.CONNECTING;
AppWebSocket.OPEN = NativeWebSocket.OPEN;

export const dataIdFromObject = function (object: Object): string {
  switch (object['__typename']) {
    // For generic VO: add entityName in the cache key (to distinguish by entity)
    case 'ReferentialVO': return object['entityName'] + ':' + object['id'];
    case 'MeasurementVO': return object['entityName'] + ':' + object['id'];
    // Fallback to default cache key
    default: return defaultDataIdFromObject(object);
  }
};

@NgModule({
  imports: [
    HttpClientModule,
    ApolloModule,
    HttpLinkModule
  ],
  exports: [
    HttpClientModule,
    ApolloModule,
    HttpLinkModule
  ]
})
export class AppGraphQLModule {

  private _started = false;
  private wsConnectionParams: { authToken?: string } = {};
  private httpParams;

  constructor(
    private apollo: Apollo,
    private httpLink: HttpLink,
    private networkService: NetworkService,
    private accountService: AccountService) {

    // Update auth Token
    this.accountService.onAuthTokenChange.subscribe((token) => {
      if (token) {
        console.debug("[apollo] Setting new authentication token");
        this.wsConnectionParams.authToken = token;
      } else {
        console.debug("[apollo] Resetting authentication token");
        delete this.wsConnectionParams.authToken;
      }
    });

    if (this.networkService.started) {
      this.start();
    }
    else {
      this.networkService.onStart.subscribe(async () => {
        if (this._started) await this.stop();
        await this.start();
      });
    }
  }

  protected async start() {
    console.info("[apollo] Starting GraphQL module...");

    // Waiting for network service
    await this.networkService.ready();
    const peer = this.networkService.peer;
    if (!peer) throw Error("[apollo] Missing peer. Unable to start GraphQL client");

    const uri = peer.url + '/graphql';
    const wsUri = String.prototype.replace.call(uri, "http", "ws") + '/websocket';
    console.info("[apollo] GraphQL base uri: " + uri);
    console.info("[apollo] GraphQL subscription uri: " + wsUri);

    this.httpParams = this.httpParams || {};
    this.httpParams.uri = uri;

    if (!this.apollo.getClient()) {

      console.debug("[apollo] Creating GraphQL client...");

      // Http link
      const http = this.httpLink.create(this.httpParams);

      // Websocket link
      const ws = new WebSocketLink({
        uri: wsUri,
        options: {
          lazy: true,
          reconnect: true,
          connectionParams: this.wsConnectionParams,
          addTypename: true
        },
        webSocketImpl: AppWebSocket
      });

      const authLink = new ApolloLink((operation, forward) => {

        // Use the setContext method to set the HTTP headers.
        operation.setContext({
          headers: {
            authorization: this.wsConnectionParams.authToken ? `token ${this.wsConnectionParams.authToken}` : ''
          }
        });

        // Call the next link in the middleware chain.
        return forward(operation);
      });

      const imCache = new InMemoryCache({
        dataIdFromObject: dataIdFromObject
      });

      // create Apollo
      this.apollo.create({
        link: ApolloLink.split(
          ({query}) => {
            const def = getMainDefinition(query);
            return def.kind === 'OperationDefinition' && def.operation === 'subscription';
          },
          ws,
          authLink.concat(http)
        ),
        cache: imCache,
        connectToDevTools: !environment.production
      });
    }

    this._started = true;
  }

  protected async stop() {
    if (!this._started) return;

    if (this.apollo.getClient()) {
      console.info("[apollo] Stopping GraphQL module...");
      await this.apollo.getClient().resetStore();

    }

    this._started = false;
  }

}
