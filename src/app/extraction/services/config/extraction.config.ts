import {TypePolicies} from "@apollo/client/core";

export const EXTRACTION_GRAPHQL_TYPE_POLICIES = <TypePolicies>{
  'ExtractionTypeVO': {
    keyFields: ['category', 'label']
  }
};
