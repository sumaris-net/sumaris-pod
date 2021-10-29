'use strict';

const { join } = require('path');
const { readdirSync, renameSync, readFileSync, copyFileSync } = require('fs');

const i18nDir = './www/assets/i18n/';
let pkgStr = readFileSync('./package.json', {encoding: 'UTF-8'});
const pkg = JSON.parse(pkgStr);

console.debug('Insert version into I18n files...');
// For each files
readdirSync(i18nDir)
  // Filter in src i18n files (skip renamed files)
  .filter(file => file.match(/^[a-z]{2}(-[A-Z]{2})?\.json$/))
  .forEach(file => {
    const filePath = join(i18nDir, file);
    const newFilePath = join(i18nDir, file.replace(/([a-z]{2}(:?-[A-Z]{2})?)\.json/, '$1-' + pkg.version + '.json'));

    console.debug(' - ' + filePath + ' -> ' + newFilePath);

    copyFileSync(filePath, newFilePath, );
  });

console.debug('Insert version into I18n files [OK]');
