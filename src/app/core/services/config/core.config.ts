import {FormFieldDefinition} from '@sumaris-net/ngx-components';

export const APP_CORE_CONFIG_OPTIONS = Object.freeze({

    UPDATE_TECHNICAL_TABLES: <FormFieldDefinition> {
      key: 'sumaris.persistence.technicalTables.update',
      label: 'CONFIGURATION.OPTIONS.UPDATE_TECHNICAL_TABLES',
      type: 'boolean',
      defaultValue: false
    },
    PROFILE_ADMIN_LABEL: <FormFieldDefinition>{
        key: 'sumaris.enumeration.UserProfile.ADMIN.label',
        label: 'CONFIGURATION.OPTIONS.PROFILE.ADMIN',
        type: 'string',
        defaultValue: 'ADMIN'
    },
    PROFILE_USER_LABEL: <FormFieldDefinition>{
        key: 'sumaris.enumeration.UserProfile.USER.label',
        label: 'CONFIGURATION.OPTIONS.PROFILE.USER',
        type: 'string',
        defaultValue: 'USER'
    },
    PROFILE_SUPERVISOR_LABEL: <FormFieldDefinition>{
        key: 'sumaris.enumeration.UserProfile.SUPERVISOR.label',
        label: 'CONFIGURATION.OPTIONS.PROFILE.SUPERVISOR',
        type: 'string',
        defaultValue: 'SUPERVISOR'
    },
    PROFILE_GUEST_LABEL: <FormFieldDefinition>{
        key: 'sumaris.enumeration.UserProfile.GUEST.label',
        label: 'CONFIGURATION.OPTIONS.PROFILE.GUEST',
        type: 'string',
        defaultValue: 'GUEST'
    }
});
