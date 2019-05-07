import { Environment } from "./environment.class";
const pkg = require('../../package.json')

export const environment: Environment = {
  name: (pkg.name as string),
  version: (pkg.version as string),
  production: true,
  baseUrl: '/',
  remoteBaseUrl: "https://www.sumaris.net",
  defaultLocale: 'fr',
  defaultLatLongFormat: 'DDMM',
  defaultProgram: 'SUMARIS',
  apolloFetchPolicy: 'cache-first',
  mock: false,

  // FIXME: GraphQL subscription never unsubscribe...
  listenRemoteChanges: false,

  defaultPeers: [
    {
      host: 'www.sumaris.net',
      port: 443
    },
    {
      // TODO: change to production ADAP node
      host: 'adap.e-is.pro',
      port: 443
    }
  ],
};
