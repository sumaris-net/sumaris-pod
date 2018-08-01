import { Environment } from "./environment.class";
const pkg = require('../../package.json')

export const environment: Environment = {
    production: true,
    baseUrl: '/',
    remoteBaseUrl: "https://test.sumaris.net",
    defaultLocale: 'en',
    version: pkg.version,
    defaultProgram: "SUMARiS",

    mock: false
};
