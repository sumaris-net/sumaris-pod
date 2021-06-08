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
        /uuid\//,
      ]
    }
  }
};
