import {TypePolicies} from "@apollo/client/core";
import {FormFieldDefinition, FormFieldDefinitionMap} from "../../../shared/form/field.model";
import {changeCaseToUnderscore} from "../../../shared/functions";

export const REFERENTIAL_GRAPHQL_TYPE_POLICIES = <TypePolicies>{
  'MetierVO': {
    keyFields: ['entityName', 'id']
  },
  'PmfmVO': {
    keyFields: ['entityName', 'id']
  },
  'TaxonGroupVO': {
    keyFields: ['entityName', 'id']
  },
  'TaxonNameVO': {
    keyFields: ['entityName', 'id']
  },
  'LocationVO': {
    keyFields: ['entityName', 'id']
  },
  'ReferentialVO': {
    keyFields: ['entityName', 'id']
  },
  'TaxonGroupStrategyVO': {
    keyFields: ['__typename', 'strategyId', 'taxonGroup', ['entityName', 'id']]
  },
  'TaxonNameStrategyVO': {
    keyFields: ['__typename', 'strategyId', 'taxonName', ['entityName', 'id']]
  }
};

export const REFERENTIAL_CONFIG_OPTIONS: FormFieldDefinitionMap = {
  TESTING: <FormFieldDefinition>{
    key: 'sumaris.referential.vessel.enable',
    label: 'REFERENTIAL.OPTIONS.VESSELS_ENABLE',
    type: 'boolean',
    defaultValue: 'false'
  }
};

export const REFERENTIAL_LOCAL_SETTINGS_OPTIONS: FormFieldDefinitionMap = {

  // Display attributes for vessel
  FIELD_VESSEL_SNAPSHOT_ATTRIBUTES: <FormFieldDefinition>{
    key: 'sumaris.field.vesselSnapshot.attributes',
    label: 'SETTINGS.FIELDS.VESSEL.NAME',
    type: 'enum',
    values: [
      {key: 'exteriorMarking,name',   value: 'SETTINGS.FIELDS.VESSEL.ATTRIBUTES.EXTERIOR_MARKING_NAME'},
      {key: 'registrationCode,name',   value: 'SETTINGS.FIELDS.VESSEL.ATTRIBUTES.REGISTRATION_CODE_NAME'}
    ]
  },
  // Display attributes for referential useful entities
  ... ['department', 'location', 'qualitativeValue', 'taxonGroup', 'taxonName', 'gear']
    // Allow user to choose how to display field (by code+label, code, etc)
    .reduce((res, fieldName) => {
      res[`FIELD_${changeCaseToUnderscore(fieldName).toUpperCase()}_ATTRIBUTES`] = {
        key: `sumaris.field.${fieldName}.attributes`,
        label: `SETTINGS.FIELDS.${changeCaseToUnderscore(fieldName).toUpperCase()}`,
        type: 'enum',
        values: [
          {key: 'label,name',   value: 'SETTINGS.FIELDS.ATTRIBUTES.LABEL_NAME'},
          {key: 'name',         value: 'SETTINGS.FIELDS.ATTRIBUTES.NAME'},
          {key: 'name,label',   value: 'SETTINGS.FIELDS.ATTRIBUTES.NAME_LABEL'},
          {key: 'label',        value: 'SETTINGS.FIELDS.ATTRIBUTES.LABEL'}
        ]
      };
      return res;
    }, {})
};
