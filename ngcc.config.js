module.exports = {
  packages: {
    'angular2-text-mask': {
      ignorableDeepImportMatchers: [
        /text-mask-core\//,
      ]
    },
    '@sumaris-net/ngx-components': {
      ignorableDeepImportMatchers: [
        /@ionic-native\//,
        /ionic-cache\//,
        /uuid\//,
        /zone.js\//,
        /zone-error\//,
        /@apollo\//,
        /apollo-angular\//,
        /moment\//,
        /ngx-material-table\//
      ]
    },
    'apollo-link-queue': {
      ignorableDeepImportMatchers: [
        /@apollo\//
      ]
    }
  }
};
