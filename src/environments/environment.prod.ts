const pkg = require('../../package.json')

export const environment = {
    production: true,
    baseUrl: "https://test.sumaris.net",
    defaultLocale: 'en',
    mock: false,
    version: pkg.version
};
