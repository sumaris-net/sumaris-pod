// This file can be replaced during build by using the `fileReplacements` array.
// `ng build ---prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

import {Environment} from "./environment.class";

const pkg = require('../../package.json');

export const environment: Environment = {
  name: (pkg.name as string),
  version: (pkg.version as string),
  production: false,
  baseUrl: "/",
  defaultLocale: "fr",
  defaultLatLongFormat: "DDMM",
  apolloFetchPolicy: "cache-first",
  mock: false,

  // FIXME: GraphQL subscription never unsubscribe...
  listenRemoteChanges: true,

  // FIXME: enable cache
  persistCache: false,

  // defaultPeer: {
  //   host: '192.168.0.28',
  //   port: 8080
  // },
  defaultPeers: [
    {
      host: '192.168.0.45',
      port: 8080
    },
    {
      host: '192.168.0.28',
      port: 8080
    },
    {
      host: 'localhost',
      port: 8080
    },
    {
      host: 'adap.pecheursdebretagne.eu',
      port: 443
    },
    {
      host: 'www.sumaris.net',
      port: 443
    },
    {
      host: 'test.sumaris.net',
      port: 443
    },
    {
      host: 'adap.e-is.pro',
      port: 443
    }
  ],


};

/*
 * In development mode, to ignore zone related error stack frames such as
 * `zone.run`, `zoneDelegate.invokeTask` for easier debugging, you can
 * import the following file, but please comment it out in production mode
 * because it will have performance impact when throw error
 */
import 'zone.js/dist/zone-error';
