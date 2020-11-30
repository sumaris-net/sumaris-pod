import {TypePolicies} from "@apollo/client";

export const MODEL_TYPES_POLICIES: TypePolicies = {
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
  'MeasurementVO': {
    keyFields: ['entityName', 'id']
  },
  'TaxonGroupStrategyVO': {
    keyFields: ['__typename', 'strategyId', 'taxonGroup', ['entityName', 'id']]
  },
  'TaxonNameStrategyVO': {
    keyFields: ['__typename', 'strategyId', 'taxonName', ['entityName', 'id']]
  }
};
