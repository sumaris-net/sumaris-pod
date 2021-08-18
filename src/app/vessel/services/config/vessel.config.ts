import { TypePolicies } from '@apollo/client/core';
import { FormFieldDefinition, FormFieldDefinitionMap, StatusIds } from '@sumaris-net/ngx-components';

export const VESSEL_FEATURE_NAME = 'vessel';

export const VESSEL_GRAPHQL_TYPE_POLICIES = <TypePolicies>{};

export const VESSEL_CONFIG_OPTIONS: FormFieldDefinitionMap = {
  VESSEL_DEFAULT_STATUS: <FormFieldDefinition>{
    key: 'sumaris.vessel.status.default',
    label: 'CONFIGURATION.OPTIONS.VESSEL.DEFAULT_NEW_VESSEL_STATUS',
    type: 'enum',
    values: [
      {
        key: StatusIds.ENABLE.toString(),
        value: 'REFERENTIAL.STATUS_ENUM.ENABLE',
      },
      {
        key: StatusIds.TEMPORARY.toString(),
        value: 'REFERENTIAL.STATUS_ENUM.TEMPORARY',
      },
    ],
  },
  REFERENTIAL_VESSEL_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.referential.vessel.enable',
    label: 'REFERENTIAL.OPTIONS.VESSELS_ENABLE',
    type: 'boolean',
    defaultValue: 'false',
  },
};

export const VESSEL_LOCAL_SETTINGS_OPTIONS: FormFieldDefinitionMap = {};
