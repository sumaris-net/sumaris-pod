import { Environment } from "./environment.class";
const pkg = require('../../package.json')

export const environment: Environment = {
  name: (pkg.name as string),
  version: (pkg.version as string),
  production: true,
  baseUrl: '/',
  defaultLocale: 'fr',
  defaultLatLongFormat: 'DDMM',
  apolloFetchPolicy: 'cache-first',
  mock: false,

  // FIXME: GraphQL subscription never unsubscribe...
  listenRemoteChanges: false,
  persistCache: false,

  // Leave null,
  defaultPeer: null,

  // Production and public peers
  defaultPeers: [
    // TODO: change to production peer
    {
      host: 'test.sumaris.net',
      port: 443
    },
    // TODO: change to production peer
    {
      host: 'adap.e-is.pro',
      port: 443
    }
  ],
};
