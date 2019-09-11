import {Component, Inject} from '@angular/core';
import {MenuItem} from './core/menu/menu.component';
import {AccountService, isNotNil, joinProperties, LocalSettingsService} from './core/core.module';
import {ReferentialRefService} from './referential/referential.module';
import {ConfigService} from './core/services/config.service';
import {DOCUMENT} from "@angular/common";
import {Configuration} from "./core/services/model";
import {PlatformService} from "./core/services/platform.service";
import {throttleTime} from "rxjs/operators";
import {changeCaseToUnderscore} from "./shared/shared.module";
import {FormFieldDefinition} from "./shared/form/field.model";


@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {

  logo: String;
  appName: String;
  menuItems: Array<MenuItem> = [
    {title: 'MENU.HOME', path: '/', icon: 'home'},

    // Data entry
    {title: 'MENU.DATA_ENTRY_DIVIDER', profile: 'USER'},
    {title: 'MENU.TRIPS', path: '/trips', icon: 'pin', profile: 'USER'},
    {
      title: 'MENU.OBSERVED_LOCATIONS', path: '/observations',
      matIcon: 'verified_user',
      profile: 'USER'
    },

    // Data extraction
    {title: 'MENU.EXTRACTION_DIVIDER', profile: 'SUPERVISOR'},
    {title: 'MENU.TRIPS', path: '/extraction', icon: 'download', profile: 'SUPERVISOR'},
    {title: 'MENU.MAP', path: '/map', icon: 'globe', profile: 'SUPERVISOR'},

    // Referential
    {title: 'MENU.REFERENTIAL_DIVIDER', profile: 'USER'},
    {title: 'MENU.VESSELS', path: '/referential/vessels', icon: 'boat', profile: 'USER'},
    {title: 'MENU.REFERENTIAL', path: '/referential/list', icon: 'list', profile: 'ADMIN'},
    {title: 'MENU.USERS', path: '/admin/users', icon: 'people', profile: 'ADMIN'},
    {title: 'MENU.SERVER_SETTINGS', path: '/admin/config', matIcon: 'build', profile: 'ADMIN'},

    // Settings
    {title: '' /*empty divider*/},
    {title: 'MENU.LOCAL_SETTINGS', path: '/settings', icon: 'settings'},
    {title: 'MENU.ABOUT', action: 'about', matIcon: 'help_outline', cssClass: 'visible xs visible-sm'},
    {title: 'MENU.LOGOUT', action: 'logout', icon: 'log-out', profile: 'GUEST', cssClass: 'ion-color-danger'}

  ];

  constructor(
    @Inject(DOCUMENT) private _document: HTMLDocument,
    private platform: PlatformService,
    private accountService: AccountService,
    private referentialRefService: ReferentialRefService,
    private configurationService: ConfigService,
    private settings: LocalSettingsService
  ) {

    this.platform.ready().then(() => {

      // Listen for config changed
      this.configurationService.config.subscribe(config => this.onConfigChanged(config));

      // Add additional account fields
      this.addAccountFields();

      this.addSettingsFields();
    });
  }

  public onActivate(event) {
    // Make sure to scroll on top before changing state
    // See https://stackoverflow.com/questions/48048299/angular-5-scroll-to-top-on-every-route-click
    const scrollToTop = window.setInterval(() => {
      let pos = window.pageYOffset;
      if (pos > 0) {
        window.scrollTo(0, pos - 20); // how far to scroll on each step
      } else {
        window.clearInterval(scrollToTop);
      }
    }, 16);
  }

  protected onConfigChanged(config: Configuration) {

    this.logo = config.smallLogo || config.largeLogo;
    this.appName = config.label;

    // Set document title
    const title = isNotNil(config.name) ? `${config.label} - ${config.name}` : config.label;
    this._document.getElementById('appTitle').textContent = title;

    // Set document favicon
    const favicon = config.properties && config.properties["sumaris.favicon"];
    if (isNotNil(favicon)) {
      this._document.getElementById('appFavicon').setAttribute('href', favicon);
    }

    if (config.properties) {
      this.updateTheme({
        colors: {
          primary: config.properties["sumaris.color.primary"],
          secondary: config.properties["sumaris.color.secondary"],
          tertiary: config.properties["sumaris.color.tertiary"]
        }
      });
    }

  }


  protected updateTheme(options: { colors?: { primary?: string; secondary?: string; tertiary?: string; } }) {
    if (!options) return;

    console.info("[app] Changing theme colors ", options);

    // Settings colors
    if (options.colors) {
      Object.getOwnPropertyNames(options.colors).forEach(colorName => {

        // Remove existing value
        document.documentElement.style.removeProperty(`--ion-color-${colorName}`);

        // Set new value, if any
        const color = options.colors[colorName];
        if (isNotNil(color)) {
          document.documentElement.style.setProperty(`--ion-color-${colorName}`, color);
          // TODO compute shade, hint, ...
        }
      });
    }
  }

  protected addAccountFields() {

    console.debug("[app] Add additional account fields...");

    const attributes = this.settings.getFieldDisplayAttributes('department');
    const departmentDefinition = {
      key: 'department',
      label: 'USER.DEPARTMENT',
      type: 'entity',
      autocomplete: {
        service: this.referentialRefService,
        filter: {entityName: 'Department'},
        displayWith: (value) => joinProperties(value, attributes),
        attributes: attributes
      },
      extra: {
        registration: {
          required: true
        },
        account: {
          required: true,
          disable: true
        }
      }
    } as FormFieldDefinition;

    // Add account field: department
    this.accountService.registerAdditionalField(departmentDefinition);

    // When settings changed
    this.settings.onChange
      .pipe(throttleTime(400))
      .subscribe(() => {
        // Update the display fn
        const attributes = this.settings.getFieldDisplayAttributes('department');
        departmentDefinition.autocomplete.displayWith = (value) => joinProperties(value, attributes);
        departmentDefinition.autocomplete.attributes = attributes;
      });
  }

  protected addSettingsFields() {

    console.debug("[app] Add additional settings fields...");

    this.settings.registerAdditionalFields(
      // Configurable fields
      ['department', 'location', 'qualitativeValue', 'taxonGroup', 'taxonName', 'gear']
        // Map into option definition
        .map(fieldName => {
        return {
          key: `sumaris.field.${fieldName}.attributes`,
          label: `SETTINGS.FIELDS.${changeCaseToUnderscore(fieldName).toUpperCase()}`,
          type: 'enum',
          values: [
            {key: 'label,name',   value: 'SETTINGS.FIELDS.ATTRIBUTES.LABEL_NAME'},
            {key: 'name',         value: 'SETTINGS.FIELDS.ATTRIBUTES.NAME'},
            {key: 'name,label',   value: 'SETTINGS.FIELDS.ATTRIBUTES.NAME_LABEL'},
            {key: 'label',        value: 'SETTINGS.FIELDS.ATTRIBUTES.LABEL'}
          ]
        } as FormFieldDefinition;
      }));
  }

}

