import {TypePolicies} from '@apollo/client/core';
import {FormFieldDefinition, PRIORITIZED_AUTHORITIES} from '@sumaris-net/ngx-components';

export const DATA_GRAPHQL_TYPE_POLICIES = <TypePolicies>{
  'DataReferenceVO': {
    keyFields: ['entityName', 'id']
  }
};

export const DATA_CONFIG_OPTIONS = Object.freeze({
  DATA_NOT_SELF_ACCESS_ROLE: <FormFieldDefinition>{
    key: "sumaris.auth.notSelfDataAccess.role",
    label: "CONFIGURATION.OPTIONS.NOT_SELF_DATA_ACCESS_MIN_ROLE",
    type: 'enum',
    values: PRIORITIZED_AUTHORITIES.map(key => ({
      key: 'ROLE_' + key,
      value: 'USER.PROFILE_ENUM.' + key
    }))
  },
  ENTITY_TRASH: <FormFieldDefinition> {
    key: 'sumaris.persistence.trash.enable',
    label: 'CONFIGURATION.OPTIONS.ENTITY_TRASH',
    type: 'boolean',
    defaultValue: true
  },
  DATA_QUALITY_PROCESS_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.data.quality.process.enable',
    label: 'CONFIGURATION.OPTIONS.DATA_QUALITY_PROCESS_ENABLE',
    type: 'boolean',
    defaultValue: true
  },
  DATA_SHOW_OBSERVERS_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.data.show.observer.enable',
    label: 'CONFIGURATION.OPTIONS.DATA_SHOW_OBSERVERS_ENABLE',
    type: 'boolean',
    defaultValue: true
  }
});
