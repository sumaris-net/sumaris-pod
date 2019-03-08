import { Environment } from "./environment.class";
const pkg = require('../../package.json')

export const environment: Environment = {
  name: (pkg.name as string),
  version: (pkg.version as string),
  production: true,
  baseUrl: '/',
  remoteBaseUrl: "https://adap.e-is.pro",
  defaultLocale: 'fr',
  defaultLatLongFormat: 'DDMM',
  defaultProgram: "SUMARiS",
  apolloFetchPolicy: 'cache-first'
};
