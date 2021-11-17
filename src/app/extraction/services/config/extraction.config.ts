import {TypePolicies} from "@apollo/client/core";
import {FormFieldDefinition, PRIORITIZED_AUTHORITIES} from '@sumaris-net/ngx-components';

export const EXTRACTION_GRAPHQL_TYPE_POLICIES = <TypePolicies>{
  'ExtractionTypeVO': {
    keyFields: ['category', 'label']
  }
};

/**
 * Define configuration options
 */
export const EXTRACTION_CONFIG_OPTIONS = Object.freeze({
  EXTRACTION_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.extraction.enabled',
    label: 'EXTRACTION.OPTIONS.ENABLE',
    type: 'boolean',
    defaultValue: 'false'
  },
  EXTRACTION_MAP_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.extraction.map.enable',
    label: 'EXTRACTION.OPTIONS.MAP_ENABLE',
    type: 'boolean',
    defaultValue: 'false'
  },
  EXTRACTION_PRODUCT_ENABLE: <FormFieldDefinition>{
    key: 'sumaris.extraction.product.enable',
    label: 'EXTRACTION.OPTIONS.PRODUCT_ENABLE',
    type: 'boolean',
    defaultValue: 'false'
  },
  EXTRACTION_ACCESS_NOT_SELF_ROLE: <FormFieldDefinition>{
    key: "sumaris.extraction.accessNotSelfExtraction.role",
    label: "EXTRACTION.OPTIONS.ACCESS_NOT_SELF_ROLE",
    type: 'enum',
    values: PRIORITIZED_AUTHORITIES.map(label => ({
      key: 'ROLE_' + label,
      value: 'USER.PROFILE_ENUM.' + label
    }))
  }
});
