// TODO: rename to CONFIG_OPTIONS_MAP
// then declare a type like this :
// > export declare type ConfigOptions = key of CONFIG_OPTIONS_MAP
import {FormFieldDefinition, FormFieldDefinitionMap, FormFieldValue} from "../../../shared/form/field.model";
import {StatusIds} from "../model/model.enum";
import {PRIORITIZED_USER_PROFILES} from "../model/person.model";
import {Locales} from "../model/settings.model";
import {LocationLevelIds} from "../../../referential/services/model/model.enum";

export const ConfigOptions = Object.freeze({
    LOGO: <FormFieldDefinition>{
        key: 'sumaris.logo',
        label: 'CONFIGURATION.OPTIONS.LOGO',
        type: 'string'
    },
    FAVICON: <FormFieldDefinition>{
        key: 'sumaris.favicon',
        label: 'CONFIGURATION.OPTIONS.FAVICON',
        type: 'string'
    },
    DEFAULT_LOCALE: <FormFieldDefinition>{
        key: 'sumaris.defaultLocale',
        label: 'CONFIGURATION.OPTIONS.DEFAULT_LOCALE',
        type: 'enum',
        values: Locales.map(l => {
            return <FormFieldValue>{key: l.id, value: l.name};
        })
    },
    DEFAULT_LAT_LONG_FORMAT: <FormFieldDefinition>{
        key: 'sumaris.defaultLatLongFormat',
        label: 'CONFIGURATION.OPTIONS.DEFAULT_LATLONG_FORMAT',
        type: 'enum',
        values: [
            {
                key: 'DDMMSS',
                value: 'COMMON.LAT_LONG.DDMMSS_PLACEHOLDER'
            },
            {
                key: 'DDMM',
                value: 'COMMON.LAT_LONG.DDMM_PLACEHOLDER'
            },
            {
                key: 'DD',
                value: 'COMMON.LAT_LONG.DD_PLACEHOLDER'
            }
        ]
    },
    DATA_NOT_SELF_ACCESS_ROLE: <FormFieldDefinition>{
        key: "sumaris.auth.notSelfDataAccess.role",
        label: "CONFIGURATION.OPTIONS.NOT_SELF_DATA_ACCESS_MIN_ROLE",
        type: 'enum',
        values: PRIORITIZED_USER_PROFILES.map(key => ({
            key: 'ROLE_' + key,
            value: 'USER.PROFILE_ENUM.' + key
        }))
    },
    ENTITY_TRASH: <FormFieldDefinition> {
        key: 'sumaris.persistence.trash.enable',
        label: 'CONFIGURATION.OPTIONS.ENTITY_TRASH',
        type: 'boolean'
    },
    TESTING: <FormFieldDefinition>{
        key: 'sumaris.testing.enable',
        label: 'CONFIGURATION.OPTIONS.TESTING',
        type: 'boolean'
    },
    VESSEL_DEFAULT_STATUS: <FormFieldDefinition>{
        key: 'sumaris.vessel.status.default',
        label: 'CONFIGURATION.OPTIONS.VESSEL.DEFAULT_NEW_VESSEL_STATUS',
        type: 'enum',
        values: [
            {
                key: StatusIds.ENABLE.toString(),
                value: 'REFERENTIAL.STATUS_ENUM.ENABLE'
            },
            {
                key: StatusIds.TEMPORARY.toString(),
                value: 'REFERENTIAL.STATUS_ENUM.TEMPORARY'
            }
        ]
    },
    LOGO_LARGE: <FormFieldDefinition>{
        key: 'sumaris.logo.large',
        label: 'CONFIGURATION.OPTIONS.HOME.LOGO_LARGE',
        type: 'string'
    },
    HOME_PARTNERS_DEPARTMENTS: <FormFieldDefinition>{
        key: 'sumaris.partner.departments',
        label: 'CONFIGURATION.OPTIONS.HOME.PARTNER_DEPARTMENTS',
        type: 'string'
    },
    HOME_BACKGROUND_IMAGE: <FormFieldDefinition>{
        key: 'sumaris.background.images',
        label: 'CONFIGURATION.OPTIONS.HOME.BACKGROUND_IMAGES',
        type: 'string'
    },
    COLOR_PRIMARY: <FormFieldDefinition>{
        key: 'sumaris.color.primary',
        label: 'CONFIGURATION.OPTIONS.COLORS.PRIMARY',
        type: 'color'
    },
    COLOR_SECONDARY: <FormFieldDefinition>{
        key: 'sumaris.color.secondary',
        label: 'CONFIGURATION.OPTIONS.COLORS.SECONDARY',
        type: 'color'
    },
    COLOR_TERTIARY: <FormFieldDefinition>{
        key: 'sumaris.color.tertiary',
        label: 'CONFIGURATION.OPTIONS.COLORS.TERTIARY',
        type: 'color'
    },
    COLOR_SUCCESS: <FormFieldDefinition>{
        key: 'sumaris.color.success',
        label: 'CONFIGURATION.OPTIONS.COLORS.SUCCESS',
        type: 'color'
    },
    COLOR_WARNING: <FormFieldDefinition>{
        key: 'sumaris.color.warning',
        label: 'CONFIGURATION.OPTIONS.COLORS.WARNING',
        type: 'color'
    },
    COLOR_ACCENT: <FormFieldDefinition>{
        key: 'sumaris.color.accent',
        label: 'CONFIGURATION.OPTIONS.COLORS.ACCENT',
        type: 'color'
    },
    COLOR_DANGER: <FormFieldDefinition>{
        key: 'sumaris.color.danger',
        label: 'CONFIGURATION.OPTIONS.COLORS.DANGER',
        type: 'color'
    },
    PROFILE_ADMIN_LABEL: <FormFieldDefinition>{
        key: 'sumaris.userProfile.ADMIN.label',
        label: 'CONFIGURATION.OPTIONS.PROFILE.ADMIN',
        type: 'string'
    },
    PROFILE_USER_LABEL: <FormFieldDefinition>{
        key: 'sumaris.userProfile.USER.label',
        label: 'CONFIGURATION.OPTIONS.PROFILE.USER',
        type: 'string'
    },
    PROFILE_SUPERVISOR_LABEL: <FormFieldDefinition>{
        key: 'sumaris.userProfile.SUPERVISOR.label',
        label: 'CONFIGURATION.OPTIONS.PROFILE.SUPERVISOR',
        type: 'string'
    },
    PROFILE_GUEST_LABEL: <FormFieldDefinition>{
        key: 'sumaris.userProfile.GUEST.label',
        label: 'CONFIGURATION.OPTIONS.PROFILE.GUEST',
        type: 'string'
    },
    ANDROID_INSTALL_URL: <FormFieldDefinition>{
        key: 'sumaris.android.install.url',
        label: 'CONFIGURATION.OPTIONS.ANDROID_INSTALL_URL',
        type: 'string'
    },
    LOCATION_LEVEL_COUNTRY_ID: <FormFieldDefinition>{
        key: 'sumaris.enumeration.LocationLevel.COUNTRY.id',
        label: 'CONFIGURATION.OPTIONS.ENUMERATION.LOCATION_LEVEL_COUNTRY_ID',
        type: 'entity',
        autocomplete: {
          filter: {
            entityName: 'LocationLevel',
            statusIds: [0,1]
          }
        },
        defaultValue: LocationLevelIds.COUNTRY
    },
    LOCATION_LEVEL_PORT_ID: <FormFieldDefinition>{
        key: 'sumaris.enumeration.LocationLevel.HARBOUR.id',
        label: 'CONFIGURATION.OPTIONS.ENUMERATION.LOCATION_LEVEL_PORT_ID',
        type: 'entity',
        autocomplete: {
          filter: {
            entityName: 'LocationLevel',
            statusIds: [0,1]
          }
        },
        defaultValue: LocationLevelIds.PORT
    },
    LOCATION_LEVEL_AUCTION_ID: <FormFieldDefinition>{
        key: 'sumaris.enumeration.locationLevel.AUCTION.id',
        label: 'CONFIGURATION.OPTIONS.ENUMERATION.LOCATION_LEVEL_AUCTION_ID',
        type: 'entity',
        autocomplete: {
          filter: {
            entityName: 'LocationLevel',
            statusIds: [0,1]
          }
        },
        defaultValue: LocationLevelIds.AUCTION
    },
    LOCATION_LEVEL_ICES_RECTANGLE_ID: <FormFieldDefinition>{
      key: 'sumaris.enumeration.locationLevel.RECTANGLE_ICES.id',
      label: 'CONFIGURATION.OPTIONS.ENUMERATION.LOCATION_LEVEL_ICES_RECTANGLE_ID',
      type: 'entity',
      autocomplete: {
        filter: {
          entityName: 'LocationLevel',
          statusIds: [0,1]
        }
      },
      defaultValue: LocationLevelIds.ICES_RECTANGLE
  }
});
