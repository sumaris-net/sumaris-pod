import { Environment } from "./environment.class";
const pkg = require('../../package.json')

/* tslint:disable */
export const environment: Environment = {
  name: (pkg.name as string),
  version: (pkg.version as string),
  production: true,
  baseUrl: "/",
  defaultLocale: "fr",
  defaultLatLongFormat: "DDMM",
  apolloFetchPolicy: "cache-first",
  mock: false,

  // FIXME: GraphQL subscription never unsubscribe...
  listenRemoteChanges: false,

  // FIXME: enable cache
  persistCache: false,

  // Leave null,
  defaultPeer: null,

  // Production and public peers
  defaultPeers: [
    {
      host: 'www.sumaris.net',
      port: 443
    },
    {
      host: 'adap.pecheursdebretagne.eu',
      port: 443
    },
    {
      host: 'test.sumaris.net',
      port: 443
    }
  ]
};
/* tslint:enable */
